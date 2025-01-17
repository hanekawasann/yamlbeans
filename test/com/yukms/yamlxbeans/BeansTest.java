package com.yukms.yamlxbeans;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("synthetic-access")
public class BeansTest {

    private static final double DELTA = 0.0001;

    private YamlxConfig yamlConfig;

    @Before
    public void setup() throws Exception {
        yamlConfig = new YamlxConfig();
    }

    @Test
    public void isScalar() throws Exception {
        // Scalar Type
        assertTrue(Beans.isScalar(String.class));
        assertTrue(Beans.isScalar(Integer.class));
        assertTrue(Beans.isScalar(Boolean.class));
        assertTrue(Beans.isScalar(Float.class));
        assertTrue(Beans.isScalar(Long.class));
        assertTrue(Beans.isScalar(Double.class));
        assertTrue(Beans.isScalar(Short.class));
        assertTrue(Beans.isScalar(Byte.class));
        assertTrue(Beans.isScalar(Character.class));
        // Other Type
        assertFalse(Beans.isScalar(Void.class));
        assertFalse(Beans.isScalar(Number.class));
        assertFalse(Beans.isScalar(Beans.class));
        assertFalse(Beans.isScalar(YamlxReader.class));
        assertFalse(Beans.isScalar(YamlxWriter.class));
    }

    @Test
    public void getDeferredConstruction() throws Exception {
        DeferredConstruction construction = Beans.getDeferredConstruction(MockClass.class, yamlConfig);
        assertEquals(null, construction);
    }

    @Test
    public void getDeferredConstructionWithPrivateConstructor() throws Exception {
        DeferredConstruction construction = Beans
            .getDeferredConstruction(MockClassWithPrivateConstructor.class, yamlConfig);
        assertEquals(null, construction);
    }

    @Test
    public void createObject() throws Exception {
        MockClass mockClass = (MockClass) Beans.createObject(MockClass.class, false);
        MockClassWithPrivateConstructor mockClassWithPrivateConstructor = (MockClassWithPrivateConstructor) Beans
            .createObject(MockClassWithPrivateConstructor.class, true);
        List listCase = (List) Beans.createObject(List.class, false);
        Set setCase = (Set) Beans.createObject(Set.class, false);
        Map mapCase = (Map) Beans.createObject(Map.class, false);

        assertNotNull(mockClass);
        assertNotNull(mockClassWithPrivateConstructor);
        assertNotNull(listCase);
        assertNotNull(setCase);
        assertNotNull(mapCase);

        if (!(listCase instanceof ArrayList)) {
            fail();
        }
        if (!(setCase instanceof HashSet)) {
            fail();
        }
        if (!(mapCase instanceof HashMap)) {
            fail();
        }

        try {
            MockClassWithoutNoArgConstructor mockClassWithoutNoArgConstructor = (MockClassWithoutNoArgConstructor) Beans
                .createObject(MockClassWithoutNoArgConstructor.class, false);
            fail();
        } catch (InvocationTargetException e) {

        }
        try {
            mockClassWithPrivateConstructor = (MockClassWithPrivateConstructor) Beans
                .createObject(MockClassWithPrivateConstructor.class, false);
            fail();
        } catch (InvocationTargetException e) {

        }
    }

    @Test
    public void getPropertiesWithNullType() throws Exception {
        try {
            Beans.getProperties(null, false, false, yamlConfig);
            fail();
        } catch (IllegalArgumentException e) {
            // nothing
        }
    }

    @Test
    public void getPropertiesWithBeanProperties() throws Exception {
        final Set<Beans.Property> properties = Beans.getProperties(MockClass.class, true, false, yamlConfig);
        assertEquals(9, properties.size());
    }

    @Test
    public void getPropertiesWithPrivateFields() throws Exception {
        final Set<Beans.Property> properties = Beans.getProperties(MockClass.class, false, true, yamlConfig);
        assertEquals(9, properties.size());
    }

    @Test
    public void getPropertyWithNullType() throws Exception {
        try {
            Beans.getProperty(null, null, false, false, yamlConfig);
            fail();
        } catch (IllegalArgumentException e) {
            // nothing
        }
    }

    @Test
    public void getPropertyWithNullName() throws Exception {
        try {
            Beans.getProperty(MockClass.class, null, false, false, yamlConfig);
            fail();
        } catch (IllegalArgumentException e) {
            // nothing
        }
    }

    @Test
    public void getPropertyWithEmptyName() throws Exception {
        try {
            Beans.getProperty(MockClass.class, "", false, false, yamlConfig);
            fail();
        } catch (IllegalArgumentException e) {
            // nothing
        }

    }

