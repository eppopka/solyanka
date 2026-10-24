/* SPDX-License-Identifier: GPL-3.0-only; Stray CC0 KnifeRender/KnifeGpu adaptation.
 * Native 26.2 custom-geometry submission, CPU-weighted skinning. No global GPU interception. */
package dev.solyanka.client.cs2.render;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.PrimitiveTopology;
import dev.solyanka.client.cs2.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;
public final class KnifeRender {
 public static final float VIEW_SCALE=.04f;
 private static RenderPipeline pipeline;
 private static RenderType type;
 public static synchronized void register() {
  if(pipeline!=null)return;
  var p=RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
   .withLocation(Identifier.fromNamespaceAndPath("solyanka","cs2_knife"))
   .withVertexShader(Identifier.fromNamespaceAndPath("solyanka","core/cs2_knife"))
   .withFragmentShader(Identifier.fromNamespaceAndPath("solyanka","core/cs2_knife"))
   .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler3").build())
   .withShaderDefine("NO_OVERLAY").withVertexBinding(0,DefaultVertexFormat.ENTITY)
   .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withColorTargetState(ColorTargetState.DEFAULT).withCull(false).build();
  BindGroupLayout.ensureCompatible(p.getBindGroupLayouts());
  pipeline=RenderPipelines.register(p);
 }
 private static synchronized RenderType type() {
  if(type==null) {register();type=RenderType.create("solyanka_cs2_knife",RenderSetup.builder(pipeline)
   .withTexture("Sampler0",Cs2Knives.ALBEDO,()->RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
   .withTexture("Sampler3",Cs2Knives.MATERIAL,()->RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR))
   .useLightmap().affectsCrumbling().setOutline(RenderSetup.OutlineProperty.NONE).createRenderSetup());}
  return type;
 }
 public static void submit(PoseStack pose,SubmitNodeCollector collector,int light,Cs2Knives.Knife knife,Matrix4f toView,Matrix4f[] skin) {
  Matrix4f view=new Matrix4f(toView);Matrix4f[] bones=null;
  if(skin!=null){bones=new Matrix4f[skin.length];for(int i=0;i<skin.length;i++)bones[i]=new Matrix4f(skin[i]);}
  final Matrix4f[] copy=bones;
  collector.submitCustomGeometry(pose,type(),(p,out)->{
   for(var part:knife.model.parts) {
    float[] transformed=dev.solyanka.utility.KnifeSkinning.transform(part.pos,part.normal,part.bones,part.weights,copy,view);
    for(int i:part.indices){int at=i*6;if(at<0||at+5>=transformed.length)continue;
     out.addVertex(p,transformed[at],transformed[at+1],transformed[at+2]).setColor(-1)
      .setUv(part.uv[i*2],part.uv[i*2+1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
      .setNormal(p,transformed[at+3],transformed[at+4],transformed[at+5]);
    }
   }
  });
 }
 public static Matrix4f sourceToView(float scale){return new Matrix4f(0,0,-scale,0,-scale,0,0,0,0,scale,0,0,0,0,0,1);}
 public static Matrix4f thirdPerson(String id,HumanoidArm arm) {
  Matrix4f m=new Matrix4f().translate(0,4f/16f,.5f/16f).rotateXYZ(0,(float)Math.toRadians(-90),(float)Math.toRadians(55)).scale(.85f).translate(-.5f,-.5f,-.5f).translate(4.5f/16f,4.5f/16f,.5f);
  Vector3f z=new Vector3f(1,1,0).normalize(),y=new Vector3f(0,0,1),x=new Vector3f(y).cross(z);
  return m.mul(new Matrix4f(x.x,x.y,x.z,0,y.x,y.y,y.z,0,z.x,z.y,z.z,0,0,0,0,1)).scale(.065f);
 }
}
