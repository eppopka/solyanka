import java.util.regex.*;
public class CheckCompactTrackerRows {
 public static void main(String[] args) {
  Pattern p=Pattern.compile("^(.*)( x[0-9,]+)( \\()(.+?)((?: coins)?\\))$");int checks=0;
  for(String line:new String[]{"Flint x13,943 (319k)","Flint x13,943 (319,284 coins)","Flint x1 (?)","Flint x1 (? coins)","Enchanted Flint x1 (1m)"}) {
   Matcher m=p.matcher(line);if(!m.matches()||m.groupCount()!=5)throw new AssertionError(line);
   StringBuilder b=new StringBuilder();for(int i=1;i<=5;i++)b.append(m.group(i));if(!b.toString().equals(line))throw new AssertionError(line);checks++;
  }
  System.out.println("PASS: "+checks+" exact compact/full Tracker regex fixtures");
 }
}
