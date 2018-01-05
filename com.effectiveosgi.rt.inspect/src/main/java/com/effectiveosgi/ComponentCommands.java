package com.effectiveosgi;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.effectiveosgi.lib.PropertiesUtil;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.service.log.LogService;

class ComponentCommands implements Converter {

	private final BundleContext context;
	private final ServiceComponentRuntime scr;
	private final LogService log;

	ComponentCommands(BundleContext context, ServiceComponentRuntime scr, LogService log) {
		this.context = context;
		this.scr = scr;
		this.log = log;
	}

	public ComponentDescriptionDTO[] list() {
		return scr.getComponentDescriptionDTOs().toArray(new ComponentDescriptionDTO[0]);
	}

	public ComponentDescriptionDTO info(String name) {
		String lowerName = name.toLowerCase();
		List<ComponentDescriptionDTO> partialMatches = new LinkedList<>();
		for (ComponentDescriptionDTO dto : scr.getComponentDescriptionDTOs()) {
			if (dto.name.equalsIgnoreCase(name))
				return dto;
			if (dto.name.toLowerCase().contains(lowerName))
				partialMatches.add(dto);
		}
		
		if (partialMatches.isEmpty()) {
			throw new IllegalArgumentException(MessageFormat.format("No component description matching \"{0}\".", name));
		} else if (partialMatches.size() > 1) {
			throw new IllegalArgumentException(MessageFormat.format("Multiple components matching \"{0}\": [{1}]", name,  partialMatches.stream().map(dto -> dto.name).collect(Collectors.joining(", "))));
		}
		return partialMatches.get(0);
	}

	public ComponentConfigurationDTO info(long id) {
		for (ComponentDescriptionDTO descDto : scr.getComponentDescriptionDTOs()) {
			for (ComponentConfigurationDTO configDto : scr.getComponentConfigurationDTOs(descDto)) {
				if (configDto.id == id)
					return configDto;
			}
		}
		return null;
	}

