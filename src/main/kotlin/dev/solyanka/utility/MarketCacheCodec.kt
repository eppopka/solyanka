/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility
import com.google.gson.*
data class MarketCache(val paid:Map<String,Long>,val recipes:Map<String,List<CraftRecipe>>)
object MarketCacheCodec {
 const val LIMIT=8_388_608
 fun decode(text:String):MarketCache {
  require(text.toByteArray().size<=LIMIT);val root=JsonParser.parseString(text).asJsonObject
  fun key(s:String) {require(s.isNotBlank() && s.length<=256)}
  val paid=root.getAsJsonObject("paid")?.entrySet()?.associate {(id,value)->key(id);require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber);val n=value.asBigDecimal.longValueExact();require(n>0);id to n} ?: emptyMap()
  require(paid.size<=20000)
  var count=0
  val recipes=root.getAsJsonObject("recipes")?.entrySet()?.associate {(id,array)->key(id);require(array.isJsonArray);id to array.asJsonArray.map {element->
   require(++count<=20000);val o=element.asJsonObject;val q=o["quantity"];require(q.isJsonPrimitive && q.asJsonPrimitive.isNumber);val quantity=q.asBigDecimal.intValueExact();require(quantity>0)
   val ingredients=o["items"].asJsonObject;require(ingredients.size() in 1..9)
   CraftRecipe(quantity,ingredients.entrySet().associate {(ingredient,v)->key(ingredient);require(v.isJsonPrimitive && v.asJsonPrimitive.isNumber);val n=v.asBigDecimal.intValueExact();require(n>0);ingredient to n})
  }} ?: emptyMap()
  return MarketCache(paid,recipes)
 }
 fun encode(paid:Map<String,Long>,recipes:Map<String,List<CraftRecipe>>):String=GsonBuilder().setPrettyPrinting().create().toJson(MarketCache(paid,recipes)).also {decode(it)}
}
