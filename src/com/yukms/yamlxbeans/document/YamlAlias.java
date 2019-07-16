package com.yukms.yamlxbeans.document;

import java.io.IOException;

import com.yukms.yamlxbeans.YamlxConfig.WriteConfig;
import com.yukms.yamlxbeans.emitter.Emitter;
import com.yukms.yamlxbeans.emitter.EmitterException;
import com.yukms.yamlxbeans.parser.AliasEvent;

public class YamlAlias extends YamlElement {

    @Override
    public void emitEvent(Emitter emitter, WriteConfig config) throws EmitterException, IOException {
        emitter.emit(new AliasEvent(anchor));
    }

    @Override
    public String toString() {
        return "*" + anchor;
    }
}
