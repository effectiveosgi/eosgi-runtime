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

[1]: https://felix.apache.org/documentation/subprojects/apache-felix-file-install.html
     "Apache Felix File Install"
