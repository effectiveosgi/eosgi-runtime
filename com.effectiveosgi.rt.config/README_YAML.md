Effective OSGi Runtime: YAML Configuration
==========================================

The YAML processor allows configuration files to be read in
[YAML 1.1][1] format. For example:

```yaml
# A singleton config with PID "org.example"
org.example:
  foo: bar
  bar: baz
  Deutsch: &id01       # String array with an anchor
    - Ein
    - Zwei
    - Drei
  German: *id01        # A reference to the above anchor
  numbers:             # an Integer array
    - 1
    - 2
    - 3

# A factory config with Factory PID "org.example"
# and PID "one".
org.example.server~one:
  host: 0.0.0.0
  port: 8080

# A factory config with Factory PID "org.example"
# and PID "two".
org.example.server~two:
  host: 127.0.0.1
  port: 443
  useSsl: Yes
  logRequests: No
```

Implementation Notes
--------------------

A YAML file can be divided into multiple "documents" using the "`---`" marker,
however these document divisions are not significant to the configuration
processor. Each entry at the top level of the file is processed as a
configuration. The name of the file is not significant.

The top level of the document must be an associative array.

The **key** of each top-level element indicates the PID and/or Factory PID of the
configuration record. If the key contains a hyphen character then everything
before the hyphen is the Factory PID and everything after the hyphen is the PID.
If there is no hyphen character then the whole key is the PID of a singleton
configuration.

The **content** of each top-level must be an associative array. These are the
property fields that will populate the configuration map. Note that implicit
conversion from YAML to Java types takes place. For example the value of `port`
in the above example is implicitly mapped to a `java.lang.Integer`. If a value
greater than `Integer.MAX_VALUE` is used then this is mapped to a
`java.lang.Long`. Similarly, numeric values containing a decimal point are
mapped to `java.lang.Double`.

For full details of the type mappings available, refer to the documentation for
[SnakeYAML][2]. Note that not all of the
types that can be produced by SnakeYAML are consumable by OSGi Configuration
Admin.

Acknowledgement
---------------

The YAML file format reader is implemented with [SnakeYAML][2], which is
embedded under the terms of the Apache License Version 2.0.

[1]: http://yaml.org/
     "YAML Ain't Markup Language"

[2]: https://bitbucket.org/asomov/snakeyaml
     "SnakeYAML parser for Java"