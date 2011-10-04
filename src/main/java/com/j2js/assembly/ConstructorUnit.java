package com.j2js.assembly;

/**
 * ConstructorUnit provides information about, and access to, a single constructor for a class.
 * 
 * @author wolle
 */
public class ConstructorUnit extends ProcedureUnit {
    
    public ConstructorUnit(Signature theSignature, ClassUnit theDeclaringClazz) {
        super(theSignature, theDeclaringClazz);
    }

}
