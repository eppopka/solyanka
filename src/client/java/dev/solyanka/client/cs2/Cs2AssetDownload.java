/* SPDX-License-Identifier: GPL-3.0-only; explicit opt-in downloader for upstream Stray's published pack. */
package dev.solyanka.client.cs2;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.io.*;
import java.security.*;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.loader.api.FabricLoader;
/** No network request until start() is explicitly confirmed in settings. No game assets in the JAR. */
public final class Cs2AssetDownload {
 public static final long SIZE=134557137L;
 public static final String SHA256="8e726f0f3f6429c27841ccae3a37d050f9a620f8a83c4018ce571d1ba4152166";
 private static final String BASE="https://raw.githubusercontent.com/camberX/Stray/cs2-data/";
 private static final String[] PARTS={"cs2.pack.00","cs2.pack.01","cs2.pack.02"};
 private static volatile Thread worker;
 private static volatile InputStream stream;
 private static volatile String status="No asset download in progress";
 private static volatile boolean cancel;
 public static final AtomicLong revision=new AtomicLong();
 private Cs2AssetDownload(){}
 public static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("solyanka/cs2.pack");}
 public static boolean cached(){return Files.isRegularFile(path(),LinkOption.NOFOLLOW_LINKS);}
 public static String status(){return status;}
 public static boolean running(){return worker!=null;}
 public static boolean verified(Path file)throws IOException {
  if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)!=SIZE)return false;
  try {MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=Files.newInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();d.update(b,0,n);}}return SHA256.equals(HexFormat.of().formatHex(d.digest()));}
  catch(NoSuchAlgorithmException e){throw new IOException(e);}
 }
 public static synchronized void start(){
  if(worker!=null)return;cancel=false;
  worker=new Thread(Cs2AssetDownload::download,"solyanka knife asset download");worker.setDaemon(true);worker.start();
 }
 public static void cancel(){cancel=true;Thread t=worker;if(t!=null)t.interrupt();InputStream in=stream;if(in!=null)try{in.close();}catch(IOException ignored){}}
 private static void download(){
  Path target=path(),temp=target.resolveSibling("cs2.pack.download");
  try {
   Files.createDirectories(target.getParent());
   if(Files.isSymbolicLink(temp)||Files.isSymbolicLink(target))throw new IOException("Refusing symbolic-link asset path");
   if(verified(target)){status="Verified cached assets ready";revision.incrementAndGet();return;}
   try(HttpClient http=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(15)).build()) {
    MessageDigest digest=MessageDigest.getInstance("SHA-256");long done=0;
    try(OutputStream out=Files.newOutputStream(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,LinkOption.NOFOLLOW_LINKS)) {
     for(String part:PARTS) {
      if(cancel)throw new InterruptedIOException("Cancelled");
      status=String.format(java.util.Locale.ROOT,"Downloading knife assets: %.1f / 134.6 MB",done/1e6);
      var response=http.send(HttpRequest.newBuilder(URI.create(BASE+part)).timeout(Duration.ofMinutes(5)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
      try(InputStream in=response.body()) {
       stream=in;
       if(response.statusCode()!=200)throw new IOException("HTTP "+response.statusCode());
       byte[] b=new byte[65536];int n;
       while((n=in.read(b))!=-1){if(cancel||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");done+=n;if(done>SIZE)throw new IOException("Pack exceeds pinned size");out.write(b,0,n);digest.update(b,0,n);status=String.format(java.util.Locale.ROOT,"Downloading knife assets: %.1f / 134.6 MB",done/1e6);}
      } finally {stream=null;}
     }
    }
    if(cancel)throw new InterruptedIOException("Cancelled");
    if(done!=SIZE||!SHA256.equals(HexFormat.of().formatHex(digest.digest())))throw new IOException("Asset checksum/size mismatch");
    try(var pack=new dev.solyanka.client.cs2.pack.Cs2Pack(temp)){if(pack.paths().isEmpty())throw new IOException("Empty pack");}
    try {Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
    status="Verified knife assets ready";revision.incrementAndGet();
   }
  } catch(Exception e){status=cancel?"Asset download cancelled":"Download failed: "+e.getClass().getSimpleName()+": "+e.getMessage();if(!cancel)org.slf4j.LoggerFactory.getLogger("solyanka/cs2").warn(status,e);}
  finally {try{Files.deleteIfExists(temp);}catch(IOException ignored){}stream=null;worker=null;}
 }
}
