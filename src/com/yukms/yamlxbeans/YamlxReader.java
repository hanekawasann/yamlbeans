/*
 * Copyright (c) 2008 Nathan Sweet
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.yukms.yamlxbeans;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.yukms.yamlxbeans.Beans.Property;
import com.yukms.yamlxbeans.parser.AliasEvent;
import com.yukms.yamlxbeans.parser.CollectionStartEvent;
import com.yukms.yamlxbeans.parser.Event;
import com.yukms.yamlxbeans.parser.Parser;
import com.yukms.yamlxbeans.parser.ScalarEvent;
import com.yukms.yamlxbeans.scalar.ScalarSerializer;
import com.yukms.yamlxbeans.parser.EventType;
import com.yukms.yamlxbeans.tokenizer.Tokenizer;

/**
 * Deserializes Java objects from YAML.
 *
 * @author <a href="mailto:misc@n4te.com">Nathan Sweet</a>
 */
public class YamlxReader {
    private final YamlxConfig config;
    private Parser parser;
    private final Map<String, Object> anchors = new HashMap<>();

    public YamlxReader(Reader reader) {
        this(reader, new YamlxConfig());
    }

    public YamlxReader(Reader reader, YamlxConfig config) {
        this.config = config;
        parser = new Parser(reader, config.readConfig.defaultVersion);
    }

    public YamlxReader(String yaml) {
        this(new StringReader(yaml));
    }

    public YamlxReader(String yaml, YamlxConfig config) {
        this(new StringReader(yaml), config);
    }

    public YamlxConfig getConfig() {
        return config;
    }

    /**
     * Return the object with the given alias, or null. This is only valid after objects have been read and before
     * {@link #close()}
     */
    public Object get(String alias) {
        return anchors.get(alias);
    }

    public void close() throws IOException {
        parser.close();
        anchors.clear();
    }

    /**
     * Reads the next YAML document and deserializes it into an object. The type of object is defined by the YAML tag. If there is
     * no YAML tag, the object will be an {@link ArrayList}, {@link HashMap}, or String.
     */
    public Object read() throws YamlxException {
        return read(null);
    }

    /**
     * Reads an object of the specified type from YAML.
     *
     * @param type The type of object to read. If null, behaves the same as {{@link #read()}.
     */
    public <T> T read(Class<T> type) throws YamlxException {
        return read(type, null);
    }

    /**
     * Reads an array, Map, List, or Collection object of the specified type from YAML, using the specified element type.
     *
     * @param type The type of object to read. If null, behaves the same as {{@link #read()}.
     */
    public <T> T read(Class<T> type, Class elementType) throws YamlxException {
        try {
            while (true) {
                Event event = parser.getNextEvent();
                if (event == null) {
                    return null;
                }
                if (event.type == EventType.STREAM_END) {
                    return null;
                }
                if (event.type == EventType.DOCUMENT_START) {
                    break;
                }
            }
            return (T) readValue(type, elementType, null);
        } catch (Parser.ParserException ex) {
            throw new YamlxException("Error parsing YAML.", ex);
        } catch (Tokenizer.TokenizerException ex) {
            throw new YamlxException("Error tokenizing YAML.", ex);
        }
    }

    /** Reads an object from the YAML. Can be overidden to take some action for any of the objects returned. */
    protected Object readValue(Class type, Class elementType, Class defaultType)
        throws YamlxException, Parser.ParserException, Tokenizer.TokenizerException {
        String tag = null;
        String anchor = null;
        Event event = parser.peekNextEvent();

        switch (event.type) {
            case ALIAS:
                parser.getNextEvent();
                anchor = ((AliasEvent) event).anchor;
                Object value = anchors.get(anchor);
                if (value == null) {
                    throw new YamlReaderException("Unknown anchor: " + anchor);
                }
                return value;
            case MAPPING_START:
            case SEQUENCE_START:
                tag = ((CollectionStartEvent) event).tag;
                anchor = ((CollectionStartEvent) event).anchor;
                break;
            case SCALAR:
                tag = ((ScalarEvent) event).tag;
                anchor = ((ScalarEvent) event).anchor;
                break;
            default:
        }

        return readValueInternal(this.chooseType(tag, defaultType, type), elementType, anchor);
    }

