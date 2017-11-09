Effective OSGi RT: JSON Configuration Processor
===============================================

This bundle extends [Felix FileInstall](https://felix.apache.org/documentation/subprojects/apache-felix-file-install.html) to process configuration files in JSON format.

## File Name Convention

Files must be created in the `load` directory defined by FileInstall and end with the extension `.json`.

The `.json` suffix is dropped from the filename to derive the configuration PID or factory PID. For example a file named `org.example.json` shall be mapped to the PID `org.example`.

## JSON Structure

The structure of the file must be either a single top-level JSON object or a JSON array of objects.

Where the content is a single object, a singleton configuration record is created/updated using the properties of that object. For example:

    {
      "host.ip" : "127.0.0.1",
      "host.port" : 8080,
      "enableSSL" : false
    }

Where the content is an array of objects, a factory configuration is created with one record per object in the array. For example:

    [
      {
        "host.ip" : "0.0.0.0",
        "host.port" : 80,
        "enableSSL" : false
      }
      ,
      {
        "host.ip" : "0.0.0.0",
        "host.port" : 443,
        "enableSSL" : true,
        "ssl.keystore" : "server.jks"
      }
    ]

## JSON Types

Types in JSON documents are mapped to Java as follows:

* JSON strings are mapped to Java String.
* JSON numbers are mapped to Java Double.
* JSON booleans are mapped to Java Boolean.
* Arrays of JSON strings are mapped to List<String>.
* Arrays of JSON numbers are mapped to List<Double>.

## Update Handling for Factory Configurations

In FileInstall's normal operation, factory configuration instances are correlated to individual files on the filesystem -- this makes it easy to detect when a single instances has been updated, since the last-updated timestamp of the correlated file will have changed.

Under our JSON-based system, the last-updated timestamp of the file tells us that at least one record has changed, added, or removed. But it does not tell us *which* record has changed. This is a problem because in a large file with many records, an edit action on the file usually only affects a small subset of those records.

We address this by generating a content hash of each JSON array entry and storing this in a hidden property of the configuration record. When the file is updated we regenerate the content hashes for every entry, and those that have not changed will not be updated in Configuration Admin. Entries that have changed will have a new content hash, so the configuration record for the old hash is deleted and a new one created.

This scheme has the following minor drawbacks:

1. Factory configuration records are never updated as such, but only added or deleted. If a DS component defines a @Modified method to optimise dynamic reconfiguration, that method is never called.

2. Duplicate entries with the exact same content cannot be created. However, if it is desired to create two identically configured component instances, then a simple dummy property can be used, e.g.:


    [
      {
        "message" : "Hello",
        "language" : "en",
        ".dummy" : 1
      }
      ,
      {
        "message" : "Hello",
        "language" : "en",
        ".dummy" : 2
      }
    ]

The revision to Configuration Admin in OSGi Release 7 is expected to address these issues.

N.B.: content hash generation is based on the **logical** JSON content, so adding whitespace does not affect it.