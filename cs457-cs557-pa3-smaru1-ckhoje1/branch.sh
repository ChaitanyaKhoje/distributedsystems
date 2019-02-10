#!/bin/bash +vx
LIB_PATH=$"/home/vchaska1/protobuf/protobuf-java-3.5.1.jar"
#port
java -classpath bin/classes:$LIB_PATH Branch $1 $2 $3