    private Class<?> chooseType(String tag, Class<?> defaultType, Class<?> providedType) throws YamlReaderException {
        if (tag != null && config.readConfig.classTags) {
            Class<?> userConfiguredByTag = config.tagToClass.get(tag);
            if (userConfiguredByTag != null) {
                return userConfiguredByTag;
            }

            ClassLoader classLoader = (config.readConfig.classLoader == null ? this.getClass().getClassLoader()
                : config.readConfig.classLoader);

            try {
                Class<?> loadedFromTag = findTagClass(tag, classLoader);
                if (loadedFromTag != null) {
                    return loadedFromTag;
                }
            } catch (ClassNotFoundException e) {
                throw new YamlReaderException("Unable to find class specified by tag: " + tag);
            }
        }

        if (defaultType != null) {
            return defaultType;
        }

        // This may be null.
        return providedType;
    }

    /**
     * Used during reading when a tag is present, and {@link YamlxConfig#setClassTag(String, Class)} was not used for that tag.
     * Attempts to load the class corresponding to that tag.
     * <p>
     * If this returns a non-null Class, that will be used as the deserialization type regardless of whether a type was explicitly
     * asked for or if a default type exists.
     * <p>
     * If this returns null, no guidance will be provided by the tag and we will fall back to the default type or a requested
     * target type, if any exist.
     * <p>
     * If this throws a ClassNotFoundException, parsing will fail.
     * <p>
     * The default implementation is simply
     *
     * <pre>
     *  {@code Class.forName(tag, true, classLoader);}
     * </pre>
     * <p>
     * and never returns null.
     * <p>
     * You can override this to handle cases where you do not want to respect the type tags found in a document - e.g., if they
     * were output by another program using classes that do not exist on your classpath.
     */
    protected Class<?> findTagClass(String tag, ClassLoader classLoader) throws ClassNotFoundException {
        return Class.forName(tag, true, classLoader);
    }

