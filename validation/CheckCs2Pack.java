/* SPDX-License-Identifier: GPL-3.0-only; synthetic fixtures only. */
import dev.solyanka.client.cs2.pack.Cs2Pack;
import java.nio.file.*;
import java.util.*;
public class CheckCs2Pack {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception {
  Path dir=Path.of(args[0]);int count=0;
  try(var p=new Cs2Pack(dir.resolve("good.pack"))){check(p.paths().size()==2,"index");count++;check(Arrays.equals(p.assets().read("data.txt"),"hello".getBytes()),"inflate raw");count++;var t=p.assets().texture("pixel.vtex",1,false);check(t!=null&&t.w==1&&t.h==1&&t.px[0]==1f,"RGBA8");count++;}
  for(String name:List.of("magic","index","offset","rawsize","duplicate","texture")) {boolean rejected=false;try(var p=new Cs2Pack(dir.resolve(name+".pack"))){}catch(java.io.IOException e){rejected=true;}check(rejected,name);count++;}
  try(var p=new Cs2Pack(dir.resolve("deflate.pack"))){check(p.assets().read("data.txt")==null,"truncated deflate fails, no infinite loop");count++;}
  System.out.println(count+" synthetic pack checks passed");
 }
}
