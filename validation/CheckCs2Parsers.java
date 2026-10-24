/* SPDX-License-Identifier: GPL-3.0-only; synthetic fixtures only, no Valve data. */
import dev.solyanka.client.cs2.s2.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
public class CheckCs2Parsers {
 static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 public static void main(String[] args)throws Exception {
  byte[] hello="hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  check(Arrays.equals(Lz4.decode(new byte[]{0x50,104,101,108,108,111},0,6,5),hello),"LZ4 literals");
  check(Arrays.equals(Lz4.decode(new byte[]{0x10,97,1,0},0,4,5),"aaaaa".getBytes()),"LZ4 overlap");
  var kv=Kv1.parse("// comment\n\"root\" { \"name\" \"knife\" \"seed\" \"7\" }");
  check(((Map<?,?>)kv.get("root")).get("name").equals("knife"),"KV1 nested");
  byte[] red={(byte)0,(byte)0xf8,0,0,0,0,0,0};byte[] rgba=Bcn.decode(red,0,4,4,8,Bcn.BC1);
  for(int i=0;i<16;i++)check((rgba[i*4]&255)==255&&rgba[i*4+1]==0&&rgba[i*4+2]==0&&(rgba[i*4+3]&255)==255,"BC1 red");
  // VPK v1 with one directory-inline, read-only text entry.
  var stream=new java.io.ByteArrayOutputStream();stream.write("txt\0 \0test\0".getBytes());
  ByteBuffer entry=ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);entry.putInt(0).putShort((short)0).putShort((short)0x7fff).putInt(0).putInt(hello.length).putShort((short)0xffff);stream.write(entry.array());stream.write(new byte[3]);byte[] tree=stream.toByteArray();
  ByteBuffer file=ByteBuffer.allocate(12+tree.length+hello.length).order(ByteOrder.LITTLE_ENDIAN);file.putInt(0x55aa1234).putInt(1).putInt(tree.length).put(tree).put(hello);
  Path fixture=Path.of(args[0]);Files.write(fixture,file.array());
  try(Vpk v=new Vpk(fixture)){check(v.entries().size()==1,"VPK index");check(Arrays.equals(v.read("test.txt"),hello),"VPK inline read");check(v.read("missing.txt")==null,"VPK miss");}
  byte[] encoded=Files.readAllBytes(Path.of(args[1])),plain=Files.readAllBytes(Path.of(args[2]));check(Arrays.equals(Zstd.decompress(encoded,0,encoded.length,plain.length),plain),"Zstd compressed fixture");
  System.out.println("8 synthetic parser checks passed: LZ4 literals/overlap, KV1, BC1, VPK index/read/miss, Zstd");
 }
}
