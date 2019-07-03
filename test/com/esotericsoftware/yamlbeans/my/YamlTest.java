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
        // 文件字段顺序与字段定义顺序相同
        writeConfig.setKeepBeanPropertyOrder(true);
        // 格式化输出
        //writeConfig.setCanonical(true);
        // 不换行
        writeConfig.setWrapColumn(Integer.MAX_VALUE);
        // 中文不转义
        writeConfig.setEscapeUnicode(false);
        // 总是输出类名
        writeConfig.setWriteClassname(YamlConfig.WriteClassName.ALWAYS);
    }

    @Test
    public void test_general_obj() throws IOException {
        People people = new People();
        people.setName("yukms");
        people.setAge(18);
        YamlWriter writer = new YamlWriter(new FileWriter("test/resource/yaml/generalObj.yaml"), CONFIG);
        writer.write(people);
        writer.close();

        YamlReader reader = new YamlReader(new FileReader("test/resource/yaml/generalObj.yaml"), CONFIG);
        People read = (People) reader.read();
        Assert.assertEquals("yukms", read.getName());
        Assert.assertEquals(18, read.getAge());
    }

    @Test
    public void test_subclass_obj() throws IOException {
        List<People> people = getSubclassesOfPeople();
        YamlWriter writer = new YamlWriter(new FileWriter("test/resource/yaml/subclassObj.yaml"), CONFIG);
        writer.write(people);
        writer.close();

        YamlReader reader = new YamlReader(new FileReader("test/resource/yaml/subclassObj.yaml"), CONFIG);
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
        Teacher teacher = new Teacher();
        teacher.setName("teacher");
        teacher.setAge(18);

        List<People> people = new ArrayList<>();
        people.add(teacher);
        people.add(teacher);
        YamlWriter writer = new YamlWriter(new FileWriter("test/resource/yaml/drawDot.yaml"), CONFIG);
        writer.write(people);
        writer.close();

        YamlReader reader = new YamlReader(new FileReader("test/resource/yaml/drawDot.yaml"), CONFIG);
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
        NullObject object = new NullObject();
        YamlWriter writer = new YamlWriter(new FileWriter("test/resource/yaml/nullField.yaml"), CONFIG);
        writer.write(object);
        writer.close();

        YamlReader reader = new YamlReader(new FileReader("test/resource/yaml/nullField.yaml"), CONFIG);
        NullObject read = reader.read(NullObject.class);
        Assert.assertNull(read.getObject());
    }

    @Test(expected = NullPointerException.class)
    public void test_null_obj() throws IOException {
        NullObject object = null;
        YamlWriter writer = new YamlWriter(new FileWriter("test/resource/yaml/nullObj.yaml"), CONFIG);
        writer.write(object);
        writer.close();
    }

    @Test(expected = YamlException.class)
    public void test_exception() throws IOException {
        YamlWriter writer = new YamlWriter(new FileWriter("test/resource/yaml/exception.yaml"));
        writer.write(new RuntimeException("exception"));
        writer.close();
    }

    @Test
    public void test_no_getter_object() throws IOException {
        NoGetterObject object = new NoGetterObject("NoGetterObject");
        YamlWriter writer = new YamlWriter(new FileWriter("test/resource/yaml/noGetterObject.yaml"));
        writer.write(object);
        writer.close();
    }
}
