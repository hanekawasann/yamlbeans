package com.yukms.yamlxbeans;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import junit.framework.TestCase;

public class BooleanTest extends TestCase {
    public void testBooleanBean() throws Exception {
        // Create a bean with a value differing from it's default
        BeanWithBoolean val = new BeanWithBoolean();
        val.setBool(true);
        val.setBoolObj(true);

        // Store the bean
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YamlxWriter yamlWriter = new YamlxWriter(new OutputStreamWriter(out));
        yamlWriter.write(val);
        yamlWriter.close();

        // Load the bean
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        YamlxReader yamlReader = new YamlxReader(new InputStreamReader(in));
        BeanWithBoolean got = yamlReader.read(BeanWithBoolean.class);

        assertEquals(val, got);
    }

    public static class BeanWithBoolean {
        private boolean bool = false;
        private Boolean boolObj = false;

        public boolean isBool() {
            return bool;
        }

        public void setBool(boolean bool) {
            this.bool = bool;
        }

        public Boolean getBoolObj() {
            return boolObj;
        }

        public void setBoolObj(Boolean boolObj) {
            this.boolObj = boolObj;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            BeanWithBoolean that = (BeanWithBoolean) o;

            if (bool != that.bool) { return false; }
            return !(boolObj != null ? !boolObj.equals(that.boolObj) : that.boolObj != null);

        }

        @Override
        public int hashCode() {
            int result = (bool ? 1 : 0);
            result = 31 * result + (boolObj != null ? boolObj.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "BeanWithBoolean{" + "bool=" + bool + ", boolObj=" + boolObj + '}';
        }
    }
}
