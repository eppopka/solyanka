/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.cs2;
import dev.solyanka.utility.KnifeBindings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
public final class Cs2ItemIdentity {
 private Cs2ItemIdentity(){}
 public static String uuid(ItemStack stack) {
  if(stack==null||stack.isEmpty())return null;
  var data=stack.get(DataComponents.CUSTOM_DATA);if(data==null)return null;
  var tag=data.copyTag();
  String root=KnifeBindings.canonical(tag.getStringOr("uuid",""));
  String legacy=KnifeBindings.canonical(tag.getCompoundOrEmpty("ExtraAttributes").getStringOr("uuid",""));
  if(root!=null && legacy!=null && !root.equals(legacy))return null;
  return root!=null?root:legacy;
 }
}
