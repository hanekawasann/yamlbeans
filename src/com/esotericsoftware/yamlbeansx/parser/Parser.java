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

package com.esotericsoftware.yamlbeansx.parser;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.ALIAS;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.ANCHOR;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.BLOCK_END;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.BLOCK_ENTRY;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.BLOCK_MAPPING_START;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.BLOCK_SEQUENCE_START;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.DIRECTIVE;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.DOCUMENT_END;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.DOCUMENT_START;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.FLOW_ENTRY;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.FLOW_MAPPING_END;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.FLOW_MAPPING_START;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.FLOW_SEQUENCE_END;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.FLOW_SEQUENCE_START;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.KEY;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.SCALAR;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.STREAM_END;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.TAG;
import static com.esotericsoftware.yamlbeansx.tokenizer.TokenType.VALUE;
import com.esotericsoftware.yamlbeansx.Versionx;
import com.esotericsoftware.yamlbeansx.tokenizer.AliasToken;
import com.esotericsoftware.yamlbeansx.tokenizer.AnchorToken;
import com.esotericsoftware.yamlbeansx.tokenizer.DirectiveToken;
import com.esotericsoftware.yamlbeansx.tokenizer.ScalarToken;
import com.esotericsoftware.yamlbeansx.tokenizer.TagToken;
import com.esotericsoftware.yamlbeansx.tokenizer.Token;
import com.esotericsoftware.yamlbeansx.tokenizer.TokenType;
import com.esotericsoftware.yamlbeansx.tokenizer.Tokenizer;
import com.esotericsoftware.yamlbeansx.tokenizer.Tokenizer.TokenizerException;

