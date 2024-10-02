#include <jni.h>
#include "com_example_fbxchecker_NativeJavaCpp.h"
#include "/project/fbxchecker/fbxchecker/libs/fbx_parse/src/fbxdocument.h"

JNIEXPORT jstring JNICALL Java_com_example_fbxchecker_NativeJavaCpp_fbxParseJson(JNIEnv *env, jobject obj, jstring param) {
    const char *nativeString = env->GetStringUTFChars(param, 0);
    std::string fbxPath(nativeString);
    env->ReleaseStringUTFChars(param, nativeString);

    fbx::FBXDocument doc;
    std::string jsonPath = doc.readFBXwriteJsonFiltered(fbxPath);

    return env->NewStringUTF(jsonPath.c_str());
}
