import java.util.regex.*;
public class CheckTrackerPattern {
 public static void main(String[] args) {
  Pattern p=Pattern.compile("^(.*)( x[0-9,]+)( \\()(.+?)( coins\\))$");
  String[][] cases={{"Flint x13,943 (194,293 coins)","Flint"," x13,943","194,293"},{"Enchanted Flint x12 (? coins)","Enchanted Flint"," x12","?"},{"Item x1 Name x1 (0 coins)","Item x1 Name"," x1","0"}};
  int checks=0;
  for(String[] c:cases){Matcher m=p.matcher(c[0]);if(!m.matches())throw new AssertionError(c[0]);checks++;
   if(!m.group(1).equals(c[1])||!m.group(2).equals(c[2])||!m.group(4).equals(c[3]))throw new AssertionError(c[0]);checks++;
   StringBuilder b=new StringBuilder();for(int i=1;i<=5;i++)b.append(m.group(i));if(!b.toString().equals(c[0]))throw new AssertionError(c[0]);checks++;
  }
  for(String text:new String[]{"Total: 1 coins","Tracker · 00:12:30","Add items in Tracker settings"}){if(p.matcher(text).matches())throw new AssertionError(text);checks++;}
  System.out.println("PASS: "+checks+" exact Tracker regex/group/reconstruction assertions");
 }
}
