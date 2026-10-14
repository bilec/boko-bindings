package io.github.bilec.boko

object Boko {
    init {
        System.loadLibrary("boko_jni")
    }

    @JvmStatic
    external fun convert(input: String, output: String)
}