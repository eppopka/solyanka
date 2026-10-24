/* SPDX-License-Identifier: GPL-3.0-only
 * Adapted from NoFrills PriceTooltips and RecipeData; see NOTICE. */
package dev.solyanka.utility
import com.google.gson.*
import java.util.Locale

data class MarketPrice(val buy:Double=0.0,val sell:Double=0.0,val bin:Double=0.0,val npc:Double=0.0,val mote:Double=0.0)
data class CraftRecipe(val quantity:Int,val items:Map<String,Int>)
object PriceLogic {
 fun quantity(count:Int,lore:List<String>):Int {
  for(line in lore) {
   val raw=when { line.startsWith("Stored: ") && '/' in line->line.substringAfter(": ").substringBefore('/');line.startsWith("Compost Available: ")->line.substringAfter(": ");line.startsWith("Syphon") && line.endsWith("more to level up!")->line.removePrefix("Syphon").trim().substringBefore(' ');line.startsWith("Owned: ")->line.substringAfter(": ").substringBefore(' ');else->continue }
   raw.replace(",","").trim().toIntOrNull()?.let { return it.coerceAtLeast(1) }
  };return count.coerceAtLeast(1)
 }
 fun purchaseCost(lore:List<String>):Long=lore.firstOrNull { it.startsWith("Cost: ") }?.substringAfter(": ")?.replace(",","")?.replace("coins","",true)?.trim()?.toLongOrNull()?.coerceAtLeast(0) ?: 0L
 fun craft(id:String,prices:Map<String,MarketPrice>,recipes:Map<String,List<CraftRecipe>>):Double? {
  fun market(item:String)=prices[item]?.let { listOf(it.buy,it.bin).filter { n->n>0 && n.isFinite() }.minOrNull() }
  fun visit(item:String,path:Set<String>,depth:Int):Double? {
   val buy=market(item)
   if(item in path || depth>32) return buy
   val costs=recipes[item]?.mapNotNull { recipe ->
    if(recipe.quantity<=0 || recipe.items.isEmpty()) return@mapNotNull null
    var total=0.0
    for((ingredient,count) in recipe.items) { if(count<=0) return@mapNotNull null;val cost=visit(ingredient,path+item,depth+1) ?: return@mapNotNull null;total+=cost*count/recipe.quantity }
    total.takeIf { it>0 && it.isFinite() }
   } ?: emptyList()
   return (costs+listOfNotNull(buy)).minOrNull()
  }
  // Only show a craft estimate if an actual observed recipe exists, never a guessed zero.
  return if(recipes.containsKey(id)) visit(id,emptySet(),0) else null
 }
 fun parseHypixelBazaar(text:String):Map<String,MarketPrice> {
  val root=JsonParser.parseString(text).asJsonObject;require(root.get("success")?.asBoolean==true)
  return root.getAsJsonObject("products").entrySet().associate {(id,value)->
   val q=value.asJsonObject.getAsJsonObject("quick_status")
   // Hypixel buyPrice is the buy-order bid (player insta-sell); sellPrice is
   // the sell-order ask (player insta-buy). Preserve user-facing label direction.
   val bid=q.get("buyPrice").asDouble;val ask=q.get("sellPrice").asDouble
   require(bid.isFinite() && ask.isFinite() && bid>=0 && ask>=0)
   id to MarketPrice(buy=ask,sell=bid)
  }.also {require(it.isNotEmpty())}
 }
 fun parsePricing(text:String):Map<String,MarketPrice> {
  val root=JsonParser.parseString(text).asJsonObject;val result=mutableMapOf<String,MarketPrice>()
  fun num(v:JsonElement?):Double { val n=if(v?.isJsonPrimitive==true && v.asJsonPrimitive.isNumber) v.asDouble else 0.0;require(n.isFinite() && n>=0);return n }
  root.getAsJsonObject("auction")?.entrySet()?.forEach { (id,v)->result[id]=MarketPrice(bin=num(v)) }
  root.getAsJsonObject("bazaar")?.entrySet()?.forEach { (id,v)->val o=v.asJsonObject;result[id]=(result[id] ?: MarketPrice()).copy(buy=num(o.get("buy")),sell=num(o.get("sell"))) }
  root.getAsJsonObject("npc")?.entrySet()?.forEach { (id,v)->val o=v.asJsonObject;result[id]=(result[id] ?: MarketPrice()).copy(npc=num(o.get("coin")),mote=num(o.get("mote"))) }
  require(result.isNotEmpty()) { "Empty price response" };return result.toMap()
 }
 fun format(value:Double)=String.format(Locale.US,"%,.1f",value).removeSuffix(".0")
}
