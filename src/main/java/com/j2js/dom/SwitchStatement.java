/*
 * Created on Oct 24, 2004
 */
package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class SwitchStatement extends Block {
	
	private Expression expression;

	public SwitchStatement() {
    	super();
    }
	
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
	
	public SwitchCase getDefault() {
		return (SwitchCase) getLastChild();
	}
	
	/**
	 * @return Returns the expression.
	 */
	public Expression getExpression() {
		return expression;
	}
	/**
	 * @param theExpression The expression to set.
	 */
	public void setExpression(Expression theExpression) {
		widen(theExpression);
		expression = theExpression;
	}
}
