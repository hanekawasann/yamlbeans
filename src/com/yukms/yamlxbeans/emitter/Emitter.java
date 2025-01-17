/*
 * Copyright (c) 2008 Nathan Sweet, Copyright (c) 2006 Ola Bini
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.yukms.yamlxbeans.emitter;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static com.yukms.yamlxbeans.parser.EventType.ALIAS;
import static com.yukms.yamlxbeans.parser.EventType.DOCUMENT_END;
import static com.yukms.yamlxbeans.parser.EventType.DOCUMENT_START;
import static com.yukms.yamlxbeans.parser.EventType.MAPPING_END;
import static com.yukms.yamlxbeans.parser.EventType.MAPPING_START;
import static com.yukms.yamlxbeans.parser.EventType.SCALAR;
import static com.yukms.yamlxbeans.parser.EventType.SEQUENCE_END;
import static com.yukms.yamlxbeans.parser.EventType.SEQUENCE_START;
import static com.yukms.yamlxbeans.parser.EventType.STREAM_END;
import static com.yukms.yamlxbeans.parser.EventType.STREAM_START;
import com.yukms.yamlxbeans.parser.CollectionStartEvent;
import com.yukms.yamlxbeans.parser.DocumentEndEvent;
import com.yukms.yamlxbeans.parser.DocumentStartEvent;
import com.yukms.yamlxbeans.parser.Event;
import com.yukms.yamlxbeans.parser.MappingStartEvent;
import com.yukms.yamlxbeans.parser.NodeEvent;
import com.yukms.yamlxbeans.parser.Parser;
import com.yukms.yamlxbeans.parser.ScalarEvent;
import com.yukms.yamlxbeans.parser.SequenceStartEvent;
import org.apache.commons.lang3.StringUtils;

/**
 * Converts events into YAML output.
 *
 * @author <a href="mailto:misc@n4te.com">Nathan Sweet</a>
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Emitter {
    static private final Pattern HANDLE_FORMAT = Pattern.compile("^![-\\w]*!$");
    static private final Pattern ANCHOR_FORMAT = Pattern.compile("^[-\\w]*$");

    private final EmitterConfigx config;
    private final EmitterWriter writer;
    private final EmitterState[] table = new EmitterState[18];
    private int state = S_STREAM_START;
    private final List<Integer> states = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final List<Integer> indents = new ArrayList<>();
    private boolean isVersion10 = false;
    private Event event;
    private int flowLevel = 0;
    private int indent = -1;
    private boolean mappingContext = false;
    private boolean simpleKeyContext = false;
    private Map<String, String> tagPrefixes;
    private String preparedTag;
    private String preparedAnchor;
    private ScalarAnalysis analysis;
    private char style = 0;

    private Emitter(Writer writer) {
        this(writer, new EmitterConfigx());
    }

    public Emitter(Writer writer, EmitterConfigx config) {
        this.config = config;
        if (writer == null) { throw new IllegalArgumentException("stream cannot be null."); }
        if (!(writer instanceof BufferedWriter)) { writer = new BufferedWriter(writer); }
        this.writer = new EmitterWriter(writer);
        initStateTable();
    }

    public void emit(Event event) throws IOException, EmitterException {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null.");
        }
        events.add(event);
        while (!needMoreEvents()) {
            this.event = events.remove(0);
            table[state].expect();
            this.event = null;
        }
    }

    public void close() throws IOException {
        writer.close();
    }

    private boolean needMoreEvents() {
        if (events.isEmpty()) {
            return true;
        }
        event = events.get(0);
        if (event == null) {
            return false;
        }
        switch (event.type) {
            case DOCUMENT_START:
                return needEvents(1);
            case SEQUENCE_START:
                return needEvents(2);
            case MAPPING_START:
                return needEvents(3);
            default:
                return false;
        }
    }

    private boolean needEvents(int count) {
        int level = 0;
        Iterator<Event> iter = events.iterator();
        iter.next();
        while (iter.hasNext()) {
            Event curr = iter.next();
            if (curr.type == DOCUMENT_START || curr.type == MAPPING_START || curr.type == SEQUENCE_START) {
                level++;
            } else if (curr.type == DOCUMENT_END || curr.type == MAPPING_END || curr.type == SEQUENCE_END) {
                level--;
            } else if (curr.type == STREAM_END) { level = -1; }
            if (level < 0) { return false; }
        }
        return events.size() < count + 1;
    }

    private void initStateTable() {
        table[S_STREAM_START] = () -> {
            if (event.type == STREAM_START) {
                writer.writeStreamStart();
                state = S_FIRST_DOCUMENT_START;
            } else {
                throw new EmitterException("Expected 'stream start' but found: " + event);
            }
        };
        table[S_FIRST_DOCUMENT_START] = () -> {
            if (event.type == DOCUMENT_START) {
                DocumentStartEvent documentStartEvent = (DocumentStartEvent) event;
                if (documentStartEvent.version != null) {
                    if (documentStartEvent.version.major != 1) {
                        throw new EmitterException("Unsupported YAML version: " + documentStartEvent.version);
                    }
                    writer.writeVersionDirective(documentStartEvent.version.toString());
                }
                if ((documentStartEvent.version != null && documentStartEvent.version.equals(1, 0))//
                    || config.version.equals(1, 0)) {
                    isVersion10 = true;
                    tagPrefixes = new HashMap<>(DEFAULT_TAG_PREFIXES_1_0);
                } else {
                    tagPrefixes = new HashMap<>(DEFAULT_TAG_PREFIXES_1_1);
                }

                if (documentStartEvent.tags != null) {
                    Set<String> handles = new TreeSet<>(documentStartEvent.tags.keySet());
                    for (String handle : handles) {
                        String prefix = documentStartEvent.tags.get(handle);
                        tagPrefixes.put(prefix, handle);
                        String handleText = prepareTagHandle(handle);
                        String prefixText = prepareTagPrefix(prefix);
                        writer.writeTagDirective(handleText, prefixText);
                    }
                }
            }
            expectDocumentStart(true);
        };
        table[S_DOCUMENT_ROOT] = () -> {
            states.add(0, S_DOCUMENT_END);
            expectNode(true, false, false, false);
        };
        table[S_NOTHING] = () -> {
            throw new EmitterException("Expected no event but found: " + event);
        };
        table[S_DOCUMENT_START] = () -> expectDocumentStart(false);
        table[S_DOCUMENT_END] = () -> {
            if (event.type == DOCUMENT_END) {
                writer.writeIndent(indent);
                if (((DocumentEndEvent) event).isExplicit) {
                    writer.writeIndicator("...", true, false, false);
                    writer.writeIndent(indent);
                }
                writer.flushStream();
                state = S_DOCUMENT_START;
            } else { throw new EmitterException("Expected 'document end' but found: " + event); }
        };
        table[S_FIRST_FLOW_SEQUENCE_ITEM] = () -> {
            if (event.type == SEQUENCE_END) {
                indent = indents.remove(0);
                flowLevel--;
                writer.writeIndicator("]", false, false, false);
                state = states.remove(0);
            } else {
                if (config.canonical || writer.column > config.wrapColumn) { writer.writeIndent(indent); }
                states.add(0, S_FLOW_SEQUENCE_ITEM);
                expectNode(false, true, false, false);
            }
        };
        table[S_FLOW_SEQUENCE_ITEM] = () -> {
            if (event.type == SEQUENCE_END) {
                indent = indents.remove(0);
                flowLevel--;
                if (config.canonical) {
                    // 在结束时不应该放逗号
                    //writer.writeIndicator(",", false, false, false);
                    writer.writeIndent(indent);
                }
                writer.writeIndicator("]", false, false, false);
                state = states.remove(0);
            } else {
                writer.writeIndicator(",", false, false, false);
                if (config.canonical || writer.column > config.wrapColumn) { writer.writeIndent(indent); }
                states.add(0, S_FLOW_SEQUENCE_ITEM);
                expectNode(false, true, false, false);
            }
        };
        table[S_FIRST_FLOW_MAPPING_KEY] = () -> {
            if (event.type == MAPPING_END) {
                indent = indents.remove(0);
                flowLevel--;
                writer.writeIndicator("}", false, false, false);
                state = states.remove(0);
            } else {
                if (config.canonical || writer.column > config.wrapColumn) { writer.writeIndent(indent); }
                if (!config.canonical && checkSimpleKey()) {
                    states.add(0, S_FLOW_MAPPING_SIMPLE_VALUE);
                    expectNode(false, false, true, true);
                } else {
                    writer.writeIndicator("?", true, false, false);
                    states.add(0, S_FLOW_MAPPING_VALUE);
                    expectNode(false, false, true, false);
                }
            }
        };
        table[S_FLOW_MAPPING_KEY] = () -> {
            if (event.type == MAPPING_END) {
                indent = indents.remove(0);
                flowLevel--;
                if (config.canonical) {
                    writer.writeIndicator(",", false, false, false);
                    writer.writeIndent(indent);
                }
                writer.writeIndicator("}", false, false, false);
                state = states.remove(0);
            } else {
                writer.writeIndicator(",", false, false, false);
                if (config.canonical || writer.column > config.wrapColumn) { writer.writeIndent(indent); }
                if (!config.canonical && checkSimpleKey()) {
                    states.add(0, S_FLOW_MAPPING_SIMPLE_VALUE);
                    expectNode(false, false, true, true);
                } else {
                    writer.writeIndicator("?", true, false, false);
                    states.add(0, S_FLOW_MAPPING_VALUE);
                    expectNode(false, false, true, false);
                }
            }
        };
        table[S_FLOW_MAPPING_SIMPLE_VALUE] = () -> {
            writer.writeIndicator(": ", false, true, false);
            states.add(0, S_FLOW_MAPPING_KEY);
            expectNode(false, false, true, false);
        };
        table[S_FLOW_MAPPING_VALUE] = () -> {
            if (config.canonical || writer.column > config.wrapColumn) { writer.writeIndent(indent); }
            writer.writeIndicator(": ", false, true, false);
            states.add(0, S_FLOW_MAPPING_KEY);
            expectNode(false, false, true, false);
        };
        table[S_BLOCK_SEQUENCE_ITEM] = () -> expectBlockSequenceItem(false);
        table[S_FIRST_BLOCK_MAPPING_KEY] = () -> expectBlockMappingKey(true);
        table[S_BLOCK_MAPPING_SIMPLE_VALUE] = () -> {
            writer.writeIndicator(": ", false, true, false);
            states.add(0, S_BLOCK_MAPPING_KEY);
            expectNode(false, false, true, false);
        };
        table[S_BLOCK_MAPPING_VALUE] = () -> {
            writer.writeIndent(indent);
            writer.writeIndicator(": ", true, true, true);
            states.add(0, S_BLOCK_MAPPING_KEY);
            expectNode(false, false, true, false);
        };
        table[S_BLOCK_MAPPING_KEY] = () -> expectBlockMappingKey(false);
        table[S_FIRST_BLOCK_SEQUENCE_ITEM] = () -> expectBlockSequenceItem(true);
    }

    private void increaseIndent(boolean flow, boolean indentless) {
        indents.add(0, indent);
        if (indent == -1) {
            if (flow) {
                indent = config.indentSize;
            } else {
                indent = 0;
            }
        } else if (!indentless) {
            indent += config.indentSize;
        }
    }

    private void expectDocumentStart(boolean first) throws IOException {
        if (event.type == DOCUMENT_START) {
            DocumentStartEvent ev = (DocumentStartEvent) event;
            boolean implicit = first//
                && !ev.isExplicit //
                && !config.canonical//
                && ev.version == null //
                && ev.tags == null //
                && !checkEmptyDocument();
            if (!implicit) {
                writer.writeIndent(indent);
                writer.writeIndicator("--- ", true, true, false);
                if (config.canonical) {
                    writer.writeIndent(indent);
                }
            }
            state = S_DOCUMENT_ROOT;
        } else if (event.type == STREAM_END) {
            writer.writeStreamEnd();
            state = S_NOTHING;
        } else {
            throw new EmitterException("Expected 'document start' but found: " + event);
        }
    }

    private void expectBlockSequenceItem(boolean first) throws IOException {
        if (!first && event.type == SEQUENCE_END) {
            indent = indents.remove(0);
            state = states.remove(0);
        } else {
            writer.writeIndent(indent);
            writer.writeIndicator("-", true, false, true);
            states.add(0, S_BLOCK_SEQUENCE_ITEM);
            expectNode(false, true, false, false);
        }
    }

    private void expectBlockMappingKey(boolean first) throws IOException {
        if (!first && event.type == MAPPING_END) {
            indent = indents.remove(0);
            state = states.remove(0);
        } else {
            writer.writeIndent(indent);
            if (checkSimpleKey()) {
                states.add(0, S_BLOCK_MAPPING_SIMPLE_VALUE);
                expectNode(false, false, true, true);
            } else {
                writer.writeIndicator("?", true, false, true);
                states.add(0, S_BLOCK_MAPPING_VALUE);
                expectNode(false, false, true, false);
            }
        }
    }

    private void expectNode(boolean root, boolean sequence, boolean mapping, boolean simpleKey) throws IOException {
        mappingContext = mapping;
        simpleKeyContext = simpleKey;
        if (event.type == ALIAS) {
            expectAlias();
        } else if (event.type == SCALAR || event.type == MAPPING_START || event.type == SEQUENCE_START) {
            processAnchor("&");
            processTag();
            if (event.type == SCALAR) {
                expectScalar();
            } else if (event.type == SEQUENCE_START) {
                if (flowLevel != 0 || config.canonical || ((SequenceStartEvent) event).isFlowStyle//
                    || checkEmptySequence()) {
                    expectFlowSequence();
                } else {
                    expectBlockSequence();
                }
            } else if (event.type == MAPPING_START) {
                if (flowLevel != 0 || config.canonical || ((MappingStartEvent) event).isFlowStyle//
                    || checkEmptyMapping()) {
                    expectFlowMapping();
                } else {
                    expectBlockMapping();
                }
            }
        } else {
            throw new EmitterException("Expected 'scalar', 'mapping start', or 'sequence start' but found: " + event);
        }
    }

    private void expectAlias() throws IOException {
        if (((NodeEvent) event).anchor == null) {
            throw new EmitterException("Anchor is not specified for alias.");
        }
        processAnchor("*");
        state = states.remove(0);
    }

    private void expectScalar() throws IOException {
        increaseIndent(true, false);
        processScalar();
        indent = indents.remove(0);
        state = states.remove(0);
    }

    private void expectFlowSequence() throws IOException {
        writer.writeIndicator("[", true, true, false);
        flowLevel++;
        increaseIndent(true, false);
        state = S_FIRST_FLOW_SEQUENCE_ITEM;
    }

    private void expectBlockSequence() {
        increaseIndent(false, mappingContext && !writer.indentation);
        state = S_FIRST_BLOCK_SEQUENCE_ITEM;
    }

    private void expectFlowMapping() throws IOException {
        writer.writeIndicator("{", true, true, false);
        flowLevel++;
        increaseIndent(true, false);
        state = S_FIRST_FLOW_MAPPING_KEY;
    }

    private void expectBlockMapping() {
        increaseIndent(false, false);
        state = S_FIRST_BLOCK_MAPPING_KEY;
    }

    private boolean checkEmptySequence() {
        return event.type == SEQUENCE_START && !events.isEmpty() && events.get(0).type == SEQUENCE_END;
    }

    private boolean checkEmptyMapping() {
        return event.type == MAPPING_START && !events.isEmpty() && events.get(0).type == MAPPING_END;
    }

    private boolean checkEmptyDocument() {
        if (event.type != DOCUMENT_START || events.isEmpty()) {
            return false;
        }
        Event ev = events.get(0);
        return ev.type == SCALAR//
            && ((ScalarEvent) ev).anchor == null//
            && ((ScalarEvent) ev).tag == null//
            && ((ScalarEvent) ev).implicit != null//
            // 修复保存空对象报错
            //&& ((ScalarEvent) ev).value.equals("");
            && StringUtils.isNotBlank(((ScalarEvent) ev).value);
    }

    private boolean checkSimpleKey() {
        int length = 0;
        if (event instanceof NodeEvent && ((NodeEvent) event).anchor != null) {
            if (preparedAnchor == null) { preparedAnchor = prepareAnchor(((NodeEvent) event).anchor); }
            length += preparedAnchor.length();
        }
        String tag = null;
        if (event.type == SCALAR) { tag = ((ScalarEvent) event).tag; } else if (event.type == MAPPING_START ||
            event.type == SEQUENCE_START) {
            tag = ((CollectionStartEvent) event).tag;
        }
        if (tag != null) {
            if (preparedTag == null) { preparedTag = prepareTag(tag); }
            length += preparedTag.length();
        }
        if (event.type == SCALAR && analysis == null) {
            analysis = ScalarAnalysis.analyze(((ScalarEvent) event).value, config.escapeUnicode);
            length += analysis.scalar.length();
        }

        return length < 1024 &&
            (event.type == ALIAS || event.type == SCALAR && !analysis.empty && !analysis.multiline ||
                checkEmptySequence() || checkEmptyMapping());
    }

    private void processTag() throws IOException {
        String tag;
        if (event.type == SCALAR) {
            ScalarEvent ev = (ScalarEvent) event;
            tag = ev.tag;
            if (style == 0) {
                style = chooseScalarStyle();
            }
            if ((!config.canonical || tag == null)//
                && ((0 == style && ev.implicit[0])//
                || (0 != style && ev.implicit[1]))) {
                preparedTag = null;
                return;
            }
            if (ev.implicit[0] && tag == null) {
                tag = "!";
                preparedTag = null;
            }
        } else {
            CollectionStartEvent ev = (CollectionStartEvent) event;
            tag = ev.tag;
            if ((!config.canonical || tag == null) && ev.isImplicit) {
                preparedTag = null;
                return;
            }
        }
        if (tag == null) {
            throw new EmitterException("Tag is not specified.");
        }
        if (preparedTag == null) {
            preparedTag = prepareTag(tag);
        }
        if (preparedTag != null && !"".equals(preparedTag)) {
            writer.writeIndicator(preparedTag, true, false, false);
        }
        preparedTag = null;
    }

    private void processAnchor(String indicator) throws IOException {
        NodeEvent ev = (NodeEvent) event;
        if (ev.anchor == null) {
            preparedAnchor = null;
            return;
        }
        if (preparedAnchor == null) {
            preparedAnchor = prepareAnchor(ev.anchor);
        }
        if (preparedAnchor != null && !"".equals(preparedAnchor)) {
            writer.writeIndicator(indicator + preparedAnchor, true, false, false);
        }
        preparedAnchor = null;
    }

    private char chooseScalarStyle() {
        ScalarEvent ev = (ScalarEvent) event;
        if (analysis == null) {
            analysis = ScalarAnalysis.analyze(ev.value, config.escapeUnicode);
        }
        if (ev.style == '"' || config.canonical) {
            return '"';
        }
        if (ev.style == 0//
            && !(simpleKeyContext && (analysis.empty || analysis.multiline))//
            && (flowLevel != 0 && analysis.allowFlowPlain || flowLevel == 0 && analysis.allowBlockPlain)) {
            return 0;
        }
        if ((ev.style == '|' || ev.style == '>') && flowLevel == 0 && analysis.allowBlock) {
            return '\'';
        }
        if ((ev.style == 0 || ev.style == '\'') && analysis.allowSingleQuoted &&
            !(simpleKeyContext && analysis.multiline)) {
            return '\'';
        }
        if (ev.style == 0 && analysis.multiline && flowLevel == 0 && analysis.allowBlock) {
            return '|';
        }
        return '"';
    }

    private void processScalar() throws IOException {
        ScalarEvent ev = (ScalarEvent) event;
        if (analysis == null) {
            analysis = ScalarAnalysis.analyze(ev.value, config.escapeUnicode);
        }
        if (style == 0) {
            style = chooseScalarStyle();
        }
        boolean split = !simpleKeyContext;
        String scalar = analysis.scalar;
        if (scalar != null) {
            if (style == '"') {
                writer.writeDoubleQuoted(scalar, split, indent, config.wrapColumn, config.escapeUnicode);
            } else if (style == '\'') {
                writer.writeSingleQuoted(scalar, split, indent, config.wrapColumn);
            } else if (style == '>') {
                writer.writeFolded(scalar, indent, config.wrapColumn);
            } else if (style == '|') {
                writer.writeLiteral(scalar, indent);
            } else {
                writer.writePlain(scalar, split, indent, config.wrapColumn);
            }
        }
        analysis = null;
        style = 0;
    }

    private String prepareTag(String tag) {
        if (tag == null || "".equals(tag)) {
            throw new EmitterException("Tag cannot be empty.");
        }
        if (tag.equals("!")) {
            return tag;
        }
        String handle = null;
        String suffix = tag;
        for (String prefix : tagPrefixes.keySet()) {
            if (tag.startsWith(prefix) && (prefix.equals("!") || prefix.length() < tag.length())) {
                handle = tagPrefixes.get(prefix);
                suffix = tag.substring(prefix.length());
            }
        }
        StringBuilder chunks = new StringBuilder();
        int start = 0, ending = 0;
        while (ending < suffix.length()) {
            ending++;
        }
        if (start < ending) {
            chunks.append(suffix, start, ending);
        }
        String suffixText = chunks.toString();
        if (tag.charAt(0) == '!' && isVersion10) {
            return tag;
        }
        if (handle != null) {
            return handle + suffixText;
        }
        if (config.useVerbatimTags) {
            return "!<" + suffixText + ">";
        } else {
            return "!" + suffixText;
        }
    }

    private String prepareTagHandle(String handle) {
        if (handle == null || "".equals(handle)) {
            throw new EmitterException("Tag handle cannot be empty.");
        } else if (handle.charAt(0) != '!' || handle.charAt(handle.length() - 1) != '!') {
            throw new EmitterException("Tag handle must begin and end with '!': " + handle);
        } else if (!"!".equals(handle) && !HANDLE_FORMAT.matcher(handle).matches()) {
            throw new EmitterException("Invalid syntax for tag handle: " + handle);
        }
        return handle;
    }

    private String prepareTagPrefix(String prefix) {
        if (prefix == null || "".equals(prefix)) { throw new EmitterException("Tag prefix cannot be empty."); }
        StringBuilder chunks = new StringBuilder();
        int start = 0, ending = 0;
        if (prefix.charAt(0) == '!') { ending = 1; }
        while (ending < prefix.length()) { ending++; }
        if (start < ending) { chunks.append(prefix.substring(start, ending)); }
        return chunks.toString();
    }

    private String prepareAnchor(String anchor) {
        if (anchor == null || "".equals(anchor)) { throw new EmitterException("Anchor cannot be empty."); }
        if (!ANCHOR_FORMAT.matcher(anchor).matches()) {
            throw new EmitterException("Invalid syntax for anchor: " + anchor);
        }
        return anchor;
    }

    private interface EmitterState {
        public void expect() throws IOException;
    }

    static private final int S_STREAM_START = 0;
    static private final int S_FIRST_DOCUMENT_START = 1;
    static private final int S_DOCUMENT_ROOT = 2;
    static private final int S_NOTHING = 3;
    static private final int S_DOCUMENT_START = 4;
    static private final int S_DOCUMENT_END = 5;
    static private final int S_FIRST_FLOW_SEQUENCE_ITEM = 6;
    static private final int S_FLOW_SEQUENCE_ITEM = 7;
    static private final int S_FIRST_FLOW_MAPPING_KEY = 8;
    static private final int S_FLOW_MAPPING_SIMPLE_VALUE = 9;
    static private final int S_FLOW_MAPPING_VALUE = 10;
    static private final int S_FLOW_MAPPING_KEY = 11;
    static private final int S_BLOCK_SEQUENCE_ITEM = 12;
    static private final int S_FIRST_BLOCK_MAPPING_KEY = 13;
    static private final int S_BLOCK_MAPPING_SIMPLE_VALUE = 14;
    static private final int S_BLOCK_MAPPING_VALUE = 15;
    static private final int S_BLOCK_MAPPING_KEY = 16;
    static private final int S_FIRST_BLOCK_SEQUENCE_ITEM = 17;

    private static final Map<String, String> DEFAULT_TAG_PREFIXES_1_0 = new HashMap<>();
    private static final Map<String, String> DEFAULT_TAG_PREFIXES_1_1 = new HashMap<>();

    static {
        DEFAULT_TAG_PREFIXES_1_0.put("tag:yaml.org,2002:", "!");
        DEFAULT_TAG_PREFIXES_1_1.put("!", "!");
        DEFAULT_TAG_PREFIXES_1_1.put("tag:yaml.org,2002:", "!!");
    }

    public static void main(String[] args) throws IOException {
        Parser parser = new Parser(new FileReader("test/test.yml"));
        Emitter emitter = new Emitter(new OutputStreamWriter(System.out));
        while (true) {
            Event event = parser.getNextEvent();
            if (event == null) { break; }
            emitter.emit(event);
        }
    }
}
