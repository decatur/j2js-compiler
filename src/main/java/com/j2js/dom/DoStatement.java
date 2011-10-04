package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/*
 * DoStatement.java
 *
 * Created on 21. Mai 2004, 17:30
 */

/**
 *
 * @author  kuehn
 */
public class DoStatement extends LoopStatement {
    
    public DoStatement() {
    	super();
    }
    
    public DoStatement(int theBeginIndex) {
    	super(theBeginIndex);
    }
    
    public DoStatement(int theBeginIndex, int theEndIndex) {
        super(theBeginIndex, theEndIndex);
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
}
