/* SPDX-License-Identifier: GPL-3.0-only; synthetic geometry/paint only, no Valve assets. */
import dev.solyanka.client.cs2.s2.S2Model;
import dev.solyanka.client.cs2.skin.Composer;
import dev.solyanka.client.cs2.render.SoftwareRender;
import java.util.*;
public class CheckKnifePreview {
 static void check(boolean x,String m){if(!x)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception {
  var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var unsafe=(sun.misc.Unsafe)f.get(null);
  var model=(S2Model)unsafe.allocateInstance(S2Model.class);
  var part=new S2Model.Part();part.vertexCount=6;
  part.pos=new float[]{-1,0,-1, 1,0,-1, 0,0,1, -1,-1,-1, 1,-1,-1, 0,-1,1};
  part.uv=new float[]{0,0,0,0,0,0, .5f,.5f,.5f,.5f,.5f,.5f};
  part.normal=new float[]{0,-1,0,0,-1,0,0,-1,0, 0,-1,0,0,-1,0,0,-1,0};part.indices=new int[]{0,1,2,3,4,5};
  var field=S2Model.class.getDeclaredField("parts");field.setAccessible(true);field.set(model,List.of(part));
  var c=Composer.Result.class.getDeclaredConstructor(int.class);c.setAccessible(true);var tex=c.newInstance(2);
  Arrays.fill(tex.albedo,0xffff2020);tex.albedo[3]=0xff2020ff;
  for(int i=0;i<4;i++){tex.material[i*4]=(byte)180;tex.material[i*4+1]=0;tex.material[i*4+2]=(byte)255;tex.material[i*4+3]=(byte)255;}
  int[] pixels=SoftwareRender.render(model,tex,64,64,180f);check(pixels.length==4096,"size");
  int filled=0;for(int p:pixels)if((p>>>24)!=0)filled++;check(filled>500 && filled<3000,"opaque triangle with transparent background");
  check(pixels[0]==0,"transparent corner");int center=pixels[32*64+32];check((center&255)>((center>>16)&255),"near blue triangle occludes red triangle");
  check(Arrays.equals(pixels,SoftwareRender.render(model,tex,64,64,180f)),"deterministic preview");
  var image=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,64,64,pixels,0,64);javax.imageio.ImageIO.write(image,"PNG",new java.io.File(args[0]));
  System.out.println("5 synthetic preview checks passed");
 }
}
