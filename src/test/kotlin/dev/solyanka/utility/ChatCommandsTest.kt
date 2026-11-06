/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.*

class ChatCommandsTest {
    @Test fun `own command responses work and only remembered echoes are suppressed`() {
        val settings=ChatCommandsSettings(commands=listOf(ChatCommand("bpm","{name} 93")))
        val own=ChatCommandsLogic.parse("Party > [MVP+] 3ppopka: !bpm")!!
        assertEquals("3ppopka 93",ChatCommandsLogic.response(settings,own))
        val echoes=ChatReplyEchoes()
        assertFalse(echoes.consume("party","!bpm",100))
        echoes.remember("party","!bpm",100)
        assertFalse(echoes.consume("guild","!bpm",101))
        assertTrue(echoes.consume("party","!bpm",101))
        assertFalse(echoes.consume("party","!bpm",102))
        echoes.remember("party","!bpm",100)
        assertFalse(echoes.consume("party","!bpm",30_101))
    }
    @Test fun `party message and template are parsed`() {
        val chat=ChatCommandsLogic.parse("Party > [MVP+] 3ppopka: !bpm")!!
        assertEquals("party",chat.channel);assertEquals("3ppopka",chat.name)
        val settings=ChatCommandsSettings(commands=listOf(ChatCommand("bpm","{name} {randint(60,100)}")))
        val text=ChatCommandsLogic.response(settings,chat,Random(1))!!
        assertTrue(text.startsWith("3ppopka "));assertTrue(text.substringAfter(' ').toInt() in 60..100)
        assertNull(ChatCommandsLogic.response(settings.copy(party=false),chat))
    }
    @Test fun `unicode commands and responses match ignoring case`() {
        val settings=ChatCommandsSettings(commands=listOf(ChatCommand("привет","Привет, {name}!")))
        val chat=ChatCommandsLogic.parse("Party > Player: !ПРИВЕТ")!!
        assertEquals("Привет, Player!",ChatCommandsLogic.response(settings,chat))
    }
    @Test fun `command inside message is optional and uses whole words`() {
        val settings=ChatCommandsSettings(commands=listOf(ChatCommand("bpm","{name} 93"),ChatCommand("привет","Здравствуй")))
        fun chat(text:String)=IncomingCommandChat("party","Player",text)
        assertNull(ChatCommandsLogic.response(settings,chat("текст !bpm")))
        val anywhere=settings.copy(matchAnywhere=true)
        assertEquals("Player 93",ChatCommandsLogic.response(anywhere,chat("текст !bpm ещё текст")))
        assertEquals("Здравствуй",ChatCommandsLogic.response(anywhere,chat("текст !ПРИВЕТ")))
        assertNull(ChatCommandsLogic.response(anywhere,chat("текст!bpm")))
        assertNull(ChatCommandsLogic.response(anywhere,chat("!bpmm")))
        assertEquals("baby shark",ChatCommandsLogic.match(anywhere,"текст !suggest baby shark",true)?.suggestion)
        assertNull(ChatCommandsLogic.match(settings,"текст !suggest baby shark",true))
        assertNull(ChatCommandsLogic.response(anywhere.copy(party=false),chat("текст !bpm")))
        val decoded=FeatureCodec.decode(com.google.gson.Gson().toJsonTree(FeatureSettings(chatCommands=anywhere)).asJsonObject)
        assertTrue(decoded.chatCommands.matchAnywhere)
        assertFalse(FeatureCodec.decode(com.google.gson.JsonObject()).chatCommands.matchAnywhere)
    }
    @Test fun `other channels and malformed messages`() {
        assertEquals("guild",ChatCommandsLogic.parse("Guild > [VIP] Player [Member]: !bpm")?.channel)
        assertEquals("private",ChatCommandsLogic.parse("From [VIP] Player: !bpm")?.channel)
        assertEquals("public",ChatCommandsLogic.parse("[VIP] Player: !bpm")?.channel)
        assertNull(ChatCommandsLogic.parse("Random server message"))
        assertNull(ChatCommandsLogic.response(ChatCommandsSettings(commands=listOf(ChatCommand("bpm","/p kick victim"))),IncomingCommandChat("party","Player","!bpm")))
    }
    @Test fun `settings validate duplicates and preserve round trip`() {
        ChatCommandsSettings(commands=listOf(ChatCommand("привет-мир","x"))).validate()
        assertFails { ChatCommandsSettings(commands=listOf(ChatCommand("bpm","x"),ChatCommand("BPM","y"))).validate() }
        val original=FeatureSettings(chatCommands=ChatCommandsSettings(commands=listOf(ChatCommand("bpm","{name}"))))
        assertEquals(original.chatCommands,FeatureCodec.decode(com.google.gson.Gson().toJsonTree(original).asJsonObject).chatCommands)
    }
}
