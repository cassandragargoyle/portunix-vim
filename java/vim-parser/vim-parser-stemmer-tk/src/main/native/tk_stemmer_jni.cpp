/*
 * This file is part of CassandraGargoyle Community Project
 * Licensed under the MIT License - see LICENSE file for details
 *
 * JNI bridge for TK Stemmer C++ library
 * Provides native method implementations for org.cassandragargoyle.tk.TKStemmer
 */

#include <jni.h>
#include <tk/stemmer/Stemmer.h>
#include <unordered_map>
#include <mutex>
#include <memory>
#include <string>
#include <vector>

using namespace tk::stemmer;

// Handle management for stemmer instances
static std::unordered_map<jlong, std::shared_ptr<Stemmer>> g_stemmers;
static std::mutex g_mutex;
static jlong g_nextHandle = 1;

extern "C" {

/**
 * Create a new stemmer instance for the specified language
 *
 * Class:     org_cassandragargoyle_tk_TKStemmer
 * Method:    nCreate
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_org_cassandragargoyle_tk_TKStemmer_nCreate(
    JNIEnv* env, jclass clazz, jstring jlanguage)
{
    if (jlanguage == nullptr) {
        return 0;
    }

    const char* language = env->GetStringUTFChars(jlanguage, nullptr);
    if (language == nullptr) {
        return 0;
    }

    auto stemmer = Stemmer::create(language);
    env->ReleaseStringUTFChars(jlanguage, language);

    if (!stemmer) {
        return 0;  // Unsupported language
    }

    std::lock_guard<std::mutex> lock(g_mutex);
    jlong handle = g_nextHandle++;
    g_stemmers[handle] = stemmer;
    return handle;
}

/**
 * Stem a single word
 *
 * Class:     org_cassandragargoyle_tk_TKStemmer
 * Method:    nStem
 * Signature: (JLjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_org_cassandragargoyle_tk_TKStemmer_nStem(
    JNIEnv* env, jclass clazz, jlong handle, jstring jword)
{
    if (jword == nullptr) {
        return nullptr;
    }

    std::shared_ptr<Stemmer> stemmer;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        auto it = g_stemmers.find(handle);
        if (it == g_stemmers.end()) {
            return nullptr;
        }
        stemmer = it->second;
    }

    const char* word = env->GetStringUTFChars(jword, nullptr);
    if (word == nullptr) {
        return nullptr;
    }

    std::string stem = stemmer->stemUtf8(word);
    env->ReleaseStringUTFChars(jword, word);

    return env->NewStringUTF(stem.c_str());
}

/**
 * Stem multiple words in batch
 *
 * Class:     org_cassandragargoyle_tk_TKStemmer
 * Method:    nStemBatch
 * Signature: (J[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_org_cassandragargoyle_tk_TKStemmer_nStemBatch(
    JNIEnv* env, jclass clazz, jlong handle, jobjectArray jwords)
{
    if (jwords == nullptr) {
        return nullptr;
    }

    std::shared_ptr<Stemmer> stemmer;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        auto it = g_stemmers.find(handle);
        if (it == g_stemmers.end()) {
            return nullptr;
        }
        stemmer = it->second;
    }

    jsize length = env->GetArrayLength(jwords);
    if (length == 0) {
        return jwords;
    }

    // Collect input words
    std::vector<std::string> words;
    words.reserve(length);

    for (jsize i = 0; i < length; i++) {
        jstring jword = (jstring)env->GetObjectArrayElement(jwords, i);
        if (jword == nullptr) {
            words.emplace_back("");
            continue;
        }

        const char* word = env->GetStringUTFChars(jword, nullptr);
        if (word != nullptr) {
            words.emplace_back(word);
            env->ReleaseStringUTFChars(jword, word);
        } else {
            words.emplace_back("");
        }
        env->DeleteLocalRef(jword);
    }

    // Stem all words
    std::vector<std::string> stems;
    stems.reserve(length);
    for (const auto& word : words) {
        stems.push_back(stemmer->stemUtf8(word));
    }

    // Create result array
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(length, stringClass, nullptr);

    for (jsize i = 0; i < length; i++) {
        jstring jstem = env->NewStringUTF(stems[i].c_str());
        env->SetObjectArrayElement(result, i, jstem);
        env->DeleteLocalRef(jstem);
    }

    return result;
}

/**
 * Destroy a stemmer instance
 *
 * Class:     org_cassandragargoyle_tk_TKStemmer
 * Method:    nDestroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_org_cassandragargoyle_tk_TKStemmer_nDestroy(
    JNIEnv* env, jclass clazz, jlong handle)
{
    std::lock_guard<std::mutex> lock(g_mutex);
    g_stemmers.erase(handle);
}

} // extern "C"
