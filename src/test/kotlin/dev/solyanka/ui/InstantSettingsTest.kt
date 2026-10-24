/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui

import dev.solyanka.utility.*
import kotlin.test.*
import org.junit.jupiter.api.Test

class InstantSettingsTest {
    private class Sink {
        var profile=CommandProfile();var general=GeneralSettings();var calls=0;var fail:String?=null
        fun model()=InstantSettings(profile,general) { p,g ->
            calls++;if(fail==null) { profile=p;general=g };fail
        }
    }
    @Test fun `opening settings does not write or mutate anything`() {
        val s=Sink();val m=s.model();assertEquals(0,s.calls);assertFalse(m.pending());assertNull(m.error)
    }
    @Test fun `toggle persists immediately without save action`() {
        val s=Sink();val m=s.model()
        assertTrue(m.updateGeneral { it.copy(tooltips=it.tooltips.copy(skyblockId=true)) })
        assertTrue(s.general.tooltips.skyblockId);assertEquals(1,s.calls);assertFalse(m.pending())
    }
    @Test fun `valid numeric edits save immediately`() {
        val s=Sink();val m=s.model();assertTrue(m.lines("5000"));assertTrue(m.feedback("0"))
        assertEquals(5000,s.general.chat.lines);assertEquals(0,s.general.chat.feedbackLimit);assertEquals(2,s.calls)
    }
    @Test fun `temporary blank numeric input never replaces last good config`() {
        val s=Sink();val m=s.model();m.lines("2000");assertFalse(m.lines(""))
        assertEquals("",m.lines);assertEquals(2000,s.general.chat.lines);assertEquals(1,s.calls);assertNotNull(m.error)
        assertTrue(m.lines("3000"));assertEquals(3000,s.general.chat.lines);assertFalse(m.pending())
    }
    @Test fun `out of range and fractional numbers are held locally`() {
        val s=Sink();val m=s.model()
        for(value in listOf("99","5001","1.2","2147483648","abc")) assertFalse(m.lines(value))
        assertEquals(0,s.calls);assertEquals(1000,s.general.chat.lines)
    }
    @Test fun `keyboard and mouse captures save typed identities`() {
        val s=Sink();val m=s.model()
        m.updateGeneral { it.copy(chat=it.chat.copy(copy=CopyBinding(InputDevice.MOUSE,2))) }
        assertEquals(CopyBinding(InputDevice.MOUSE,2),s.general.chat.copy)
        m.updateGeneral { it.copy(chat=it.chat.copy(copy=CopyBinding())) }
        assertEquals(-1,s.general.chat.copy.key)
    }
    @Test fun `add edit and delete persist without leaving list or editor`() {
        val s=Sink();val m=s.model();val b=CommandBinding(id="one")
        m.updateProfile { it.copy(binds=it.binds+b) };assertEquals(1,s.profile.binds.size)
        m.updateProfile { it.copy(binds=it.binds.map { b -> b.copy(name="Changed",command="/help",enabled=true) }) }
        assertEquals("Changed",s.profile.binds.single().name);assertTrue(s.profile.binds.single().enabled)
        m.updateProfile { it.copy(binds=emptyList()) };assertTrue(s.profile.binds.isEmpty());assertEquals(3,s.calls)
    }
    @Test fun `invalid enabled command stays local until corrected`() {
        val s=Sink();s.profile=CommandProfile(binds=listOf(CommandBinding(id="one",command="/help",enabled=true)))
        val m=s.model();assertFalse(m.updateProfile { it.copy(binds=it.binds.map { b -> b.copy(command="") }) })
        assertEquals("/help",s.profile.binds.single().command);assertEquals("",m.profile.binds.single().command)
        assertTrue(m.updateProfile { it.copy(binds=it.binds.map { b -> b.copy(command="/list") }) })
        assertEquals("/list",s.profile.binds.single().command);assertNull(m.error)
    }
    @Test fun `reserved shortcut and cycles are not activated`() {
        val s=Sink();val m=s.model()
        assertFalse(m.updateProfile { it.copy(shortcuts=listOf(Shortcut("one","solyanka","/help",true))) })
        assertEquals(0,s.calls);assertTrue(s.profile.shortcuts.isEmpty())
        assertFalse(m.updateProfile { it.copy(shortcuts=listOf(Shortcut("one","a","/a",true))) })
        assertEquals(0,s.calls)
    }
    @Test fun `persistence failure keeps last good live state and visible error`() {
        val s=Sink();s.fail="Read-only config";val m=s.model()
        assertFalse(m.updateGeneral { it.copy(tooltips=it.tooltips.copy(museum=true)) })
        assertFalse(s.general.tooltips.museum);assertTrue(m.general.tooltips.museum);assertEquals("Read-only config",m.error)
        s.fail=null;assertTrue(m.updateGeneral { it });assertTrue(s.general.tooltips.museum);assertFalse(m.pending())
    }
    @Test fun `failed transient text blocks remaining pending edits until fixed`() {
        val s=Sink();val m=s.model();m.lines("")
        assertFalse(m.updateGeneral { it.copy(chat=it.chat.copy(keepHistory=true)) });assertEquals(0,s.calls)
        assertTrue(m.lines("1500"));assertTrue(s.general.chat.keepHistory);assertEquals(1500,s.general.chat.lines)
    }
    @Test fun `closing and reopening keeps successful edits not invalid pending input`() {
        val s=Sink();val m=s.model();m.lines("2500");m.lines("bad")
        val reopened=s.model();assertEquals("2500",reopened.lines);assertNull(reopened.error);assertFalse(reopened.pending())
    }
}
