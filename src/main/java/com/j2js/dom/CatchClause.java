package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;


/*
 * CatchStatement.java
 *
 * Created on 22. Mai 2004, 22:49
 */

/**
 *
 * @author  kuehn
 */
public class CatchClause extends Block {
    
    private VariableDeclaration exception;
    
    /** Creates a new instance of CatchStatement */
    //public CatchStatement() {
    //}
    
    public CatchClause(int theBeginIndex) {
    	super(theBeginIndex);
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }

    /**
     * @return Returns the exception.
     */
    public VariableDeclaration getException() {
        return exception;
    }
    
    /**
     * @param theException The exception to set.
     */
    public void setException(VariableDeclaration theException) {
        exception = theException;
    }
}
