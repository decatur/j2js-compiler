package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/*
 * WhileStatement.java
 *
 * Created on 21. Mai 2004, 17:30
 */

/**
 *
 * @author  kuehn
 */
public class WhileStatement extends LoopStatement {
    
    public WhileStatement() {
    	super();
    }
    
    public WhileStatement(int theBeginIndex) {
    	super(theBeginIndex);
    }
    
    /** Creates a new instance of WhileStatement */
    public WhileStatement(int theBeginIndex, int theEndIndex) {
        super(theBeginIndex, theEndIndex);
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
}