	@Override
	public Object convert(Class<?> desiredType, Object in) throws Exception {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public CharSequence format(Object target, int level, Converter escape) throws Exception {
		final CharSequence result;
		if (target instanceof ComponentDescriptionDTO[]) {
			result = format((ComponentDescriptionDTO[]) target, level, escape);
		} else if (target instanceof ComponentDescriptionDTO) {
			result = format((ComponentDescriptionDTO) target, level, escape);
		} else if (target instanceof ComponentConfigurationDTO) {
			result = format((ComponentConfigurationDTO) target, level, escape);
		} else {
			result = null;
		}
		return result;
	}

	private CharSequence format(ComponentDescriptionDTO[] dtoArray, int level, Converter escape) throws Exception {
		StringBuilder sb = new StringBuilder();
		if (dtoArray == null || dtoArray.length == 0) {
			sb.append("No component descriptions found");
		} else {
			for (int i = 0; i < dtoArray.length; i++) {
				if (i > 0) sb.append('\n');
				sb.append(escape.format(dtoArray[i], Converter.LINE, escape));
			}
		}
		return sb;
	}

	private CharSequence format(ComponentDescriptionDTO dto, int level, Converter escape) throws Exception {
		final StringBuilder builder = new StringBuilder();
		switch (level) {
		case Converter.LINE:
			Collection<ComponentConfigurationDTO> children = scr.getComponentConfigurationDTOs(dto);
			if (children == null)
				children = Collections.emptyList();

			builder.append(MessageFormat.format("{0} in bundle [{1}] ({2}:{3}) {4}, {5,choice,0#0 instances|1#1 instance|1<{5} instances}.",
				dto.name,
				dto.bundle.id,
				dto.bundle.symbolicName,
				dto.bundle.version,
				dto.defaultEnabled ? "enabled" : "disabled",
				children.size()
			));

			for (ComponentConfigurationDTO child : children)
				builder.append("\n    ").append(escape.format(child, Converter.LINE, escape));
			break;
		case Converter.INSPECT:
			Map<String, String> out = new LinkedHashMap<>();
			out.put("Class", dto.implementationClass);
			out.put("Bundle", String.format("%d (%s:%s)", dto.bundle.id, dto.bundle.symbolicName, dto.bundle.version));
			if (dto.factory != null) {
				out.put("Factory", dto.factory);
			}
			out.put("Enabled", Boolean.toString(dto.defaultEnabled));
			out.put("Immediate", Boolean.toString(dto.immediate));
			out.put("Services", arrayToString(dto.serviceInterfaces));
			if (dto.scope != null) {
				out.put("Scope", dto.scope);
			}
			out.put("Config PID(s)", String.format("%s, Policy: %s", arrayToString(dto.configurationPid), dto.configurationPolicy));
			out.put("Base Props", printProperties(dto.properties));
			printColumnsAligned(String.format("Component Description: %s", dto.name), out, builder);
			break;
		case Converter.PART:
			break;
		}
		return builder;
	}

	private CharSequence format(ComponentConfigurationDTO dto, int level, Converter escape) throws Exception {
		final StringBuilder builder = new StringBuilder();
		switch (level) {
		case Converter.INSPECT:

			// Inspect base descriptor DTO
			ComponentDescriptionDTO desc = dto.description;
			builder.append(format(desc, Converter.INSPECT, escape));

			// Blank line separator
			builder.append("\n\n");

			// Inspect configuration DTO
			final Map<String, String> out = new LinkedHashMap<>();
			String title = String.format("Component Configuration Id: %d", dto.id);
			out.put("State", stateToString(dto.state));

			// Print service registration
			ServiceReference<?>[] serviceRefs = context.getAllServiceReferences(null, String.format("(%s=%d)", ComponentConstants.COMPONENT_ID, dto.id));
			if (serviceRefs != null && serviceRefs.length > 0) {
				out.put("Service Id", printPublishedService(serviceRefs[0]));
			}

			// Print Configuration Properties
			out.put("Config Props", printProperties(dto.properties));

			// Print References
			out.put("References", printServiceReferences(dto.satisfiedReferences, dto.unsatisfiedReferences, desc.references));

			if (dto.failure != null) {
				out.put("Failure", dto.failure);
			}
			printColumnsAligned(title, out, builder);
			break;
		case Converter.LINE:
			builder.append("Id: ").append(dto.id);
			builder.append(", ").append("State:").append(stateToString(dto.state ));
			String[] pids = PropertiesUtil.getStringArray(dto.properties, Constants.SERVICE_PID, null);
			if (pids != null && pids.length > 0) {
				builder.append(", ").append("PID(s): ").append(Arrays.toString(pids));
			}
			break;
		case Converter.PART:
			break;
		}
		return builder;
	}

	String printPublishedService(ServiceReference<?> serviceRef) {
		StringBuilder sb = new StringBuilder();
		sb.append(serviceRef.getProperty(Constants.SERVICE_ID));
		sb.append(' ').append(Arrays.toString((String[]) serviceRef.getProperty(Constants.OBJECTCLASS)));

		Bundle[] consumers = serviceRef.getUsingBundles();
		if (consumers != null) for (Bundle consumer : consumers) {
			sb.append("\n\t").append(String.format("Bound by bundle [%d] (%s:%s)", consumer.getBundleId(), consumer.getSymbolicName(), consumer.getVersion()));
		}

		return sb.toString();
	}

	private String arrayToString(String[] array) {
		return array == null || array.length == 0 ? "<<none>>" : Arrays.stream(array).collect(Collectors.joining(", "));
	}

	static final String stateToString(int state) {
		final String string;
		switch (state) {
		case ComponentConfigurationDTO.ACTIVE:
			string = "ACTIVE";
			break;
		case ComponentConfigurationDTO.SATISFIED:
			string = "INACTIVE";
			break;
		case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
			string = "UNSATISFIED CONFIGURATION";
			break;
		case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
			string = "UNSATISFIED REFERENCE";
			break;
		case ComponentConfigurationDTO.FAILED_ACTIVATION:
			string = "FAILED ACTIVATION";
			break;
		default:
			string = String.format("<<UNKNOWN: %d>>", state);
		}
		return string;
	}

	static final String repeat(int count, char character) {
		char[] array = new char[count];
		Arrays.fill(array, character);
		return new String(array);
	}

	static final String repeatForDigits(int number, char character) {
		return repeat((int) Math.floor(Math.log10(number)) + 1, character);
	}

	static String printProperties(Map<String, ?> props) {
		StringBuilder builder = new StringBuilder();
		int size = props.size();
		builder.append('(').append(Integer.toString(size)).append(' ').append(size == 1 ? "entry" : "entries").append(')');
        if (size > 0) {
            builder.append("\n\t").append(props.entrySet().stream()
                    .map(e -> String.format("%s<%s> = %s", e.getKey(), e.getValue() != null ? e.getValue().getClass().getSimpleName() : "<<null>>", e.getValue()))
                    .sorted()
                    .collect(Collectors.joining("\n\t"))
            );
        }
        return builder.toString();
	}

	String printServiceReferences(SatisfiedReferenceDTO[] satisfiedReferences, UnsatisfiedReferenceDTO[] unsatisfiedReferences, ReferenceDTO[] references) {
		StringBuilder builder = new StringBuilder();
		final Map<String, ReferenceDTO> refDtoMap = new HashMap<>();
		if (references != null) {
			for (ReferenceDTO refDto : references)
				refDtoMap.put(refDto.name, refDto);
		}
		int refCount = (satisfiedReferences != null ? satisfiedReferences.length : 0)
				+ (unsatisfiedReferences != null ? unsatisfiedReferences.length : 0);
		builder.append("(total ").append(Integer.toString(refCount)).append(")");
		if (unsatisfiedReferences != null) {
			for (UnsatisfiedReferenceDTO refDto : unsatisfiedReferences)
				printUnsatisfiedReference(refDto, refDtoMap.get(refDto.name), builder);
		}
		if (satisfiedReferences != null) {
			for (SatisfiedReferenceDTO refDto : satisfiedReferences)
				printSatisfiedReference(refDto, refDtoMap.get(refDto.name), builder);
		}
		return builder.toString();
	}

	void printReference(String name, String objectClass, String state, String target, String cardinality, String policy, String policyOption, ServiceReferenceDTO[] bindings, StringBuilder builder) {
		StringBuilder policyWithOption = new StringBuilder().append(policy);
		if (!"reluctant".equals(policyOption))
			policyWithOption.append('+').append(policyOption);

		builder.append(String.format("%n\t%s: %s %s" // name, objectClass, state
						+ "%n\t\t%s %s target=%s" // cardinality, policy+option, target
				, name, objectClass, state
				, cardinality, policyWithOption, target == null ? "(*)" : target));

		if (bindings != null) {
			if (bindings.length == 0) {
				builder.append('\n').append("      Unbound");
			} else {
				for (ServiceReferenceDTO svcDto : bindings) {
					Bundle provider = context.getBundle(svcDto.bundle);
					builder.append(String.format("%n\tBound to [%d] from bundle [%d] %s:%s", svcDto.id, svcDto.bundle, provider.getSymbolicName(), provider.getVersion()));
				}
			}
		}
	}

	void printSatisfiedReference(SatisfiedReferenceDTO satisfiedRefDto, ReferenceDTO refDto, StringBuilder builder) {
		printReference(satisfiedRefDto.name, refDto.interfaceName, "SATISFIED", refDto.target,
				refDto.cardinality, refDto.policy, refDto.policyOption,
				satisfiedRefDto.boundServices != null ? satisfiedRefDto.boundServices : new ServiceReferenceDTO[0], builder);
	}

	void printUnsatisfiedReference(UnsatisfiedReferenceDTO unsatisfiedRefDto, ReferenceDTO refDto, StringBuilder builder) {
		printReference(unsatisfiedRefDto.name, refDto.interfaceName, "UNSATISFIED", refDto.target,
				refDto.cardinality, refDto.policy, refDto.policyOption, null, builder);
	}

	static String underlined(String string, char underlineChar) {
		StringBuilder sb = new StringBuilder();
		sb.append(string);
		sb.append('\n');

		char[] carray = new char[string.length()];
		Arrays.fill(carray, '=');
		sb.append(carray);
		return sb.toString();
	}

	static String printColumnsAligned(String title, Map<String, String> properties) {
		StringBuilder sb = new StringBuilder();
		printColumnsAligned(title, properties, sb);
		return sb.toString();
	}

	static void printColumnsAligned(String title, Map<String, String> properties, StringBuilder builder) {
		builder.append(underlined(title, '='));

		int widestHeader = properties.keySet()
				.stream()
				.mapToInt(String::length)
				.max().orElse(0);
		builder.append('\n').append(properties.entrySet()
				.stream()
				.map(e -> {
					String heading = e.getKey();
					int padLength = widestHeader - heading.length();
					char[] padding = new char[padLength];
					Arrays.fill(padding, ' ');
					return new StringBuilder()
							.append(heading)
							.append(": ")
							.append(padding)
							.append(e.getValue())
							.toString();
				})
				.collect(Collectors.joining("\n")));
	}

}
