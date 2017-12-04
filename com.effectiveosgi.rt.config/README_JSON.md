Effective OSGi Runtime: INI Configuration
=========================================

This file format is specified by the [OSGi Compendium Release 7][1]
specification, Chapter 150 ("Configurator"). **N.B.:** The Configurator
specification is currently in draft and subject to change.

Example:

```json
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
  "org.example.threadpool~main" : {
    "threads" : 50,
    // ...
  }
}
```

Note that this format explicitly supports comments, both single line using `//`
and multi-line using `/*...*/`, even though comments are not strictly supported
in standard JSON.

A file using this format must be named with the `.json` extension.

[1]: https://www.osgi.org/developer/specifications/drafts/
     "OSGi Compendium Release 7 Specification"
