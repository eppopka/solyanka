/* SPDX-License-Identifier: GPL-3.0-only; Stray CC0 model/skin/animation integration. */
package dev.solyanka.client.cs2;
import com.mojang.blaze3d.platform.NativeImage;
import dev.solyanka.client.SolyankaClient;
import dev.solyanka.utility.Cs2KnifeSettings;
import dev.solyanka.client.cs2.anim.*;
import dev.solyanka.client.cs2.s2.*;
import dev.solyanka.client.cs2.skin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.LoggerFactory;
/** Local install or explicit opt-in verified pack. File/decode/composition work runs on one daemon; uploads run on client/render thread. */
public final class Cs2Knives {
 private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("solyanka/cs2");
 public static final Identifier ALBEDO=Identifier.fromNamespaceAndPath("solyanka","cs2/knife_albedo");
 public static final Identifier MATERIAL=Identifier.fromNamespaceAndPath("solyanka","cs2/knife_material");
 public static final Identifier BLADE=Identifier.fromNamespaceAndPath("solyanka","cs2/knife_blade");
 public static final int SKIN_SIZE=1024;
 private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"solyanka CS2 loader");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});
	/** A knife ready to draw. */
	public static final class Knife {
		public final String id;
		public final String key;
		public final S2Model model;
		public final Pose pose;
		public final Map<String, NmClip> clips;
		final Composer.Result skin;

		private int[][] bladeTriangles;

		/**
		 * Per model part, the first index of every triangle that can show the blade shader: its
		 * UV bounds touch texels the blade mask keeps (the fill pass discards everything else, so
		 * skipping the rest changes nothing on screen).
		 */
		public synchronized int[][] bladeTriangles() {
			if (bladeTriangles != null) {
				return bladeTriangles;
			}
			int cells = 64;
			boolean[] keep = new boolean[cells * cells];
			int n = skin.size;
			for (int y = 0; y < n; y++) {
				for (int x = 0; x < n; x++) {
					int i = y * n + x;
					int alpha = Math.max(skin.material[i * 4 + 3] & 0xFF, skin.material[i * 4 + 1] & 0xFF);
					if (alpha >= 16) {
						int cx = x * cells / n;
						int cy = y * cells / n;
						// dilate by one cell for texture filtering at cell edges
						for (int dy = -1; dy <= 1; dy++) {
							for (int dx = -1; dx <= 1; dx++) {
								int ex = cx + dx;
								int ey = cy + dy;
								if (ex >= 0 && ey >= 0 && ex < cells && ey < cells) {
									keep[ey * cells + ex] = true;
								}
							}
						}
					}
				}
			}
			int[][] out = new int[model.parts.size()][];
			for (int pi = 0; pi < out.length; pi++) {
				S2Model.Part part = model.parts.get(pi);
				int[] idx = part.indices;
				int[] list = new int[idx.length / 3];
				int count = 0;
				for (int t = 0; t + 2 < idx.length; t += 3) {
					if (part.uv == null) {
						list[count++] = t;
						continue;
					}
					float u0 = Float.MAX_VALUE;
					float v0 = Float.MAX_VALUE;
					float u1 = -Float.MAX_VALUE;
					float v1 = -Float.MAX_VALUE;
					for (int k = 0; k < 3; k++) {
						int vi = idx[t + k];
						float u = part.uv[vi * 2];
						float v = part.uv[vi * 2 + 1];
						u0 = Math.min(u0, u);
						v0 = Math.min(v0, v);
						u1 = Math.max(u1, u);
						v1 = Math.max(v1, v);
					}
					if (u0 < 0f || v0 < 0f || u1 > 1f || v1 > 1f) {
						list[count++] = t; // wrapped UVs: keep, to be safe
						continue;
					}
					int cx0 = Math.min(cells - 1, (int) (u0 * cells));
					int cx1 = Math.min(cells - 1, (int) (u1 * cells));
					int cy0 = Math.min(cells - 1, (int) (v0 * cells));
					int cy1 = Math.min(cells - 1, (int) (v1 * cells));
					boolean hit = false;
					for (int cy = cy0; cy <= cy1 && !hit; cy++) {
						for (int cx = cx0; cx <= cx1; cx++) {
							if (keep[cy * cells + cx]) {
								hit = true;
								break;
							}
						}
					}
					if (hit) {
						list[count++] = t;
					}
				}
				out[pi] = java.util.Arrays.copyOf(list, count);
			}
			bladeTriangles = out;
			return out;
		}

		Knife(String id, String key, S2Model model, Pose pose, Map<String, NmClip> clips, Composer.Result skin) {
			this.id = id;
			this.key = key;
			this.model = model;
			this.pose = pose;
			this.clips = clips;
			this.skin = skin;
		}

		private org.joml.Matrix4f[] rest;

		/** Bone matrices for the knife at rest in its own model space (no viewmodel rig). */
		public synchronized org.joml.Matrix4f[] restSkin() {
			if (rest == null) {
				rest = new org.joml.Matrix4f[Math.max(1, model.bones.size())];
				for (int i = 0; i < rest.length; i++) {
					rest[i] = new org.joml.Matrix4f();
				}
			}
			return rest;
		}

		/** The clip whose name starts with the prefix, preferring the plainest (shortest) name. */
		public NmClip clip(String prefix) {
			String best = null;
			for (String name : clips.keySet()) {
				if (name.startsWith(prefix) && (best == null || name.length() < best.length()
					|| name.length() == best.length() && name.compareTo(best) < 0)) {
					best = name;
				}
			}
			return best == null ? null : clips.get(best);
		}

		/** Inspect variants (lookat01, lookat02, ...), skipping the draw-into-inspect ones. */
		public java.util.List<NmClip> inspects() {
			java.util.List<String> names = new java.util.ArrayList<>();
			for (String name : clips.keySet()) {
				if (name.startsWith("lookat") && !name.contains("_draw")) {
					names.add(name);
				}
			}
			java.util.Collections.sort(names);
			java.util.List<NmClip> out = new java.util.ArrayList<>();
			for (String name : names) {
				out.add(clips.get(name));
			}
			return out;
		}
	}

 private record Opened(Cs2Library library,NmSkeleton rig,int generation) {}
 private record Built(Knife knife,String key,int generation) {}
 private static Cs2Library library;
 private static NmSkeleton viewmodel;
 private static Knife current;
 private static volatile String status="Disabled — no assets loaded";
 private static CompletableFuture<Opened> opening;
 private static CompletableFuture<Built> pending;
 private static String libraryPath,requestedKey,attemptedKey;
 private static boolean inspectDown,failedUpload,texturesUploaded;
 private static volatile int generation;
 private static long changedAt,pathChangedAt;
 private static String desiredPath;
 private static final Map<String,Knife> CACHE=new LinkedHashMap<>(8,.75f,true) {protected boolean removeEldestEntry(Map.Entry<String,Knife> e){return size()>4;}};
 private static Cs2KnifeSettings settings() {return SolyankaClient.INSTANCE.currentGeneral().getFeatures().getCs2Knife();}
 public static boolean enabled() {return SolyankaClient.INSTANCE.isEnabled(SolyankaClient.CS2_KNIFE);}
 public static String status() {return status;}
 public static Cs2Library library() {return library;}
 public static Knife current() {return current;}
 public static List<Cs2Library.Knife> knives(){return library==null?List.of():List.copyOf(library.knives().values());}
 public static boolean applies(ItemStack stack){return enabled() && !failedUpload && current!=null && matches(stack);}
 public static boolean matches(ItemStack stack){String rule=settings().getApplyTo();if(rule.equals("empty"))return stack==null||stack.isEmpty();if(stack==null||stack.isEmpty())return false;return rule.equals("held")||stack.is(ItemTags.SWORDS)||stack.is(ItemTags.AXES);}
 private static String key(Cs2KnifeSettings s){return s.getKnife()+"|"+s.getFinish()+"|"+s.getSeed()+"|"+Math.round(s.getWear()*10000);}
 public static void tick(Minecraft mc) {
  if(!enabled())return;
  var s=settings();boolean inspect=false;
  if(mc.gui.screen()==null && s.getInspectKey()>=0 && mc.isWindowActive())inspect=org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().handle(),s.getInspectKey())==org.lwjgl.glfw.GLFW.GLFW_PRESS;
  if(inspect&&!inspectDown)Cs2Viewmodel.inspect();inspectDown=inspect;
  String sourceToken=s.getSource()+"|"+s.getPath()+"|"+Cs2AssetDownload.revision.get();
  if(!Objects.equals(libraryPath,sourceToken)) {
   if(!Objects.equals(desiredPath,sourceToken)) {desiredPath=sourceToken;pathChangedAt=System.nanoTime();return;}
   if(System.nanoTime()-pathChangedAt<500_000_000L)return;
   clear();libraryPath=sourceToken;int g=generation;String path=s.getPath(),mode=s.getSource();status="Reading knife assets...";
   opening=CompletableFuture.supplyAsync(()->{Cs2Library lib=openSource(mode,path);NmSkeleton rig=null;try {if(lib!=null){Resource r=lib.assets().resource("animation/skeletons/characters/viewmodel.vnmskel");if(r!=null)rig=new NmSkeleton(r);}return new Opened(lib,rig,g);}catch(RuntimeException e){if(lib!=null)lib.close();throw e;}},WORKER);
  }
  if(opening!=null&&opening.isDone()) {try {var o=opening.join();if(o.generation==generation){library=o.library;viewmodel=o.rig;status=library==null?"Assets missing or invalid. Download the Stray pack, or set a local CS2 folder.":"Knife assets ready";}else if(o.library!=null)WORKER.execute(o.library::close);}catch(CompletionException e){status="Could not read CS2; see latest.log";LOG.warn(status,e);}finally {opening=null;}}
  if(library==null || mc.player==null || failedUpload)return;
  String key=key(s);
  if(!key.equals(requestedKey)){requestedKey=key;changedAt=System.nanoTime();}
  if(pending!=null&&pending.isDone()) {try {var b=pending.join();if(b.generation==generation && b.key.equals(requestedKey) && b.knife!=null){upload(b.knife);current=b.knife;CACHE.put(b.key,b.knife);status="Loaded "+b.knife.id;Cs2Viewmodel.draw();}}catch(RuntimeException e){failedUpload=true;current=null;status="Knife load/upload failed; vanilla retained. See latest.log";LOG.warn(status,e);}finally {pending=null;}}
  if(current!=null&&current.key.equals(key))return;
  if(pending!=null||Objects.equals(key,attemptedKey)||System.nanoTime()-changedAt<250_000_000L)return;
  attemptedKey=key;Knife cached=CACHE.get(key);
  if(cached!=null){try {upload(cached);current=cached;status="Loaded "+cached.id;Cs2Viewmodel.draw();}catch(RuntimeException e){failedUpload=true;current=null;LOG.warn("Knife cached upload failed",e);}return;}
  int g=generation;status="Building knife...";
  // Serial worker owns all decoder reads; obsolete results cannot publish after reload/disable.
  Cs2Library source=library;NmSkeleton rig=viewmodel;
  pending=CompletableFuture.supplyAsync(()->new Built(build(source,rig,g,key,s.getKnife(),s.getFinish(),s.getSeed(),(float)s.getWear()),key,g),WORKER);
 }
 private static Cs2Library openSource(String mode,String path) {
  Cs2Library local=mode.equals("pack")?null:Cs2Library.open(path);
  if(local!=null||mode.equals("install"))return local;
  try {return Cs2AssetDownload.verified(Cs2AssetDownload.path())?Cs2Library.openPack(Cs2AssetDownload.path()):null;}
  catch(java.io.IOException e){LOG.warn("Cached knife pack is invalid",e);return null;}
 }
 public static void reload(){clear();}
 public static void clear(){
  generation++;current=null;CACHE.clear();viewmodel=null;requestedKey=null;attemptedKey=null;libraryPath=null;desiredPath=null;inspectDown=false;failedUpload=false;Cs2Viewmodel.reset();
  if(opening!=null){opening.thenAccept(o->{if(o.library!=null)o.library.close();});opening=null;}
  // Do not interrupt a decoder mid-read; close its install after already-queued work.
  pending=null;var old=library;library=null;if(old!=null)WORKER.execute(old::close);
  if(texturesUploaded) {var textures=Minecraft.getInstance().getTextureManager();textures.release(ALBEDO);textures.release(MATERIAL);textures.release(BLADE);texturesUploaded=false;}status="Disabled — no assets loaded";
 }
 public static void shutdown(){Cs2AssetDownload.cancel();clear();WORKER.shutdown();}
	private static Knife build(Cs2Library lib,NmSkeleton rig,int jobGeneration,String key, String knifeId, String finish, int seed, float wear) {
		try {
			Cs2Library.Knife info = lib.knives().get(knifeId);
			if (info == null) {
				info = lib.knives().values().stream().findFirst().orElse(null);
			}
			if (info == null) {
				if(jobGeneration==generation) status = "No knives found in Counter-Strike 2.";
				return null;
			}
			S2Model model = new S2Model(lib.assets().resource(info.model()));
            validateModel(model);
			KnifeMaterial material = new KnifeMaterial(lib.assets(), model.parts.get(0).material);
			PaintKit kit = finish == null || finish.isEmpty() ? null : lib.kit(finish);
			Composer.Result skin = Composer.compose(lib.assets(), material, kit, seed, wear, SKIN_SIZE);
			NmSkeleton knifeSkeleton = null;
			String knifeSkeletonId = null;
			for (String ref : lib.assets().resource(info.model()).externalRefs().values()) {
				if (ref.endsWith(".vnmskel")) {
					knifeSkeletonId = ref;
					Resource r = lib.assets().resource(ref);
					knifeSkeleton = r == null ? null : new NmSkeleton(r);
				}
			}
			Map<String, NmClip> clips = new HashMap<>();
			String folder = info.model().replaceAll("^.*/(knife_[a-z0-9_]+)/[^/]+$", "$1");
			if (rig != null) {
				for (String path : lib.list("animation/anims/viewmodel/knife/" + folder + "/")) {
					String name = path.substring(path.lastIndexOf('/') + 1).replace(".vnmclip_c", "").toLowerCase(Locale.ROOT);
					if (!path.endsWith(".vnmclip_c") || path.substring(("animation/anims/viewmodel/knife/" + folder + "/").length()).contains("/")) {
						continue;
					}
					Resource r = lib.assets().resource(path);
					if (r != null) {
						clips.put(name, new NmClip(r));
					}
				}
			}
			Pose pose = rig == null ? null : new Pose(rig, knifeSkeleton, model).withKnifeSkeletonId(knifeSkeletonId);
			return new Knife(info.id(), key, model, pose, clips, skin);
		} catch (RuntimeException e) {
			if(jobGeneration==generation) status = "Could not build the knife: " + e.getMessage();
			LOG.warn("CS2 knives: build failed", e);
			return null;
		} finally {lib.assets().clearCache();}
	}

 private static void validateModel(S2Model model) {
  if(model.parts.isEmpty())throw new IllegalArgumentException("Knife has no mesh parts");
  long vertices=0,indices=0;
  for(var part:model.parts) {
   vertices+=part.vertexCount;indices+=part.indices.length;
   if(part.vertexCount<1 || vertices>250000 || indices>1500000 || part.indices.length%3!=0 || part.pos.length!=part.vertexCount*3 || part.normal.length!=part.vertexCount*3 || part.uv==null || part.uv.length!=part.vertexCount*2)throw new IllegalArgumentException("Invalid/oversized knife mesh");
   for(int index:part.indices)if(index<0||index>=part.vertexCount)throw new IllegalArgumentException("Knife index out of range");
   for(float[] values:new float[][]{part.pos,part.normal,part.uv})for(float value:values)if(!Float.isFinite(value))throw new IllegalArgumentException("Non-finite knife channel");
  }
 }
 private static void upload(Knife knife) {
  var skin=knife.skin;int n=skin.size;
  uploadTexture(ALBEDO,n,(x,y)->skin.albedo[y*n+x]);
  uploadTexture(MATERIAL,n,(x,y)->{int i=(y*n+x)*4;return 0xff000000|((skin.material[i]&255)<<16)|((skin.material[i+1]&255)<<8)|(skin.material[i+2]&255);});
 }
 /** No per-pixel temporary allocation; transfer image ownership only after native texture creation. */
 private static void uploadTexture(Identifier id,int n,java.util.function.IntBinaryOperator pixels) {
  NativeImage image=new NativeImage(n,n,false);DynamicTexture dynamic=null;
  try {
   for(int y=0;y<n;y++)for(int x=0;x<n;x++)image.setPixel(x,y,pixels.applyAsInt(x,y));
   dynamic=new DynamicTexture(()->"solyanka-cs2-"+id.getPath(),image);image=null;
   // A failed registration also triggers release on clear; no silently retained half-uploaded state.
   texturesUploaded=true;Minecraft.getInstance().getTextureManager().register(id,dynamic);dynamic=null;
  } finally {if(dynamic!=null)dynamic.close();if(image!=null)image.close();}
 }
}
