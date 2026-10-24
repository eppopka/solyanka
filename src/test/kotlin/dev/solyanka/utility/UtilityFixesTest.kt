/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.test.*
import org.junit.jupiter.api.Test

class UtilityFixesTest {
    private fun eligible(title:String="SkyBlock Menu",button:Int=0,pickup:Boolean=true,skyblock:Boolean=true,empty:Boolean=false,id:String="",lore:List<String> = emptyList(),loadout:Boolean=false)=MiddleClickPolicy.override(title,button,pickup,skyblock,empty,id,lore,loadout)
    @Test fun `generic menu icons without SkyBlock ID qualify`() { assertTrue(eligible()) }
    @Test fun `only manual left pickup and real SkyBlock context qualify`() {
        assertFalse(eligible(button=1));assertFalse(eligible(button=2));assertFalse(eligible(pickup=false));assertFalse(eligible(skyblock=false));assertFalse(eligible(empty=true));assertFalse(eligible(loadout=true))
    }
    @Test fun `ordinary real item does not override unless whitelisted or transaction`() { assertFalse(eligible(id="SWORD"));assertTrue(eligible(title="Pets",id="PET"));assertTrue(eligible(title="Special Bits Shop",id="SWORD")) }
    @Test fun `all transaction lore lines qualify but substrings do not`() {
        for(line in listOf("Cost","Sell Price","Bazaar Price")) assertTrue(eligible(id="SWORD",lore=listOf(line)))
        assertFalse(eligible(id="SWORD",lore=listOf("Costs 5 coins")))
    }
    @Test fun `blacklist wins over transactions and absent IDs`() {
        for(title in listOf("Chest","Large Chest","Attribute Fusion","Anvil","Storage","Hunting Box","Composter","Auction House","The Hex","Midas Anvil")) {
            assertFalse(eligible(title=title));assertFalse(eligible(title=title,id="SWORD",lore=listOf("Cost")))
        }
    }
    @Test fun `paginated exact blacklist and whitelist match source`() {
        assertFalse(eligible(title="(2/5) Storage"));assertTrue(eligible(title="(2/5) Pets",id="PET"))
        assertFalse(eligible(title="(2/5) Reforge Item",id="SWORD",lore=listOf("Cost")))
    }
    @Test fun `bingo phone and all source menu fragments remain excluded`() {
        for(title in listOf("Redstone Minion VI","Abiphone XIII","AⒷiphone","Enchant Item","Cosmetic Shop","Trap Gear","Gemstones","Heart of the Mountain","Widgets")) assertFalse(eligible(title=title))
    }
    @Test fun `loadout edit prevents override even in whitelist`() { assertFalse(eligible(title="Stats & Equipment",loadout=true)) }
    @Test fun `cursor starts empty and captures finite positions`() {
        val c=CursorMemory();assertNull(c.position(800,600));c.capture(200.0,300.0);assertEquals(200.0 to 300.0,c.position(800,600))
        c.capture(Double.NaN,0.0);c.capture(-1.0,0.0);assertEquals(200.0 to 300.0,c.position(800,600))
    }
    @Test fun `cursor expires only in world without GUI`() {
        val c=CursorMemory();c.capture(10.0,20.0);c.screenChanged(2)
        c.tick(false,false);c.tick(true,true);assertEquals(2,c.ticks)
        c.tick(true,false);assertEquals(1,c.ticks);assertNotNull(c.position(800,600))
        c.tick(true,false);assertNull(c.position(800,600))
    }
    @Test fun `cursor zero expiry retains until explicit reset`() {
        val c=CursorMemory();c.capture(10.0,20.0);c.screenChanged(0);repeat(500) { c.tick(true,false) }
        assertNotNull(c.position(800,600));c.reset();assertNull(c.position(800,600))
    }
    @Test fun `cursor screen changes restart timeout`() {
        val c=CursorMemory();c.capture(10.0,20.0);c.screenChanged(2);c.tick(true,false);c.screenChanged(2);assertEquals(2,c.ticks)
    }
    @Test fun `cursor clamps stored position on resize and rejects zero windows`() {
        val c=CursorMemory();c.capture(999.0,999.0);assertEquals(99.0 to 199.0,c.position(100,200));assertNull(c.position(0,1))
    }
    @Test fun `loading guard leaves singleplayer and LAN intact by default`() {
        assertTrue(LoadingPolicy.hide(true,true,true,false));assertFalse(LoadingPolicy.hide(true,true,false,false));assertFalse(LoadingPolicy.hide(true,true,true,true))
    }
    @Test fun `loading disabled and all server opt in modes`() {
        assertFalse(LoadingPolicy.hide(false,false,true,false));assertTrue(LoadingPolicy.hide(true,false,false,false));assertTrue(LoadingPolicy.hide(true,false,true,true))
    }
}
