package com.yukms.yamlxbeans.document;

import java.io.IOException;

import com.yukms.yamlxbeans.YamlxConfig.WriteConfig;
import com.yukms.yamlxbeans.emitter.Emitter;
import com.yukms.yamlxbeans.emitter.EmitterException;

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
