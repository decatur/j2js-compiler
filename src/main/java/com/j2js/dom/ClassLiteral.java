package com.j2js.dom;

import com.j2js.assembly.Signature;
import com.j2js.visitors.AbstractVisitor;

public class ClassLiteral extends Expression {

    private Signature signature;
    
    public ClassLiteral(Signature theSignature) {
        signature = theSignature;
    }

    public void visit(AbstractVisitor visitor) {
        visitor.visit(this);
    }
    
    public Signature getSignature() {
        return signature;
    }
    
}
