package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/*
 * Created on Sep 12, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

/**
 * @author wolfgang
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ReturnStatement extends ASTNode {
	
	private Expression expression;
	
    public ReturnStatement(int theBeginIndex, int theEndIndex) {
        setRange(theBeginIndex, theEndIndex);
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }

	/**
	 * @param theExpression The expression to set.
	 */
	public void setExpression(Expression theExpression) {
		widen(theExpression);
		expression = theExpression;
	}

	/**
	 * @return Returns the expression.
	 */
	public Expression getExpression() {
		return expression;
	}
}
