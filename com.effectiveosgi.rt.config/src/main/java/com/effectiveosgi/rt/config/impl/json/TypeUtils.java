package com.effectiveosgi.rt.config.impl.json;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

final class TypeUtils {

    static final Function<JsonElement,JsonElement> ID = Function.identity();

    private static final class EntryImpl<K,V> implements Map.Entry<K,V> {
        private final K key;
        private V value;
        private EntryImpl(K key, V value) {
            this.key = key; this.value = value;
        }
        @Override
        public K getKey() {
            return key;
        }
        @Override
        public V getValue() {
            return value;
        }
        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }

    static Map.Entry<String, Object> parseEntry(Map.Entry<String, JsonElement> entry) {
        return parse(entry.getKey(), entry.getValue());
    }

    static Map.Entry parse(String label, JsonElement element) {
        final String name;
        final String typeName;
        int colonIndex = label.indexOf(":");
        if (colonIndex < 0) {
            name = label;
            typeName = null;
        } else {
            name = label.substring(0, colonIndex);
            typeName = label.substring(colonIndex + 1);
        }
        Type<?> type = findType(typeName);
        return new EntryImpl(name, type.parse(element));
    }

    static class Type<T> {
        private final Class<T> outputType;
        private final Function<JsonElement, T> parser;
        Type(Class<T> outputType, Function<JsonElement, T> parser) {
            this.outputType = outputType; this.parser = parser;
        }
        public Class<T> getOutputType() {
            return outputType;
        }
        public Function<JsonElement, T> getParser() {
            return parser;
        }
        public T parse(JsonElement element) throws IllegalArgumentException {
            return parser.apply(element);
        }
    }

    static Type<?> findType(String typeName) {
        final Function<JsonElement, Object> parser;
        final Class<?> outputType;
        if (typeName == null) {
            // Default handling
            outputType = Object.class;
            parser = ID.andThen(TypeUtils::toPrimitive).andThen(TypeUtils::parseDefault);
        } else if (typeName.endsWith("[]")) {
            String componentTypeName = typeName.substring(0, typeName.length() - 2);
            Type<?> componentType = findType(componentTypeName);

            // Have to instantiate a zero-element array to get the array Class
            outputType = Array.newInstance(componentType.getOutputType(), 0).getClass();
            parser = TypeUtils.arrayParser(componentType.getOutputType(), componentType.getParser());
        } else if (typeName.startsWith("Collection<") && typeName.endsWith(">")) {
            String componentTypeName = typeName.substring("Collection<".length(), typeName.length() - 1);
            Type<?> componentType = findType(componentTypeName);
            outputType = Collection.class;
            parser = TypeUtils.collectionParser(componentType.getParser());
        } else {
            final Function<JsonPrimitive, Object> primitiveParser;
            switch (typeName) {
                case "String":
                    primitiveParser = JsonPrimitive::getAsString;
                    outputType = String.class;
                    break;
                case "Integer":
                    outputType = Integer.class;
                    primitiveParser = JsonPrimitive::getAsInt;
                    break;
                case "int":
                    outputType = Integer.TYPE;
                    primitiveParser = JsonPrimitive::getAsInt;
                    break;
                case "Long":
                    outputType = Long.class;
                    primitiveParser = JsonPrimitive::getAsLong;
                    break;
                case "long":
                    outputType = Long.TYPE;
                    primitiveParser = JsonPrimitive::getAsLong;
                    break;
                case "Float":
                    outputType = Float.class;
                    primitiveParser = JsonPrimitive::getAsFloat;
                    break;
                case "float":
                    outputType = Float.TYPE;
                    primitiveParser = JsonPrimitive::getAsFloat;
                    break;
                case "Double":
                    outputType = Double.class;
                    primitiveParser = JsonPrimitive::getAsDouble;
                    break;
                case "double":
                    outputType = Double.TYPE;
                    primitiveParser = JsonPrimitive::getAsDouble;
                    break;
                case "Byte":
                    outputType = Byte.class;
                    primitiveParser = JsonPrimitive::getAsByte;
                    break;
                case "byte":
                    outputType = Byte.TYPE;
                    primitiveParser = JsonPrimitive::getAsByte;
                    break;
                case "Short":
                    outputType = Short.class;
                    primitiveParser = JsonPrimitive::getAsShort;
                    break;
                case "short":
                    outputType = Short.TYPE;
                    primitiveParser = JsonPrimitive::getAsShort;
                    break;
                case "Character":
                    outputType = Character.class;
                    primitiveParser = TypeUtils::parseCharacter;
                    break;
                case "char":
                    outputType = Character.TYPE;
                    primitiveParser = TypeUtils::parseCharacter;
                    break;
                case "Boolean":
                    outputType = Boolean.class;
                    primitiveParser = TypeUtils::parseBoolean;
                    break;
                case "boolean":
                    outputType = Boolean.TYPE;
                    primitiveParser = TypeUtils::parseBoolean;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type: " + typeName);
            }
            parser = ID.andThen(TypeUtils::toPrimitive).andThen(primitiveParser);
        }

        Type<?> result = new Type(outputType, parser);
        return result;
    }

