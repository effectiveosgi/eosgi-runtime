Effective OSGi Runtime: Configuration Processor
===============================================

This bundle extends [Apache Felix File Install][1] to process configuration
files in a number of hierarchical formats. Currently supported formats are as
follows:

* [INI](README_INI.md): a properties file format with named sections, similar to
  Windows INI files.
* [YAML 1.1](README_YAML.md): a YAML file parser based on SnakeYAML.
* [Configurator JSON](README_JSON.md): a JSON-based format defined by the draft
  OSGi Configurator specification (from OSGi Compendium Release 7).

Installation
------------

See the [general installation instructions](../README.md).

Use With Felix File Install
---------------------------

When this bundle is used alongside File Install, it enables File Install to read
and manage files using any of the above supported formats. For example to add a
YAML-based configuration, simply create a YAML file in the `load` directory (or
any other watched directory). To remove that configuration, simply delete the
file (or move it out of the load folder, or rename to a non-supported extension
e.g. `.yaml.bak`).

Note that File Install is not a runtime dependency of this bundle, therefore it
must be added to the run requirements explicitly.

Use Without Felix File Install — Shell Commands
-----------------------------------------------

This bundle published three commands that can be used to manipulate the
configuration of the runtime system:

* `config:install <file>`: installs the config(s) found in the specified file.
* `config:update <file>`: updates any configs that were loaded from the
  specified file.
* `config:uninstall <file>`: uninstalls any configs that were loaded from the
  specified file.

Use Without Felix File Install — Programmatically
-------------------------------------------------

This bundle can be used without File Install to programmatically install, update
and uninstall configurations. Simply bind to the `ArtifactInstaller` service and
invoke it as follows:

```java
@Reference(target = "(type=hierarchical)")
ArtifactInstaller installer;

public void doInstall() {
  File yamlFile = // get file
  installer.install(yamlFile);
  // Later...
  installer.uninstall(yamlFile);
}
```

Note that the `ArtifactInstaller` interface is exported by a File Install API
package, `org.apache.felix.fileinstall`. However, this bundle also exports that
package (substitutably) to allow for use without the File Install bundle.

Extending
---------

The configuration processor can be extended to support new file formats by
implementing a service of type `ConfigFileReader`. This interface has a single
method named `load` that takes a `File` object and returns a stream of
`ParsedRecord`, where `ParsedRecord` has an ID, an optional factory ID, and a
map of properties. The configuration processor takes care of matching these
records against existing Configuration Admin records and either updating or
creating them as required.

The `ConfigFileReader` service must be published with a `patterns` property,
which is a string or string-array of regexes that are matched against the input
file name. For example:

```java
/** ConfigFileReader for XML files */
@Component(property = ConfigFileReader.PROP_FILE_PATTERN + "=.*\\.xml")
public class XmlConfigFileReader implements ConfigFileReader {
  @Override
  public Stream<ParsedRecord> load(File artifact) throws IOException {
    // TODO: parse XML content
  }
}
```

Implementation Note: ensure that any `InputStream` that you open on the file is
properly closed when the returned stream closes. This can be achieved for
example by adding an [onClose
handler](https://docs.oracle.com/javase/8/docs/api/java/util/stream/BaseStream.html#onClose-java.lang.Runnable-).

Note that if multiple `ConfigFileReaders` match the artifact file name, usual
OSGi selection rules apply (i.e. highest rank followed by lowest service ID).

[1]: https://felix.apache.org/documentation/subprojects/apache-felix-file-install.html
     "Apache Felix File Install"
