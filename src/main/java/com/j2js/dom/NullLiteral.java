package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class NullLiteral extends Expression {
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
}
