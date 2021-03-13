#!/bin/bash

export JAVA_HOME=/home/niels/git/vimulator2/bin/jdk-15.0.2
export PATH=$JAVA_HOME/bin:$PATH

ant dist
java -jar bin/jEdit/jedit.jar -log=7