Effective OSGi RT Command Line Bridge
=====================================

This bundle is a wrapper and drop-in replacement for the Apache Felix Gogo
shell. It functions mostly the same as the Felix Gogo shell, but it adds the
ability to call commands directly from the Operating System command line. It
requires that the application is launched by the [bnd
launcher](https://bnd.bndtools.org/chapters/300-launching.html), for example if
you have generated a standalone application JAR from a `.bndrun` file.

Example:

```plain
$ java -jar myapp.jar lb -s
START LEVEL 1
   ID|State      |Level|Symbolic name
    0|Active     |    0|org.apache.felix.framework (6.0.0)|6.0.0
    1|Active     |    1|org.apache.felix.gogo.command (1.1.0)|1.1.0
    2|Active     |    1|org.apache.felix.gogo.runtime (1.0.0)|1.0.0
    3|Active     |    1|com.effectiveosgi.rt.command (0.0.3)|0.0.3
    4|Active     |    1|org.apache.felix.log (1.2.0)|1.2.0
```

In this example, the `lb -s` command is passed through to the Gogo shell and
executed directly.

When the command completes, the application terminates. If the command returned
an object, it will be printed to standard output using the Gogo formatting
mechanism.

Note that if the invoked Gogo command fails (i.e. throws an Exception) then a
non-zero response code is returned by the process, which allows you to
integrate your OSGi application into a traditional script. In all other cases
the process returns zero.

If the same application JAR file is invoked without any commands on the command
line, then the normal interactive Gogo shell will be started (unless the
`-n`/`--noshell` option is used, see below).

Installation
------------

The bundle can be installed from Maven:

```xml
<dependency>
    <groupId>com.effectiveosgi</groupId>
    <artifactId>com.effectiveosgi.rt.command</artifactId>
    <version>0.0.6</version>
    <scope>runtime</scope>
</dependency>
```

Usage
-----

The simplest usage pattern is to simply pass the command name, and any
arguments required by the command, after the `java -jar myapp.jar` invocation.
The command and its arguments are interpreted directly by Gogo:

    $ java -jar myapp.jar COMMAND-NAME [PARAMS]

The bridge takes additional arguments which can the command name, e.g.:

    $ java -jar myapp.jar --detail=full COMMAND-NAME [PARAMS]

In this pattern, the command name is assumed to be the FIRST argument that does
not begin with a dash ("-") character. The command name and any arguments after
it are NOT interpreted by the bridge, but passed straight through to Gogo.

It is also possible to explicitly indicate the end of the bridge options with a
double-dash ("--") argument. This could be useful for example if you need to
execute a command whose name begins with a dash (though this would be very
unusual!):

    $ java -jar myapp.jar -d full -- COMMAND-NAME [PARAMS]

### Executing a Script

Instead of executing a command directly from the command line, you can execute
a script file using the `-s`/`--script` option:

    $ java -jar myapp.jar --script=example.gosh

### Full Options

* `-?` or `--help`: Show usage instructions and exit.

* `-d LEVEL` or `--detail=LEVEL`: Set the detail level at which the return
  object from the command will be printed. Possible values are none, basic and
  full. The default is basic.

* `-n` or `--noshell`: Do not run an interactive shell when no commands are
  found on the command line.

* `-s FILE` or `--script=FILE`: Run the specified Gogo script. When this option
  is used, any command passed on the command line will be ignored.

* `-q` or `--quiet`: Don't print the message-of-the-day when the shell starts.

Replacing the Message-of-the-Day
--------------------------------

The message-of-the-day (MOTD) is printed whenever the interactive shell starts,
except when the `-q`/`--quiet` option is used. You can also print it at any time
by invoking the `motd` command.

You can override the MOTD in your own bundle by providing a Capability in the
namespace `eosgi.rt.command.motd` with a `path` attribute that points at a
resource inside the bundle containing the message text.

For example in your bundle's `META-INF/MANIFEST.MF`:

    Provide-Capability: eosgi.rt.command.motd; path=resources/message.txt

Where the bundle must contain a resource at path `resources/message.txt`.

If multiple bundles with valid MOTD capabilities are found, then a random one
will be chosen each time the shell opens or the `motd` command is executed
(like the UNIX "fortune" command). You can even contribute multiople MOTDs
from a single bundle.
