package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

public class FieldWrite extends FieldAccess implements Assignable {

    public FieldWrite() {
    }
    
    public void visit(AbstractVisitor visitor) {
        visitor.visit(this);
    }

}
