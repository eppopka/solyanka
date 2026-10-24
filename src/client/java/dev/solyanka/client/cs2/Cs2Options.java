/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.client.cs2;
import dev.solyanka.client.SolyankaClient;
public final class Cs2Options {
 public final boolean cs2Animations;
 public final float[] cs2ViewOffset;
 public final java.util.Map<String,float[]> cs2Grip=java.util.Map.of();
 private Cs2Options() {var s=SolyankaClient.INSTANCE.currentGeneral().getFeatures().getCs2Knife();cs2Animations=s.getAnimations();cs2ViewOffset=new float[]{(float)s.getOffsetX(),(float)s.getOffsetY(),(float)s.getDistance()};}
 public static Cs2Options get() {return new Cs2Options();}
}
