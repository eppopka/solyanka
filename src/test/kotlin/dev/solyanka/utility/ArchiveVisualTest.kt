/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import com.google.gson.*
import dev.solyanka.ui.*
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.*
import java.nio.file.Path
import kotlin.math.sin

class ArchiveVisualTest {
    @Test fun `new settings migrate from an empty legacy config`() {assertEquals(FeatureSettings(),FeatureCodec.decode(JsonObject()))}
    @Test fun `three new settings roundtrip without touching previous features`() {
        val f=FeatureSettings(ambientParticles=AmbientParticleSettings("firefly",8,24),rotatingItem=RotatingItemSettings("wave",100,80,.2,-.1,.3),moveHearts=MoveHeartsSettings(true,true,HudSpec(x=42,y=84,scale=1.5)))
        assertEquals(f,FeatureCodec.decode(Gson().toJsonTree(f).asJsonObject))
    }
    @Test fun `ambient settings reject unsupported styles and unbounded density`() {
        for(s in listOf(AmbientParticleSettings(particle="dust"),AmbientParticleSettings(amount=0),AmbientParticleSettings(amount=11),AmbientParticleSettings(radius=3),AmbientParticleSettings(radius=33))) assertFails {s.validate()}
    }
    @Test fun `every source particle style is exposed and roundtrips`() {assertEquals(8,AmbientParticleLogic.types.distinct().size);assertEquals(8,AmbientParticleLogic.labels.size);for(t in AmbientParticleLogic.types) FeatureCodec.decode(Gson().toJsonTree(FeatureSettings(ambientParticles=AmbientParticleSettings(t))).asJsonObject)}
    @Test fun `particle sample distribution stays inside source bounds`() {
        val r=Random(42);for(radius in listOf(4,12,32)) repeat(1000) {
            val a=AmbientParticleLogic.sample(AmbientParticleSettings(radius=radius),r)
            assertTrue(a.x>=-radius && a.x<radius);assertTrue(a.z>=-radius && a.z<radius);assertTrue(a.y>=-1.5 && a.y<6.5)
            assertTrue(a.vx>=-.01 && a.vx<.01);assertTrue(a.vz>=-.01 && a.vz<.01);assertTrue(a.vy>-.015 && a.vy<=-.005)
        }
    }
    @Test fun `seeded particle samples are reproducible and not constant`() {val s=AmbientParticleSettings();val a=Random(1);val b=Random(1);val samples=List(10) {AmbientParticleLogic.sample(s,a)};assertEquals(samples,List(10) {AmbientParticleLogic.sample(s,b)});assertEquals(10,samples.distinct().size)}
    @Test fun `rotation rejects unknown modes and out of range values`() {
        for(s in listOf(RotatingItemSettings(mode="invalid"),RotatingItemSettings(speed=4),RotatingItemSettings(speed=201),RotatingItemSettings(amplitude=4),RotatingItemSettings(amplitude=101),RotatingItemSettings(offsetX=.51),RotatingItemSettings(offsetY=Double.NaN),RotatingItemSettings(offsetZ=Double.NEGATIVE_INFINITY))) assertFails {s.validate()}
    }
    @Test fun `rotation mode labels and keys cover all five source modes`() {assertEquals(5,RotatingItemLogic.modes.distinct().size);assertEquals(5,RotatingItemLogic.labels.size);for(m in RotatingItemLogic.modes) RotatingItemSettings(mode=m).validate()}
    @Test fun `clockwise and counterclockwise source degrees are opposed`() {
        assertEquals(-90.0,RotatingItemLogic.motion(1_000_000_000,RotatingItemSettings(mode="clockwise"),false).rz)
        assertEquals(90.0,RotatingItemLogic.motion(1_000_000_000,RotatingItemSettings(mode="counterclockwise"),false).rz)
    }
    @Test fun `spin uses twice the roll rate around Y only`() {val m=RotatingItemLogic.motion(1_000_000_000,RotatingItemSettings(mode="spin"),false);assertEquals(180.0,m.ry);assertEquals(0.0,m.rx);assertEquals(0.0,m.rz)}
    @Test fun `rotation speed multiplier matches source and wraps`() {
        assertEquals(180.0,RotatingItemLogic.motion(1_000_000_000,RotatingItemSettings(mode="counterclockwise",speed=100),false).rz)
        assertEquals(0.0,RotatingItemLogic.motion(4_000_000_000,RotatingItemSettings(mode="clockwise"),false).rz,.000001)
    }
    @Test fun `chaotic math matches source frequencies and amplitudes`() {val m=RotatingItemLogic.motion(1_000_000_000,RotatingItemSettings(),false);assertEquals(sin(2.1)*14,m.rx,1e-9);assertEquals(sin(3.1)*18,m.ry,1e-9);assertEquals(sin(5.6)*14,m.rz,1e-9)}
    @Test fun `wave keeps offsets and adds only Y bob and Z tilt`() {val s=RotatingItemSettings(mode="wave",offsetX=.2,offsetY=.1,offsetZ=-.2);val m=RotatingItemLogic.motion(1_000_000_000,s,false);assertEquals(.2,m.x);assertEquals(-.2,m.z);assertEquals(.1+sin(2.0)*.02,m.y,1e-9);assertEquals(sin(2.8)*6,m.rz,1e-9);assertEquals(0.0,m.rx);assertEquals(0.0,m.ry)}
    @Test fun `left hand mirrors only the horizontal offset`() {val s=RotatingItemSettings(offsetX=.3,offsetY=.2,offsetZ=-.1);val a=RotatingItemLogic.motion(12_345_678,s,false);val b=RotatingItemLogic.motion(12_345_678,s,true);assertEquals(a.copy(x=-a.x),b)}
    @Test fun `negative elapsed time behaves as the reset epoch`() {for(m in RotatingItemLogic.modes) {val s=RotatingItemSettings(mode=m);assertEquals(RotatingItemLogic.motion(0,s,false),RotatingItemLogic.motion(-10,s,false))}}
    @Test fun `long running animation remains finite in every mode`() {for(mode in RotatingItemLogic.modes) {val m=RotatingItemLogic.motion(Long.MAX_VALUE,RotatingItemSettings(mode=mode,speed=200,amplitude=100),true);assertTrue(listOf(m.x,m.y,m.z,m.rx,m.ry,m.rz).all {it.isFinite()});assertTrue(kotlin.math.abs(m.rz)<=360 || mode=="spin")}}
    @Test fun `default heart transform is identity on native baseline`() {val t=HeartLayout.transform(MoveHeartsSettings(),960,540);assertEquals(389.0,t.x);assertEquals(501.0,t.y);assertEquals(0.0,t.translateX);assertEquals(0.0,t.translateY);assertEquals(1.0,t.scale)}
    @Test fun `heart default follows window dimensions without changing config`() {val s=MoveHeartsSettings();val a=HeartLayout.editorHud(s,640,360);val b=HeartLayout.editorHud(s,960,540);assertEquals(229,a.x);assertEquals(321,a.y);assertEquals(389,b.x);assertEquals(501,b.y);assertFalse(s.customPosition)}
    @Test fun `heart scale transform maps native baseline to the saved coordinates`() {val s=MoveHeartsSettings(true,true,HudSpec(x=70,y=100,scale=2.0));val t=HeartLayout.transform(s,960,540);assertEquals(70.0,389*t.scale+t.translateX);assertEquals(100.0,501*t.scale+t.translateY)}
    @Test fun `heart coordinates and tiny windows are bounded`() {for(w in listOf(0,20,320)) for(h in listOf(0,10,180)) {val t=HeartLayout.transform(MoveHeartsSettings(true,hud=HudSpec(x=9999,y=9999,scale=4.0)),w,h);assertTrue(t.x>=0 && t.y>=0);assertTrue(t.x<=(w-81*4).coerceAtLeast(0));assertTrue(t.y<=(h-9*4).coerceAtLeast(0))}}
    @Test fun `heart settings reject invalid HUD coordinates and scale`() {for(h in listOf(HudSpec(x=-1),HudSpec(y=10001),HudSpec(scale=Double.NaN),HudSpec(scale=.49))) assertFails {MoveHeartsSettings(hud=h).validate()}}
    @Test fun `editor hearts geometry matches the live baseline size`() {val e=HudLayout.entries(FeatureSettings(),640,360).single {it.key=="archive.hearts"};assertEquals(81,e.baseWidth);assertEquals(9,e.baseHeight);assertEquals(HudBounds(229,321,81,9),HudLayout.bounds(e,999,640,360))}
    @Test fun `first editor scale change uses current screen not default resolution`() {val f=FeatureSettings();val n=HudLayout.update(f,"archive.hearts",640,360) {it.copy(scale=1.5)};assertTrue(n.moveHearts.customPosition);assertEquals(229,n.moveHearts.hud.x);assertEquals(321,n.moveHearts.hud.y);assertEquals(1.5,n.moveHearts.hud.scale);assertEquals(f.copy(moveHearts=n.moveHearts),n)}
    @Test fun `heart editor supports explicit origin without auto reset`() {val n=HudLayout.update(FeatureSettings(),"archive.hearts") {it.copy(x=0,y=0)};assertTrue(n.moveHearts.customPosition);assertEquals(0,HeartLayout.editorHud(n.moveHearts,960,540).x);assertEquals(0,HeartLayout.editorHud(n.moveHearts,960,540).y)}
    @Test fun `new sections reject type errors and retain old independent values`() {for(raw in listOf("""{"ambientParticles":{"amount":2.5}}""","""{"rotatingItem":{"offsetX":"0"}}""","""{"moveHearts":{"customPosition":1}}""")) assertFails {FeatureCodec.decode(JsonParser.parseString(raw).asJsonObject)};assertEquals(80,FeatureCodec.decode(JsonParser.parseString("""{"stopwatch":{"hud":{"x":80}}}""").asJsonObject).stopwatch.hud.x)}
    @Test fun `new settings use atomic model commits and reject a failed save`() {val original=GeneralSettings();var live=original;val m=InstantSettings(CommandProfile(),original) {_,g->if(g.features.ambientParticles.amount==9) "read only" else {live=g;null}};assertFalse(m.updateGeneral {it.copy(features=it.features.copy(ambientParticles=AmbientParticleSettings(amount=9)))});assertEquals(original,live);assertEquals(9,m.general.features.ambientParticles.amount);assertTrue(m.pending());assertNotNull(m.error)}
    @Test fun `native heart wrapper delegates and restores the pose stack`() {val s=source("feature/VanillaHearts.kt");assertTrue(s.contains("HudElementRegistry.replaceElement"));assertTrue(s.contains("original.extractRenderState(g,d)"));assertTrue(s.contains("finally {g.pose().popMatrix()}"));assertFalse(s.contains("setHealth"));assertFalse(s.contains("removeElement"));val c=source("SolyankaClient.kt");assertTrue(c.indexOf("Suppression.register()")<c.indexOf("VanillaHearts.register()"))}
    @Test fun `ambient spawning is world paused bounded and client only`() {val s=source("feature/ArchiveVisuals.kt");for(t in listOf("!S.isEnabled(S.AMBIENT_PARTICLES)","client.isPaused","client.level ?: return","client.player ?: return","repeat(s.amount)","level.addParticle(")) assertTrue(t in s,t);assertFalse("sendCommand" in s);assertFalse("sendChat" in s)}
    @Test fun `new visual settings are embedded and use sliders and choice rows`() {val s=source("ui/ArchiveVisualUi.kt");for(t in listOf("host.showPanel(SettingsPanel(","model.updateGeneral","GuiKit.choiceRow(","GuiKit.sliderRow(")) assertTrue(t in s,t);assertFalse("setScreen" in s);assertFalse("Save & Back" in s)}
    @Test fun `successful heart change commits immediately without a save button`() {var live=GeneralSettings();var calls=0;val m=InstantSettings(CommandProfile(),live) {_,g->live=g;calls++;null};assertTrue(m.updateGeneral {it.copy(features=HudLayout.update(it.features,"archive.hearts",640,360) {h->h.copy(x=100,y=50,scale=2.0)})});assertEquals(1,calls);assertEquals(100,live.features.moveHearts.hud.x);assertTrue(live.features.moveHearts.customPosition);assertFalse(m.pending())}
    private fun source(path:String)=Path.of("src/client/kotlin/dev/solyanka/client/$path").toFile().readText()
}
