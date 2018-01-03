package com.effectiveosgi.lib;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public final class PropertiesUtil {

    public static String[] getStringArray(Map<String, ?> map, String name, String[] defaultValue) throws IllegalArgumentException {
        Object o = map.get(name);
        if (o instanceof String) {
            return new String[] { (String) o };
        } else if (o instanceof String[]) {
            return (String[]) o;
        } else if (o instanceof Collection) {
            Collection<?> c = (Collection<?>) o;
            if (c.isEmpty()) {
                return new String[0];
            } else {
                String[] a = new String[c.size()];
                Iterator<?> iter = c.iterator();
                for (int i = 0; i < a.length; i++) {
                    Object elem = iter.next();
                    if (!(elem instanceof String))
                        throw new IllegalArgumentException(String.format("Collection value for field '%s' contains non-String element at index %d.", name, i));
                    a[i] = (String) elem;
                }
                return a;
            }
        } else if (o == null) {
            return defaultValue;
        } else {
            throw new IllegalArgumentException(String.format("Value for field '%s' is not a String, String-array or Collection of String. Actual type was %s.", name, o.getClass().getName()));
        }
    }

}
