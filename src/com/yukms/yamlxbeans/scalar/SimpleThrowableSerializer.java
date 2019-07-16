package com.yukms.yamlxbeans.scalar;

import java.lang.reflect.Field;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yukms.yamlxbeans.YamlxException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author hudingpeng hudingpeng@souche.com 2019/7/13 19:21
 */
public class SimpleThrowableSerializer implements ScalarSerializer<Throwable> {
    private static final String NAME = "className";
    private static final String MESSAGE = "message";

    @Override
    public String write(Throwable object) throws YamlxException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(NAME, object.getClass().getName());
        jsonObject.put(MESSAGE, object.getMessage());
        return jsonObject.toJSONString();
    }

    @Override
    public Throwable read(String value) throws YamlxException {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(value);
            String name = jsonObject.getString(NAME);
            if (StringUtils.isBlank(name)) {
                return null;
            }
            Class<? extends Throwable> clazz = (Class<? extends Throwable>) Class.forName(name);
            Throwable throwable = clazz.newInstance();
            Field field = ReflectionUtils.findField(clazz, "detailMessage");
            if (field != null) {
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.setField(field, throwable, jsonObject.getString(MESSAGE));
            }
            return throwable;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new YamlxException(e);
        }
    }
}
