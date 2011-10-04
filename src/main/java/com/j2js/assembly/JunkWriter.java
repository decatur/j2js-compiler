package com.j2js.assembly;

import com.j2js.J2JSCompiler;

import java.io.File;
import java.io.FileWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.StringWriter;

import com.j2js.Log;

public class JunkWriter extends FilterWriter {
    
    private File assembly;
    private int junkCount = 0;
    private int sizeOfCurrentJunk;
    private int sizeOfAllJunks = 0;
    
    public JunkWriter(File assembly) throws IOException {
        super(new StringWriter());
        this.assembly = assembly;
        startNewJunk();
    }

    private void startNewJunk() throws IOException {
        sizeOfAllJunks += sizeOfCurrentJunk;
        
        if (junkCount > 0) {
            write("j2js.loadScript(" + sizeOfAllJunks + ");");
            out.flush();
            out.close();
        }

        Log logger = Log.getLogger();
        String newJunkName = (junkCount + 1) + ".js";
        logger.info("Creating assembly " + newJunkName);
        out = new FileWriter(new File(assembly, newJunkName));
        sizeOfCurrentJunk = 0;
        junkCount++;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        super.write(cbuf, off, len);
        sizeOfCurrentJunk += len;
    }

    @Override
    public void write(int c) throws IOException {
        super.write(c);
        sizeOfCurrentJunk++;
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        super.write(str, off, len);
        sizeOfCurrentJunk += len;
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        if (sizeOfCurrentJunk/1024 > J2JSCompiler.compiler.getJunkSizeInKiloBytes()) {
            startNewJunk();
        }
    }

    @Override
    public void close() throws IOException {
        sizeOfAllJunks += sizeOfCurrentJunk;
        // Set to 0 in case super.close() calls flush().
        sizeOfCurrentJunk = 0;
        super.close();
    }

    public int getSize() {
        return sizeOfAllJunks;
    }

}
