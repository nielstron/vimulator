# Vimulator
[![Build Status](https://app.travis-ci.com/nielstron/vimulator.svg?branch=master)](https://app.travis-ci.com/nielstron/vimulator)

A vi-emulator for jEdit.

This project is a "fork" of the jEdit Plugin [Vimulator](http://plugins.jedit.org/plugins/?Vimulator),
modified to work for JDK 15 and jEdit 5+ (or whatever [Isabelle/jEdit](https://isabelle.in.tum.de/) is currently using).

This project is WIP, but already features
- vi-like navigation
- basic commands (i.e. w,q)
- insertion, replace and visual modes
- the 3 visual modes, visual, visual block and visual line
- yanking
- multiple caret editing

## Installation

This plugin is available via the [official Plugin Manager](http://plugins.jedit.org).
You can install it using the Plugin Manager

#### Manual installation
If you do not want to install the plugin through the normal route, you may manually install the compiled Plugin [as a "User Plugin"](http://plugins.jedit.org/install.php).
For Isabelle/jEdit 2021 on Linux/Mac, the user plugin path is `$HOME/.isabelle/Isabelle2021/jedit/jars`.
For Isabelle/jEdit 2021 on Windows, the user plugin path is `%HOMEPATH%\.isabelle\Isabelle2021\jedit\jars`.

You may find the corresponding `Vimulator.jar` under [Releases](https://github.com/nielstron/vimulator/releases) or by building the project yourself.

## Building

Setup ant to point at the correct jdk and jEdit version in `build.xml`.
Then run
```bash
ant dist
```

to build the jar File.
This file can then be manually installed in jEdit via the Plugin Manager.
