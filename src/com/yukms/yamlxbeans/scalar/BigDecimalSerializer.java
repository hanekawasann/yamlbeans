package com.yukms.yamlxbeans.scalar;

import java.math.BigDecimal;

import com.yukms.yamlxbeans.YamlxException;
import org.apache.commons.lang3.StringUtils;

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
        return StringUtils.isBlank(value) ? null : new BigDecimal(value);
    }
}
