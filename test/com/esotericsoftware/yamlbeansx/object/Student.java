package com.esotericsoftware.yamlbeansx.object;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yukms 763803382@qq.com 2019/6/27 18:39
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Student extends People {
    private String teacherName;
}
