package com.effectiveosgi.annotation;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.namespace.unresolvable.UnresolvableNamespace;

@Requirement(namespace = UnresolvableNamespace.UNRESOLVABLE_NAMESPACE, filter = "(&(must.not.resolve=*)(!(must.not.resolve=*)))")
public final class PreventResolve {
}
