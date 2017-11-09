Effective OSGi Runtime: Configuration Processor
===============================================

This bundle extends [Felix FileInstall][1] to process configuration files in a
number of hierarchical formats. Currently supported formats are listed below.

[1]: https://felix.apache.org/documentation/subprojects/apache-felix-file-install.html

INI Format
----------

The INI file format will be familiar to Windows users. The format is essentially
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

If zero sections appear in the file then the properties from the "root" of the
file are used to populate a single configuration with a PID derived from file
name. If one or more named sections appear then each section defines a single
configuration record inside a Factory Configuration. Blah blah blah blah 

In each case, the PID or the Factory PID is derived by the filename by dropping
the ".ini" extension.

### File Name Convention

Files must be created in the `load` directory defined by FileInstall and end
with the extension `.json`. The `.json` suffix is dropped from the filename to
derive the configuration PID or factory PID. For example a file named
`org.example.json` shall be mapped to the PID `org.example`.

Configurator JSON Format
------------------------

This file format is specified by the [OSGi Compendium Release 7][2]
specification, Chapter 150 ("Configurator"). **NB** The Configurator
specification is currently in Draft and subject to change.

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

Note that this format explicitly supports comments -- both single line using `//`
and multi-line using `/*...*/` -- even though comments are not technically
supported in standard JSON.

[2]: https://www.osgi.org/developer/specifications/drafts/