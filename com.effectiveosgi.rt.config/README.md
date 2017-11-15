Effective OSGi Runtime: Configuration Processor
===============================================

This bundle extends [Apache Felix File Install][1] to process configuration
files in a number of hierarchical formats. Currently supported formats are as
follows:

* [INI](README_INI.md): a properties file format with named sections, similar to
  Windows INI files.
* [YAML 1.1](README_YAML.md): a YAML file parser based on SnakeYAML.
* [Configurator JSON](README_JSON.md): a JSON-based format defined by the draft
  OSGi Configuration specification (from OSGi Compendium Release 7).

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

Use Without Felix File Install
------------------------------

This bundle can be used without File Install to programmatically install, update
and uninstall configurations. Simply bind to the `ArtifactInstaller` service and
invoke it as follows:

    @Reference(target = "(type=hierarchical)")
    ArtifactInstaller installer;

    public void doInstall() {
      File yamlFile = // get file
      installer.install(yamlFile);
      // Later...
      installer.uninstall(yamlFile);
    }

Note that the `ArtifactInstaller` interface is exported by a File Install API
package, `org.apache.felix.fileinstall`. However, this bundle also exports that
package (substitutably) to allow for use without the File Install bundle.

[1]: https://felix.apache.org/documentation/subprojects/apache-felix-file-install.html
     "Apache Felix File Install"