    static final JsonPrimitive toPrimitive(JsonElement element) {
        if (!element.isJsonPrimitive())
            throw new IllegalArgumentException(String.format("Unsupported element type: required %s, was %s", JsonPrimitive.class.getSimpleName(), element.getClass().getSimpleName()));
        return element.getAsJsonPrimitive();
    }

    static final JsonPrimitive[] toPrimitiveArray(JsonElement element) {
        final JsonPrimitive[] resultArray;
        if (element.isJsonPrimitive()) {
            // Convert a single primitive into a single-valued array
            resultArray = new JsonPrimitive[] { element.getAsJsonPrimitive() };
        } else if (element.isJsonArray()){
            JsonArray jsonArray = element.getAsJsonArray();
            resultArray = new JsonPrimitive[jsonArray.size()];
            for (int i = 0; i < resultArray.length; i++) {
                JsonElement entryElem = jsonArray.get(i);
                if (!entryElem.isJsonPrimitive())
                    throw new IllegalArgumentException(String.format("Unsupported array entry type at index %d: required %s, was %s", i, JsonPrimitive.class.getSimpleName(), entryElem.getClass().getSimpleName()));
                resultArray[i] = entryElem.getAsJsonPrimitive();
            }
        } else {
            throw new IllegalArgumentException(String.format("Unsupported element type: required %s or %s, was %s", JsonArray.class.getSimpleName(), JsonPrimitive.class.getSimpleName(), element.getClass().getSimpleName()));
        }
        return resultArray;
    }

    static final <T> Function<JsonElement, Object> arrayParser(Class<?> componentClass, Function<JsonElement, T> componentParser) {
        return ID.andThen(TypeUtils::toPrimitiveArray).andThen(primitives -> {
            Object array = Array.newInstance(componentClass, primitives.length);
            for (int i = 0; i < primitives.length; i++)
                Array.set(array, i, componentParser.apply(primitives[i]));
            return array;
        });
    }

    static final <T> Function<JsonElement, Object> collectionParser(Function<JsonElement, T> componentParser) {
        return ID.andThen(TypeUtils::toPrimitiveArray).andThen(primitives -> {
            Collection<Object> coll = new ArrayList<>(primitives.length);
            for (JsonPrimitive primitive : primitives)
                coll.add(componentParser.apply(primitive));
            return coll;
        });
    }

    static final Object parseDefault(JsonPrimitive primitive) {
        if (primitive.isBoolean())
            return primitive.getAsBoolean();
        if (primitive.isNumber()) {
            double num = primitive.getAsNumber().doubleValue();
            if (num == Math.floor(num) && !(java.lang.Double.isInfinite(num))) {
                return (long) num;
            } else {
                return num;
            }
        }
        return primitive.getAsString();
    }

    static final Character parseCharacter(JsonPrimitive primitive) {
        int i = primitive.getAsInt();
        if (java.lang.Character.MIN_VALUE > i || i > java.lang.Character.MAX_VALUE)
            throw new IllegalArgumentException(String.format("Cannot convert to character: value must be between %d and %d, was %d", java.lang.Character.MIN_VALUE, java.lang.Character.MAX_VALUE, i));
        return java.lang.Character.valueOf((char) i);
    }

    static final Boolean parseBoolean(JsonPrimitive primitive) {
        boolean b;
        if (primitive.isBoolean())
            b = primitive.getAsBoolean();
        else if (primitive.isString())
            b = java.lang.Boolean.parseBoolean(primitive.getAsString());
        else if (primitive.isNumber())
            b = (primitive.getAsNumber().doubleValue() > 0d);
        else
            throw new IllegalArgumentException(String.format("Cannot convert to boolean: %s [type %s]", primitive.getAsString(), primitive.getClass().getSimpleName()));
        return (Boolean) b;
    }

}