package com.subhranil.clouddnsmanager.email

import com.subhranil.clouddnsmanager.email.domain.EmailValidators
import com.subhranil.clouddnsmanager.email.domain.RandomAliasGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class RandomAliasGeneratorTest {

    private val shape = Regex("^[a-z]+-[a-z]+-[0-9]{4}$")

    @Test
    fun `output has the adjective-noun-digits shape`() {
        val generator = RandomAliasGenerator()
        repeat(500) {
            val alias = generator.generate()
            assertTrue("bad alias $alias", shape.matches(alias))
        }
    }

    @Test
    fun `output is always a valid local part`() {
        val generator = RandomAliasGenerator()
        repeat(500) {
            val alias = generator.generate()
            assertTrue("invalid $alias", EmailValidators.validateLocalPart(alias, "example.com").isValid)
        }
    }

    @Test
    fun `words come from the built-in lists`() {
        val generator = RandomAliasGenerator(Random(42))
        repeat(200) {
            val (adjective, noun, _) = generator.generate().split('-')
            assertTrue(adjective in RandomAliasGenerator.ADJECTIVES)
            assertTrue(noun in RandomAliasGenerator.NOUNS)
        }
    }

    @Test
    fun `word lists are large, lowercase and without duplicates`() {
        val adjectives = RandomAliasGenerator.ADJECTIVES
        val nouns = RandomAliasGenerator.NOUNS
        assertTrue(adjectives.size >= 150)
        assertTrue(nouns.size >= 150)
        assertEquals(adjectives.size, adjectives.toSet().size)
        assertEquals(nouns.size, nouns.toSet().size)
        (adjectives + nouns).forEach { assertTrue("bad word $it", Regex("^[a-z]{2,12}$").matches(it)) }
    }

    @Test
    fun `digits are zero padded`() {
        // A Random that always returns 7 for nextInt, so the number part is "0007"
        val fixed = object : Random() {
            override fun nextInt(bound: Int): Int = 7 % bound
        }
        val alias = RandomAliasGenerator(fixed).generate()
        assertTrue(alias.endsWith("-0007"))
    }

    @Test
    fun `generated aliases are varied`() {
        val generator = RandomAliasGenerator()
        val aliases = List(1000) { generator.generate() }.toSet()
        assertTrue(aliases.size > 990)
    }
}