    private Object readValueInternal(Class type, Class elementType, String anchor)
        throws YamlxException, Parser.ParserException, Tokenizer.TokenizerException {
        if (type == null || type == Object.class) {
            Event event = parser.peekNextEvent();
            switch (event.type) {
                case MAPPING_START:
                    type = LinkedHashMap.class;
                    break;
                case SCALAR:
                    if (config.readConfig.guessNumberTypes) {
                        String value = ((ScalarEvent) event).value;
                        if (value != null) {
                            try {
                                Integer convertedValue = Integer.decode(value);
                                if (anchor != null) {
                                    anchors.put(anchor, convertedValue);
                                }
                                parser.getNextEvent();
                                return convertedValue;
                            } catch (NumberFormatException ignored) { }
                            try {
                                Float convertedValue = Float.valueOf(value);
                                if (anchor != null) {
                                    anchors.put(anchor, convertedValue);
                                }
                                parser.getNextEvent();
                                return convertedValue;
                            } catch (NumberFormatException ignored) { }
                        }
                    }
                    type = String.class;
                    break;
                case SEQUENCE_START:
                    type = ArrayList.class;
                    break;
                default:
                    throw new YamlReaderException("Expected scalar, sequence, or mapping but found: " + event.type);
            }
        }

        if (type == String.class) {
            Event event = parser.getNextEvent();
            if (event.type != EventType.SCALAR) {
                throw new YamlReaderException("Expected scalar for String type but found: " + event.type);
            }
            String value = ((ScalarEvent) event).value;
            if (anchor != null) {
                anchors.put(anchor, value);
            }
            return value;
        }

        if (Beans.isScalar(type)) {
            Event event = parser.getNextEvent();
            if (event.type != EventType.SCALAR) {
                throw new YamlReaderException(
                    "Expected scalar for primitive type '" + type + "' but found: " + event.type);
            }
            String value = ((ScalarEvent) event).value;
            try {
                Object convertedValue;
                if (type == Integer.TYPE) {
                    convertedValue = value.length() == 0 ? 0 : Integer.decode(value);
                } else if (type == Integer.class) {
                    convertedValue = value.length() == 0 ? null : Integer.decode(value);
                } else if (type == Boolean.TYPE) {
                    convertedValue = value.length() == 0 ? false : Boolean.valueOf(value);
                } else if (type == Boolean.class) {
                    convertedValue = value.length() == 0 ? null : Boolean.valueOf(value);
                } else if (type == Float.TYPE) {
                    convertedValue = value.length() == 0 ? 0 : Float.valueOf(value);
                } else if (type == Float.class) {
                    convertedValue = value.length() == 0 ? null : Float.valueOf(value);
                } else if (type == Double.TYPE) {
                    convertedValue = value.length() == 0 ? 0 : Double.valueOf(value);
                } else if (type == Double.class) {
                    convertedValue = value.length() == 0 ? null : Double.valueOf(value);
                } else if (type == Long.TYPE) {
                    convertedValue = value.length() == 0 ? 0 : Long.decode(value);
                } else if (type == Long.class) {
                    convertedValue = value.length() == 0 ? null : Long.decode(value);
                } else if (type == Short.TYPE) {
                    convertedValue = value.length() == 0 ? 0 : Short.decode(value);
                } else if (type == Short.class) {
                    convertedValue = value.length() == 0 ? null : Short.decode(value);
                } else if (type == Character.TYPE) {
                    convertedValue = value.length() == 0 ? 0 : value.charAt(0);
                } else if (type == Character.class) {
                    convertedValue = value.length() == 0 ? null : value.charAt(0);
                } else if (type == Byte.TYPE) {
                    convertedValue = value.length() == 0 ? 0 : Byte.decode(value);
                } else if (type == Byte.class) {
                    convertedValue = value.length() == 0 ? null : Byte.decode(value);
                } else {
                    throw new YamlxException("Unknown field type.");
                }
                if (anchor != null) {
                    anchors.put(anchor, convertedValue);
                }
                return convertedValue;
            } catch (Exception ex) {
                throw new YamlReaderException("Unable to convert value to required type \"" + type + "\": " + value,
                    ex);
            }
        }

        if (Enum.class.isAssignableFrom(type)) {
            Event event = parser.getNextEvent();
            if (event.type != EventType.SCALAR) {
                throw new YamlReaderException("Expected scalar for enum type but found: " + event.type);
            }
            String enumValueName = ((ScalarEvent) event).value;
            if (enumValueName.length() == 0) {
                return null;
            }
            try {
                return Enum.valueOf(type, enumValueName);
            } catch (Exception ex) {
                throw new YamlReaderException(
                    "Unable to find enum value '" + enumValueName + "' for enum class: " + type.getName());
            }
        }

        for (Entry<Class, ScalarSerializer> entry : config.scalarSerializers.entrySet()) {
            if (entry.getKey().isAssignableFrom(type)) {
                ScalarSerializer serializer = entry.getValue();
                Event event = parser.getNextEvent();
                if (event.type != EventType.SCALAR) {
                    throw new YamlReaderException(
                        "Expected scalar for type '" + type + "' to be deserialized by scalar serializer '" +
                            serializer.getClass().getName() + "' but found: " + event.type);
                }
                Object value = serializer.read(((ScalarEvent) event).value);
                if (anchor != null) {
                    anchors.put(anchor, value);
                }
                return value;
            }
        }

        Event event = parser.peekNextEvent();
        switch (event.type) {
            case MAPPING_START: {
                // Must be a map or an object.
                event = parser.getNextEvent();
                Object object;
                try {
                    object = createObject(type);
                } catch (InvocationTargetException ex) {
                    throw new YamlReaderException("Error creating object.", ex);
                }
                if (anchor != null) {
                    anchors.put(anchor, object);
                }
                List<Object> keys = new ArrayList<>();
                while (true) {
                    if (parser.peekNextEvent().type == EventType.MAPPING_END) {
                        parser.getNextEvent();
                        break;
                    }
                    Object key = readValue(null, null, null);
                    // Explicit key/value pairs (using "? key\n: value\n") will come back as a map.
                    boolean isExplicitKey = key instanceof Map;
                    Object value = null;
                    if (isExplicitKey) {
                        Entry nameValuePair = (Entry) ((Map) key).entrySet().iterator().next();
                        key = nameValuePair.getKey();
                        value = nameValuePair.getValue();
                    }
                    if (object instanceof Map) {
                        // Add to map.
                        if (config.tagSuffix != null) {
                            Event nextEvent = parser.peekNextEvent();
                            switch (nextEvent.type) {
                                case MAPPING_START:
                                case SEQUENCE_START:
                                    ((Map) object).put(key + config.tagSuffix, ((CollectionStartEvent) nextEvent).tag);
                                    break;
                                case SCALAR:
                                    ((Map) object).put(key + config.tagSuffix, ((ScalarEvent) nextEvent).tag);
                                    break;
                            }
                        }
                        if (!isExplicitKey) {
                            value = readValue(elementType, null, null);
                        }
                        if (!config.allowDuplicates && ((Map) object).containsKey(key)) {
                            throw new YamlReaderException("Duplicate key found '" + key + "'");
                        }
                        if (config.readConfig.autoMerge && "<<".equals(key) && value != null) {
                            mergeMap((Map) object, value);
                        } else {
                            ((Map) object).put(key, value);
                        }
                    } else {
                        // Set field on object.
                        try {
                            if (!config.allowDuplicates && keys.contains(key)) {
                                throw new YamlReaderException("Duplicate key found '" + key + "'");
                            }
                            keys.add(key);

                            Property property = Beans
                                .getProperty(type, (String) key, config.beanProperties, config.privateFields, config);
                            if (property == null) {
                                if (config.readConfig.ignoreUnknownProperties) {
                                    continue;
                                }
                                throw new YamlReaderException(
                                    "Unable to find property '" + key + "' on class: " + type.getName());
                            }
                            Class propertyElementType = config.propertyToElementType.get(property);
                            if (propertyElementType == null) {
                                propertyElementType = property.getElementType();
                            }
                            Class propertyDefaultType = config.propertyToDefaultType.get(property);
                            if (!isExplicitKey) {
                                value = readValue(property.getType(), propertyElementType, propertyDefaultType);
                            }
                            property.set(object, value);
                        } catch (Exception ex) {
                            if (ex instanceof YamlReaderException) {
                                throw (YamlReaderException) ex;
                            }
                            throw new YamlReaderException(
                                "Error setting property '" + key + "' on class: " + type.getName(), ex);
                        }
                    }
                }
                if (object instanceof DeferredConstruction) {
                    try {
                        object = ((DeferredConstruction) object).construct();
                        if (anchor != null) {
                            anchors.put(anchor, object); // Update anchor with real object.
                        }
                    } catch (InvocationTargetException ex) {
                        throw new YamlReaderException("Error creating object.", ex);
                    }
                }
                return object;
            }
            case SEQUENCE_START: {
                // Must be a collection or an array.
                event = parser.getNextEvent();
                Collection collection;
                if (Collection.class.isAssignableFrom(type)) {
                    try {
                        collection = (Collection) Beans.createObject(type, config.privateConstructors);
                    } catch (InvocationTargetException ex) {
                        throw new YamlReaderException("Error creating object.", ex);
                    }
                } else if (type.isArray()) {
                    collection = new ArrayList();
                    elementType = type.getComponentType();
                } else {
                    throw new YamlReaderException("A sequence is not a valid value for the type: " + type.getName());
                }
                if (!type.isArray() && anchor != null) {
                    anchors.put(anchor, collection);
                }
                while (true) {
                    event = parser.peekNextEvent();
                    if (event.type == EventType.SEQUENCE_END) {
                        parser.getNextEvent();
                        break;
                    }
                    collection.add(readValue(elementType, null, null));
                }
                if (!type.isArray()) {
                    return collection;
                }
                Object array = Array.newInstance(elementType, collection.size());
                int i = 0;
                for (Object object : collection) {
                    Array.set(array, i++, object);
                }
                if (anchor != null) {
                    anchors.put(anchor, array);
                }
                return array;
            }
            case SCALAR:
                // Interpret an empty scalar as null.
                if (((ScalarEvent) event).value == null || ((ScalarEvent) event).value.length() == 0) {
                    event = parser.getNextEvent();
                    return null;
                }
                // Fall through.
            default:
                throw new YamlReaderException(
                    "Expected data for a " + type.getName() + " field but found: " + event.type);
        }
    }

