package com.esotericsoftware.yamlbeansx.document;

import com.esotericsoftware.yamlbeansx.YamlxException;

public interface YamlDocument {

    String getTag();

    int size();

    YamlEntry getEntry(String key) throws YamlxException;

    YamlEntry getEntry(int index) throws YamlxException;

    boolean deleteEntry(String key) throws YamlxException;

    void setEntry(String key, boolean value) throws YamlxException;

    void setEntry(String key, Number value) throws YamlxException;

    void setEntry(String key, String value) throws YamlxException;

    void setEntry(String key, YamlElement value) throws YamlxException;

    YamlElement getElement(int item) throws YamlxException;

    void deleteElement(int element) throws YamlxException;

    void setElement(int item, boolean value) throws YamlxException;

    void setElement(int item, Number value) throws YamlxException;

    void setElement(int item, String value) throws YamlxException;

    void setElement(int item, YamlElement element) throws YamlxException;

    void addElement(boolean value) throws YamlxException;

    void addElement(Number value) throws YamlxException;

    void addElement(String value) throws YamlxException;

    void addElement(YamlElement element) throws YamlxException;

}
