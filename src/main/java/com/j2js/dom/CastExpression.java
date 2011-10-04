package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author kuehn
 */
public class CastExpression extends Expression {

	private Expression expression;
	
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
