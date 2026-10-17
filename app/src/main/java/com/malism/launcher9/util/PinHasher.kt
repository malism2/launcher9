package com.malism.launcher9.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object PinHasher {
    private const val ITERATIONS = 20_000
    private const val HASH_BYTES = 32

    fun create(pin: String): Pair<ByteArray, ByteArray> {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return salt to pbkdf2(pin, salt)
    }

    fun verify(pin: String, salt: ByteArray, expected: ByteArray): Boolean {
        val actual = pbkdf2(pin, salt)
        return MessageDigest.isEqual(actual, expected)
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val password = pin.toByteArray(Charsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))
        val block = pbkdf2Block(mac, salt, ITERATIONS)
        password.fill(0)
        return block.copyOf(HASH_BYTES)
    }

    private fun pbkdf2Block(mac: Mac, salt: ByteArray, iterations: Int): ByteArray {
        mac.reset()
        mac.update(salt)
        mac.update(byteArrayOf(0, 0, 0, 1))
        var u = mac.doFinal()
        val result = u.copyOf()
        for (round in 1 until iterations) {
            mac.reset()
            u = mac.doFinal(u)
            for (index in result.indices) {
                result[index] = (result[index].toInt() xor u[index].toInt()).toByte()
            }
        }
        return result
    }
}
