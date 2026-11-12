/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.JsonParser
import org.junit.jupiter.api.Test
import kotlin.test.*

class MusicLogicTest {
    private fun state()=JsonParser.parseString("""{"activeDeviceIdOptional":"device","playerState":{"status":{"paused":false,"progressMs":123},"playerQueue":{"entityType":"PLAYLIST","currentPlayableIndex":0,"playableList":[{"playableId":"42","playableType":"TRACK","title":"Song"}],"options":{"repeatMode":"NONE"},"version":{"deviceId":"old","version":1},"customField":"preserved"}}}""").asJsonObject
    @Test fun `connection errors identify stage without exposing exception messages`() {
        assertEquals("Music redirect: unexpected response",MusicLogic.failure("Music redirect",IllegalStateException("secret-token")))
        assertEquals("Sign-in: HTTP 403",MusicLogic.failure("Sign-in",MusicServiceFailure("HTTP 403")))
        assertEquals("Music connection: timeout",MusicLogic.failure("Music connection",java.util.concurrent.ExecutionException(java.util.concurrent.TimeoutException())))
    }
    @Test fun `unavailable registration retries are bounded and respect server backoff`() {
        val diagnostic=MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":14,"http_code":503,"details":{"ynison-error-code":"400090001","message":"secret"}}"""))
        assertEquals("Music registration: server error 14 / HTTP 503 / 400090001",MusicLogic.failure("Music registration",diagnostic))
        val unsafe=MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":14,"details":{"ynison-error-code":"secret-token"}}"""))
        assertEquals("server error 14",unsafe.reason)
        val unavailable=MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":14}"""))
        assertEquals(1000L,MusicLogic.retryDelay(unavailable,0));assertEquals(5000L,MusicLogic.retryDelay(unavailable,1))
        assertEquals(30000L,MusicLogic.retryDelay(unavailable,2));assertEquals(30000L,MusicLogic.retryDelay(unavailable,3));assertNull(MusicLogic.retryDelay(unavailable,4))
        val serverLadder=MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":14,"details":{"ynison-backoff-millis":"0:1000:5000:30000"}}"""))
        assertEquals(30000L,MusicLogic.retryDelay(serverLadder,3))
        val specified=MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":14,"details":{"ynison-backoff-millis":"2000:70000"}}"""))
        assertEquals(2000L,MusicLogic.retryDelay(specified,0));assertNull(MusicLogic.retryDelay(specified,1))
        val goAway=MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":14,"extra_headers":{"ynison-go-away-for-seconds":"3600"}}"""))
        assertNull(MusicLogic.retryDelay(goAway,0));assertEquals(3600L,goAway.goAwaySeconds)
        val nested=MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":14,"details":{},"extra_headers":{"ynison-go-away-for-seconds":"3600"}}"""))
        assertNull(MusicLogic.retryDelay(nested,0))
        val badWait=MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":14,"details":{"ynison-go-away-for-seconds":"bad"}}"""))
        assertNull(badWait.goAwaySeconds);assertNull(MusicLogic.retryDelay(badWait,0))
        val tooLong=MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":14,"details":{"ynison-go-away-for-seconds":"604801"}}"""))
        assertNull(tooLong.goAwaySeconds);assertNull(MusicLogic.retryDelay(tooLong,0))
        val malformed=MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":14,"details":{"ynison-backoff-millis":"bad"}}"""))
        assertNull(MusicLogic.retryDelay(malformed,0))
        assertNull(MusicLogic.retryDelay(MusicLogic.serverFailure(JsonParser.parseString("""{"grpc_code":16}""")),0))
    }
    @Test fun `redirect accepts both protocol field formats without exposing tickets`() {
        for (body in listOf("""{"redirect_ticket":"ticket","session_id":"123"}""", """{"redirectTicket":"ticket","sessionId":123}""")) {
            val json=JsonParser.parseString(body).asJsonObject
            assertEquals("ticket",MusicLogic.redirectValue(json,"redirectTicket"));assertEquals("123",MusicLogic.redirectValue(json,"sessionId"))
        }
        assertFailsWith<MusicServiceFailure> {MusicLogic.redirectValue(JsonParser.parseString("{}").asJsonObject,"redirectTicket")}
    }
    @Test fun `snake case state is read and queue update retains its format`() {
        val state=JsonParser.parseString("""{"active_device_id_optional":"device","player_state":{"status":{"paused":false},"player_queue":{"current_playable_index":0,"entity_type":"PLAYLIST","playable_list":[{"playable_id":"42","playable_type":"TRACK"}]}}}""").asJsonObject
        assertEquals("42" to "",MusicLogic.current(state));assertEquals(1,MusicLogic.queueSize(state))
        val request=MusicLogic.appendRequest(state,"123","remote","rid",1000)
        val player=request.getAsJsonObject("update_player_state").getAsJsonObject("player_state")
        val queue=player.getAsJsonObject("player_queue")
        assertEquals("123",queue.getAsJsonArray("playable_list")[1].asJsonObject["playable_id"].asString)
        val response=JsonParser.parseString("""{"rid":"rid","player_state":$player}""").asJsonObject
        assertTrue(MusicLogic.confirmed(response,"123",1,"rid"));assertEquals(1,MusicLogic.queueSize(state))
        queue.add("shuffle_optional",JsonParser.parseString("{}"))
        assertFails {MusicLogic.appendRequest(response,"99","remote","rid",1000)}
    }
    @Test fun `local track handles blank and unsafe metadata`() {
        assertEquals("Исполнитель — Песня",MusicLogic.localTrack("Песня","Исполнитель"))
        assertEquals("Title",MusicLogic.localTrack("Title",null))
        assertEquals("Artist — Song",MusicLogic.localTrack("Song\n","Artist§"))
        assertNull(MusicLogic.localTrack("  ","Artist"))
        assertEquals(300,MusicLogic.localTrack("x".repeat(500),null)?.length)
    }
    @Test fun `local playback and timed lyrics are bounded and handle missing values`() {
        val basic=JsonParser.parseString("""{"title":"Песня","artist":"Исполнитель"}""").asJsonObject
        val p=MusicLogic.localPlayback(basic,0)!!;assertEquals(0.0,p.duration);assertFalse(p.playing)
        basic.addProperty("art","a".repeat(1_400_000))
        assertEquals(1_400_000,MusicLogic.localPlayback(basic,0)!!.art.length)
        basic.addProperty("art","a".repeat(1_400_001))
        assertTrue(MusicLogic.localPlayback(basic,0)!!.art.isEmpty())
        basic.addProperty("duration",100);basic.addProperty("position",12);basic.addProperty("playing",true)
        val active=MusicLogic.localPlayback(basic,1_000_000_000)!!
        assertEquals(14.0,MusicLogic.elapsed(active,3_000_000_000));assertEquals(17.0,MusicLogic.elapsed(active,20_000_000_000))
        assertEquals("1:14",MusicLogic.timestamp(74.0))
        val lines=MusicLogic.lyrics("\uFEFF[00:03.50]Second\r\n[00:01.00][00:02.00] First\n[00:90.00]bad\n[ar:Artist]\n[01:02:03.250]Hour")
        assertEquals(listOf("First","First","Second","Hour"),lines.map {it.text})
        assertNull(MusicLogic.lyricAt(lines,.9));assertEquals("First",MusicLogic.lyricAt(lines,2.5));assertEquals("Second",MusicLogic.lyricAt(lines,4.0))
        assertEquals("Hour",MusicLogic.lyricAt(lines,3724.0))
        assertTrue(MusicLogic.lyrics("x".repeat(200001)).isEmpty())
        assertEquals(0xffff6675.toInt(),MusicLogic.progressColor(0xffff6675.toInt(),0,1.0,0.0))
        assertNotEquals(MusicLogic.progressColor(-1,1,1.0,0.0),MusicLogic.progressColor(-1,1,1.0,1.0))
        val settings=MusicSettings(serviceLabel="Музыка",playingLabel="Играет",progressEffect=2,showLyrics=true)
        assertEquals(settings,FeatureCodec.decode(com.google.gson.Gson().toJsonTree(FeatureSettings(music=settings)).asJsonObject).music)
    }
    @Test fun `reads active track safely`() {
        assertEquals("42" to "Song",MusicLogic.current(state()))
        assertEquals(CurrentTrack("42","Song","",0.0),MusicLogic.currentTrack(state()))
        val inactive=state();inactive.remove("activeDeviceIdOptional");assertNull(MusicLogic.current(inactive))
    }
    @Test fun `reads track artist duration and all supported cover locations`() {
        val state=JsonParser.parseString("""{
            "activeDeviceIdOptional":"device",
            "playerState":{"playerQueue":{"currentPlayableIndex":0,"playableList":[{
                "playableId":"42","playableType":"TRACK","title":"Song",
                "artists":[{"name":"Artist"}],"durationMs":123000
            }]}}
        }""".trimIndent()).asJsonObject
        assertEquals(CurrentTrack("42","Song","Artist",123.0),MusicLogic.currentTrack(state))
        assertEquals("avatars.yandex.net/get-music-content/abc/%%",MusicLogic.coverUri(
            JsonParser.parseString("""{"coverUri":"avatars.yandex.net/get-music-content/abc/%%"}""").asJsonObject))
        assertEquals("avatars.yandex.net/get-music-content/album/%%",MusicLogic.coverUri(
            JsonParser.parseString("""{"albums":[{"coverUri":"avatars.yandex.net/get-music-content/album/%%"}]}""").asJsonObject))
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
    @Test fun `PKCE S256 matches RFC vector and sign-in codes are bounded`() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",MusicLogic.pkceChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
        assertTrue(MusicLogic.authorizationCodeValid("1234567"))
        assertFalse(MusicLogic.authorizationCodeValid("123456"))
        assertFalse(MusicLogic.authorizationCodeValid("12345678"))
        assertFalse(MusicLogic.authorizationCodeValid("1234abc"))
        assertFails {MusicLogic.pkceChallenge("short")}
    }
}
