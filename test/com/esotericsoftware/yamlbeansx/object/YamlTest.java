package com.esotericsoftware.yamlbeansx.object;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.esotericsoftware.yamlbeansx.YamlxConfig;
import com.esotericsoftware.yamlbeansx.YamlxException;
import com.esotericsoftware.yamlbeansx.YamlxReader;
import com.esotericsoftware.yamlbeansx.YamlxWriter;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yukms 763803382@qq.com 2019/6/27 18:25
 */
public class YamlTest {
    private static final YamlxConfig CONFIG = new YamlxConfig();

    static {
        YamlxConfig.WriteConfig writeConfig = CONFIG.writeConfig;
        //保存默认字段（无法保存null）
        writeConfig.setWriteDefaultValues(true);
        // 缩进
        writeConfig.setIndentSize(3);
        // 文件字段顺序与字段定义顺序相同
        writeConfig.setKeepBeanPropertyOrder(true);
        // 格式化输出
        writeConfig.setCanonical(true);
        // 不换行
        writeConfig.setWrapColumn(Integer.MAX_VALUE);
        // 中文不转义
        writeConfig.setEscapeUnicode(false);
        // 总是输出类名
        writeConfig.setWriteClassname(YamlxConfig.WriteClassName.ALWAYS);
        // 输出开始标记
        writeConfig.setExplicitFirstDocument(true);
        // 输出结束标记
        writeConfig.setExplicitEndDocument(true);
    }

    @Test
    public void test_ListFieldObject_null() throws IOException {
        String path = "test/resource/yaml/ListFieldObject_null.yaml";
        ListFieldObject object = new ListFieldObject();
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(object);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        ListFieldObject read = (ListFieldObject) reader.read();
        Assert.assertNull(read.getAddress());
    }

    @Test
    public void test_ListFieldObject_empty() throws IOException {
        String path = "test/resource/yaml/ListFieldObject_empty.yaml";
        ListFieldObject object = new ListFieldObject();
        object.setAddress(new ArrayList<>());
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(object);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        ListFieldObject read = (ListFieldObject) reader.read();
        Assert.assertTrue(read.getAddress().isEmpty());
    }

    @Test
    public void test_ListFieldObject() throws IOException {
        String path = "test/resource/yaml/ListFieldObject.yaml";
        ListFieldObject object = new ListFieldObject();
        ArrayList<String> list = new ArrayList<>();
        list.add("1");
        object.setAddress(list);
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(object);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        ListFieldObject read = (ListFieldObject) reader.read();
        List<String> address = read.getAddress();
        Assert.assertEquals(1, address.size());
        Assert.assertEquals("1", address.get(0));
    }

    @Test
    public void test_array_list() throws IOException {
        String path = "test/resource/yaml/ArrayList.yaml";
        List<String> strings = new ArrayList<>();
        strings.add("1");
        strings.add("2");
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(strings);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        List<String> read = (List<String>) reader.read();
        Assert.assertEquals(2, read.size());
        Assert.assertEquals("1", read.get(0));
        Assert.assertEquals("2", read.get(1));
    }

    @Test
    public void test_linked_list() throws IOException {
        String path = "test/resource/yaml/LinkedList.yaml";
        List<String> strings = new LinkedList<>();
        strings.add("1");
        strings.add("2");
        YamlxWriter writer = new YamlxWriter(new FileWriter(path));
        writer.write(strings);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        List<String> read = (List<String>) reader.read();
        Assert.assertEquals("1", read.get(0));
        Assert.assertEquals("2", read.get(1));
    }

    @Test
    public void test_linked_list_config() throws IOException {
        String path = "test/resource/yaml/LinkedListConfig.yaml";
        List<String> strings = new LinkedList<>();
        strings.add("1");
        strings.add("2");
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(strings);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        List<String> read = (List<String>) reader.read();
        Assert.assertEquals("1", read.get(0));
        Assert.assertEquals("2", read.get(1));
    }

    @Test
    public void test_CollectionFieldObject() throws IOException {
        String path = "test/resource/yaml/CollectionFieldObject.yaml";
        CollectionFieldObject object = new CollectionFieldObject();
        List<String> strings = new ArrayList<>();
        strings.add("1");
        strings.add("2");
        object.setStrings(strings);
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(object);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        CollectionFieldObject read = reader.read(CollectionFieldObject.class);
        Assert.assertTrue(read.getStrings().isEmpty());
    }

    @Test
    public void test_CollectionFieldObject_null() throws IOException {
        String path = "test/resource/yaml/CollectionFieldObjectNull.yaml";
        CollectionFieldObject object = new CollectionFieldObject();
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(object);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        CollectionFieldObject read = reader.read(CollectionFieldObject.class);
        Assert.assertNull(read.getStrings());
    }

    @Test
    public void test_general_obj() throws IOException {
        String path = "test/resource/yaml/generalObj.yaml";
        People people = new People();
        people.setName("yukms");
        people.setAge(18);
        YamlxWriter writer = new YamlxWriter(new FileWriter(path));
        writer.write(people);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        People read = (People) reader.read();
        Assert.assertEquals("yukms", read.getName());
        Assert.assertEquals(18, read.getAge());
    }

