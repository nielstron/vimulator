# Vimulator
A vi-emulator for jEdit.

This project is a "fork" of the jEdit Plugin [Vimulator](plugins.jedit.org/plugins/?Vimulator),
modified to work for jdk 15 and jEdit 5+ (or whatever [Isabelle/jEdit](https://isabelle.in.tum.de/) is currently using).

This project is WIP, but already featuring vi-like navigation, yanking, newlines, a fast insertion mode and a visual mode.

## Building

Setup ant to point at the correct jdk and jEdit version in `build.xml`.
Then run
```bash
ant dist
```

to build the jar File.
This file can the be manually installed in jEdit via the Plugin Manager.
