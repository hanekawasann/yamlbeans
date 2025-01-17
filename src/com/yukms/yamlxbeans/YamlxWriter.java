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

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.yukms.yamlxbeans.Beans.Property;
import com.yukms.yamlxbeans.YamlxConfig.WriteClassName;
import com.yukms.yamlxbeans.YamlxConfig.WriteConfig;
import com.yukms.yamlxbeans.document.YamlElement;
import com.yukms.yamlxbeans.emitter.Emitter;
import com.yukms.yamlxbeans.emitter.EmitterException;
import com.yukms.yamlxbeans.parser.AliasEvent;
import com.yukms.yamlxbeans.parser.DocumentEndEvent;
import com.yukms.yamlxbeans.parser.DocumentStartEvent;
import com.yukms.yamlxbeans.parser.Event;
import com.yukms.yamlxbeans.parser.MappingStartEvent;
import com.yukms.yamlxbeans.parser.ScalarEvent;
import com.yukms.yamlxbeans.parser.SequenceStartEvent;
import com.yukms.yamlxbeans.scalar.ScalarSerializer;

/**
 * Serializes Java objects as YAML.
 *
 * @author <a href="mailto:misc@n4te.com">Nathan Sweet</a>
 */
public class YamlxWriter {
    private final YamlxConfig config;
    private final Emitter emitter;
    private boolean started;
    private Map<Class, Object> defaultValuePrototypes = new IdentityHashMap<>();
    /** 需要保存的对象 */
    private final List<Object> queuedObjects = new ArrayList<>();
    /** 锚点 */
    private final Map<Object, Integer> referenceCount = new IdentityHashMap<>();
    private final Map<Object, String> anchoredObjects = new HashMap<>();
    private int nextAnchor = 1;
    private boolean isRoot;

    public YamlxWriter(Writer writer) {
        this(writer, new YamlxConfig());
    }

    public YamlxWriter(Writer writer, YamlxConfig config) {
        this.config = config;
        emitter = new Emitter(writer, config.writeConfig.emitterConfigx);
    }

    public void setAlias(Object object, String alias) {
        anchoredObjects.put(object, alias);
    }

    public void write(Object object) throws YamlxException {
        if (config.writeConfig.autoAnchor) {
            countObjectReferences(object);
            queuedObjects.add(object);
            return;
        }
        writeInternal(object);
    }

    public YamlxConfig getConfig() {
        return config;
    }

    private void writeInternal(Object object) throws YamlxException {
        try {
            if (!started) {
                emitter.emit(Event.STREAM_START);
                started = true;
            }
            emitter.emit(new DocumentStartEvent(config.writeConfig.explicitFirstDocument, null, null));
            isRoot = true;
            writeValue(object, config.writeConfig.writeRootTags ? null : object.getClass(), null, null);
            emitter.emit(new DocumentEndEvent(config.writeConfig.explicitEndDocument));
        } catch (EmitterException | IOException ex) {
            throw new YamlxException("Error writing YAML.", ex);
        }
    }

    /** Returns the YAML emitter, which allows the YAML output to be configured. */
    public Emitter getEmitter() {
        return emitter;
    }

    /**
     * Writes any buffered objects, then resets the list of anchored objects.
     *
     * @see WriteConfig#setAutoAnchor(boolean)
     */
    public void clearAnchors() throws YamlxException {
        for (Object object : queuedObjects) {
            writeInternal(object);
        }
        queuedObjects.clear();
        referenceCount.clear();
        nextAnchor = 1;
    }

    /**
     * Finishes writing any buffered output and releases all resources.
     *
     * @throws YamlxException If the buffered output could not be written or the writer could not be closed.
     */
    public void close() throws YamlxException {
        clearAnchors();
        defaultValuePrototypes.clear();
        try {
            emitter.emit(Event.STREAM_END);
            emitter.close();
        } catch (EmitterException | IOException ex) {
            throw new YamlxException(ex);
        }
    }

