package com.j2js.dom;

import java.util.List;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class ArrayInitializer extends Expression {
	
	private List<Expression> expressions = new java.util.ArrayList<Expression>();

	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
	
	/**
	 * @return Returns the expressions.
	 */
	public List<Expression> getExpressions() {
		return expressions;
	}
}
