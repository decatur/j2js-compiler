package com.j2js.visitors;

import com.j2js.J2JSCompiler;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.j2js.Log;
import com.j2js.dom.TypeDeclaration;

public abstract class Generator extends AbstractVisitor {
     
    final String whiteSpace;
    int depth;
    char lastChar;
    String[] indents;
    private PrintStream stream;
    
    TypeDeclaration typeDecl;
    
    public Generator() {
        if (J2JSCompiler.compiler.isCompression()) {
            whiteSpace = "";
        } else {
            whiteSpace = " ";
        }
    }
    
    public PrintStream getOutputStream() {
        return stream;
    }
    
    public void setOutputPath(String path) throws FileNotFoundException {
        Log.getLogger().info("Destination file is " + path);
        setOutputStream(new PrintStream(new FileOutputStream(path)));
    }
    
    public void setOutputStream(PrintStream theStream) {
        stream = theStream;
    }
    
    public void flush() {
        stream.flush();
    }

    void indent() {
        // No indentation if compression is on.
        if (J2JSCompiler.compiler.isCompression()) return;
        String INDENT = "\t";
        if (indents == null || depth >= indents.length) {
            indents = new String[2 * depth];
            indents[0] = "";
            for (int i=1; i<indents.length; i++) {
                indents[i] = indents[i-1] + INDENT;
            }
        }
        print(indents[depth]);
    }
    
    void print(String s) {
        stream.print(s);
        if (s.length()>0) lastChar = s.charAt(s.length()-1);
    }
    
    void println(String s) {
        print(s);
        stream.println("");
    }
        
    void indentln(String s) {
        indent();
        println(s);
    }

    void indent(String s) {
        indent();
        print(s);
    }
}
