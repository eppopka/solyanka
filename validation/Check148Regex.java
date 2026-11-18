/* SPDX-License-Identifier: GPL-3.0-only */
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Tests the exact JVM regex strings extracted from the Kotlin source.
 * This is NOT a Kotlin/JUnit or Minecraft runtime test. */
public class Check148Regex {
    static int checks;
    static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
        checks++;
    }
    static Pattern fromSource(String source, String name) {
        String start = "private val " + name + "=Regex(\"\"\"";
        int at = source.indexOf(start);
        if (at < 0) throw new AssertionError("Missing source pattern: " + name);
        at += start.length();
        int end = source.indexOf("\"\"\"", at);
        return Pattern.compile(source.substring(at,end),Pattern.CASE_INSENSITIVE);
    }
    static double price(Pattern amount, String raw) {
        var m=amount.matcher(raw.trim().replace(",","").replace("$",""));
        if(!m.matches())return Double.NaN;
        double factor=switch(m.group(2).toLowerCase(Locale.ROOT)) {case "k"->1e3;case "m"->1e6;case "b"->1e9;default->1;};
        return Double.parseDouble(m.group(1))*factor;
    }
    public static void main(String[] args) throws Exception {
        var file=Path.of(args.length>0?args[0]:"src/main/kotlin/dev/solyanka/utility/MiningHelperLogic.kt");
        var source=Files.readString(file);
        var found=fromSource(source,"found");var tracker=fromSource(source,"tracker");
        var amount=fromSource(source,"amount");var corpse=fromSource(source,"corpse");
        for(var text:List.of("Me just found a Rare Item!","Me just found an Alloy!","Me found a Dye!","[MVP+] Me just found an Alloy!","[RNG] [MVP+] Me just found a Rare Item!"))
            check(found.matcher(text).matches(),text);
        var m=found.matcher("[MVP+] Test_User just found an Alloy!");check(m.matches(),"ranked message");
        check(m.group(1).equals("Test_User"),"player capture");check(m.group(2).equals("Alloy"),"item capture");
        for(var text:List.of("Party > Me: Me just found an Alloy!","[MVP+] Me: Me just found an Alloy!","Guild > Me: [SkyHanni] +Tracker Drop: Item (30M)","Me found some Alloy!"))
            check(!found.matcher(text).matches(),"reject player-chat echo: "+text);
        check(tracker.matcher("[SkyHanni] +Tracker Drop: Rare Item (29.9M)").matches(),"tracker plus");
        check(tracker.matcher("[SkyHanni] Tracker Drop: Rare Item (30,000,000 coins)").matches(),"tracker no plus");
        check(!tracker.matcher("Party > Me: [SkyHanni] +Tracker Drop: Rare Item (30M)").matches(),"reject tracker echo");
        check(price(amount,"29.9M")<30e6,"below threshold");check(price(amount,"30M")==30e6,"inclusive threshold");
        check(price(amount,"1,234,567 coins")==1234567,"commas");check(price(amount,"1.5B")==1.5e9,"billions");
        check(price(amount,"123k")==123000,"thousands");check(Double.isNaN(price(amount,"NaN")),"invalid value");
        check(Double.isNaN(price(amount,"-30M")),"negative value");
        for(var type:List.of("Lapis","Tungsten","Umber","Vanguard"))for(var state:List.of("NOT LOOTED","LOOTED"))
            check(corpse.matcher(" "+type+": "+state).matches(),type+state);
        check(!corpse.matcher("Frozen Corpses: 3").matches(),"not a corpse detail line");
        System.out.println("PASS: "+checks+" JVM-regex fixture assertions (patterns extracted from Kotlin source)");
    }
}
