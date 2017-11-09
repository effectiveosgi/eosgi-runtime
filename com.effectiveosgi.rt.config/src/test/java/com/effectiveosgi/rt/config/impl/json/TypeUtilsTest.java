package com.effectiveosgi.rt.config.impl.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import static org.junit.Assert.*;

public class TypeUtilsTest {

    private final Gson gson = new GsonBuilder().setLenient().create();

    @Test
    public void testTypes() throws Exception {
        try (InputStream stream = TypeUtilsTest.class.getClassLoader().getResourceAsStream("datatypes.json")) {
            InputStreamReader reader = new InputStreamReader(stream);
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            // Defaults
            assertEquals("stringval", getEntry(root, "def1"));
            assertEquals(123L, getEntry(root, "def2"));
            assertEquals(123.4d, getEntry(root, "def3"));

            // Primitive types
            assertEquals(300, getEntry(root, "int1:Integer"));
            assertEquals(300, getEntry(root, "int2:int"));
            assertEquals(400L, getEntry(root, "long1:Long"));
            assertEquals(400L, getEntry(root, "long2:long"));
            assertEquals(123f, getEntry(root, "float1:float"));
            assertEquals(123f, getEntry(root, "float2:Float"));

            // Primitive arrays
            assertArrayEquals(new int[] { 123 }, (int[]) getEntry(root, "int_array1:int[]"));
            assertArrayEquals(new int[] { 1, 2, 3 }, (int[]) getEntry(root, "int_array2:int[]"));
            assertArrayEquals(new long[] { 123 }, (long[]) getEntry(root, "long_array1:long[]"));
            assertArrayEquals(new long[] { 1, 2, 3 }, (long[]) getEntry(root, "long_array2:long[]"));

            // Object arrays
            assertArrayEquals(new String[] { "one" }, (String[]) getEntry(root, "string_array:String[]"));

            // Collections
            assertEquals(Arrays.asList(new Integer[] { 1, 2, 3}), getEntry(root, "int_collection1:Collection<Integer>"));
            assertEquals(Arrays.asList(new Integer[] { 1, 2, 3}), getEntry(root, "int_collection2:Collection<int>"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidType1() throws Exception {
        String label = "foo:Float:";
        JsonObject root = gson.fromJson(String.format("{ '%s' : 123 }", label), JsonObject.class);
        TypeUtils.parse(label, root.get(label));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidType2() throws Exception {
        String label = "foo:";
        JsonObject root = gson.fromJson(String.format("{ '%s' : 123 }", label), JsonObject.class);
        TypeUtils.parse(label, root.get(label));
    }

    Object getEntry(JsonObject object, String name) {
        JsonElement element = object.get(name);
        assertNotNull("missing entry " + name, element);
        return TypeUtils.parse(name, element).getValue();
    }

}
