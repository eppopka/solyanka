/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.test.*
import org.junit.jupiter.api.Test

class CommandResolverTest {
    private fun profile(vararg shortcuts: Pair<String,String>) = CommandProfile(shortcuts=shortcuts.map { Shortcut(alias=it.first,message=it.second,enabled=true) })
    private fun text(result: Resolution) = assertIs<Resolution.Message>(result).text
    @Test fun `unrelated long server commands do not enter alias interceptor`() {
        val p=profile("a" to "/help")
        assertFalse(CommandResolver.isAliasInvocation("/unrelated " + "x".repeat(1000),p))
        assertTrue(CommandResolver.isAliasInvocation("/a Steve",p))
        assertFalse(CommandResolver.isAliasInvocation("/another",p))
    }
    @Test fun `disabled aliases and chat do not enter interceptor`() {
        assertFalse(CommandResolver.isAliasInvocation("/a",CommandProfile(shortcuts=listOf(Shortcut(alias="a",message="/help")))))
        assertFalse(CommandResolver.isAliasInvocation("a",profile("a" to "/help")))
    }
    @Test fun `exact alias appends supplied arguments`() {
        assertEquals("/party invite Steve", text(CommandResolver.resolve("/pi Steve",profile("pi" to "/party invite"))))
    }
    @Test fun `alias names never match prefixes`() {
        assertEquals("/ping Steve",text(CommandResolver.resolve("/ping Steve",profile("pi" to "/party invite"))))
    }
    @Test fun `chain resolves once with argument order preserved`() {
        assertEquals("/party invite Alex Steve", text(CommandResolver.resolve("/a Steve",profile("a" to "/b Alex", "b" to "/party invite"))))
    }
    @Test fun `plain chat target preserves appended arguments`() {
        assertEquals("hello Steve",text(CommandResolver.resolve("/hello Steve",profile("hello" to "hello"))))
    }
    @Test fun `disabled feature does not resolve aliases`() {
        assertEquals("/pi Steve",text(CommandResolver.resolve("/pi Steve",profile("pi" to "/party invite"),false)))
    }
    @Test fun `disabled entry does not resolve`() {
        assertEquals("/a",text(CommandResolver.resolve("/a",CommandProfile(shortcuts=listOf(Shortcut(alias="a",message="/b"))))))
    }
    @Test fun `self cycle with growing arguments is rejected`() {
        assertIs<Resolution.Error>(CommandResolver.resolve("/a",profile("a" to "/a x")))
    }
    @Test fun `mutual cycle is rejected`() {
        assertIs<Resolution.Error>(CommandResolver.resolve("/a",profile("a" to "/b", "b" to "/a")))
    }
    @Test fun `long acyclic chains are bounded`() {
        val p=profile(*(0..16).map { "a$it" to "/a${it+1}" }.toTypedArray())
        assertIs<Resolution.Error>(CommandResolver.resolve("/a0",p))
    }
    @Test fun `oversized expansion is rejected`() {
        assertIs<Resolution.Error>(CommandResolver.resolve("/a "+"x".repeat(250),profile("a" to "/party invite")))
    }
    @Test fun `control characters and empty commands are rejected`() {
        listOf("", "/", "hello\nworld", "hello\u0000", "§ahello").forEach { assertIs<Resolution.Error>(CommandResolver.resolve(it,CommandProfile())) }
    }
    @Test fun `normal messages are unchanged and not marked expanded`() {
        val result=assertIs<Resolution.Message>(CommandResolver.resolve("/party list",profile("pi" to "/party invite")))
        assertEquals("/party list",result.text); assertFalse(result.expanded)
    }
    @Test fun `shortcut token matching is case sensitive`() {
        assertEquals("/A",text(CommandResolver.resolve("/A",profile("a" to "/help"))))
    }
    @Test fun `optional slash and surrounding whitespace normalize alias`() {
        assertEquals("/help",text(CommandResolver.resolve("/a",profile(" /a " to " /help "))))
    }
    @Test fun `reserved shortcut and duplicates are rejected`() {
        assertFailsWith<IllegalArgumentException> { CommandRules.validate(profile("solyanka" to "/help")) }
        assertFailsWith<IllegalArgumentException> { CommandRules.validate(profile("a" to "/help", "/a" to "/list")) }
    }
    @Test fun `blank disabled draft is allowed`() { CommandRules.validate(CommandProfile(binds=listOf(CommandBinding()),shortcuts=listOf(Shortcut()))) }
    @Test fun `invalid keyboard and mouse codes are rejected`() {
        assertFailsWith<IllegalArgumentException> { CommandRules.validate(CommandProfile(binds=listOf(CommandBinding(key=2)))) }
        assertFailsWith<IllegalArgumentException> { CommandRules.validate(CommandProfile(binds=listOf(CommandBinding(device=InputDevice.MOUSE,key=8)))) }
    }
    @Test fun `unbound enabled entry remains configurable`() { CommandRules.validate(CommandProfile(binds=listOf(CommandBinding(command="/help",enabled=true)))) }
}
