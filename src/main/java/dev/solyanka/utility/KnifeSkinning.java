/* SPDX-License-Identifier: GPL-3.0-only; Stray weighted mesh/animation adaptation. */
package dev.solyanka.utility;
import org.joml.Matrix4f;
import org.joml.Vector3f;
/** Pure weighted Source 2 skinning; six floats per output vertex: position, normal. */
public final class KnifeSkinning {
 public static float[] transform(float[] pos,float[] normals,int[] bones,float[] weights,Matrix4f[] skin,Matrix4f view) {
  if(pos.length%3!=0 || normals.length!=pos.length)throw new IllegalArgumentException("Invalid mesh channels");
  int count=pos.length/3;if(count>250000)throw new IllegalArgumentException("Knife vertex budget exceeded");
  if(bones!=null && bones.length<count*4 || weights!=null && weights.length<count*4)throw new IllegalArgumentException("Invalid skin channels");
  float[] out=new float[count*6];Vector3f point=new Vector3f(),normal=new Vector3f(),tmp=new Vector3f();
  for(int i=0;i<count;i++) {
   float x=pos[i*3],y=pos[i*3+1],z=pos[i*3+2],nx=normals[i*3],ny=normals[i*3+1],nz=normals[i*3+2];
   point.zero();normal.zero();float total=0;
   if(skin!=null && bones!=null)for(int k=0;k<4;k++) {
    int bi=bones[i*4+k];float w=weights==null?(k==0?1:0):weights[i*4+k];
    if(!Float.isFinite(w))throw new IllegalArgumentException("Non-finite bone weight");
    if(w<=0 || bi<0 || bi>=skin.length)continue;
    skin[bi].transformPosition(x,y,z,tmp);point.fma(w,tmp);skin[bi].transformDirection(nx,ny,nz,tmp);normal.fma(w,tmp);total+=w;
   }
   if(total==0){point.set(x,y,z);normal.set(nx,ny,nz);}else if(total!=1){point.div(total);normal.div(total);}
   view.transformPosition(point);view.transformDirection(normal);if(normal.lengthSquared()>1e-12f)normal.normalize();else normal.set(0,1,0);
   if(!point.isFinite()||!normal.isFinite())throw new IllegalArgumentException("Non-finite knife vertex");
   int at=i*6;out[at]=point.x;out[at+1]=point.y;out[at+2]=point.z;out[at+3]=normal.x;out[at+4]=normal.y;out[at+5]=normal.z;
  }
  return out;
 }
}
