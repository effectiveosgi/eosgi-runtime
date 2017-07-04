package com.effectiveosgi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
		property = {
	        "osgi.command.scope=config",
	        "osgi.command.function=list",
	        "osgi.command.function=info",
	        Converter.CONVERTER_CLASSES + "=org.osgi.service.cm.Configuration"
		})
public class ConfigurationCommands implements Converter {

    @Reference
    ConfigurationAdmin configAdmin;

    @Descriptor("List configurations")
    public Configuration[] list() throws Exception {
    	return list(null);
    }
    
    @Descriptor("List confgurations")
    public Configuration[] list(@Descriptor("PID prefix") String prefix) throws Exception {
        Configuration[] configs = configAdmin.listConfigurations(prefix != null ? String.format("(%s=%s*)", Constants.SERVICE_PID, prefix) : null);
        if (configs == null) configs = new Configuration[0];

        return configs;
    }
    
    // Returns a single Configuration for inspection, or an array of Configuration for listing
    public Object info(String pid) throws Exception {
        Configuration[] configs = configAdmin.listConfigurations(String.format("(%s=%s*)", Constants.SERVICE_PID, pid));
        if (configs != null && configs.length == 1)
        	return configs[0];

        return configs;
    }

	@Override
	public Object convert(Class<?> desiredType, Object in) throws Exception {
		return null;
	}

	@Override
	public CharSequence format(Object target, int level, Converter escape) throws Exception {
		if (target instanceof Configuration[])
			return format((Configuration[]) target, level, escape);
		
		if (target instanceof Configuration)
			return format((Configuration) target, level, escape);
		
		return null;
	}
	
    private CharSequence format(Configuration[] configs, int level, Converter escape) throws Exception {
    	final CharSequence result;
    	switch (level) {
    	case Converter.INSPECT:
    		StringBuilder builder = new StringBuilder();

    		Map<String, List<Configuration>> factoryConfigs = new LinkedHashMap<>();
    		Arrays.stream(configs)
    			.sorted(ComponentCommands::compareConfigurations)
    			.forEach(c -> {
    				List<Configuration> l = factoryConfigs.get(c.getFactoryPid());
    				if (l == null) {
    					l = new LinkedList<>();
    					factoryConfigs.put(c.getFactoryPid(), l);
    				}
    				l.add(c);
    			});
    		for (Entry<String,List<Configuration>> e : factoryConfigs.entrySet()) {
    			if (e.getKey() != null) {
    				builder.append(String.format("%s [%d record(s)]:%n", e.getKey(), e.getValue().size()));
    				for (Configuration c : e.getValue())
    					builder.append("  ").append(escape.format(c, Converter.LINE, escape)).append('\n');
    			} else {
    				for (Configuration c : e.getValue())
    					builder.append(escape.format(c, Converter.LINE, escape)).append('\n');
    			}
    		}
    		result = builder;
    		break;
    	case Converter.LINE:
    		result = escape.format(configs, LINE, escape);
    		break;
    	case Converter.PART:
    	default:
    		throw new UnsupportedOperationException("Requested format for Configuration unsupported");
    	}
    	return result;
    }
    
    private CharSequence format(Configuration config, int level, Converter escape) throws Exception {
    	final CharSequence result;
    	switch (level) {
    	case Converter.INSPECT:
    		StringBuilder builder = new StringBuilder();
    		builder.append(String.format("%s (%d changes) ", config.getPid(), config.getChangeCount()));
    		builder.append(config.getBundleLocation() != null ? " Bound to: " + config.getBundleLocation() : " Unbound").append('\n');
    		
    		for (Enumeration<String> keys = config.getProperties().keys(); keys.hasMoreElements(); ) {
    			String key = keys.nextElement();
    			Object value = config.getProperties().get(key);
				builder.append("  ").append(key).append(':').append(formatType(value)).append("=").append(value).append('\n');
    		}
    		
    		result = builder;
    		break;
    	case Converter.LINE:
    		result = String.format("%s [%d properties]", config.getPid(), config.getProperties().size());
    		break;
    	case Converter.PART:
		default:
    		result = config.getPid();
    	}
    	return result;
    }
    
    private static final String UNKNOWN_TYPE_NAME = "?";

	private String formatType(Object value) {
		if (value == null)
			return UNKNOWN_TYPE_NAME;

		Class<?> clazz = value.getClass();
		if (clazz.isPrimitive() || clazz == String.class)
			return clazz.getSimpleName();
		
		if (clazz.isArray())
			return formatType(clazz.getComponentType()) + "[]";

		if (value instanceof Collection) {
			Collection<?> coll = (Collection<?>) value;
			final String componentTypeName = coll.isEmpty() ? UNKNOWN_TYPE_NAME : formatType(coll.iterator().next());

			return String.format("Collection<%s>", componentTypeName);
		}
		
		return UNKNOWN_TYPE_NAME;
	}

}
