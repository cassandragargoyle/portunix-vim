cmake_minimum_required(VERSION 3.20)
project(tk-stemmer-jni VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find JNI
find_package(JNI REQUIRED)

# TK Stemmer location (must be provided)
set(TK_STEMMER_DIR "" CACHE PATH "Path to TK Stemmer installation")
if(NOT TK_STEMMER_DIR)
    message(FATAL_ERROR "TK_STEMMER_DIR must be set to the path of TK Stemmer installation")
endif()

# Verify TK Stemmer exists
if(NOT EXISTS "${TK_STEMMER_DIR}/include/tk/stemmer/Stemmer.h")
    message(FATAL_ERROR "TK Stemmer not found at ${TK_STEMMER_DIR}. "
                        "Expected header: ${TK_STEMMER_DIR}/include/tk/stemmer/Stemmer.h")
endif()

# JNI bridge library
add_library(tk-stemmer-jni SHARED
    tk_stemmer_jni.cpp
)

target_include_directories(tk-stemmer-jni PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${TK_STEMMER_DIR}/include
)

target_link_directories(tk-stemmer-jni PRIVATE
    ${TK_STEMMER_DIR}/lib
)

target_link_libraries(tk-stemmer-jni PRIVATE
    tk-stemmer
)

# Platform-specific settings
if(WIN32)
    set_target_properties(tk-stemmer-jni PROPERTIES
        OUTPUT_NAME "tk-stemmer-jni"
        PREFIX ""
        SUFFIX ".dll"
    )
elseif(APPLE)
    set_target_properties(tk-stemmer-jni PROPERTIES
        OUTPUT_NAME "tk-stemmer-jni"
        PREFIX "lib"
        SUFFIX ".dylib"
    )
else()
    set_target_properties(tk-stemmer-jni PROPERTIES
        OUTPUT_NAME "tk-stemmer-jni"
        PREFIX "lib"
        SUFFIX ".so"
    )
endif()

# Installation
install(TARGETS tk-stemmer-jni
    LIBRARY DESTINATION lib
    RUNTIME DESTINATION bin
)
