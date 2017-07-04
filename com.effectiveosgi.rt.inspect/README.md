Effective OSGi RT: Inspection Commands
======================================

This bundle provides Gogo commands for inspecting the state of OSGi.

Component Commands
------------------

These commands provide access to the state of Declarative Services (SCR). They are intended to be a more user-friendly replacement for the `scr:list` and `scr:info` commands.

* `comp:list`: List all known DS components.
* `comp:info <id>`: Show full information on a specific DS component instance ID.

Example Usage:

    g! comp:list
    com.effectiveosgi.ConfigurationCommands in bundle [1] (com.effectiveosgi.rt.inspect:1.0.0.201707041900) enabled, 1 instances
        # id=8 state=active
    com.effectiveosgi.ComponentCommands in bundle [1] (com.effectiveosgi.rt.inspect:1.0.0.201707041900) enabled, 1 instances
        # id=7 state=active
    com.effectiveosgi.dummy.Dummy1 in bundle [1] (com.effectiveosgi.rt.inspect:1.0.0.201707041900) enabled, 1 instances
        # id=9 state=unsatisfied-reference

    g! comp:info 18
    Id: 18 Name: com.effectiveosgi.dummy.Dummy1
    State: active
    Class: com.effectiveosgi.dummy.Dummy1
    Service: <<none>>
    Config (Policy=optional): com.effectiveosgi.dummy.Dummy1
    References (total 3):
      ds: org.osgi.service.component.runtime.ServiceComponentRuntime SATISFIED
          1..1 static target=(service.id=0)
          Bound to [24] from bundle [9] org.apache.felix.scr:2.0.10
      event: org.osgi.service.event.EventAdmin SATISFIED
          1..1 static target=(*)
          Bound to [6] from bundle [3] org.apache.felix.eventadmin:1.4.8
      log: org.osgi.service.log.LogService SATISFIED
          1..1 static target=(*)
          Bound to [22] from bundle [8] org.apache.felix.log:1.0.1

Configuration Commands
----------------------

These commands provide access to the state of Configuration Admin.

* `config:list`: List all configurations.
* `config:list <prefix>`: List all configurations with PID having the specified prefix.
* `config:info <pid>`: Show details of the specified configuration PID. A prefix for the PID can be used as long as it is unique.

Example Usage:

    g! config:list
    org.example.foo [2 record(s)]:
      org.example.foo.1a1fe6b4-fad2-4682-96ad-56a2510c7e4d [4 properties]
      org.example.foo.31b1de74-5a13-4410-a12a-3e2224df0a07 [4 properties]
    com.effectiveosgi.dummy.Dummy1 [5 properties]

    g! config:info com.effectiveosgi.dummy.Dummy1
    com.effectiveosgi.dummy.Dummy1 (2 changes)  Unbound
      ds.target:String=(service.id=*)
      felix.fileinstall.filename:String=file:/.../com.effectiveosgi.dummy.Dummy1.cfg
      host:String=127.0.0.1
      port:String=4447
      service.pid:String=com.effectiveosgi.dummy.Dummy1