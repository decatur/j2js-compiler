import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.j2js.Const;

/**
 * Copyright by Wolfgang Kuehn 2005
 * Created on Feb 27, 2005
 */
public class Checker {
    public static void main(String argv[]) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(new File("src/com/j2js/Pass1.java")));
        Pattern p = Pattern.compile(".*case\\s+Const\\.(\\w*)\\s*:.*");
        HashSet<String> set = new HashSet<String>();
        
        String line;
        while ((line=reader.readLine())!=null) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                String name = m.group(1);
                //System.out.println("Adding " + name);
                set.add(name);
            }
        }
        
        int missingCount = 0;
        for (int i=0; i<Const.instructionTypes.length; i++) {
            String name = Const.instructionTypes[i].getName();
            if (!set.contains(name.toUpperCase())) {
                missingCount++;
                System.out.println(name.toUpperCase());
            }
        }
        System.out.println("Missing count: " + missingCount);
    }
}
