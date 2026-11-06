/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonParser
import org.junit.jupiter.api.Test
import kotlin.test.*

class MusicLogicTest {
    private fun state()=JsonParser.parseString("""{"activeDeviceIdOptional":"device","playerState":{"status":{"paused":false,"progressMs":123},"playerQueue":{"entityType":"PLAYLIST","currentPlayableIndex":0,"playableList":[{"playableId":"42","playableType":"TRACK","title":"Song"}],"options":{"repeatMode":"NONE"},"version":{"deviceId":"old","version":1},"customField":"preserved"}}}""").asJsonObject
    @Test fun `reads active track safely`() {
        assertEquals("42" to "Song",MusicLogic.current(state()))
        val inactive=state();inactive.remove("activeDeviceIdOptional");assertNull(MusicLogic.current(inactive))
    }
    @Test fun `append preserves observed queue and does not alter input`() {
        val original=state();val request=MusicLogic.appendRequest(original,"123","remote","rid",1000)
        val player=request.getAsJsonObject("updatePlayerState").getAsJsonObject("playerState")
        val queue=player.getAsJsonObject("playerQueue")
        assertEquals(2,queue.getAsJsonArray("playableList").size());assertEquals("preserved",queue["customField"].asString)
        assertEquals(123,player.getAsJsonObject("status")["progressMs"].asInt)
        assertEquals(1,original.getAsJsonObject("playerState").getAsJsonObject("playerQueue").getAsJsonArray("playableList").size())
        val result=JsonParser.parseString("""{"rid":"rid","playerState":${player}}""").asJsonObject
        assertTrue(MusicLogic.confirmed(result,"123",1,"rid"));assertFalse(MusicLogic.confirmed(result,"123",1,"wrong"))
    }
    @Test fun `reject shuffle and radio`() {
        val shuffle=state();shuffle.getAsJsonObject("playerState").getAsJsonObject("playerQueue").add("shuffleOptional",JsonParser.parseString("{}"))
        assertFails {MusicLogic.appendRequest(shuffle,"123","remote","rid",1000)}
        val radio=state();radio.getAsJsonObject("playerState").getAsJsonObject("playerQueue").add("queue",JsonParser.parseString("{\"waveQueue\":{}}"))
        assertFails {MusicLogic.appendRequest(radio,"123","remote","rid",1000)}
    }
}
