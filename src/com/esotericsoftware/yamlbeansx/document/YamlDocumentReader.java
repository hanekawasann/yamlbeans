package com.esotericsoftware.yamlbeansx.document;

import java.io.Reader;
import java.io.StringReader;

import static com.esotericsoftware.yamlbeansx.parser.EventType.ALIAS;
import static com.esotericsoftware.yamlbeansx.parser.EventType.MAPPING_END;
import static com.esotericsoftware.yamlbeansx.parser.EventType.MAPPING_START;
import static com.esotericsoftware.yamlbeansx.parser.EventType.SCALAR;
import static com.esotericsoftware.yamlbeansx.parser.EventType.SEQUENCE_END;
import static com.esotericsoftware.yamlbeansx.parser.EventType.SEQUENCE_START;
import com.esotericsoftware.yamlbeansx.Versionx;
import com.esotericsoftware.yamlbeansx.YamlxException;
import com.esotericsoftware.yamlbeansx.parser.AliasEvent;
import com.esotericsoftware.yamlbeansx.parser.Event;
import com.esotericsoftware.yamlbeansx.parser.MappingStartEvent;
import com.esotericsoftware.yamlbeansx.parser.Parser;
import com.esotericsoftware.yamlbeansx.parser.Parser.ParserException;
import com.esotericsoftware.yamlbeansx.parser.ScalarEvent;
import com.esotericsoftware.yamlbeansx.parser.SequenceStartEvent;
import com.esotericsoftware.yamlbeansx.tokenizer.Tokenizer.TokenizerException;

public class YamlDocumentReader {

    Parser parser;

    public YamlDocumentReader(String yaml) {
        this(new StringReader(yaml));
    }

    public YamlDocumentReader(String yaml, Versionx version) {
        this(new StringReader(yaml), version);
    }

    public YamlDocumentReader(Reader reader) {
        this(reader, null);
    }

    public YamlDocumentReader(Reader reader, Versionx version) {
        if (version == null) { version = Versionx.DEFAULT_VERSION; }
        parser = new Parser(reader, version);
    }

    public YamlDocument read() throws YamlxException {
        try {
            while (true) {
                Event event = parser.peekNextEvent();
                if (event == null) { return null; }
                switch (event.type) {
                    case STREAM_START:
                        parser.getNextEvent(); // consume it
                        break;
                    case STREAM_END:
                        parser.getNextEvent(); // consume it
                        return null;
                    case DOCUMENT_START:
                        parser.getNextEvent(); // consume it
                        return readDocument();
                    default:
                        throw new IllegalStateException();
                }
            }
        } catch (ParserException ex) {
            throw new YamlxException("Error parsing YAML.", ex);
        } catch (TokenizerException ex) {
            throw new YamlxException("Error tokenizing YAML.", ex);
        }

    }

    private YamlDocument readDocument() {
        Event event = parser.peekNextEvent();
        switch (event.type) {
            case MAPPING_START:
                return readMapping();
            case SEQUENCE_START:
                return readSequence();
            default:
                throw new IllegalStateException();
        }
    }

    private YamlMapping readMapping() {
        Event event = parser.getNextEvent();
        if (event.type != MAPPING_START) { throw new IllegalStateException(); }
        YamlMapping element = new YamlMapping();
        MappingStartEvent mapping = (MappingStartEvent) event;
        element.setTag(mapping.tag);
        element.setAnchor(mapping.anchor);
        readMappingElements(element);
        return element;
    }

    private void readMappingElements(YamlMapping mapping) {
        while (true) {
            Event event = parser.peekNextEvent();
            if (event.type == MAPPING_END) {
                parser.getNextEvent(); // consume it
                return;
            } else {
                YamlEntry entry = readEntry();
                mapping.addEntry(entry);
            }
        }
    }

    private YamlEntry readEntry() {
        YamlScalar scalar = readScalar();
        YamlElement value = readValue();
        return new YamlEntry(scalar, value);
    }

    private YamlElement readValue() {
        Event event = parser.peekNextEvent();
        switch (event.type) {
            case SCALAR:
                return readScalar();
            case ALIAS:
                return readAlias();
            case MAPPING_START:
                return readMapping();
            case SEQUENCE_START:
                return readSequence();
            default:
                throw new IllegalStateException();
        }
    }

    private YamlAlias readAlias() {
        Event event = parser.getNextEvent();
        if (event.type != ALIAS) { throw new IllegalStateException(); }
        YamlAlias element = new YamlAlias();
        AliasEvent alias = (AliasEvent) event;
        element.setAnchor(alias.anchor);
        return element;
    }

    private YamlSequence readSequence() {
        Event event = parser.getNextEvent();
        if (event.type != SEQUENCE_START) { throw new IllegalStateException(); }
        YamlSequence element = new YamlSequence();
        SequenceStartEvent sequence = (SequenceStartEvent) event;
        element.setTag(sequence.tag);
        element.setAnchor(sequence.anchor);
        readSequenceElements(element);
        return element;
    }

    private void readSequenceElements(YamlSequence sequence) {
        while (true) {
            Event event = parser.peekNextEvent();
            if (event.type == SEQUENCE_END) {
                parser.getNextEvent(); // consume it
                return;
            } else {
                YamlElement element = readValue();
                sequence.addElement(element);
            }
        }
    }

    private YamlScalar readScalar() {
        Event event = parser.getNextEvent();
        if (event.type != SCALAR) { throw new IllegalStateException(); }
        ScalarEvent scalar = (ScalarEvent) event;
        YamlScalar element = new YamlScalar();
        element.setTag(scalar.tag);
        element.setAnchor(scalar.anchor);
        element.setValue(scalar.value);
        return element;
    }

}