    @Test
    public void test_null_field_config() throws IOException {
        String path = "test/resource/yaml/nullFieldConfig.yaml";
        NullFieldObject object = new NullFieldObject();
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(object);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path), CONFIG);
        NullFieldObject read = (NullFieldObject) reader.read();
        Assert.assertNull(read.getName());
        Assert.assertNull(read.getObject());
    }

    @Test
    public void test_general_obj_config() throws IOException {
        String path = "test/resource/yaml/generalObjConfig.yaml";
        People people = new People();
        //people.setEmptyString("");
        people.setName("yukms");
        people.setAge(18);
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(people);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        People read = (People) reader.read();
        Assert.assertEquals("yukms", read.getName());
        Assert.assertEquals(18, read.getAge());
        //Assert.assertEquals("", read.getEmptyString());
    }

    @Test
    public void test_null_field() throws IOException {
        String path = "test/resource/yaml/nullField.yaml";
        NullFieldObject object = new NullFieldObject();
        YamlxWriter writer = new YamlxWriter(new FileWriter(path));
        writer.write(object);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        NullFieldObject read = reader.read(NullFieldObject.class);
        //Assert.assertNull(read.getName());
        Assert.assertNull(read.getObject());
    }

    @Test
    public void test_BigDecimalFieldObject() throws IOException {
        String path = "test/resource/yaml/BigDecimalFieldObject.yaml";
        BigDecimalFieldObject object = new BigDecimalFieldObject();
        object.setDecimal(new BigDecimal(123));
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(object);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path), CONFIG);
        BigDecimalFieldObject read = reader.read(BigDecimalFieldObject.class);
        Assert.assertNull(read.getDecimal());
    }

    @Test
    public void test_null_obj() throws IOException {
        String path = "test/resource/yaml/nullObj.yaml";
        YamlxWriter writer = new YamlxWriter(new FileWriter(path));
        writer.write(null);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        Object read = reader.read();
        Assert.assertNull(read);
    }

    @Test
    public void test_null_obj_config() throws IOException {
        String path = "test/resource/yaml/nullObjConfig.yaml";
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(null);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path), CONFIG);
        Object read = reader.read();
        Assert.assertNull(read);
    }


    @Test
    public void test_subclass_obj() throws IOException {
        String path = "test/resource/yaml/subclassObj.yaml";
        List<People> people = getSubclassesOfPeople();
        YamlxWriter writer = new YamlxWriter(new FileWriter(path));
        writer.write(people);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path));
        List<People> read = (List<People>) reader.read();
        Assert.assertEquals(2, read.size());
        Teacher read_0 = (Teacher) read.get(0);
        Assert.assertEquals("teacher", read_0.getName());
        Assert.assertEquals(18, read_0.getAge());
        Student read_1 = (Student) read.get(1);
        Assert.assertEquals("student", read_1.getName());
        Assert.assertEquals(18, read_1.getAge());
        Assert.assertEquals("teacher", read_1.getTeacherName());
    }

    @Test
    public void test_subclass_obj_config() throws IOException {
        String path = "test/resource/yaml/subclassObjConfig.yaml";
        List<People> people = getSubclassesOfPeople();
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(people);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path), CONFIG);
        List<People> read = (List<People>) reader.read();
        Assert.assertEquals(2, read.size());
        Teacher read_0 = (Teacher) read.get(0);
        Assert.assertEquals("teacher", read_0.getName());
        Assert.assertEquals(18, read_0.getAge());
        Student read_1 = (Student) read.get(1);
        Assert.assertEquals("student", read_1.getName());
        Assert.assertEquals(18, read_1.getAge());
        Assert.assertEquals("teacher", read_1.getTeacherName());
    }

    protected List<People> getSubclassesOfPeople() {
        Teacher teacher = new Teacher();
        teacher.setName("teacher");
        teacher.setAge(18);
        Student student = new Student();
        student.setName("student");
        student.setAge(18);
        student.setTeacherName("teacher");

        List<People> people = new ArrayList<>();
        people.add(teacher);
        people.add(student);
        return people;
    }

    @Test
    public void test_draw_dot() throws IOException {
        String path = "test/resource/yaml/drawDot.yaml";
        Teacher teacher = new Teacher();
        teacher.setName("teacher");
        teacher.setAge(18);

        List<People> people = new ArrayList<>();
        people.add(teacher);
        people.add(teacher);
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(people);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path), CONFIG);
        List<People> read = (List<People>) reader.read();
        Assert.assertEquals(2, read.size());
        Teacher read_0 = (Teacher) read.get(0);
        Assert.assertEquals("teacher", read_0.getName());
        Assert.assertEquals(18, read_0.getAge());
        People read_1 = read.get(1);
        Assert.assertSame(read_0, read_1);
    }

    @Test
    public void test_number() throws IOException {
        String path = "test/resource/yaml/number.yaml";
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write(123);
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path), CONFIG);
        Integer read = reader.read(Integer.class);
        Assert.assertEquals(new Integer(123), read);
    }

    @Test
    public void test_string() throws IOException {
        String path = "test/resource/yaml/string.yaml";
        YamlxWriter writer = new YamlxWriter(new FileWriter(path), CONFIG);
        writer.write("string");
        writer.close();

        YamlxReader reader = new YamlxReader(new FileReader(path), CONFIG);
        String read = reader.read(String.class);
        Assert.assertEquals("string", read);
    }

    @Test(expected = YamlxException.class)
    public void test_exception() throws IOException {
        String path = "test/resource/yaml/exception.yaml";
        YamlxWriter writer = new YamlxWriter(new FileWriter(path));
        writer.write(new RuntimeException("exception"));
        writer.close();
    }

    @Test
    public void test_no_getter_object() throws IOException {
        String path = "test/resource/yaml/noGetterObject.yaml";
        NoGetterObject object = new NoGetterObject("NoGetterObject");
        YamlxWriter writer = new YamlxWriter(new FileWriter(path));
        writer.write(object);
        writer.close();
    }
}

