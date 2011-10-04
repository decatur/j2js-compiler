import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.j2js.Const;
import com.j2js.Form;
import com.j2js.InstructionType;

/**
 * Copyright by Wolfgang Kuehn 2005
 * Created on Feb 27, 2005
 */
public class Enhancer {
    public static void main(String argv[]) throws Exception {
        File source = new File("src/com/j2js/Pass1.java");
        File temp = new File("temp/Pass1_" + new Date().getTime() + ".java");
        if (!source.renameTo(temp)) {
            throw new RuntimeException("Could not move " + source + " to "+ temp);
        }
        BufferedReader reader = new BufferedReader(new FileReader(temp));
        FileWriter writer = new FileWriter(source);
        Pattern p = Pattern.compile(".*case\\s+Const\\.(\\w*)\\s*:.*");
        
        String line;
        while ((line=reader.readLine())!=null) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                String name = m.group(1);
                System.out.println("Enhancing " + name);
                int index = ((Integer) Const.class.getField(name).get(null)).intValue();
                InstructionType it = Const.instructionTypes[index];
                
                for (int i=0; i<it.getFormCount(); i++) {
                    Form form = it.getForm(i);
                    // Discard previously generated code.
                    reader.readLine();
                    reader.readLine();
                    String head = "\n                //";
                    if (it.getFormCount() > 1) {
                        head += " (" + i + ")";
                    }
                    line += head + " Format: " + it.getName();
                    for (int j=0; j<form.getOperands().length; j++) {
                        Form.Value value = form.getOperands()[j];
                        line += ", " + value.name + "(" + value.type + ")";
                    }
                    line += head + " Operand stack: ...";
                    for (int j=0; j<form.getIns().length; j++) {
                        Form.Value value = form.getIns()[j];
                        line += ", " + value.name + "(" + value.type + ")";
                    }
                    line += " -> ...";
                    for (int j=0; j<form.getOuts().length; j++) {
                        Form.Value value = form.getOuts()[j];
                        line += ", " + value.name + "(" + value.type + ")";
                    }
                }
            }
            writer.write(line + "\n");
        }
        writer.close();
    }
}
