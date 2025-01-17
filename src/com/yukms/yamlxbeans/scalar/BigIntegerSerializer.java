package com.yukms.yamlxbeans.scalar;

import java.math.BigInteger;

import com.yukms.yamlxbeans.YamlxException;
import org.apache.commons.lang3.StringUtils;

/**
 * @author hudingpeng hudingpeng@souche.com 2019/7/13 18:13
 */
public class BigIntegerSerializer implements ScalarSerializer<BigInteger> {
    @Override
    public String write(BigInteger object) throws YamlxException {
        return object.toString();
    }

    @Override
    public BigInteger read(String value) throws YamlxException {
        return StringUtils.isBlank(value) ? null : new BigInteger(value);
    }
}
