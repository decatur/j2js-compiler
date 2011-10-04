package com.j2js;

import com.j2js.J2JSCompiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A FileManager can resolve relative file names against a class path.
 * <p>
 * The file names "java/lang.package-info", "java/lang/package-info" and
 * "java.lang.package-info" are valid and equivalent.
 * </p>
 * 
 * @author wolle
 */
public class FileManager {

    private List<Object> path = new ArrayList<Object>();
    
    /**
     * Create a new FileManager.
     * 
     * @param classPath list of file system directories or jar files.
     */
    public FileManager(List<File> classPath) {
        Log.getLogger().info("Resolving class path " + classPath);
        
        // Replace all jar files on classPath by instances of JarFile.
        // Non-existing files are sorted out.
        for (File file : classPath) {
            if (!file.exists()) {
                J2JSCompiler.errorCount++;
                Log.getLogger().error("Cannot find resource on class path: " + file.getAbsolutePath());
                continue;
            }
            
            if (file.getName().endsWith(".jar")) {
                try {
                    path.add(new JarFile(file));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                path.add(file);
            }
        }
    }
    
    /**
     * Resolves a file given by name along the class path.
     */
    public FileObject getFileForInput(String relativeName) {
        for (Object o : path) {
            if (o instanceof JarFile) {
                JarFile jarFile = (JarFile) o;
                JarEntry entry = jarFile.getJarEntry(relativeName);
                if (entry != null) {
                    return new FileObject(jarFile, entry);
                }
            } else {
                File file = new File(((File) o), relativeName);
                if (file.exists()) {
                    return new FileObject(file);
                }
            }
        }
        
        throw new RuntimeException("Could not find " + relativeName + " on class path");
    }

}
