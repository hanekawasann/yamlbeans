package com.esotericsoftware.yamlbeansx.document;

import java.io.IOException;

import com.esotericsoftware.yamlbeansx.YamlxConfig.WriteConfig;
import com.esotericsoftware.yamlbeansx.emitter.Emitter;
import com.esotericsoftware.yamlbeansx.emitter.EmitterException;

public abstract class YamlElement {

    String tag;
    String anchor;

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public String getTag() {
        return tag;
    }

    public String getAnchor() {
        return anchor;
    }

    public abstract void emitEvent(Emitter emitter, WriteConfig config) throws EmitterException, IOException;
}
