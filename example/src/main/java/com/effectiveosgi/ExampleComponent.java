package com.effectiveosgi;

import org.osgi.service.component.annotations.*;

@Component
public class ExampleComponent {

    @Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
    Runnable runnable;

}
