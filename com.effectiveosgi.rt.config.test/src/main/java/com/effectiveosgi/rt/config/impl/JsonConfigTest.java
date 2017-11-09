package com.effectiveosgi.rt.config.impl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import org.junit.Test;

@SuppressWarnings("serial")
public class JsonConfigTest extends TestBase {

	@Test
	public void testJsonConfig1() throws Exception {
		Set<Dictionary<String, Object>> expected = new HashSet<>();
		expected.add(new Hashtable<String, Object>() {
			{
				put("service.pid", "pid.a");
				put("key", "val");
				put("some_long", 123L);
				put("some_double", 123.4d);
			}
		});
		expected.add(new Hashtable<String, Object>() {
			{
				put("service.pid", "pid.b");
				put("a_boolean", true);
			}
		});

		InputStream stream = getClass().getResourceAsStream("/sample1.json");
		final File jsonFile = writeConfigFile(stream, "sample1.json");
		assertTrue("installer can handle json file", installer.canHandle(jsonFile));

		installer.install(jsonFile);
		Thread.sleep(500);
		assertDeepConfigsEqual("", expected, configAdmin.listConfigurations(null));

		installer.uninstall(jsonFile);
		;
		Thread.sleep(500);
		assertDeepConfigsEqual("", Collections.emptySet(), configAdmin.listConfigurations(null));
	}

	@Test
	public void testTypeMapping() throws Exception {
		Set<Dictionary<String, Object>> expected = new HashSet<>();
		expected.add(new Hashtable<String, Object>() {
			{
				put("service.pid", "my.pid");

				put("int1", 300);
				put("int2", 300);

				put("long1", 400L);
				put("long2", 400L);

				put("float1", 123.0f);
				put("float2", 123.0f);

				put("double1", 123.0d);
				put("double2", 123.0d);

				put("byte1", (byte) 321);
				put("byte2", (byte) 321);

				put("short1", (short) 111);
				put("short2", (short) 111);

				put("char1", (char) 40000);
				put("char2", (char) 40000);
				
				put("bool1", true);
				put("bool2", true);
				put("bool3", false);
				put("bool4", true);
				put("bool5", true);
				put("bool6", false);

				put("int_array1", new int[] { 123 });
				put("int_array2", new int[] { 1, 2, 3 });

				put("long_array1", new long[] { 123L });
				put("long_array2", new long[] { 1L, 2L, 3L });

				put("float_array1", new float[] { 123.1f });
				put("float_array2", new float[] { 1f, 2f, 3f });

				put("double_array1", new double[] { 123.1d });
				put("double_array2", new double[] { 1f, 2d, 3d });

				put("byte_array1", new byte[] { (byte) 123 });
				put("byte_array2", new byte[] { (byte) 1, (byte) 2, (byte) 3 });

				put("short_array1", new short[] { (short) 123 });
				put("short_array2", new short[] { (short) 1, (short) 2, (short) 3 });

				put("char_array1", new char[] { (char) 123 });
				put("char_array2", new char[] { (char) 1, (char) 2, (char) 3 });

				put("boolean_array1", new boolean[] { true });
				put("boolean_array2", new boolean[] { true, false, true });

				put("int_coll1", new ArrayList<Integer>(){{
					add(123);
				}});
				put("int_coll2", new ArrayList<Integer>(){{
					add(1); add(2); add(3);
				}});

				put("string_coll1", new ArrayList<String>(){{
					add("one");
				}});
				put("string_coll2", new ArrayList<String>(){{
					add("one"); add("two"); add("three");
				}});
			}
		});

		InputStream stream = getClass().getResourceAsStream("/datatypes.json");
		final File jsonFile = writeConfigFile(stream, "datatypes.json");
		assertTrue("installer can handle json file", installer.canHandle(jsonFile));

		installer.install(jsonFile);
		Thread.sleep(500);
		assertDeepConfigsEqual("", expected, configAdmin.listConfigurations(null));

		installer.uninstall(jsonFile);
		;
		Thread.sleep(500);
		assertDeepConfigsEqual("", Collections.emptySet(), configAdmin.listConfigurations(null));
	}

}
