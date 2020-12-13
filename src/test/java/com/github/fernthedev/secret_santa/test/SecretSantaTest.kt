package com.github.fernthedev.secret_santa.test

import com.github.fernthedev.secret_santa.getSecretSanta
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SecretSantaTest {

    private val santaList = listOf("Tom", "Jerry", "Sam", "Santa")

    @Test
    fun testAlgorithm() {
        val secretSantaMap = getSecretSanta(santaList)

        assertEquals(santaList.size, secretSantaMap.size)


        secretSantaMap.forEach { (santa, receiver) ->
            assertNotEquals(santa, receiver)
        }

    }
}