    private void writeValue(Object object, Class fieldClass, Class elementType, Class defaultType)
        throws EmitterException, IOException {
        boolean isRoot = this.isRoot;
        this.isRoot = false;

        if (object instanceof YamlElement) {
            ((YamlElement) object).emitEvent(emitter, config.writeConfig);
            return;
        }
        if (object == null) {
            // 在属性值为null的时候也能打印出类型
            //String tag = null;
            String tag = fieldClass == null ? null : fieldClass.getName();
            emitter.emit(new ScalarEvent(null, tag, new boolean[] { true, true }, null, (char) 0));
            return;
        }

        Class valueClass = object.getClass();
        boolean unknownType = fieldClass == null;
        if (unknownType) {
            fieldClass = valueClass;
        }

        if (object instanceof Enum) {
            emitter.emit(new ScalarEvent(null, null, new boolean[] { true, true }, ((Enum) object).name(),
                this.config.writeConfig.quote.c));
            return;
        }

        String anchor = null;
        if (!Beans.isScalar(valueClass)) {
            anchor = anchoredObjects.get(object);
            if (config.writeConfig.autoAnchor) {
                Integer count = referenceCount.get(object);
                if (count == null) {
                    emitter.emit(new AliasEvent(anchoredObjects.get(object)));
                    return;
                }
                if (count > 1) {
                    referenceCount.remove(object);
                    if (anchor == null) {
                        anchor = String.valueOf(nextAnchor++);
                        anchoredObjects.put(object, anchor);
                    }
                }
            }
        }

        String tag = null;
        boolean showTag = false;
        if ((unknownType//
            || valueClass != fieldClass//
            || config.writeConfig.writeClassName == WriteClassName.ALWAYS)//
            && config.writeConfig.writeClassName != WriteClassName.NEVER) {
            showTag = (!unknownType && fieldClass != List.class) || valueClass != ArrayList.class;
            if ((unknownType || fieldClass == Map.class) && valueClass == HashMap.class) {
                showTag = false;
            }
            if (fieldClass == Set.class && valueClass == HashSet.class) {
                showTag = false;
            }
            if (valueClass == defaultType) {
                showTag = false;
            }
            if (showTag) {
                tag = config.classNameToTag.get(valueClass.getName());
                if (tag == null) {
                    tag = valueClass.getName();
                }
            }
        }

        for (Entry<Class, ScalarSerializer> entry : config.scalarSerializers.entrySet()) {
            if (entry.getKey().isAssignableFrom(valueClass)) {
                ScalarSerializer serializer = entry.getValue();
                emitter.emit(
                    new ScalarEvent(null, tag, new boolean[] { tag == null, tag == null }, serializer.write(object),
                        (char) 0));
                return;
            }
        }

        if (Beans.isScalar(valueClass)) {
            char style = 0;
            String string = String.valueOf(object);
            if (valueClass == String.class) {
                try {
                    Float.parseFloat(string);
                    style = this.config.writeConfig.quote.c;
                } catch (NumberFormatException ignored) {}
            }
            emitter.emit(new ScalarEvent(null, tag, new boolean[] { true, true }, string, style));
            return;
        }

        if (object instanceof Collection) {
            emitter.emit(new SequenceStartEvent(anchor, tag, !showTag, false));
            for (Object item : (Collection) object) {
                if (isRoot && !config.writeConfig.writeRootElementTags) {
                    elementType = item.getClass();
                }
                writeValue(item, elementType, null, null);
            }
            emitter.emit(Event.SEQUENCE_END);
            return;
        }

        if (object instanceof Map) {
            emitter.emit(new MappingStartEvent(anchor, tag, !showTag, false));
            Map map = (Map) object;
            for (Object item : map.entrySet()) {
                Entry entry = (Entry) item;
                Object key = entry.getKey(), value = entry.getValue();
                if (isRoot && !config.writeConfig.writeRootElementTags) {
                    elementType = value.getClass();
                }
                if (config.tagSuffix != null && key instanceof String) {
                    // Skip tag keys.
                    if (((String) key).endsWith(config.tagSuffix)) {
                        continue;
                    }

                    // Write value with tag, if found.
                    if (value instanceof String) {
                        Object valueTag = map.get(key + config.tagSuffix);
                        if (valueTag instanceof String) {
                            String string = (String) value;
                            char style = 0;
                            try {
                                Float.parseFloat(string);
                                style = this.config.writeConfig.quote.c;
                            } catch (NumberFormatException ignored) {}
                            writeValue(key, null, null, null);
                            emitter.emit(
                                new ScalarEvent(null, (String) valueTag, new boolean[] { false, false }, string,
                                    style));
                            continue;
                        }
                    }
                }
                writeValue(key, null, null, null);
                writeValue(value, elementType, null, null);
            }
            emitter.emit(Event.MAPPING_END);
            return;
        }

        if (fieldClass.isArray()) {
            elementType = fieldClass.getComponentType();
            emitter.emit(new SequenceStartEvent(anchor, null, true, false));
            for (int i = 0, n = Array.getLength(object); i < n; i++) {
                writeValue(Array.get(object, i), elementType, null, null);
            }
            emitter.emit(Event.SEQUENCE_END);
            return;
        }

        // Value must be a bean.

        Object prototype = null;
        if (!config.writeConfig.writeDefaultValues && valueClass != Class.class) {
            prototype = defaultValuePrototypes.get(valueClass);
            if (prototype == null && Beans.getDeferredConstruction(valueClass, config) == null) {
                try {
                    prototype = Beans.createObject(valueClass, config.privateConstructors);
                } catch (InvocationTargetException ex) {
                    throw new YamlxException("Error creating object prototype to determine default values.", ex);
                }
                defaultValuePrototypes.put(valueClass, prototype);
            }
        }

        Set<Property> properties = Beans.getProperties(valueClass, config.beanProperties, config.privateFields, config);
        emitter.emit(new MappingStartEvent(anchor, tag, !showTag, false));
        for (Property property : properties) {
            try {
                Object propertyValue = property.get(object);
                if (prototype != null) {
                    // Don't output properties that have the default value for the prototype.
                    Object prototypeValue = property.get(prototype);
                    if (propertyValue == null && prototypeValue == null) { continue; }
                    if (prototypeValue != null && prototypeValue.equals(propertyValue)) {
                        continue;
                    }
                }
                emitter.emit(new ScalarEvent(null, null, new boolean[] { true, true }, property.getName(),
                    this.config.writeConfig.quote.c));
                Class propertyElementType = config.propertyToElementType.get(property);
                Class propertyDefaultType = config.propertyToDefaultType.get(property);
                writeValue(propertyValue, property.getType(), propertyElementType, propertyDefaultType);
            } catch (Exception ex) {
                throw new YamlxException("Error getting property '" + property + "' on class: " + valueClass.getName(),
                    ex);
            }
        }
        emitter.emit(Event.MAPPING_END);
    }

