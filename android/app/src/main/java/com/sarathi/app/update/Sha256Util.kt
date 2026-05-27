package com.sarathi.app.update

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object Sha256Util {
    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { b -> "%02x".format(b) }
    }

    fun matchesExpected(actualHex: String, expectedHex: String): Boolean =
        actualHex.equals(expectedHex, ignoreCase = true)
}
