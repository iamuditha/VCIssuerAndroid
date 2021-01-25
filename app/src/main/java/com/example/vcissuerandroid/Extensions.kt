package com.example.vcissuerandroid

import java.io.File
import java.io.FileOutputStream
import java.net.URL

fun String.saveTo(path: String) {
    URL(this).openStream().use { input ->
        FileOutputStream(File(path)).use { output ->
            input.copyTo(output)
        }
    }
}