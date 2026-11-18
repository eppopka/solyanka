/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.ui.nofrills

import java.util.Locale
import dev.solyanka.model.FeatureCardInfo
import dev.solyanka.model.GuiCatalog
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/** Static UI labels are translatable; unknown or dynamic strings keep their original text. */
object GuiText {
 private val unsafe=Regex("[^a-z0-9]+")
 fun component(text:String):MutableComponent {
  if(text.length>100 || text.any {it=='\n' || it=='\r'}) return Component.literal(text)
  val key=unsafe.replace(text.lowercase(Locale.ROOT),"_").trim('_')
  return if(key.isEmpty()) Component.literal(text) else Component.translatableWithFallback("solyanka.ui.$key",text)
 }
 fun featureDescription(info:FeatureCardInfo):MutableComponent = Component.translatableWithFallback(GuiCatalog.featureDescriptionTranslationKey(info.id),info.description)
}