    @Test
    public void getPropertyBooleanTypeWithBeanProperties() throws Exception {
        final Beans.Property booleanTypeProperty = Beans
            .getProperty(MockClass.class, "booleanType", true, false, yamlConfig);

        assertTrue(booleanTypeProperty instanceof Beans.MethodProperty);
        assertEquals(MockClass.class, booleanTypeProperty.getDeclaringClass());
        assertEquals("booleanType", booleanTypeProperty.getName());
        assertEquals(boolean.class, booleanTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(false, mockClass.booleanType);

        booleanTypeProperty.set(mockClass, true);
        assertEquals(true, booleanTypeProperty.get(mockClass));

        booleanTypeProperty.set(mockClass, false);
        assertEquals(false, booleanTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyCharTypeWithBeanProperties() throws Exception {
        final Beans.Property charTypeProperty = Beans.getProperty(MockClass.class, "charType", true, false, yamlConfig);

        assertTrue(charTypeProperty instanceof Beans.MethodProperty);
        assertEquals(MockClass.class, charTypeProperty.getDeclaringClass());
        assertEquals("charType", charTypeProperty.getName());
        assertEquals(char.class, charTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(0, mockClass.charType);

        charTypeProperty.set(mockClass, 'a');
        assertEquals('a', charTypeProperty.get(mockClass));

        charTypeProperty.set(mockClass, Character.MAX_VALUE);
        assertEquals(Character.MAX_VALUE, charTypeProperty.get(mockClass));

        charTypeProperty.set(mockClass, Character.MIN_VALUE);
        assertEquals(Character.MIN_VALUE, charTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyByteTypeWithBeanProperties() throws Exception {
        final Beans.Property byteTypeProperty = Beans.getProperty(MockClass.class, "byteType", true, false, yamlConfig);

        assertTrue(byteTypeProperty instanceof Beans.MethodProperty);
        assertEquals(MockClass.class, byteTypeProperty.getDeclaringClass());
        assertEquals("byteType", byteTypeProperty.getName());
        assertEquals(byte.class, byteTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(0, mockClass.byteType);

        byteTypeProperty.set(mockClass, (byte) 1);
        assertEquals((byte) 1, byteTypeProperty.get(mockClass));

        byteTypeProperty.set(mockClass, Byte.MAX_VALUE);
        assertEquals(Byte.MAX_VALUE, byteTypeProperty.get(mockClass));

        byteTypeProperty.set(mockClass, Byte.MIN_VALUE);
        assertEquals(Byte.MIN_VALUE, byteTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyShortTypeWithBeanProperties() throws Exception {
        final Beans.Property shortTypeProperty = Beans
            .getProperty(MockClass.class, "shortType", true, false, yamlConfig);

        assertTrue(shortTypeProperty instanceof Beans.MethodProperty);
        assertEquals(MockClass.class, shortTypeProperty.getDeclaringClass());
        assertEquals("shortType", shortTypeProperty.getName());
        assertEquals(short.class, shortTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(0, mockClass.shortType);

        shortTypeProperty.set(mockClass, (short) 1);
        assertEquals((short) 1, shortTypeProperty.get(mockClass));

        shortTypeProperty.set(mockClass, Short.MAX_VALUE);
        assertEquals(Short.MAX_VALUE, shortTypeProperty.get(mockClass));

        shortTypeProperty.set(mockClass, Short.MIN_VALUE);
        assertEquals(Short.MIN_VALUE, shortTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyIntTypeWithBeanProperties() throws Exception {
        final Beans.Property intTypeProperty = Beans.getProperty(MockClass.class, "intType", true, false, yamlConfig);

        assertTrue(intTypeProperty instanceof Beans.MethodProperty);
        assertEquals(MockClass.class, intTypeProperty.getDeclaringClass());
        assertEquals("intType", intTypeProperty.getName());
        assertEquals(int.class, intTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(0, mockClass.intType);

        intTypeProperty.set(mockClass, 1);
        assertEquals(1, intTypeProperty.get(mockClass));

        intTypeProperty.set(mockClass, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, intTypeProperty.get(mockClass));

        intTypeProperty.set(mockClass, Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, intTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyLongTypeWithBeanProperties() throws Exception {
        final Beans.Property longTypeProperty = Beans.getProperty(MockClass.class, "longType", true, false, yamlConfig);

        assertTrue(longTypeProperty instanceof Beans.MethodProperty);
        assertEquals(MockClass.class, longTypeProperty.getDeclaringClass());
        assertEquals("longType", longTypeProperty.getName());
        assertEquals(long.class, longTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(0, mockClass.longType);

        longTypeProperty.set(mockClass, 1l);
        assertEquals(1l, longTypeProperty.get(mockClass));

        longTypeProperty.set(mockClass, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, longTypeProperty.get(mockClass));

        longTypeProperty.set(mockClass, Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, longTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyFloatTypeWithBeanProperties() throws Exception {
        final Beans.Property floatTypeProperty = Beans
            .getProperty(MockClass.class, "floatType", true, false, yamlConfig);

        assertTrue(floatTypeProperty instanceof Beans.MethodProperty);
        assertEquals(MockClass.class, floatTypeProperty.getDeclaringClass());
        assertEquals("floatType", floatTypeProperty.getName());
        assertEquals(float.class, floatTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals((float) 0, mockClass.floatType, DELTA);

        floatTypeProperty.set(mockClass, (float) 1.0);
        assertEquals((float) 1.0, floatTypeProperty.get(mockClass));

        floatTypeProperty.set(mockClass, Float.MAX_VALUE);
        assertEquals(Float.MAX_VALUE, floatTypeProperty.get(mockClass));

        floatTypeProperty.set(mockClass, Float.MIN_VALUE);
        assertEquals(Float.MIN_VALUE, floatTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyDoubleTypeWithBeanProperties() throws Exception {
        final Beans.Property doubleTypeProperty = Beans
            .getProperty(MockClass.class, "doubleType", true, false, yamlConfig);

        assertTrue(doubleTypeProperty instanceof Beans.MethodProperty);
        assertEquals(MockClass.class, doubleTypeProperty.getDeclaringClass());
        assertEquals("doubleType", doubleTypeProperty.getName());
        assertEquals(double.class, doubleTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals((double) 0, mockClass.doubleType, DELTA);

        doubleTypeProperty.set(mockClass, (double) 1.0);
        assertEquals((double) 1.0, doubleTypeProperty.get(mockClass));

        doubleTypeProperty.set(mockClass, Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, doubleTypeProperty.get(mockClass));

        doubleTypeProperty.set(mockClass, Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, doubleTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyStringTypeWithBeanProperties() throws Exception {
        final Beans.Property stringTypeProperty = Beans
            .getProperty(MockClass.class, "stringType", true, false, yamlConfig);

        assertTrue(stringTypeProperty instanceof Beans.MethodProperty);
        assertEquals(MockClass.class, stringTypeProperty.getDeclaringClass());
        assertEquals("stringType", stringTypeProperty.getName());
        assertEquals(String.class, stringTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(null, mockClass.stringType);

        stringTypeProperty.set(mockClass, "4cho");
        assertEquals("4cho", stringTypeProperty.get(mockClass));

        stringTypeProperty.set(mockClass, "ghkim");
        assertEquals("ghkim", stringTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyNullWithBeanProperties() throws Exception {
        final Beans.Property nullProperty = Beans.getProperty(MockClass.class, "nullType", true, false, yamlConfig);

        assertNull(nullProperty);
    }

    @Test
    public void getPropertyBooleanTypeWithPrivateFields() throws Exception {
        final Beans.Property booleanTypeProperty = Beans.getProperty(MockClass.class, "booleanType", false, true, null);

        assertTrue(booleanTypeProperty instanceof Beans.FieldProperty);
        assertEquals(MockClass.class, booleanTypeProperty.getDeclaringClass());
        assertEquals("booleanType", booleanTypeProperty.getName());
        assertEquals(boolean.class, booleanTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(false, mockClass.booleanType);

        booleanTypeProperty.set(mockClass, true);
        assertEquals(true, booleanTypeProperty.get(mockClass));

        booleanTypeProperty.set(mockClass, false);
        assertEquals(false, booleanTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyCharTypeWithPrivateFields() throws Exception {
        final Beans.Property charTypeProperty = Beans.getProperty(MockClass.class, "charType", false, true, yamlConfig);

        assertTrue(charTypeProperty instanceof Beans.FieldProperty);
        assertEquals(MockClass.class, charTypeProperty.getDeclaringClass());
        assertEquals("charType", charTypeProperty.getName());
        assertEquals(char.class, charTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(0, mockClass.charType);

        charTypeProperty.set(mockClass, 'a');
        assertEquals('a', charTypeProperty.get(mockClass));

        charTypeProperty.set(mockClass, Character.MAX_VALUE);
        assertEquals(Character.MAX_VALUE, charTypeProperty.get(mockClass));

        charTypeProperty.set(mockClass, Character.MIN_VALUE);
        assertEquals(Character.MIN_VALUE, charTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyByteTypeWithPrivateFields() throws Exception {
        final Beans.Property byteTypeProperty = Beans.getProperty(MockClass.class, "byteType", false, true, yamlConfig);

        assertTrue(byteTypeProperty instanceof Beans.FieldProperty);
        assertEquals(MockClass.class, byteTypeProperty.getDeclaringClass());
        assertEquals("byteType", byteTypeProperty.getName());
        assertEquals(byte.class, byteTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(0, mockClass.byteType);

        byteTypeProperty.set(mockClass, (byte) 1);
        assertEquals((byte) 1, byteTypeProperty.get(mockClass));

        byteTypeProperty.set(mockClass, Byte.MAX_VALUE);
        assertEquals(Byte.MAX_VALUE, byteTypeProperty.get(mockClass));

        byteTypeProperty.set(mockClass, Byte.MIN_VALUE);
        assertEquals(Byte.MIN_VALUE, byteTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyShortTypeWithPrivateFields() throws Exception {
        final Beans.Property shortTypeProperty = Beans
            .getProperty(MockClass.class, "shortType", false, true, yamlConfig);

        assertTrue(shortTypeProperty instanceof Beans.FieldProperty);
        assertEquals(MockClass.class, shortTypeProperty.getDeclaringClass());
        assertEquals("shortType", shortTypeProperty.getName());
        assertEquals(short.class, shortTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(0, mockClass.shortType);

        shortTypeProperty.set(mockClass, (short) 1);
        assertEquals((short) 1, shortTypeProperty.get(mockClass));

        shortTypeProperty.set(mockClass, Short.MAX_VALUE);
        assertEquals(Short.MAX_VALUE, shortTypeProperty.get(mockClass));

        shortTypeProperty.set(mockClass, Short.MIN_VALUE);
        assertEquals(Short.MIN_VALUE, shortTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyIntTypeWithPrivateFields() throws Exception {
        final Beans.Property intTypeProperty = Beans.getProperty(MockClass.class, "intType", false, true, yamlConfig);

        assertTrue(intTypeProperty instanceof Beans.FieldProperty);
        assertEquals(MockClass.class, intTypeProperty.getDeclaringClass());
        assertEquals("intType", intTypeProperty.getName());
        assertEquals(int.class, intTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(0, mockClass.intType);

        intTypeProperty.set(mockClass, 1);
        assertEquals(1, intTypeProperty.get(mockClass));

        intTypeProperty.set(mockClass, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, intTypeProperty.get(mockClass));

        intTypeProperty.set(mockClass, Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, intTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyLongTypeWithPrivateFields() throws Exception {
        final Beans.Property longTypeProperty = Beans.getProperty(MockClass.class, "longType", false, true, yamlConfig);

        assertTrue(longTypeProperty instanceof Beans.FieldProperty);
        assertEquals(MockClass.class, longTypeProperty.getDeclaringClass());
        assertEquals("longType", longTypeProperty.getName());
        assertEquals(long.class, longTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(0, mockClass.longType);

        longTypeProperty.set(mockClass, 1l);
        assertEquals(1l, longTypeProperty.get(mockClass));

        longTypeProperty.set(mockClass, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, longTypeProperty.get(mockClass));

        longTypeProperty.set(mockClass, Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, longTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyFloatTypeWithPrivateFields() throws Exception {
        final Beans.Property floatTypeProperty = Beans
            .getProperty(MockClass.class, "floatType", false, true, yamlConfig);

        assertTrue(floatTypeProperty instanceof Beans.FieldProperty);
        assertEquals(MockClass.class, floatTypeProperty.getDeclaringClass());
        assertEquals("floatType", floatTypeProperty.getName());
        assertEquals(float.class, floatTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals((float) 0, mockClass.floatType, DELTA);

        floatTypeProperty.set(mockClass, (float) 1.0);
        assertEquals((float) 1.0, floatTypeProperty.get(mockClass));

        floatTypeProperty.set(mockClass, Float.MAX_VALUE);
        assertEquals(Float.MAX_VALUE, floatTypeProperty.get(mockClass));

        floatTypeProperty.set(mockClass, Float.MIN_VALUE);
        assertEquals(Float.MIN_VALUE, floatTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyDoubleTypeWithPrivateFields() throws Exception {
        final Beans.Property doubleTypeProperty = Beans
            .getProperty(MockClass.class, "doubleType", false, true, yamlConfig);

        assertTrue(doubleTypeProperty instanceof Beans.FieldProperty);
        assertEquals(MockClass.class, doubleTypeProperty.getDeclaringClass());
        assertEquals("doubleType", doubleTypeProperty.getName());
        assertEquals(double.class, doubleTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals((double) 0, mockClass.doubleType, DELTA);

        doubleTypeProperty.set(mockClass, (double) 1.0);
        assertEquals((double) 1.0, doubleTypeProperty.get(mockClass));

        doubleTypeProperty.set(mockClass, Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, doubleTypeProperty.get(mockClass));

        doubleTypeProperty.set(mockClass, Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, doubleTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyStringTypeWithPrivateFields() throws Exception {
        final Beans.Property stringTypeProperty = Beans
            .getProperty(MockClass.class, "stringType", false, true, yamlConfig);

        assertTrue(stringTypeProperty instanceof Beans.FieldProperty);
        assertEquals(MockClass.class, stringTypeProperty.getDeclaringClass());
        assertEquals("stringType", stringTypeProperty.getName());
        assertEquals(String.class, stringTypeProperty.getType());

        MockClass mockClass = new MockClass();
        assertEquals(null, mockClass.stringType);

        stringTypeProperty.set(mockClass, "4cho");
        assertEquals("4cho", stringTypeProperty.get(mockClass));

        stringTypeProperty.set(mockClass, "ghkim");
        assertEquals("ghkim", stringTypeProperty.get(mockClass));
    }

    @Test
    public void getPropertyNullWithPrivateFields() throws Exception {
        final Beans.Property nullProperty = Beans.getProperty(MockClass.class, "nullType", false, true, yamlConfig);

        assertNull(nullProperty);
    }

    private static class MockClass {

        private boolean booleanType;
        private char charType;
        private byte byteType;
        private short shortType;
        private int intType;
        private long longType;
        private float floatType;
        private double doubleType;
        private String stringType;

        public MockClass() {

        }

        public boolean isBooleanType() {
            return booleanType;
        }

        public void setBooleanType(boolean booleanType) {
            this.booleanType = booleanType;
        }

        public char getCharType() {
            return charType;
        }

        public void setCharType(char charType) {
            this.charType = charType;
        }

        public byte getByteType() {
            return byteType;
        }

        public void setByteType(byte byteType) {
            this.byteType = byteType;
        }

        public short getShortType() {
            return shortType;
        }

        public void setShortType(short shortType) {
            this.shortType = shortType;
        }

        public int getIntType() {
            return intType;
        }

        public void setIntType(int intType) {
            this.intType = intType;
        }

        public long getLongType() {
            return longType;
        }

        public void setLongType(long longType) {
            this.longType = longType;
        }

        public float getFloatType() {
            return floatType;
        }

        public void setFloatType(float floatType) {
            this.floatType = floatType;
        }

        public double getDoubleType() {
            return doubleType;
        }

        public void setDoubleType(double doubleType) {
            this.doubleType = doubleType;
        }

        public String getStringType() {
            return stringType;
        }

        public void setStringType(String stringType) {
            this.stringType = stringType;
        }
    }

    private static class MockClassWithPrivateConstructor {

        private boolean booleanType;
        private char charType;
        private byte byteType;
        private short shortType;
        private int intType;
        private long longType;
        private float floatType;
        private double doubleType;
        private String stringType;

        private MockClassWithPrivateConstructor() {

        }

        public boolean isBooleanType() {
            return booleanType;
        }

        public void setBooleanType(boolean booleanType) {
            this.booleanType = booleanType;
        }

        public char getCharType() {
            return charType;
        }

        public void setCharType(char charType) {
            this.charType = charType;
        }

        public byte getByteType() {
            return byteType;
        }

        public void setByteType(byte byteType) {
            this.byteType = byteType;
        }

        public short getShortType() {
            return shortType;
        }

        public void setShortType(short shortType) {
            this.shortType = shortType;
        }

        public int getIntType() {
            return intType;
        }

        public void setIntType(int intType) {
            this.intType = intType;
        }

        public long getLongType() {
            return longType;
        }

        public void setLongType(long longType) {
            this.longType = longType;
        }

        public float getFloatType() {
            return floatType;
        }

        public void setFloatType(float floatType) {
            this.floatType = floatType;
        }

        public double getDoubleType() {
            return doubleType;
        }

        public void setDoubleType(double doubleType) {
            this.doubleType = doubleType;
        }

        public String getStringType() {
            return stringType;
        }

        public void setStringType(String stringType) {
            this.stringType = stringType;
        }
    }

    private static class MockClassWithoutNoArgConstructor {

        private Object anyType;

        public MockClassWithoutNoArgConstructor(Object anyType) {
            this.anyType = anyType;
        }
    }
}
