package com.j2js.assembly;

import com.j2js.J2JSCompiler;

import java.io.Serializable;


/**
 * An instance of class Signature represents the signature of
 * a field, a constructor, a method, or a class.
 * 
 * @author wolle
 */
public class Signature implements Serializable {
    
    private String signatureString;
    private int id;
    
    Signature(String theSignatureString, int theId) {
        signatureString = theSignatureString;
        id = theId;
    }
    
    /**
     * Examples:
     * Ljava.lang.Integer; -> java.lang.Integer
     * L[I; -> [I
     */
//    public String getClassName() {
//        if (!signatureString.startsWith("L") || !signatureString.endsWith(";")) {
//            throw new RuntimeException("Not a class signature: " + signatureString);
//        }
//        return signatureString.substring(1, signatureString.length()-1);
//    }

    public int hashCode() {
        return signatureString.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof Signature) {
            return signatureString.equals(((Signature) obj).signatureString);
        }
        return false;
    }
    
    public String toString() {
        return signatureString;
    }
    
    public boolean isClass() {
        return signatureString.indexOf('#') == -1;
    }
    
    public boolean isArrayType() {
        return isClass() && signatureString.startsWith("[");
    }

    public boolean isConstructor() {
        return signatureString.startsWith("<init>");
    }
    
    public boolean isMethod() {
        return !isConstructor() && signatureString.indexOf('(') != -1;
    }
    
    public boolean isField() {
        return !isClass() && signatureString.indexOf('(') == -1;
    }
    
    public String className() {
        String array[] = signatureString.split("#");
        //if (array[0].startsWith("[")) array[0] = array[0].substring(1); 
        return array[0];
    }
    
    public String relativeSignature() {
        String array[] = signatureString.split("#");
        if (array.length != 2) {
            throw new RuntimeException("Not a method signature: " + this);
        }
        return array[1];
    }
    
    /**
     * Returns the relative signature.
     */
    public Signature relative() {
        return Project.getSingleton().getSignature(relativeSignature());
    }
    
    public int getId() {
        return id;
    }
    
    public String getCommentedId() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.valueOf(getId()));
        if (!J2JSCompiler.compiler.isCompression()) {
            sb.append(" /*");
            sb.append(toString());
            sb.append("*/");
        }
        return sb.toString();
    }
}