    private void countObjectReferences(Object object) throws YamlxException {
        if (object == null || Beans.isScalar(object.getClass())) {
            return;
        }

        // Count every reference to the object, but follow its own references the first time it is encountered.
        Integer count = referenceCount.get(object);
        if (count != null) {
            referenceCount.put(object, count + 1);
            return;
        }
        referenceCount.put(object, 1);

        if (object instanceof Collection) {
            for (Object item : (Collection) object) {
                countObjectReferences(item);
            }
            return;
        }

        if (object instanceof Map) {
            for (Object value : ((Map) object).values()) {
                countObjectReferences(value);
            }
            return;
        }

        if (object.getClass().isArray()) {
            for (int i = 0, n = Array.getLength(object); i < n; i++) {
                countObjectReferences(Array.get(object, i));
            }
            return;
        }

        // Value must be an object.

        Set<Property> properties = Beans
            .getProperties(object.getClass(), config.beanProperties, config.privateFields, config);
        for (Property property : properties) {
            if (Beans.isScalar(property.getType())) {
                continue;
            }
            Object propertyValue;
            try {
                propertyValue = property.get(object);
            } catch (Exception ex) {
                throw new YamlxException(
                    "Error getting property '" + property + "' on class: " + object.getClass().getName(), ex);
            }
            countObjectReferences(propertyValue);
        }
    }
}
