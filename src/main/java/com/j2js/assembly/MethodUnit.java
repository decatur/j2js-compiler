package com.j2js.assembly;

/**
 * A MethodUnit provides information about, and access to, a single method on a class or interface.
 * 
 * @author wolle
 */
public class MethodUnit extends ProcedureUnit {
    
    public MethodUnit(Signature theSignature, ClassUnit theDeclaringClazz) {
        super(theSignature, theDeclaringClazz);
    }

}
