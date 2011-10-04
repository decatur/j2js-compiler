package com.j2js.util;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;


public final class BuilderUtils {
    
    public static String removePrefixPath(File baseDir, File file) {
        return baseDir.toURI().relativize(file.toURI()).toString();
    }
    
    private static String findGreatestCommonPrefix(String a, String b) {
        int previousSlashIndex = -1;
        int i;
        for (i=0; i<Math.min(a.length(), b.length()) && a.charAt(i) == b.charAt(i); i++) {
            if ( a.charAt(i) == '/' ) {
                previousSlashIndex = i;
            }
        }
        
        return a.substring(0, previousSlashIndex+1);
    }
    
    /**
     * Returns the relative path from base to target.
     * Example:
     * base =   new File("aa/bb/cc/dd/foo.ext")
     * target = new File("aa/bb/ee/bar.ext");
     * result = "../../ee/bar.ext
     */
    public static String getRelativePath(File base, File target) {
        String baseString = base.getAbsolutePath().replace('\\', '/');
        String targetString = target.getAbsolutePath().replace('\\', '/');
        
        String commonPrefix = findGreatestCommonPrefix(baseString, targetString);
        
        if (commonPrefix.length() == 0) {
            throw new IllegalArgumentException("Arguments must have common prefix");
        }
        
        String relativePath = targetString.substring(commonPrefix.length());
        // relativePath = "ee/bar.ext"
        
        if (commonPrefix.length() == baseString.length()) {
            // base is prefix for target.
            return relativePath;
        } else {
            // Convert remainder to ../ sequence, for example
            // "cc/dd/foo.ext" to "../../"
            String remainder = baseString.substring(commonPrefix.length());
            StringBuffer cdParent = new StringBuffer();
            for (char c : remainder.toCharArray()) {
                if (c == '/') {
                    cdParent.append("../");
                }
            }
            return cdParent.toString() + relativePath;
        }
    }
    
    public static Matcher getProtectedRegion(String htmlContent) throws IOException {
        // We must parse lines like this:
        // <script id="j2js-loader" type='text/javascript' defer='defer' src="j2js-powerdash-manager-Manager/0.js"></script>
        
        StringBuilder sb = new StringBuilder();
        sb.append("id\\s*=\\s*[\'\"]j2js-loader[\'\"].*src\\s*=\\s*[\'\"]([^\'\"]+)/0\\.js[\'\"]");
        
        Pattern p = Pattern.compile(sb.toString(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        return p.matcher(htmlContent);
    }
    
    public static String extractScriptSrc(File htmlFile) throws IOException {
        String htmlContent = FileUtils.readFileToString(htmlFile);
        Matcher matcher = getProtectedRegion(htmlContent);
        if (!matcher.find()) return null;
        
        String scriptSrc = htmlContent.substring(matcher.start(1), matcher.end(1));
        scriptSrc = scriptSrc.trim();
        
        if (scriptSrc.length() == 0) return null;
        return scriptSrc;
    }

}
