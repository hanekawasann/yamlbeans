package com.yukms.yamlxbeans.scalar;

import com.yukms.yamlxbeans.YamlxException;

/**
 * @author hudingpeng hudingpeng@souche.com 2019/7/16 10:48
 */
public class ClassSerializer implements ScalarSerializer<Class> {
    @Override
    public String write(Class object) throws YamlxException {
        return object.getName();
    }

    @Override
    public Class read(String value) throws YamlxException {
        try {
            return Class.forName(value);
        } catch (ClassNotFoundException e) {
            throw new YamlxException(e);
        }
    }
}
