package com.yukms.yamlxbeans.scalar;

import java.math.BigDecimal;

import com.yukms.yamlxbeans.YamlxException;

/**
 * @author hudingpeng hudingpeng@souche.com 2019/7/13 18:06
 */
public class BigDecimalSerializer implements ScalarSerializer<BigDecimal> {
    @Override
    public String write(BigDecimal object) throws YamlxException {
        return object.toString();
    }

    @Override
    public BigDecimal read(String value) throws YamlxException {
        return new BigDecimal(value);
    }
}
