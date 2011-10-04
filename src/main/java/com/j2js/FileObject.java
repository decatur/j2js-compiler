package com.j2js;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * File abstraction for tools. In this context, file means an abstraction of regular
 * files and other sources of data.
 * 
 * @author wolle
 */
public class FileObject {

    private long lastModified;
    private InputStream in;
    
    FileObject(JarFile jarFile, JarEntry entry) {
        try {
            in = jarFile.getInputStream(entry);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        lastModified = entry.getTime();
    }
    
    FileObject(File file) {
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        lastModified = file.lastModified();
    }
    
    /**
     * Gets an InputStream for this file object.
     */
    public InputStream openInputStream() throws IOException {
        return in;
    }

    /**
     * @return Returns the lastModified.
     */
    public long getLastModified() {
        return lastModified;
    }

}
