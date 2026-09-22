# Visual Understanding Environment

Visual Understanding Environment (VUE) is a powerful teaching and presentation tool.

## Code Organization

The code is organized in following folders
   
  | Folder name | Folder content |
  | ----------- | -------------- |
  | src         | the source files |
  | test        | automatic JUnit tests |
  | lib         | third party libraries used in VUE |
  | linux       | code specific to Linux operating systems |

## Compiling and Running

### Requirements

  * A Java Development Kit, version 17 or newer (tested with JDK 26 on macOS 26 / Apple Silicon).
    Get one from https://adoptium.net/ or `brew install openjdk`.
  * Apache Ant 1.10 or newer (`brew install ant` on macOS).

### Building and running from source

All commands are run from the `VUE2/src` folder:

  | Command        | Action |
  | -------------- | ------ |
  | `ant compile`  | compiles the code into `build/classes` |
  | `ant vue`      | compiles (if needed) and runs VUE straight from the class files |
  | `ant jar`      | creates `build/VUE.jar`, a single runnable jar containing VUE and all its libraries |
  | `ant mac-dist` | creates `build/MacDist/VUE.app`, a self-contained macOS application (bundles a Java runtime, uses `jpackage`) |
  | `ant clean`    | deletes everything that was built |

Once `ant jar` has run, VUE can be started from anywhere with:

    java -jar build/VUE.jar

Once `ant mac-dist` has run, `build/MacDist/VUE.app` can be copied to `/Applications` and
double-clicked like any other Mac application; it does not need Java to be installed.
macOS will warn that the app is from an unidentified developer the first time; right-click
it and choose *Open* to allow it.

### Notes on modern Java

The code base dates from the Java 5/6 era. To make it build and run on current JDKs the
following was changed (see the git history for details):

  * Applet support (`VueApplet`, Zotero applet actions) was removed; the Java Applet API no
    longer exists in JDK 26.
  * Mac integration (Finder open-file, About / Preferences / Quit in the application menu)
    now uses the standard `java.awt.Desktop` handlers instead of the removed `com.apple.eawt` API.
  * Castor (used to read and write `.vue` map files) is configured to use the bundled Apache
    Xerces instead of the JDK-internal copy, which is no longer accessible.
  * A few uses of JDK-internal classes (`sun.*`, `com.sun.*`) and of `Thread.stop()` were replaced.
  * The build targets Java 17 bytecode and the jar manifest carries the `Add-Opens` /
    `Enable-Native-Access` attributes the old libraries need, so `java -jar VUE.jar` needs no extra flags.

## Contact Information
For further information on VUE, visit [VUE's homepage](http://vue.tufts.edu/).
