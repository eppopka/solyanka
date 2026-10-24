import com.mojang.blaze3d.pipeline.*;
import net.minecraft.client.renderer.RenderPipelines;
import java.util.*;
public class CheckBindings {
 static RenderPipeline.Snippet snippet(String name) throws Exception {
  var f=RenderPipelines.class.getDeclaredField(name);f.setAccessible(true);return (RenderPipeline.Snippet) f.get(null);
 }
 public static void main(String[] args) throws Exception {
  var item=snippet("ITEM_SNIPPET");var globals=snippet("GLOBALS_SNIPPET");
  var extra=BindGroupLayout.builder().withSampler("Sampler3").withSampler("Sampler4").withSampler("Sampler5").build();
  var fixed=RenderPipeline.builder(item).withLocation("solyanka/test_fixed").withBindGroupLayout(extra).withColorTargetState(ColorTargetState.DEFAULT).build();
  BindGroupLayout.ensureCompatible(fixed.getBindGroupLayouts());
  var uniforms=BindGroupLayout.flattenUniforms(fixed.getBindGroupLayouts());
  System.out.println("Fixed native layouts accepted: "+fixed.getBindGroupLayouts().size()+"; uniforms="+uniforms+"; samplers="+BindGroupLayout.flattenSamplers(fixed.getBindGroupLayouts()));
  var knife=RenderPipeline.builder(snippet("ENTITY_SNIPPET")).withLocation("solyanka/test_knife").withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler3").build()).withColorTargetState(ColorTargetState.DEFAULT).build();
  BindGroupLayout.ensureCompatible(knife.getBindGroupLayouts());
  System.out.println("Knife native layouts accepted; samplers="+BindGroupLayout.flattenSamplers(knife.getBindGroupLayouts()));
  if(DepthStencilState.DEFAULT.depthTest()!=com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL)throw new AssertionError("26.2 reversed depth convention");
  var aura=RenderPipeline.builder(snippet("DEBUG_FILLED_SNIPPET")).withLocation("solyanka/test_aura").withColorTargetState(ColorTargetState.DEFAULT).build();
  BindGroupLayout.ensureCompatible(aura.getBindGroupLayouts());
  System.out.println("Aura native layouts accepted");
  var broken=RenderPipeline.builder(item,globals).withLocation("solyanka/test_broken").withBindGroupLayout(extra).withColorTargetState(ColorTargetState.DEFAULT).build();
  try {BindGroupLayout.ensureCompatible(broken.getBindGroupLayouts());throw new AssertionError("Old layout must fail");}
  catch(IllegalArgumentException expected) {if(!expected.getMessage().contains("Duplicate bind name 'Globals'"))throw expected;System.out.println("Old layout reproduced: "+expected.getMessage());}
 }
}
