package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class ThrowStatement extends Block {
	
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
	
	/**
	 * @return Returns the expression.
	 */
	public Expression getExpression() {
		return (Expression) getFirstChild();
	}
	/**
	 * @param expression The expression to set.
	 */
	public void setExpression(Expression expression) {
		widen(expression);
		appendChild(expression);
	}
}
