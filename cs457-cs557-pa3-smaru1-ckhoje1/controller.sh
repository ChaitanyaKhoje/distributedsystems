#!/bin/bash +vx
LIB_PATH=$"/home/vchaska1/protobuf/protobuf-java-3.5.1.jar"
java -classpath bin/classes:$LIB_PATH Controller $1 $2
