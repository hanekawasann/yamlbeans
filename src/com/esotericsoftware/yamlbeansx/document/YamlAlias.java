package com.esotericsoftware.yamlbeansx.document;

import java.io.IOException;

import com.esotericsoftware.yamlbeansx.YamlxConfig.WriteConfig;
import com.esotericsoftware.yamlbeansx.emitter.Emitter;
import com.esotericsoftware.yamlbeansx.emitter.EmitterException;
import com.esotericsoftware.yamlbeansx.parser.AliasEvent;

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
