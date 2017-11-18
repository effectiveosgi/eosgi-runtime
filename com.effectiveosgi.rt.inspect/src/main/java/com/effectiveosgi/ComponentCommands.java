package com.effectiveosgi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.felix.service.command.Converter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

@Component(
    property = {
        "osgi.command.scope=comp",
        "osgi.command.function=list",
        "osgi.command.function=info",
        Converter.CONVERTER_CLASSES + "=org.osgi.service.component.runtime.dto.ComponentConfigurationDTO"
    })
public class ComponentCommands implements Converter {

    private BundleContext context;

    @Reference
    ServiceComponentRuntime scr;

    @Activate
    void activate(BundleContext context) {
        this.context = context;
    }

    public Collection<ComponentDescriptionDTO> list() {
        return scr.getComponentDescriptionDTOs();
    }

    public ComponentConfigurationDTO info(long id) {
    	for (ComponentDescriptionDTO descDto : scr.getComponentDescriptionDTOs()) {
    		for (ComponentConfigurationDTO configDto : scr.getComponentConfigurationDTOs(descDto)) {
    			if (configDto.id == id) return configDto;
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
        if (target instanceof ComponentDescriptionDTO) {
            result = format((ComponentDescriptionDTO) target, level, escape);
        } else if (target instanceof ComponentConfigurationDTO) {
            result = format((ComponentConfigurationDTO) target, level, escape);
        } else {
        	result = null;
        }
        return result;
    }

    private CharSequence format(ComponentDescriptionDTO dto, int level, Converter escape) throws Exception {
        final StringBuilder builder = new StringBuilder();
        switch (level) {
        case Converter.LINE:
            Collection<ComponentConfigurationDTO> children = scr.getComponentConfigurationDTOs(dto);
            if (children == null) children = Collections.emptyList();

            builder.append(String.format("%s in bundle [%d] (%s:%s) %s, %d instances", dto.name, dto.bundle.id, dto.bundle.symbolicName, dto.bundle.version, dto.defaultEnabled ? "enabled" : "disabled", children.size()));

            for (ComponentConfigurationDTO child : children) {
                builder.append("\n    # ").append(escape.format(child, Converter.LINE, escape));
            }
            break;
        case Converter.INSPECT:
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

            ComponentDescriptionDTO desc = dto.description;
            builder.append(String.format("Id: %d Name: %s%n", dto.id, desc.name));
            builder.append(String.format("State: %s%n", stateToString(dto.state)));
            
            builder.append(String.format("Class: %s%n", desc.implementationClass));
            builder.append(String.format("Service: %s%n", arrayToString(desc.serviceInterfaces)));
            builder.append(String.format("Config (Policy=%s): %s%n", desc.configurationPolicy, arrayToString(desc.configurationPid)));

            // Print References
            final Map<String, ReferenceDTO> refDtoMap = new HashMap<>();
			if (desc != null && desc.references != null) for (ReferenceDTO refDto : desc.references) {
                refDtoMap.put(refDto.name, refDto);
            }
            int refCount = (dto.satisfiedReferences != null ? dto.satisfiedReferences.length : 0) + (dto.unsatisfiedReferences != null ? dto.unsatisfiedReferences.length : 0);
            builder.append(String.format("References (total %d):%n", refCount));
            // builder.append(repeat(20, '-')).append(repeatForDigits(refCount, '-')).append("\n");
            if (dto.unsatisfiedReferences != null) for (UnsatisfiedReferenceDTO refDto : dto.unsatisfiedReferences) {
            	builder.append(format(refDto, refDtoMap.get(refDto.name), Converter.LINE, escape));
            }
            if (dto.satisfiedReferences != null) for (SatisfiedReferenceDTO refDto : dto.satisfiedReferences) {
                builder.append(format(refDto, refDtoMap.get(refDto.name), Converter.LINE, escape));
            }

            result = builder;
            break;
        case Converter.LINE:
            result = String.format("id=%d state=%s", dto.id, stateToString(dto.state));
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
    	return formatReference(satisfiedRefDto.name, refDto.interfaceName, "SATISFIED", refDto.target, refDto.cardinality, refDto.policy, refDto.policyOption, satisfiedRefDto.boundServices != null ? satisfiedRefDto.boundServices : new ServiceReferenceDTO[0]);
    }

    private CharSequence format(UnsatisfiedReferenceDTO unsatisfiedRefDto, ReferenceDTO refDto, int level, Converter escape) {
    	return formatReference(unsatisfiedRefDto.name, refDto.interfaceName, "UNSATISFIED", refDto.target, refDto.cardinality, refDto.policy, refDto.policyOption, null);
    }
    
    private CharSequence formatReference(String name, String objectClass, String state, String target, String cardinality, String policy, String policyOption, ServiceReferenceDTO[] bindings) {
    	StringBuilder result = new StringBuilder();

    	StringBuilder policyWithOption = new StringBuilder().append(policy);
    	if (!"reluctant".equals(policyOption)) policyWithOption.append('+').append(policyOption);

    	result.append(String.format("  %s: %s %s%n" // name, objectClass, state
    			+ "      %s %s target=%s%n" // cardinality, policy+option, target
    			, name, objectClass, state, cardinality, policyWithOption, target == null ? "(*)" : target));
    	
    	if (bindings != null) {
    		if (bindings.length == 0) {
    			result.append("      Unbound").append('\n');
    		} else {
    			for (ServiceReferenceDTO svcDto : bindings) {
    	            Bundle provider = context.getBundle(svcDto.bundle);
    	            result.append(String.format("      Bound to [%d] from bundle [%d] %s:%s%n", svcDto.id, svcDto.bundle, provider.getSymbolicName(), provider.getVersion()));
    			}
    		}
    	}
    	return result;
    }

    static final String stateToString(int state) {
        final String string;
        switch (state) {
            case ComponentConfigurationDTO.ACTIVE: 
                string = "active";
                break;
            case ComponentConfigurationDTO.SATISFIED:
                string = "satisfied";
                break;
            case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
                string = "unsatisfied-config";
                break;
            case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
                string = "unsatisfied-reference";
                break;
            default:
                string = "unknown";
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
    

}
