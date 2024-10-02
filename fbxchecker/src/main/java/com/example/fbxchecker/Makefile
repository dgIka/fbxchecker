
build_native:
	javac NativeJavaCpp.java 
	javac -h . NativeJavaCpp.java
	g++ -I"$$JAVA_HOME/include" -I"$$JAVA_HOME/include/linux" -fPIC cppIfaceLib.cpp -shared -o cppifacelib.so -L/project/fbxchecker/fbxchecker/libs/fbx_parse/build/ -lfbx-dump-lib

clean:
	rm -rf NativeJavaCpp.class com_example_fbxchecker_NativeJavaCpp.h 