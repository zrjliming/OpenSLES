
cmake_minimum_required(VERSION 3.10.2)


project("pcmplay")

aux_source_directory(. SOURCE)
add_library(
        PlayJni
        SHARED
        ${SOURCE}
)


find_library(
        log-lib
        log)


target_link_libraries(
        PlayJni
        OpenSLES
        android
        ${log-lib})