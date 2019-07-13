package com.esotericsoftware.yamlbeansx.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * @author yukms 763803382@qq.com 2019/6/27 18:24
 */
@Data
public class People implements Serializable {
    private String name;
    private int age;
    private List<String> address = new ArrayList<>();
}
