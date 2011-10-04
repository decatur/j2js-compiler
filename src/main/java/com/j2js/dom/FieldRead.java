package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

public class FieldRead extends FieldAccess {

    public FieldRead() {
    }
    
    public void visit(AbstractVisitor visitor) {
        visitor.visit(this);
    }

}
