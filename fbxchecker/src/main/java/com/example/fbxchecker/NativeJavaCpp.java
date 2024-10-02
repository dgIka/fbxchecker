package com.example.fbxchecker;

public class NativeJavaCpp {
    static {
        System.load("/project/fbxchecker/fbxchecker/libs/fbx_parse/build/libfbx-dump-lib.so");
        System.load("/project/fbxchecker/fbxchecker/src/main/java/com/example/fbxchecker/cppifacelib.so");
    }

    // Принимает путь до fbx, возвращает путь до json 
    public native String fbxParseJson(String fbxPath);
}