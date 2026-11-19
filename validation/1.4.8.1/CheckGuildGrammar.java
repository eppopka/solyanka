import java.util.regex.*;
public class CheckGuildGrammar {
 public static void main(String[] args) {
  Pattern p=Pattern.compile("^(?:(Party|Guild|Officer|Co-op) > |(From) )?(?:\\[[^]\\r\\n]{1,32}] )*([A-Za-z0-9_]{1,16})(?: \\[[^]\\r\\n]{1,32}])?: (.{1,256})$");int checks=0;
  String[][] ok={{"Guild > [MVP+] Alice: +","Alice","+"},{"Guild > Bob [Member]: +","Bob","+"},{"Guild > [VIP] Test_User: invite me","Test_User","invite me"}};
  for(String[] c:ok){Matcher m=p.matcher(c[0]);if(!m.matches()||!"Guild".equals(m.group(1))||!m.group(3).equals(c[1])||!m.group(4).equals(c[2]))throw new AssertionError(c[0]);checks++;}
  for(String text:new String[]{"Party > Alice: +","Officer > Alice: +","From Alice: +","Alice: +"}){Matcher m=p.matcher(text);if(!m.matches()||"Guild".equals(m.group(1)))throw new AssertionError(text);checks++;}
  for(String text:new String[]{"Guild > bad/name: +","Guild > Alice: +\n/p victim","Guild > NameThatIsTooLong123: +"}){if(p.matcher(text).matches())throw new AssertionError(text);checks++;}
  Matcher nested=p.matcher("Guild > Mallory: Guild > Victim: +");if(!nested.matches()||nested.group(4).equals("+"))throw new AssertionError("nested spoof");checks++;
  System.out.println("PASS: "+checks+" exact guild-regex fixtures");
 }
}
