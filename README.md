Effective OSGi Runtime Bundles
==============================

Sub-projects
------------

 * [com.effectiveosgi.rt.aws](com.effectiveosgi.rt.aws/README.md): Library and component bundle for working with Amazon Web Services.
 * [com.effectiveosgi.rt.config](com.effectiveosgi.rt.config/README.md): An extension for [Felix File Install](https://felix.apache.org/documentation/subprojects/apache-felix-file-install.html) that processes configuration files in various formats.
 * [com.effectiveosgi.rt.inspect](com.effectiveosgi.rt.inspect/README.md): Gogo commands for OSGi system state inspection (SCR, Config).

Installation
------------

This project is delivered as a set of OSGi bundles that can be installed directly
from Maven Central. We recommend adding the following dependency section(s) to
any Maven module that is visible to your index module.

```xml
<!-- AWS Module -->
<dependency>
  <groupId>com.effectiveosgi</groupId>
  <artifactId>com.effectiveosgi.rt.aws</artifactId>
  <version>0.0.1</version>
  <scope>runtime</scope>
</dependency>

<!-- Config Module -->
<dependency>
  <groupId>com.effectiveosgi</groupId>
  <artifactId>com.effectiveosgi.rt.config</artifactId>
  <version>0.1.0</version>
  <scope>runtime</scope>
</dependency>

<!-- Inspect Module -->
<dependency>
  <groupId>com.effectiveosgi</groupId>
  <artifactId>com.effectiveosgi.rt.config</artifactId>
  <version>0.0.1</version>
  <scope>runtime</scope>
</dependency>
```

For example if your project was generated from the [Effective OSGi Maven
Archetypes](https://github.com/effectiveosgi/maven-archetypes) then these can be added to the POM
of the `_distro` module.


Build Status
------------

[![Build Status](https://travis-ci.org/effectiveosgi/eosgi-runtime.svg?branch=master)](https://travis-ci.org/effectiveosgi/eosgi-runtime)