/* SPDX-License-Identifier: GPL-3.0-only
 * Odin DungeonWaypointConfig / DungeonRoom / SecretWaypoints format and transform
 * adaptation; (c)2025 odtheking, BSD-3-Clause. Bounded file storage is solyanka. */
package dev.solyanka.utility
import com.google.gson.*
import java.nio.file.*
import java.util.Base64
import java.util.zip.*
import java.io.ByteArrayOutputStream
import kotlin.math.*
data class Point3(val x:Double,val y:Double,val z:Double)
data class WayBox(val minX:Double,val minY:Double,val minZ:Double,val maxX:Double,val maxY:Double,val maxZ:Double) {
 fun validate() {require(listOf(minX,minY,minZ,maxX,maxY,maxZ).all {it.isFinite() && abs(it)<=100000});require(minX<maxX && minY<maxY && minZ<maxZ);require(maxX-minX<=1024 && maxY-minY<=1024 && maxZ-minZ<=1024)}
}
enum class WayType {NONE,NORMAL,SECRET,ETHERWARP}
data class Waypoint(val blockPos:GridPos,val color:Int=0xff55ff55.toInt(),val filled:Boolean=false,val depth:Boolean=false,val aabb:WayBox=WayBox(-.002,-.002,-.002,1.002,1.002,1.002),val title:String="",val type:WayType=WayType.NONE)
data class WaypointSettings(val selectedPacks:String="default",val editPack:String="default",val allowEdits:Boolean=false,val globalEsp:Boolean=false,val titles:Boolean=true,val titleScale:Double=1.0,val type:Int=0,val color:Int=0xff55ff55.toInt(),val filled:Boolean=false,val depth:Boolean=false,val blockSize:Boolean=true,val sizeX:Double=1.0,val sizeY:Double=1.0,val sizeZ:Double=1.0) {
 fun validate() {val names=selectedPacks.split(',').map(String::trim);require(names.size<=32 && names.all(WaypointStore::validName));require(WaypointStore.validName(editPack));require(type in 0..3);require(titleScale.isFinite() && titleScale in .1..4.0);require(listOf(sizeX,sizeY,sizeZ).all {it.isFinite() && it in .1..5.0})}
}
object WaypointLogic {
 private val secretDrops=listOf("Health Potion VIII Splash Potion","Healing Potion 8 Splash Potion","Healing Potion VIII Splash Potion","Healing VIII Splash Potion","Healing 8 Splash Potion","Decoy","Inflatable Jerry","Spirit Leap","Trap","Training Weights","Defuse Kit","Dungeon Chest Key","Treasure Talisman","Revive Stone","Architect's First Draft","Secret Dye","Candycomb")
 fun secretDrop(name:String)=secretDrops.any {ChatLogic.plain(name).contains(it,true)}
 fun rotate(p:Point3,facing:RoomFacing):Point3=when(facing) {RoomFacing.NORTH->Point3(-p.x,p.y,-p.z);RoomFacing.SOUTH->p;RoomFacing.WEST->Point3(-p.z,p.y,p.x);RoomFacing.EAST->Point3(p.z,p.y,-p.x)}
 fun box(b:WayBox,pos:GridPos,origin:GridPos,f:RoomFacing):WayBox {
  // Odin rotates box offsets around the block center, then offsets the rotated block.
  val real=DungeonLogic.real(pos,origin,f)
  val a=rotate(Point3(b.minX-.5,b.minY-.5,b.minZ-.5),f);val z=rotate(Point3(b.maxX-.5,b.maxY-.5,b.maxZ-.5),f)
  return WayBox(min(a.x,z.x)+.5+real.x,min(a.y,z.y)+.5+real.y,min(a.z,z.z)+.5+real.z,max(a.x,z.x)+.5+real.x,max(a.y,z.y)+.5+real.y,max(a.z,z.z)+.5+real.z)
 }
 data class Geometry(val origin:GridPos,val facing:RoomFacing)
 fun geometry(tiles:Set<Pair<Int,Int>>,shape:String,height:Int):Geometry? {
  val expected=when(shape) {"1x2"->2;"1x3","L"->3;"1x4","2x2"->4;else->return null}
  if(tiles.size!=expected) return null
  val minX=tiles.minOf {it.first};val maxX=tiles.maxOf {it.first};val minZ=tiles.minOf {it.second};val maxZ=tiles.maxOf {it.second}
  val tx=minX*32-185;val tz=minZ*32-185;val bx=maxX*32-185;val bz=maxZ*32-185
  val f:RoomFacing;val o:GridPos
  if(shape=="L") {
   if(maxX-minX!=1 || maxZ-minZ!=1) return null
   // Normalize the canonical L (missing north-east) by its missing corner.
   // Avoid upstream iteration-order ambiguity for the missing north-west corner.
   val missing=(minX..maxX).flatMap {x->(minZ..maxZ).map {z->x to z}}.single {it !in tiles}
   when(missing) {
    maxX to minZ -> {f=RoomFacing.SOUTH;o=GridPos(tx-15,0,tz-15)}
    minX to maxZ -> {f=RoomFacing.NORTH;o=GridPos(bx+15,0,bz+15)}
    maxX to maxZ -> {f=RoomFacing.WEST;o=GridPos(bx+15,0,tz-15)}
    else -> {f=RoomFacing.EAST;o=GridPos(tx-15,0,bz+15)}
   }
  } else {
   if(shape=="2x2") {if(maxX-minX!=1 || maxZ-minZ!=1)return null}
   else if(!((minX==maxX && maxZ-minZ==expected-1) || (minZ==maxZ && maxX-minX==expected-1))) return null
   f=if(minX==maxX) RoomFacing.WEST else RoomFacing.SOUTH
   o=if(f==RoomFacing.WEST) GridPos(tx+15,0,tz-15) else GridPos(tx-15,0,tz-15)
  }
  return Geometry(o,f)
 }
}
typealias WayPack = Map<String,List<Waypoint>>
object WaypointCodec {
 const val LIMIT=8*1024*1024
 private val gson=GsonBuilder().setPrettyPrinting().create()
 fun decode(input:String):WayPack {
  require(input.length<=LIMIT) {"Waypoint pack is too large"}
  val text=if(input.trimStart().startsWith("{")) input else {
   val bytes=Base64.getDecoder().decode(input.trim());GZIPInputStream(bytes.inputStream()).use {val data=it.readNBytes(LIMIT+1);require(data.size<=LIMIT) {"Expanded pack is too large"};data.toString(Charsets.UTF_8)}
  }
  val root=JsonParser.parseString(text);require(root.isJsonObject);require(root.asJsonObject.size()<=512)
  var count=0
  return root.asJsonObject.entrySet().associate { (room,value) ->
   require(room.isNotBlank() && room.length<=128 && value.isJsonArray && value.asJsonArray.size()<=2000)
   room to value.asJsonArray.map {el ->
    require(++count<=20000);val o=el.asJsonObject
    fun int(v:JsonElement):Int {require(v.isJsonPrimitive && v.asJsonPrimitive.isNumber);return v.asBigDecimal.intValueExact()}
    val pos=(o.get("blockPos") ?: o).asJsonObject
    val p=if(pos.has("field_11173")) GridPos(int(pos["field_11175"]),int(pos["field_11174"]),int(pos["field_11173"])) else GridPos(int(pos["x"]),int(pos["y"]),int(pos["z"]))
    require(abs(p.x.toLong())<=100000 && p.y in -2048..2048 && abs(p.z.toLong())<=100000)
    val color=o["color"].asString.removePrefix("#");require(color.matches(Regex("[0-9a-fA-F]{8}")))
    val rgba=color.toLong(16);val argb=((rgba ushr 8) or ((rgba and 255) shl 24)).toInt()
    val b=o["aabb"].asJsonObject
    val names=when {b.has("minX")->listOf("minX","minY","minZ","maxX","maxY","maxZ");b.has("field_1323")->listOf("field_1323","field_1322","field_1321","field_1320","field_1325","field_1324");else->listOf("field_72340_a","field_72338_b","field_72339_c","field_72336_d","field_72337_e","field_72334_f")}
    val values=names.map {b[it].asDouble};val box=WayBox(values[0],values[1],values[2],values[3],values[4],values[5]);box.validate()
    val title=o["title"]?.takeIf {!it.isJsonNull}?.asString ?: "";require(title.length<=128 && title.none {it=='\n' || it=='\r'})
    val type=o["type"]?.takeIf {!it.isJsonNull}?.asString?.let(WayType::valueOf) ?: if(o["secret"]?.asBoolean==true) WayType.SECRET else WayType.NONE
    fun bool(k:String):Boolean {val v=o[k];require(v.isJsonPrimitive && v.asJsonPrimitive.isBoolean);return v.asBoolean}
    Waypoint(p,argb,bool("filled"),bool("depth"),box,title,type)
   }
  }
 }
 fun encode(pack:WayPack,compressed:Boolean=false):String {
  val root=JsonObject();pack.forEach {(room,ways)->root.add(room,JsonArray().apply {ways.forEach {w->add(JsonObject().apply {
   add("blockPos",gson.toJsonTree(w.blockPos));val rgba=((w.color.toLong() and 0xffffff) shl 8) or ((w.color ushr 24).toLong());addProperty("color","#%08X".format(rgba));addProperty("filled",w.filled);addProperty("depth",w.depth);add("aabb",gson.toJsonTree(w.aabb));if(w.title.isNotEmpty()) addProperty("title",w.title);addProperty("type",w.type.name)
  })} })}
  val text=gson.toJson(root);require(text.toByteArray().size<=LIMIT);decode(text)
  if(!compressed)return text
  val out=ByteArrayOutputStream();GZIPOutputStream(out).use {it.write(text.toByteArray())};return Base64.getEncoder().encodeToString(out.toByteArray())
 }
}
class WaypointStore(private val directory:Path) {
 companion object {fun validName(s:String)=s.matches(Regex("[a-zA-Z0-9 _-]{1,64}")) && s.trim()==s}
 private fun path(name:String):Path {require(validName(name)) {"Pack name: 1–64 letters, digits, spaces, - or _"};return directory.resolve("$name.json")}
 fun names():List<String> {if(!Files.isDirectory(directory)) return emptyList();return Files.list(directory).use {it.filter {p->p.fileName.toString().endsWith(".json") && validName(p.fileName.toString().removeSuffix(".json"))}.map {p->p.fileName.toString().removeSuffix(".json")}.sorted().toList()}}
 fun load(name:String):WayPack {val f=path(name);require(Files.isRegularFile(f,LinkOption.NOFOLLOW_LINKS) && Files.size(f)<=WaypointCodec.LIMIT) {"Pack missing, too large or symlink: $name"};return WaypointCodec.decode(Files.readString(f))}
 fun write(name:String,pack:WayPack,create:Boolean=false) {
  val f=path(name);val text=WaypointCodec.encode(pack);Files.createDirectories(directory)
  require(!Files.isSymbolicLink(f));if(create) require(!Files.exists(f)) {"Pack already exists"} else load(name) // Never overwrite a corrupt/unreadable file.
  val tmp=Files.createTempFile(directory,".solyanka-waypoint-",".tmp")
  try {Files.writeString(tmp,text);try {Files.move(tmp,f,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)} catch(e:AtomicMoveNotSupportedException) {Files.move(tmp,f,StandardCopyOption.REPLACE_EXISTING)}} finally {Files.deleteIfExists(tmp)}
 }
 fun rename(old:String,next:String) {load(old);require(!Files.exists(path(next)));Files.move(path(old),path(next))}
 fun delete(name:String) {require(names().size>1) {"Cannot delete the only pack"};load(name);Files.delete(path(name))}
}
