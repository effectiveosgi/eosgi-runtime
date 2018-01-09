package com.effectiveosgi;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.effectiveosgi.lib.PropertiesUtil;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.*;
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

	private static final String INDENT = "  ";

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
			throw new IllegalArgumentException(MessageFormat.format("Multiple components matching \"{0}\": [{1}]", name, partialMatches.stream().map(dto -> dto.name).collect(Collectors.joining(", "))));
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
		Collection<ComponentConfigurationDTO> children = scr.getComponentConfigurationDTOs(dto);
		if (children == null)
			children = Collections.emptyList();

		switch (level) {
			case Converter.LINE:
			builder.append(MessageFormat.format("{0} in bundle [{1}] ({2}:{3}) {4}, {5,choice,0#0 instances|1#1 instance|1<{5} instances}.",
				dto.name,
				dto.bundle.id,
				dto.bundle.symbolicName,
				dto.bundle.version,
				dto.defaultEnabled ? "enabled" : "disabled",
				children.size()
			));

			for (ComponentConfigurationDTO child : children)
				builder.append("\n").append(INDENT).append(INDENT).append(escape.format(child, Converter.LINE, escape));
			break;
		case Converter.INSPECT:
			printComponentDescriptionAndConfigs(dto, children.toArray(new ComponentConfigurationDTO[0]), builder);
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
			printComponentDescriptionAndConfigs(dto.description, new ComponentConfigurationDTO[] { dto }, builder);
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

	void printComponentDescriptionAndConfigs(ComponentDescriptionDTO descDto, ComponentConfigurationDTO[] configs, StringBuilder builder) {
		final Map<String, String> out = new LinkedHashMap<>();

		// Component Description
		out.put("Class", descDto.implementationClass);
		out.put("Bundle", String.format("%d (%s:%s)", descDto.bundle.id, descDto.bundle.symbolicName, descDto.bundle.version));
		if (descDto.factory != null) {
			out.put("Factory", descDto.factory);
		}
		out.put("Enabled", Boolean.toString(descDto.defaultEnabled));
		out.put("Immediate", Boolean.toString(descDto.immediate));
		out.put("Services", arrayToString(descDto.serviceInterfaces));
		if (descDto.scope != null) {
			out.put("Scope", descDto.scope);
		}
		out.put("Config PID(s)", String.format("%s, Policy: %s", arrayToString(descDto.configurationPid), descDto.configurationPolicy));
		out.put("Base Props", printProperties(descDto.properties));
		printColumnsAligned(String.format("Component Description: %s", descDto.name), out, '=', builder);

		if (configs != null) for (ComponentConfigurationDTO configDto : configs) {
			out.clear();;

			// Blank line separator
			builder.append("\n\n");

			// Inspect configuration DTO
			String title = String.format("Component Configuration Id: %d", configDto.id);
			out.put("State", stateToString(configDto.state));

			// Print service registration
			try {
				ServiceReference<?>[] serviceRefs = context.getAllServiceReferences(null, String.format("(%s=%d)", ComponentConstants.COMPONENT_ID, configDto.id));
				if (serviceRefs != null && serviceRefs.length > 0) {
					out.put("Service Id", printPublishedService(serviceRefs[0]));
				}
			} catch (InvalidSyntaxException e) {
				// Shouldn't happen...
			}

			// Print Configuration Properties
			out.put("Config Props", printProperties(configDto.properties));

			// Print References
			out.put("References", printServiceReferences(configDto.satisfiedReferences, configDto.unsatisfiedReferences, descDto.references));

			// TODO: ComponentConfigurationDTO.FAILED_ACTIVATION state (==16) added in DS 1.4. Replace with non-reflective calls.
			if (configDto.state == 16) {
				String failure;
				try {
					failure = (String) configDto.getClass().getField("failure").get(configDto);
				} catch (Exception e) {
					failure = "<<unknown>>";
					log.log(LogService.LOG_ERROR, "Unable to get failure message for Component Configuration ID " + configDto.id, e);
				}
				out.put("Failure", failure);
			}
			printColumnsAligned(title, out, '~', builder);
		}
	}

	String printPublishedService(ServiceReference<?> serviceRef) {
		StringBuilder sb = new StringBuilder();
		sb.append(serviceRef.getProperty(Constants.SERVICE_ID));
		sb.append(' ').append(Arrays.toString((String[]) serviceRef.getProperty(Constants.OBJECTCLASS)));

		Bundle[] consumers = serviceRef.getUsingBundles();
		if (consumers != null) for (Bundle consumer : consumers) {
			sb.append("\n").append(INDENT).append(String.format("Used by bundle [%d] (%s:%s)", consumer.getBundleId(), consumer.getSymbolicName(), consumer.getVersion()));
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
		case 16: // TODO: ComponentConfigurationDTO.FAILED_ACTIVATION state (==16) added in DS 1.4.
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
			builder.append("\n").append(INDENT).append(props.entrySet().stream()
					.map(e -> String.format("%s<%s> = %s", e.getKey(), e.getValue() != null ? e.getValue().getClass().getSimpleName() : "<<null>>", e.getValue()))
					.sorted()
					.collect(Collectors.joining("\n" + INDENT))
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
				printServiceReference(refDtoMap.get(refDto.name), "UNSATISFIED", null, builder);
		}
		if (satisfiedReferences != null) {
			for (SatisfiedReferenceDTO refDto : satisfiedReferences)
				printServiceReference(refDtoMap.get(refDto.name), "SATISFIED", refDto.boundServices != null ? refDto.boundServices : new ServiceReferenceDTO[0], builder);
		}
		return builder.toString();
	}

	void printServiceReference(ReferenceDTO reference, String state, ServiceReferenceDTO[] bindings, StringBuilder builder) {
		StringBuilder policyWithOption = new StringBuilder().append(reference.policy);
		if (!"reluctant".equals(reference.policyOption))
			policyWithOption.append('+').append(reference.policyOption);

		builder.append(String.format("%n" + INDENT + "%s: %s %s", reference.name, reference.interfaceName, state));
		builder.append(String.format("%n" + INDENT + INDENT + "%s %s target=%s scope=%s", reference.cardinality, policyWithOption, reference.target == null ? "(*)" : reference.target, reference.scope == null ? "bundle" : reference.scope));

		if (bindings != null) {
			if (bindings.length == 0) {
				builder.append('\n').append(INDENT).append(INDENT).append("Unbound");
			} else {
				for (ServiceReferenceDTO svcDto : bindings) {
					Bundle provider = context.getBundle(svcDto.bundle);
					builder.append(String.format("%n" + INDENT + INDENT + "Bound to [%d] from bundle [%d] %s:%s", svcDto.id, svcDto.bundle, provider.getSymbolicName(), provider.getVersion()));
				}
			}
		}
	}

	static void printColumnsAligned(String title, Map<String, String> properties, char underlineChar, StringBuilder builder) {
		builder.append(title);

		// Generate the title underline
		char[] carray = new char[title.length()];
		Arrays.fill(carray, underlineChar);
		builder.append('\n');
		builder.append(carray);

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
