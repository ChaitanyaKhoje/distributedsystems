LIB_PATH= /home/vchaska1/protobuf/protobuf-java-3.5.1.jar

all: clean
	mkdir bin
	mkdir bin/classes
	mkdir Log/
	javac -classpath $(LIB_PATH) -d bin/classes/ src/* 

clean:
	rm -rf bin/ Log/
