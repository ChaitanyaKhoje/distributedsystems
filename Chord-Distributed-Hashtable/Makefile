LIB_PATH=/home/yaoliu/src_code/local/lib/usr/local/lib/libthrift-0.10.0.jar:/home/yaoliu/src_code/local/lib/usr/local/lib/slf4j-log4j12-1.7.12.jar:/home/yaoliu/src_code/local/lib/usr/local/lib/slf4j-api-1.7.12.jar:/home/ckhoje1/DS/commons-logging-1.2.jar
all: clean
	mkdir bin
	javac -classpath $(LIB_PATH) -d bin/ src/*.java

clean:
	rm -rf bin/

