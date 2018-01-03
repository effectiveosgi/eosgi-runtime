package com.effectiveosgi;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.effectiveosgi.lib.PropertiesUtil;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.ServiceReferenceDTO;
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
			builder.append(String.format("Name: %s", dto.name));
			builder.append(String.format("%nClass: %s", dto.implementationClass));
			builder.append(String.format("%nService: %s", arrayToString(dto.serviceInterfaces)));
			builder.append(String.format("%nConfig (Policy=%s): %s", dto.configurationPolicy,arrayToString(dto.configurationPid)));
			appendProperties("\nBase Properties", dto.properties, builder);
			break;
			case Converter.PART:
			default:
			break;
		}
		return builder;
	}
	
	private CharSequence format(ComponentConfigurationDTO dto, int level, Converter escape) throws Exception {
		final CharSequence result;
		switch (level) {
		case Converter.INSPECT:
			final StringBuilder builder = new StringBuilder();

			builder.append(String.format("Id: %d", dto.id));
			builder.append(String.format("%nState: %s", stateToString(dto.state)));

			if (dto.failure != null) {
				builder.append("\nFailure: ").append(dto.failure);
			}

			ComponentDescriptionDTO desc = dto.description;
			// Inspect base Descriptor
			builder.append(format(desc, Converter.INSPECT, escape));

			// Print Configuration Propertiesa
			appendProperties("\nConfiguration Properties", dto.properties, builder);

			// Print References
			final Map<String, ReferenceDTO> refDtoMap = new HashMap<>();
			if (desc != null && desc.references != null) {
				for (ReferenceDTO refDto : desc.references)
					refDtoMap.put(refDto.name, refDto);
			}
			int refCount = (dto.satisfiedReferences != null ? dto.satisfiedReferences.length : 0)
					+ (dto.unsatisfiedReferences != null ? dto.unsatisfiedReferences.length : 0);
			builder.append(String.format("%nReferences (total %d):", refCount));
			if (dto.unsatisfiedReferences != null) {
				for (UnsatisfiedReferenceDTO refDto : dto.unsatisfiedReferences)
					builder.append(format(refDto, refDtoMap.get(refDto.name), Converter.LINE, escape));
			}
			if (dto.satisfiedReferences != null) {
				for (SatisfiedReferenceDTO refDto : dto.satisfiedReferences)
					builder.append(format(refDto, refDtoMap.get(refDto.name), Converter.LINE, escape));
			}

			result = builder;
			break;
		case Converter.LINE:
		    String[] pids = PropertiesUtil.getStringArray(dto.properties, Constants.SERVICE_PID, new String[0]);
			result = String.format("Id: % 2d, State: %s, Pid(s): %s", dto.id, stateToString(dto.state), Arrays.toString(pids));
			break;
		case Converter.PART:
		default:
			result = "";
		}
		return result;
	}

	private String arrayToString(String[] array) {
		return array == null || array.length == 0 ? "<<none>>" : Arrays.stream(array).collect(Collectors.joining(", "));
	}

	private CharSequence format(SatisfiedReferenceDTO satisfiedRefDto, ReferenceDTO refDto, int level, Converter escape) {
		return formatReference(satisfiedRefDto.name, refDto.interfaceName, "SATISFIED", refDto.target,
				refDto.cardinality, refDto.policy, refDto.policyOption,
				satisfiedRefDto.boundServices != null ? satisfiedRefDto.boundServices : new ServiceReferenceDTO[0]);
	}

	private CharSequence format(UnsatisfiedReferenceDTO unsatisfiedRefDto, ReferenceDTO refDto, int level, Converter escape) {
		return formatReference(unsatisfiedRefDto.name, refDto.interfaceName, "UNSATISFIED", refDto.target,
				refDto.cardinality, refDto.policy, refDto.policyOption, null);
	}

	private CharSequence formatReference(String name, String objectClass, String state, String target, String cardinality, String policy, String policyOption, ServiceReferenceDTO[] bindings) {
		StringBuilder result = new StringBuilder();

		StringBuilder policyWithOption = new StringBuilder().append(policy);
		if (!"reluctant".equals(policyOption))
			policyWithOption.append('+').append(policyOption);

		result.append(String.format("%n  %s: %s %s" // name, objectClass, state
				+ "%n      %s %s target=%s" // cardinality, policy+option, target
				, name, objectClass, state
				, cardinality, policyWithOption, target == null ? "(*)" : target));

		if (bindings != null) {
			if (bindings.length == 0) {
				result.append('\n').append("      Unbound");
			} else {
				for (ServiceReferenceDTO svcDto : bindings) {
					Bundle provider = context.getBundle(svcDto.bundle);
					result.append(String.format("%n      Bound to [%d] from bundle [%d] %s:%s", svcDto.id, svcDto.bundle, provider.getSymbolicName(), provider.getVersion()));
				}
			}
		}
		return result;
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

	static void appendProperties(String header, Map<String, ?> props, StringBuilder builder) {
        builder.append(String.format("%s (%d entries)", header, props.size()));
        if (!props.isEmpty()) {
            builder.append(":\n\t").append(props.entrySet().stream()
                    .map(e -> String.format("%s<%s> = %s", e.getKey(), e.getValue() != null ? e.getValue().getClass().getSimpleName() : "<<null>>", e.getValue()))
                    .sorted()
                    .collect(Collectors.joining("\n\t"))
            );
        }
	}

}
