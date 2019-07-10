package com.esotericsoftware.yamlbeansx.document;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.esotericsoftware.yamlbeansx.YamlxConfig.WriteConfig;
import com.esotericsoftware.yamlbeansx.YamlxException;
import com.esotericsoftware.yamlbeansx.emitter.Emitter;
import com.esotericsoftware.yamlbeansx.emitter.EmitterException;
import com.esotericsoftware.yamlbeansx.parser.Event;
import com.esotericsoftware.yamlbeansx.parser.SequenceStartEvent;

public class YamlSequence extends YamlElement implements YamlDocument {

    List<YamlElement> elements = new LinkedList<YamlElement>();

    public int size() {
        return elements.size();
    }

    public void addElement(YamlElement element) {
        elements.add(element);
    }

    public void deleteElement(int item) throws YamlxException {
        elements.remove(item);
    }

    public YamlElement getElement(int item) throws YamlxException {
        return elements.get(item);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (anchor != null) {
            sb.append('&');
            sb.append(anchor);
            sb.append(' ');
        }
        if (tag != null) {
            sb.append(" !");
            sb.append(tag);
        }
        if (!elements.isEmpty()) {
            sb.append('[');
            for (YamlElement element : elements) {
                sb.append(element.toString());
                sb.append(',');
            }
            sb.setLength(sb.length() - 1);
            sb.append(']');
        }
        return sb.toString();
    }


    @Override
    public void emitEvent(Emitter emitter, WriteConfig config) throws EmitterException, IOException {
        emitter.emit(new SequenceStartEvent(anchor, tag, tag == null, false));
        for (YamlElement element : elements) { element.emitEvent(emitter, config); }
        emitter.emit(Event.SEQUENCE_END);
    }

    public YamlEntry getEntry(String key) throws YamlxException {
        throw new YamlxException("Can only get entry on mapping!");
    }

    public YamlEntry getEntry(int index) throws YamlxException {
        throw new YamlxException("Can only get entry on mapping!");
    }

    public boolean deleteEntry(String key) throws YamlxException {
        throw new YamlxException("Can only delete entry on mapping!");
    }

    public void setEntry(String key, boolean value) throws YamlxException {
        throw new YamlxException("Can only set entry on mapping!");
    }

    public void setEntry(String key, Number value) throws YamlxException {
        throw new YamlxException("Can only set entry on mapping!");
    }

    public void setEntry(String key, String value) throws YamlxException {
        throw new YamlxException("Can only set entry on mapping!");
    }

    public void setEntry(String key, YamlElement value) throws YamlxException {
        throw new YamlxException("Can only set entry on mapping!");
    }

    public void setElement(int item, boolean value) throws YamlxException {
        elements.set(item, new YamlScalar(value));
    }

    public void setElement(int item, Number value) throws YamlxException {
        elements.set(item, new YamlScalar(value));
    }

    public void setElement(int item, String value) throws YamlxException {
        elements.set(item, new YamlScalar(value));
    }

    public void setElement(int item, YamlElement element) throws YamlxException {
        elements.set(item, element);
    }

    public void addElement(boolean value) throws YamlxException {
        elements.add(new YamlScalar(value));
    }

    public void addElement(Number value) throws YamlxException {
        elements.add(new YamlScalar(value));
    }

    public void addElement(String value) throws YamlxException {
        elements.add(new YamlScalar(value));
    }


}
