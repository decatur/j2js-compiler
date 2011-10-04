package com.j2js.assembly;

import java.io.IOException;
import java.io.Writer;

/**
 * A FieldUnit provides information about, and dynamic access to, a single field of a class or an interface.
 * 
 * @author wolle
 */
public class FieldUnit extends MemberUnit {

    public FieldUnit(Signature theSignature, ClassUnit theDeclaringClazz) {
        super(theSignature, theDeclaringClazz);
    }
    
    void write(int depth, Writer writer) throws IOException {
        return;
    }

}