/**
 * Parses a stream of tokens into events.
 *
 * @author <a href="mailto:misc@n4te.com">Nathan Sweet</a>
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Parser {
    private Tokenizer tokenizer;
    private List<Production> parseStack;
    private final List<String> tags = new LinkedList<>();
    private final List<String> anchors = new LinkedList<>();
    private Map<String, String> tagHandles = new HashMap<>();
    private Versionx defaultVersion;
    private Versionx documentVersion;
    private final Production[] table = new Production[46];
    private Event peekedEvent;

    public Parser(Reader reader) {
        this(reader, new Versionx(1, 1));
    }

    public Parser(Reader reader, Versionx defaultVersion) {
        if (reader == null) {
            throw new IllegalArgumentException("reader cannot be null.");
        }
        if (defaultVersion == null) {
            throw new IllegalArgumentException("defaultVersion cannot be null.");
        }
        tokenizer = new Tokenizer(reader);
        this.defaultVersion = defaultVersion;
        initProductionTable();

        parseStack = new LinkedList<>();
        parseStack.add(0, table[P_STREAM]);


    }

    public Event getNextEvent() throws ParserException, TokenizerException {
        if (peekedEvent != null) {
            try {
                return peekedEvent;
            } finally {
                peekedEvent = null;
            }
        }
        while (!parseStack.isEmpty()) {
            Event event = parseStack.remove(0).produce();
            if (event != null) {
                return event;
            }
        }
        return null;
    }

    public Event peekNextEvent() throws ParserException, TokenizerException {
        if (peekedEvent != null) {
            return peekedEvent;
        }
        peekedEvent = getNextEvent();
        return peekedEvent;
    }

    public int getLineNumber() {
        return tokenizer.getLineNumber();
    }

    public int getColumn() {
        return tokenizer.getColumn();
    }

    public void close() throws IOException {
        tokenizer.close();
    }

    private void initProductionTable() {
        table[P_STREAM] = () -> {
            parseStack.add(0, table[P_STREAM_END]);
            parseStack.add(0, table[P_EXPLICIT_DOCUMENT]);
            parseStack.add(0, table[P_IMPLICIT_DOCUMENT]);
            parseStack.add(0, table[P_STREAM_START]);
            return null;
        };
        table[P_STREAM_START] = () -> {
            tokenizer.getNextToken();
            return Event.STREAM_START;
        };
        table[P_STREAM_END] = () -> {
            tokenizer.getNextToken();
            return Event.STREAM_END;
        };
        table[P_IMPLICIT_DOCUMENT] = () -> {
            TokenType type = tokenizer.peekNextTokenType();
            if (!(type == DIRECTIVE || type == DOCUMENT_START || type == STREAM_END)) {
                parseStack.add(0, table[P_DOCUMENT_END]);
                parseStack.add(0, table[P_BLOCK_NODE]);
                parseStack.add(0, table[P_DOCUMENT_START_IMPLICIT]);
            }
            return null;
        };
        table[P_EXPLICIT_DOCUMENT] = () -> {
            if (tokenizer.peekNextTokenType() != STREAM_END) {
                parseStack.add(0, table[P_EXPLICIT_DOCUMENT]);
                parseStack.add(0, table[P_DOCUMENT_END]);
                parseStack.add(0, table[P_BLOCK_NODE]);
                parseStack.add(0, table[P_DOCUMENT_START]);
            }
            return null;
        };
        table[P_DOCUMENT_START] = () -> {
            Token token = tokenizer.peekNextToken();
            DocumentStartEvent documentStartEvent = processDirectives(true);
            if (tokenizer.peekNextTokenType() != DOCUMENT_START) {
                throw new ParserException("Expected 'document start' but found: " + token.type);
            }
            tokenizer.getNextToken();
            return documentStartEvent;
        };
        table[P_DOCUMENT_START_IMPLICIT] = () -> processDirectives(false);
        table[P_DOCUMENT_END] = () -> {
            boolean explicit = false;
            while (tokenizer.peekNextTokenType() == DOCUMENT_END) {
                tokenizer.getNextToken();
                explicit = true;
            }
            return explicit ? Event.DOCUMENT_END_TRUE : Event.DOCUMENT_END_FALSE;
        };
        table[P_BLOCK_NODE] = () -> {
            // 这里之后文件就解析结束
            TokenType type = tokenizer.peekNextTokenType();
            if (type == DIRECTIVE || type == DOCUMENT_START || type == DOCUMENT_END || type == STREAM_END) {
                parseStack.add(0, table[P_EMPTY_SCALAR]);
            } else if (type == ALIAS) {
                parseStack.add(0, table[P_ALIAS]);
            } else {
                parseStack.add(0, table[P_PROPERTIES_END]);
                parseStack.add(0, table[P_BLOCK_CONTENT]);
                parseStack.add(0, table[P_PROPERTIES]);
            }
            return null;
        };
        table[P_BLOCK_CONTENT] = () -> {
            TokenType type = tokenizer.peekNextTokenType();
            if (type == BLOCK_SEQUENCE_START) {
                parseStack.add(0, table[P_BLOCK_SEQUENCE]);
            } else if (type == BLOCK_MAPPING_START) {
                parseStack.add(0, table[P_BLOCK_MAPPING]);
            } else if (type == FLOW_SEQUENCE_START) {
                parseStack.add(0, table[P_FLOW_SEQUENCE]);
            } else if (type == FLOW_MAPPING_START) {
                parseStack.add(0, table[P_FLOW_MAPPING]);
            } else if (type == SCALAR) {
                parseStack.add(0, table[P_SCALAR]);
            } else {
                throw new ParserException("Expected a sequence, mapping, or scalar but found: " + type);
            }
            return null;
        };
        table[P_PROPERTIES] = () -> {
            String anchor = null, tagHandle = null, tagSuffix = null;
            if (tokenizer.peekNextTokenType() == ANCHOR) {
                anchor = ((AnchorToken) tokenizer.getNextToken()).getInstanceName();
                if (tokenizer.peekNextTokenType() == TAG) {
                    TagToken tagToken = (TagToken) tokenizer.getNextToken();
                    tagHandle = tagToken.getHandle();
                    tagSuffix = tagToken.getSuffix();
                }
            } else if (tokenizer.peekNextTokenType() == TAG) {
                TagToken tagToken = (TagToken) tokenizer.getNextToken();
                tagHandle = tagToken.getHandle();
                tagSuffix = tagToken.getSuffix();
                if (tokenizer.peekNextTokenType() == ANCHOR) {
                    anchor = ((AnchorToken) tokenizer.getNextToken()).getInstanceName();
                }
            }
            String tag;
            if (tagHandle != null && !tagHandle.equals("!")) {
                if (!tagHandles.containsKey(tagHandle)) {
                    throw new ParserException("Undefined tag handle: " + tagHandle);
                }
                tag = tagHandles.get(tagHandle) + tagSuffix;
            } else {
                tag = tagSuffix;
            }
            anchors.add(0, anchor);
            tags.add(0, tag);
            return null;
        };
        table[P_PROPERTIES_END] = () -> {
            anchors.remove(0);
            tags.remove(0);
            return null;
        };
        table[P_FLOW_CONTENT] = () -> {
            TokenType type = tokenizer.peekNextTokenType();
            if (type == FLOW_SEQUENCE_START) {
                parseStack.add(0, table[P_FLOW_SEQUENCE]);
            } else if (type == FLOW_MAPPING_START) {
                parseStack.add(0, table[P_FLOW_MAPPING]);
            } else if (type == SCALAR) {
                parseStack.add(0, table[P_SCALAR]);
            } else if (type == KEY || type == FLOW_MAPPING_END/* || type == FLOW_SEQUENCE_END*/) {
                // 在属性值为null的时候也能打印出类型，但是解析报错
                parseStack.add(0, table[P_EMPTY_SCALAR]);
            } else {
                throw new ParserException("Expected a sequence, mapping, or scalar but found: " + type);
            }
            return null;
        };
        table[P_BLOCK_SEQUENCE] = () -> {
            parseStack.add(0, table[P_BLOCK_SEQUENCE_END]);
            parseStack.add(0, table[P_BLOCK_SEQUENCE_ENTRY]);
            parseStack.add(0, table[P_BLOCK_SEQUENCE_START]);
            return null;
        };
        table[P_BLOCK_MAPPING] = () -> {
            parseStack.add(0, table[P_BLOCK_MAPPING_END]);
            parseStack.add(0, table[P_BLOCK_MAPPING_ENTRY]);
            parseStack.add(0, table[P_BLOCK_MAPPING_START]);
            return null;
        };
        table[P_FLOW_SEQUENCE] = () -> {
            parseStack.add(0, table[P_FLOW_SEQUENCE_END]);
            parseStack.add(0, table[P_FLOW_SEQUENCE_ENTRY]);
            parseStack.add(0, table[P_FLOW_SEQUENCE_START]);
            return null;
        };
        table[P_FLOW_MAPPING] = () -> {
            parseStack.add(0, table[P_FLOW_MAPPING_END]);
            parseStack.add(0, table[P_FLOW_MAPPING_ENTRY]);
            parseStack.add(0, table[P_FLOW_MAPPING_START]);
            return null;
        };
        table[P_SCALAR] = () -> {
            ScalarToken token = (ScalarToken) tokenizer.getNextToken();
            boolean[] implicit;
            if (token.getPlain() && tags.get(0) == null || "!".equals(tags.get(0))) {
                implicit = new boolean[] { true, false };
            } else if (tags.get(0) == null) {
                implicit = new boolean[] { false, true };
            } else {
                implicit = new boolean[] { false, false };
            }
            return new ScalarEvent(anchors.get(0), tags.get(0), implicit, token.getValue(), token.getStyle());
        };
        table[P_BLOCK_SEQUENCE_ENTRY] = () -> {
            if (tokenizer.peekNextTokenType() == BLOCK_ENTRY) {
                tokenizer.getNextToken();
                TokenType type = tokenizer.peekNextTokenType();
                if (type == BLOCK_ENTRY || type == BLOCK_END) {
                    parseStack.add(0, table[P_BLOCK_SEQUENCE_ENTRY]);
                    parseStack.add(0, table[P_EMPTY_SCALAR]);
                } else {
                    parseStack.add(0, table[P_BLOCK_SEQUENCE_ENTRY]);
                    parseStack.add(0, table[P_BLOCK_NODE]);
                }
            }
            return null;
        };
        table[P_BLOCK_MAPPING_ENTRY] = () -> {
            TokenType type = tokenizer.peekNextTokenType();
            if (type == KEY) {
                tokenizer.getNextToken();
                type = tokenizer.peekNextTokenType();
                if (type == KEY || type == VALUE || type == BLOCK_END) {
                    parseStack.add(0, table[P_BLOCK_MAPPING_ENTRY]);
                    parseStack.add(0, table[P_BLOCK_MAPPING_ENTRY_VALUE]);
                    parseStack.add(0, table[P_EMPTY_SCALAR]);
                } else {
                    parseStack.add(0, table[P_BLOCK_MAPPING_ENTRY]);
                    parseStack.add(0, table[P_BLOCK_MAPPING_ENTRY_VALUE]);
                    parseStack.add(0, table[P_BLOCK_NODE_OR_INDENTLESS_SEQUENCE]);
                    parseStack.add(0, table[P_PROPERTIES]);
                }
            } else if (type == VALUE) {
                parseStack.add(0, table[P_BLOCK_MAPPING_ENTRY]);
                parseStack.add(0, table[P_BLOCK_MAPPING_ENTRY_VALUE]);
                parseStack.add(0, table[P_EMPTY_SCALAR]);
            }
            return null;
        };
        table[P_BLOCK_MAPPING_ENTRY_VALUE] = () -> {
            TokenType type = tokenizer.peekNextTokenType();
            if (type == VALUE) {
                tokenizer.getNextToken();
                type = tokenizer.peekNextTokenType();
                if (type == KEY || type == VALUE || type == BLOCK_END) {
                    parseStack.add(0, table[P_EMPTY_SCALAR]);
                } else {
                    parseStack.add(0, table[P_BLOCK_NODE_OR_INDENTLESS_SEQUENCE]);
                    parseStack.add(0, table[P_PROPERTIES]);
                }
            } else if (type == KEY) {
                parseStack.add(0, table[P_EMPTY_SCALAR]);
            }
            return null;
        };
        table[P_BLOCK_NODE_OR_INDENTLESS_SEQUENCE] = () -> {
            TokenType type = tokenizer.peekNextTokenType();
            if (type == ALIAS) {
                parseStack.add(0, table[P_ALIAS]);
            } else if (type == BLOCK_ENTRY) {
                parseStack.add(0, table[P_INDENTLESS_BLOCK_SEQUENCE]);
            } else {
                parseStack.add(0, table[P_BLOCK_CONTENT]);
            }
            return null;
        };
        table[P_BLOCK_SEQUENCE_START] = () -> {
            boolean implicit = tags.get(0) == null || tags.get(0).equals("!");
            tokenizer.getNextToken();
            return new SequenceStartEvent(anchors.get(0), tags.get(0), implicit, false);
        };
        table[P_BLOCK_SEQUENCE_END] = () -> {
            if (tokenizer.peekNextTokenType() != BLOCK_END) {
                throw new ParserException("Expected a 'block end' but found: " + tokenizer.peekNextTokenType());
            }
            tokenizer.getNextToken();
            return Event.SEQUENCE_END;
        };
        table[P_BLOCK_MAPPING_START] = () -> {
            boolean implicit = tags.get(0) == null || tags.get(0).equals("!");
            tokenizer.getNextToken();
            return new MappingStartEvent(anchors.get(0), tags.get(0), implicit, false);
        };
        table[P_BLOCK_MAPPING_END] = () -> {
            if (tokenizer.peekNextTokenType() != BLOCK_END) {
                throw new ParserException("Expected a 'block end' but found: " + tokenizer.peekNextTokenType());
            }
            tokenizer.getNextToken();
            return Event.MAPPING_END;
        };
        table[P_INDENTLESS_BLOCK_SEQUENCE] = () -> {
            parseStack.add(0, table[P_BLOCK_INDENTLESS_SEQUENCE_END]);
            parseStack.add(0, table[P_INDENTLESS_BLOCK_SEQUENCE_ENTRY]);
            parseStack.add(0, table[P_BLOCK_INDENTLESS_SEQUENCE_START]);
            return null;
        };
        table[P_BLOCK_INDENTLESS_SEQUENCE_START] = () -> {
            boolean implicit = tags.get(0) == null || tags.get(0).equals("!");
            return new SequenceStartEvent(anchors.get(0), tags.get(0), implicit, false);
        };
        table[P_INDENTLESS_BLOCK_SEQUENCE_ENTRY] = () -> {
            if (tokenizer.peekNextTokenType() == BLOCK_ENTRY) {
                tokenizer.getNextToken();
                TokenType type = tokenizer.peekNextTokenType();
                if (type == BLOCK_ENTRY || type == KEY || type == VALUE || type == BLOCK_END) {
                    parseStack.add(0, table[P_INDENTLESS_BLOCK_SEQUENCE_ENTRY]);
                    parseStack.add(0, table[P_EMPTY_SCALAR]);
                } else {
                    parseStack.add(0, table[P_INDENTLESS_BLOCK_SEQUENCE_ENTRY]);
                    parseStack.add(0, table[P_BLOCK_NODE]);
                }
            }
            return null;
        };
        table[P_BLOCK_INDENTLESS_SEQUENCE_END] = () -> Event.SEQUENCE_END;
        table[P_FLOW_SEQUENCE_START] = () -> {
            boolean implicit = tags.get(0) == null || tags.get(0).equals("!");
            tokenizer.getNextToken();
            return new SequenceStartEvent(anchors.get(0), tags.get(0), implicit, true);
        };
        table[P_FLOW_SEQUENCE_ENTRY] = () -> {
            if (tokenizer.peekNextTokenType() != FLOW_SEQUENCE_END) {
                if (tokenizer.peekNextTokenType() == KEY) {
                    parseStack.add(0, table[P_FLOW_SEQUENCE_ENTRY]);
                    parseStack.add(0, table[P_FLOW_ENTRY_MARKER]);
                    parseStack.add(0, table[P_FLOW_INTERNAL_MAPPING_END]);
                    parseStack.add(0, table[P_FLOW_INTERNAL_VALUE]);
                    parseStack.add(0, table[P_FLOW_INTERNAL_CONTENT]);
                    parseStack.add(0, table[P_FLOW_INTERNAL_MAPPING_START]);
                } else {
                    parseStack.add(0, table[P_FLOW_SEQUENCE_ENTRY]);
                    parseStack.add(0, table[P_FLOW_NODE]);
                    parseStack.add(0, table[P_FLOW_ENTRY_MARKER]);
                }
            }
            return null;
        };
        table[P_FLOW_SEQUENCE_END] = () -> {
            tokenizer.getNextToken();
            return Event.SEQUENCE_END;
        };
        table[P_FLOW_MAPPING_START] = () -> {
            boolean implicit = tags.get(0) == null || tags.get(0).equals("!");
            tokenizer.getNextToken();
            return new MappingStartEvent(anchors.get(0), tags.get(0), implicit, true);
        };
        table[P_FLOW_MAPPING_ENTRY] = () -> {
            if (tokenizer.peekNextTokenType() != FLOW_MAPPING_END) {
                if (tokenizer.peekNextTokenType() == KEY) {
                    parseStack.add(0, table[P_FLOW_MAPPING_ENTRY]);
                    parseStack.add(0, table[P_FLOW_ENTRY_MARKER]);
                    parseStack.add(0, table[P_FLOW_MAPPING_INTERNAL_VALUE]);
                    parseStack.add(0, table[P_FLOW_MAPPING_INTERNAL_CONTENT]);
                } else {
                    parseStack.add(0, table[P_FLOW_MAPPING_ENTRY]);
                    parseStack.add(0, table[P_FLOW_NODE]);
                    parseStack.add(0, table[P_FLOW_ENTRY_MARKER]);
                }
            }
            return null;
        };
        table[P_FLOW_MAPPING_END] = () -> {
            tokenizer.getNextToken();
            return Event.MAPPING_END;
        };
        table[P_FLOW_INTERNAL_MAPPING_START] = () -> {
            tokenizer.getNextToken();
            return new MappingStartEvent(null, null, true, true);
        };
        table[P_FLOW_INTERNAL_CONTENT] = () -> {
            TokenType type = tokenizer.peekNextTokenType();
            if (type == VALUE || type == FLOW_ENTRY || type == FLOW_SEQUENCE_END) {
                parseStack.add(0, table[P_EMPTY_SCALAR]);
            } else {
                parseStack.add(0, table[P_FLOW_NODE]);
            }
            return null;
        };
        table[P_FLOW_INTERNAL_VALUE] = () -> {
            if (tokenizer.peekNextTokenType() == VALUE) {
                tokenizer.getNextToken();
                if (tokenizer.peekNextTokenType() == FLOW_ENTRY || tokenizer.peekNextTokenType() == FLOW_SEQUENCE_END) {
                    parseStack.add(0, table[P_EMPTY_SCALAR]);
                } else {
                    parseStack.add(0, table[P_FLOW_NODE]);
                }
            } else {
                parseStack.add(0, table[P_EMPTY_SCALAR]);
            }
            return null;
        };
        table[P_FLOW_INTERNAL_MAPPING_END] = () -> Event.MAPPING_END;
        table[P_FLOW_ENTRY_MARKER] = () -> {
            if (tokenizer.peekNextTokenType() == FLOW_ENTRY) {
                tokenizer.getNextToken();
            }
            return null;
        };
        table[P_FLOW_NODE] = () -> {
            if (tokenizer.peekNextTokenType() == ALIAS) { parseStack.add(0, table[P_ALIAS]); } else {
                parseStack.add(0, table[P_PROPERTIES_END]);
                parseStack.add(0, table[P_FLOW_CONTENT]);
                parseStack.add(0, table[P_PROPERTIES]);
            }
            return null;
        };
        table[P_FLOW_MAPPING_INTERNAL_CONTENT] = () -> {
            TokenType type = tokenizer.peekNextTokenType();
            if (type == VALUE || type == FLOW_ENTRY || type == FLOW_MAPPING_END) {
                parseStack.add(0, table[P_EMPTY_SCALAR]);
            } else {
                tokenizer.getNextToken();
                parseStack.add(0, table[P_FLOW_NODE]);
            }
            return null;
        };
        table[P_FLOW_MAPPING_INTERNAL_VALUE] = () -> {
            if (tokenizer.peekNextTokenType() == VALUE) {
                tokenizer.getNextToken();
                if (tokenizer.peekNextTokenType() == FLOW_ENTRY || tokenizer.peekNextTokenType() == FLOW_MAPPING_END) {
                    parseStack.add(0, table[P_EMPTY_SCALAR]);
                } else {
                    parseStack.add(0, table[P_FLOW_NODE]);
                }
            } else {
                parseStack.add(0, table[P_EMPTY_SCALAR]);
            }
            return null;
        };
        table[P_ALIAS] = () -> {
            AliasToken token = (AliasToken) tokenizer.getNextToken();
            return new AliasEvent(token.getInstanceName());
        };
        table[P_EMPTY_SCALAR] = () -> new ScalarEvent(null, null, new boolean[] { true, false }, null, (char) 0);
    }

    DocumentStartEvent processDirectives(boolean explicit) {
        documentVersion = null;
        while (tokenizer.peekNextTokenType() == DIRECTIVE) {
            DirectiveToken token = (DirectiveToken) tokenizer.getNextToken();
            if (token.getDirective().equals("YAML")) {
                if (documentVersion != null) { throw new ParserException("Duplicate YAML directive."); }
                documentVersion = new Versionx(token.getValue());
                if (documentVersion.major != 1) {
                    throw new ParserException("Unsupported YAML version (1.x is required): " + documentVersion);
                }
            } else if (token.getDirective().equals("TAG")) {
                String[] values = token.getValue().split(" ");
                String handle = values[0];
                String prefix = values[1];
                if (tagHandles.containsKey(handle)) { throw new ParserException("Duplicate tag directive: " + handle); }
                tagHandles.put(handle, prefix);
            }
        }

        Versionx version;
        if (documentVersion != null) { version = documentVersion; } else { version = defaultVersion; }

        Map<String, String> tags = null;
        if (!tagHandles.isEmpty()) { tags = new HashMap<>(tagHandles); }
        Map<String, String> baseTags = version.minor == 0 ? DEFAULT_TAGS_1_0 : DEFAULT_TAGS_1_1;
        for (String key : baseTags.keySet()) {
            if (!tagHandles.containsKey(key)) { tagHandles.put(key, baseTags.get(key)); }
        }
        return new DocumentStartEvent(explicit, version, tags);
    }

    interface Production {
        Event produce();
    }

    static private final int P_STREAM = 0;
    static private final int P_STREAM_START = 1; // TERMINAL
    static private final int P_STREAM_END = 2; // TERMINAL
    static private final int P_IMPLICIT_DOCUMENT = 3;
    static private final int P_EXPLICIT_DOCUMENT = 4;
    static private final int P_DOCUMENT_START = 5;
    static private final int P_DOCUMENT_START_IMPLICIT = 6;
    static private final int P_DOCUMENT_END = 7;
    static private final int P_BLOCK_NODE = 8;
    static private final int P_BLOCK_CONTENT = 9;
    static private final int P_PROPERTIES = 10;
    static private final int P_PROPERTIES_END = 11;
    static private final int P_FLOW_CONTENT = 12;
    static private final int P_BLOCK_SEQUENCE = 13;
    static private final int P_BLOCK_MAPPING = 14;
    static private final int P_FLOW_SEQUENCE = 15;
    static private final int P_FLOW_MAPPING = 16;
    static private final int P_SCALAR = 17;
    static private final int P_BLOCK_SEQUENCE_ENTRY = 18;
    static private final int P_BLOCK_MAPPING_ENTRY = 19;
    static private final int P_BLOCK_MAPPING_ENTRY_VALUE = 20;
    static private final int P_BLOCK_NODE_OR_INDENTLESS_SEQUENCE = 21;
    static private final int P_BLOCK_SEQUENCE_START = 22;
    static private final int P_BLOCK_SEQUENCE_END = 23;
    static private final int P_BLOCK_MAPPING_START = 24;
    static private final int P_BLOCK_MAPPING_END = 25;
    static private final int P_INDENTLESS_BLOCK_SEQUENCE = 26;
    static private final int P_BLOCK_INDENTLESS_SEQUENCE_START = 27;
    static private final int P_INDENTLESS_BLOCK_SEQUENCE_ENTRY = 28;
    static private final int P_BLOCK_INDENTLESS_SEQUENCE_END = 29;
    static private final int P_FLOW_SEQUENCE_START = 30;
    static private final int P_FLOW_SEQUENCE_ENTRY = 31;
    static private final int P_FLOW_SEQUENCE_END = 32;
    static private final int P_FLOW_MAPPING_START = 33;
    static private final int P_FLOW_MAPPING_ENTRY = 34;
    static private final int P_FLOW_MAPPING_END = 35;
    static private final int P_FLOW_INTERNAL_MAPPING_START = 36;
    static private final int P_FLOW_INTERNAL_CONTENT = 37;
    static private final int P_FLOW_INTERNAL_VALUE = 38;
    static private final int P_FLOW_INTERNAL_MAPPING_END = 39;
    static private final int P_FLOW_ENTRY_MARKER = 40;
    static private final int P_FLOW_NODE = 41;
    static private final int P_FLOW_MAPPING_INTERNAL_CONTENT = 42;
    static private final int P_FLOW_MAPPING_INTERNAL_VALUE = 43;
    static private final int P_ALIAS = 44;
    static private final int P_EMPTY_SCALAR = 45;

    static private final Map<String, String> DEFAULT_TAGS_1_0 = new HashMap<>();
    static private final Map<String, String> DEFAULT_TAGS_1_1 = new HashMap<>();

    static {
        DEFAULT_TAGS_1_0.put("!", "tag:yaml.org,2002:");

        DEFAULT_TAGS_1_1.put("!", "!");
        DEFAULT_TAGS_1_1.put("!!", "tag:yaml.org,2002:");
    }

    public class ParserException extends RuntimeException {
        public ParserException(String message) {
            super("Line " + tokenizer.getLineNumber() + ", column " + tokenizer.getColumn() + ": " + message);
        }
    }

    public static void main(String[] args) throws Exception {
        Parser parser = new Parser(new FileReader("test/test.yml"));
        while (true) {
            Event event = parser.getNextEvent();
            if (event == null) { break; }
            System.out.println(event);
        }
    }
}
