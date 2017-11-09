Effective OSGi Runtime: Configuration Processor
===============================================

This bundle extends [Apache Felix File Install][1] to process configuration
files in a number of hierarchical formats. Currently supported formats are
listed below.

Ini Format
----------

The Ini file format will be familiar to Windows users. The format is essentially
a Java properties file but with labelled sections, for example:

    [http]
    host=127.0.0.1
    port=80
    protocol=http

    [https]
    host=0.0.0.0
    port=80
    protocol=https
    keyStorePath=${user.name}/.keystore

If zero section headers are present then the properties in the file are used to
populate a Singleton Configuration with a PID derived from the file name. If one
or more named sections appear then each section defines a single configuration
record inside a Factory Configuration.

A file using this format must be named with the `.ini` extension. The PID or the
Factory PID is derived from the filename by dropping the extension.

### Property Value Types

The Ini file reader loads each property value either as a string
(`java.lang.String` or a string array (`java.lang.String[]`). No other property
types are supported — if more flexible types are needed then it is recommended
to use the Configurator JSON format (see [below](#configurator-json-format)).

To create a String array value, simply repeat the property name, e.g.:

    languages: en
    languages: fr
    languages: de

The `languages` property will have the value `{"en", "fr", "de"}`. It is not
possible to create a single-element string array as this will always be
interpreted as a simple string.

### Property Substitution

Property substitution is supported using the format
`${section/property[index]}`, which can be embedded anywhere in a property
value. Where the section name is omitted then the current section is assumed and
where the index is omitted the last value is used. So in the following example:

    [http.server]
    host=localhost
    host=10.0.0.1
    port=8080

    [http.client]
    path=/index.html
    url=http://${http.server/host[0]}:${http.server/port}${path}

... the `url` property in the `http.client` section shall be resolved as
`http://localhost:8080/index.html`.

The following special section names can be used:

* `?` refers to the root context, i.e. outside of any section. In a factory
  configuration file this root section is not used directly, so it is a good
  place to store any shared values.
* `@prop` refers to Java system properties;
* `@env`refers to environment variables.

For example:

    # Common values...
    app=myapp
    global.logDir=${@env/SYSTEM_LOG}/${app}/logs
    local.logDir=${@prop/user.home}/.${app}/logs

    [server1]
    accessLog=${?/global.logDir}/access.log

    [server2]
    accessLog=${?/log.dir}/access.log

* `server1/accessLog` resolves to the value of the SYSTEM_LOG environment
  variable plus `myapp/logs/access.log`.
* `server2/accessLog` resolves to the user's home directory plus
  `.myapp/logs/access.log`.

### Acknowledgement

The Ini file format reader is implemented with [\[ini4j\]][2] and embedded under
the terms of the Apache License Version 2.0.

Configurator JSON Format
------------------------

This file format is specified by the [OSGi Compendium Release 7][3]
specification, Chapter 150 ("Configurator"). **N.B.:** The Configurator
specification is currently in draft and subject to change.

Example:

    {
      // A singleton config with PID org.example.server
      "org.example.server" : {
        "key" : "val",
        "my_long" : 123,             // Parsed as a java.lang.Long
        "my_double", : 123.4         // Parsed as a java.lang.Double
        "my_float:float", 123.4      // Parsed as a java.lang.Float

        "my_array:int[]" : [1,2,3],
        "my_collection:Collection<String>" : ["one", "two", "three"]
      },

      // A factory config with Factory PID org.example.threadpool
      "org.example.threadpool-main" : {
        "threads" : 50,
        // ...
      }
    }

Note that this format explicitly supports comments, both single line using `//`
and multi-line using `/*...*/`, even though comments are not strictly supported
in standard JSON.

[1]: https://felix.apache.org/documentation/subprojects/apache-felix-file-install.html
     "Apache Felix File Install"
[2]: http://ini4j.sourceforge.net
     "[ini4j]"
[3]: https://www.osgi.org/developer/specifications/drafts/
     "OSGi Compendium Release 7 Specification"