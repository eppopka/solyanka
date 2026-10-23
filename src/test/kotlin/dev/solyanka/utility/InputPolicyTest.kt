/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.test.*
import org.junit.jupiter.api.Test

class InputPolicyTest {
    private val bind=CommandBinding(key=71,command="/help",enabled=true)
    private val profile=CommandProfile()
    private val world=InputContext(true,true)
    private fun matches(b: CommandBinding=bind,p: CommandProfile=profile,c: InputContext=world,device: InputDevice=InputDevice.KEYBOARD,key: Int=71,mods: Int=0) = CommandRules.matches(b,p,device,key,mods,c)
    @Test fun `enabled key in focused world matches`() { assertTrue(matches()) }
    @Test fun `offline or unfocused input never fires`() {
        assertFalse(matches(c=world.copy(connected=false))); assertFalse(matches(c=world.copy(focused=false)))
    }
    @Test fun `keyboard and mouse identities cannot collide`() {
        val mouse=bind.copy(device=InputDevice.MOUSE,key=1)
        assertFalse(matches(b=mouse,key=1)); assertTrue(matches(b=mouse,device=InputDevice.MOUSE,key=1))
    }
    @Test fun `disabled unbound and different keys never match`() {
        assertFalse(matches(b=bind.copy(enabled=false))); assertFalse(matches(b=bind.copy(key=-1),key=-1)); assertFalse(matches(key=72))
    }
    @Test fun `container opt in works globally or per bind`() {
        val c=world.copy(screen=ScreenKind.CONTAINER)
        assertFalse(matches(c=c)); assertTrue(matches(c=c,b=bind.copy(allowInGui=true)))
        assertTrue(matches(c=c,p=profile.copy(allowAllInGui=true)))
    }
    @Test fun `anvils other screens and focused text are always blocked`() {
        val p=profile.copy(allowAllInGui=true);val b=bind.copy(allowInGui=true)
        listOf(ScreenKind.ANVIL,ScreenKind.OTHER).forEach { assertFalse(matches(b=b,p=p,c=world.copy(screen=it))) }
        assertFalse(matches(b=b,p=p,c=world.copy(screen=ScreenKind.CONTAINER,textFocused=true)))
    }
    @Test fun `modifier matching ignores lock keys but not extra modifiers`() {
        assertTrue(matches(b=bind.copy(modifier=Modifier.CTRL),mods=2 or 16))
        assertFalse(matches(b=bind.copy(modifier=Modifier.CTRL),mods=3))
        assertTrue(matches(b=bind.copy(modifier=Modifier.NONE),mods=16))
        assertFalse(matches(b=bind.copy(modifier=Modifier.NONE),mods=8))
        assertTrue(matches(b=bind.copy(modifier=Modifier.ANY),mods=15))
    }
    @Test fun `island and class filters are case insensitive and fail closed`() {
        val b=bind.copy(classFilters=listOf("Mage"),islandFilters=listOf("Dungeon Hub"))
        assertFalse(matches(b=b));assertTrue(matches(b=b,c=world.copy(dungeonClass="mage",island="dungeon hub")))
        assertFalse(matches(b=b,c=world.copy(dungeonClass="Tank",island="Dungeon Hub")))
    }
    @Test fun `empty filter lists do not require context`() { assertTrue(matches()) }
    @Test fun `tab context parser finds area and exact player class`() {
        val c=TabContextParser.parse(listOf("§aArea: Dungeon Hub", "[300] [MVP+] Steve (Mage XX)","[30] NotSteve (Tank 10)"),"Steve")
        assertEquals("Dungeon Hub",c.island);assertEquals("Mage",c.dungeonClass)
    }
    @Test fun `tab context does not reuse missing or dead class data`() {
        val c=TabContextParser.parse(listOf("Dungeon: Catacombs", "Steve (DEAD)","OtherSteve (Tank X)"),"Steve")
        assertEquals("Catacombs",c.island);assertNull(c.dungeonClass)
        assertEquals(TabContextParser.Context(null,null),TabContextParser.parse(emptyList(),"Steve"))
    }
}
