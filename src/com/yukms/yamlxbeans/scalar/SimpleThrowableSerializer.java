package com.yukms.yamlxbeans.scalar;

import com.yukms.yamlxbeans.YamlxException;

/**
 * @author hudingpeng hudingpeng@souche.com 2019/7/13 19:21
 */
public class SimpleThrowableSerializer implements ScalarSerializer<Throwable> {
    @Override
    public String write(Throwable object) throws YamlxException {
        return object.getClass().getName();
    }

    @Override
    public Throwable read(String value) throws YamlxException {
        try {
            return (Throwable) Class.forName(value).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new YamlxException(e);
        }
    }
}
