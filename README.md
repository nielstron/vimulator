# Vimulator
A vi-emulator for jEdit.

This project is a "fork" of the jEdit Plugin [Vimulator](plugins.jedit.org/plugins/?Vimulator),
modified to work for jdk 15 and jEdit 5+ (or whatever [Isabelle/jEdit](https://isabelle.in.tum.de/) is currently using).

This project is WIP, but already featuring vi-like navigation and newline/merging.

## Installation

This plugin is not (yet) available via the official Plugin Manager.
You may however manually install the compiled Plugin [as a "User Plugin"](http://plugins.jedit.org/install.php).
You may find the corresponding `Vimulator.jar` in the Releases section or by
building the project yourself.

## Building

Setup ant to point at the correct jdk and jEdit version in `build.xml`.
Then run
```bash
ant dist
```

to build the jar File.
This file can the be manually installed in jEdit via the Plugin Manager.