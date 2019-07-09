package com.esotericsoftware.yamlbeans.my;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yukms 763803382@qq.com 2019/6/27 18:25
 */
public class YamlTest {
    private static final YamlConfig CONFIG = new YamlConfig();

    static {
        YamlConfig.WriteConfig writeConfig = CONFIG.writeConfig;
        //保存默认字段（无法保存null）
        //writeConfig.setWriteDefaultValues(true);
        //// 缩进
        //writeConfig.setIndentSize(3);
        //// 文件字段顺序与字段定义顺序相同
        //writeConfig.setKeepBeanPropertyOrder(true);
        //// 格式化输出
        //writeConfig.setCanonical(true);
        //// 不换行
        //writeConfig.setWrapColumn(Integer.MAX_VALUE);
        //// 中文不转义
        //writeConfig.setEscapeUnicode(false);
        //// 总是输出类名
        //writeConfig.setWriteClassname(YamlConfig.WriteClassName.ALWAYS);
        //// 输出开始标记
        //writeConfig.setExplicitFirstDocument(true);
        //// 输出结束标记
        //writeConfig.setExplicitEndDocument(true);
    }

    @Test
    public void test_general_obj() throws IOException {
        String path = "test/resource/yaml/generalObj.yaml";
        People people = new People();
        people.setName("yukms");
        people.setAge(18);
        YamlWriter writer = new YamlWriter(new FileWriter(path), CONFIG);
        writer.write(people);
        writer.clearAnchors();
        writer.close();

        YamlReader reader = new YamlReader(new FileReader(path));
        People read = (People) reader.read();
        Assert.assertEquals("yukms", read.getName());
        Assert.assertEquals(18, read.getAge());
    }

    @Test
    public void test_subclass_obj() throws IOException {
        String path = "test/resource/yaml/subclassObj.yaml";
        List<People> people = getSubclassesOfPeople();
        YamlWriter writer = new YamlWriter(new FileWriter(path), CONFIG);
        writer.write(people);
        writer.close();

        YamlReader reader = new YamlReader(new FileReader(path), CONFIG);
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
        YamlWriter writer = new YamlWriter(new FileWriter(path), CONFIG);
        writer.write(people);
        writer.close();

        YamlReader reader = new YamlReader(new FileReader(path), CONFIG);
        List<People> read = (List<People>) reader.read();
        Assert.assertEquals(2, read.size());
        Teacher read_0 = (Teacher) read.get(0);
        Assert.assertEquals("teacher", read_0.getName());
        Assert.assertEquals(18, read_0.getAge());
        People read_1 = read.get(1);
        Assert.assertSame(read_0, read_1);
    }

    @Test
    public void test_null_field() throws IOException {
        String path = "test/resource/yaml/nullField.yaml";
        NullFieldObject object = new NullFieldObject();
        YamlWriter writer = new YamlWriter(new FileWriter(path), CONFIG);
        writer.write(object);
        writer.close();

        YamlReader reader = new YamlReader(new FileReader(path), CONFIG);
        NullFieldObject read = reader.read(NullFieldObject.class);
        Assert.assertNull(read.getObject());
    }

    @Test
    public void test_null_obj() throws IOException {
        String path = "test/resource/yaml/nullObj.yaml";
        YamlWriter writer = new YamlWriter(new FileWriter(path), CONFIG);
        writer.write(null);
        writer.close();

        YamlReader reader = new YamlReader(new FileReader(path), CONFIG);
        Object read = reader.read();
        Assert.assertNull(read);
    }

    @Test
    public void test_number() throws IOException {
        String path = "test/resource/yaml/number.yaml";
        YamlWriter writer = new YamlWriter(new FileWriter(path), CONFIG);
        writer.write(123);
        writer.close();

        YamlReader reader = new YamlReader(new FileReader(path), CONFIG);
        Integer read = reader.read(Integer.class);
        Assert.assertEquals(new Integer(123), read);
    }

    @Test
    public void test_string() throws IOException {
        String path = "test/resource/yaml/string.yaml";
        YamlWriter writer = new YamlWriter(new FileWriter(path), CONFIG);
        writer.write("string");
        writer.close();

        YamlReader reader = new YamlReader(new FileReader(path), CONFIG);
        String read = reader.read(String.class);
        Assert.assertEquals("string", read);
    }

    @Test(expected = YamlException.class)
    public void test_exception() throws IOException {
        String path = "test/resource/yaml/exception.yaml";
        YamlWriter writer = new YamlWriter(new FileWriter(path));
        writer.write(new RuntimeException("exception"));
        writer.close();
    }

    @Test
    public void test_no_getter_object() throws IOException {
        String path = "test/resource/yaml/noGetterObject.yaml";
        NoGetterObject object = new NoGetterObject("NoGetterObject");
        YamlWriter writer = new YamlWriter(new FileWriter(path));
        writer.write(object);
        writer.close();
    }
}
