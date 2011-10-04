package com.j2js;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

/**
 * Copyright by Wolfgang Kuehn 2005
 * Created on Feb 26, 2005
 */
public class Diff {
    
    private int lookAheadLines = 5;
    private LineNumberReader in1;
    private LineNumberReader in2;
    private int skipLineCount = 0;
    
    public Diff(Reader r1, Reader r2) {
        in1 = new LineNumberReader(r1);
        in2 = new LineNumberReader(r2);
    }
    
    public Diff(File f1, File f2) throws FileNotFoundException {
        this(new FileReader(f1), new FileReader(f2));
    }
    
    public boolean apply() throws IOException {
        boolean equal = true;
        int lineCount = 0;
        
        while (true) {
        	lineCount++;
	        String l1 = in1.readLine();
	        String l2 = in2.readLine();
	        if (l1==null || l2==null) {
	            break;
	        } else if (l1==null && l2!=null) {
	            equal = false;
	            System.out.println("File 2 extends File 1 at line " + in2.getLineNumber());
	            break;
	        } else if (l1!=null && l2==null) {
	            equal = false;
	            System.out.println("File 1 extends File 2 at line " + in1.getLineNumber());
	            break;
	        }
	        
	        if (lineCount > skipLineCount && !l1.equals(l2)) {
	            equal = false;
	            System.out.println("Mismatch");
	            System.out.println("File 1 Line " + in1.getLineNumber() + ": " + l1);
	            System.out.println("File 2 Line " + in2.getLineNumber() + ": " + l2);
	            
	            if (!sync()) {
	                break;
	            }
	        }
        }
        
        in1.close();
        in2.close();
        
        return equal;
    }
    
    private boolean sync() throws IOException {
        int maxCharactersInLine = 200;
        int readAheadLimit = lookAheadLines * maxCharactersInLine;
        
        in2.mark(readAheadLimit);
        in1.mark(readAheadLimit);
        
        for (int i=0; i<lookAheadLines; i++) {
            String l1 = in1.readLine();
            if (l1==null) break;
            
            for (int j=0; j<lookAheadLines; j++) {
                String l2 = in2.readLine();
                if (l2==null) break;
                if (l1.equals(l2)) return true;
            }
            try {
                in2.reset();
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

	/**
	 * @return Returns the skipLineCount.
	 */
	public int getSkipLineCount() {
		return skipLineCount;
	}
	/**
	 * @param theSkipLineCount The skipLineCount to set.
	 */
	public void setSkipLineCount(int theSkipLineCount) {
		skipLineCount = theSkipLineCount;
	}
}