    /** see http://yaml.org/type/merge.html */
    @SuppressWarnings("unchecked")
    private void mergeMap(Map<String, Object> dest, Object source) throws YamlReaderException {
        if (source instanceof Collection) {
            for (Object item : ((Collection<Object>) source)) { mergeMap(dest, item); }
        } else if (source instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) source;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!dest.containsKey(entry.getKey())) { dest.put(entry.getKey(), entry.getValue()); }

            }
        } else {
            throw new YamlReaderException(
                "Expected a mapping or a sequence of mappings for a '<<' merge field but found: " +
                    source.getClass().getSimpleName());
        }

    }

    /** Returns a new object of the requested type. */
    protected Object createObject(Class type) throws InvocationTargetException {
        // Use deferred construction if a non-zero-arg constructor is available.
        DeferredConstruction deferredConstruction = Beans.getDeferredConstruction(type, config);
        if (deferredConstruction != null) { return deferredConstruction; }
        return Beans.createObject(type, config.privateConstructors);
    }

    public class YamlReaderException extends YamlxException {
        public YamlReaderException(String message, Throwable cause) {
            super("Line " + parser.getLineNumber() + ", column " + parser.getColumn() + ": " + message, cause);
        }

        public YamlReaderException(String message) {
            this(message, null);
        }
    }

    public static void main(String[] args) throws Exception {
        YamlxReader reader = new YamlxReader(new FileReader("test/test.yml"));
        System.out.println(reader.read());
    }
}
