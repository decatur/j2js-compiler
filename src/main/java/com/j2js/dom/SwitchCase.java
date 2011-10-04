/*
 * Created on Oct 24, 2004
 */
package com.j2js.dom;

import java.util.List;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class SwitchCase extends Block {
	
	private List<NumberLiteral> expressions;

	public SwitchCase(int theBeginIndex) {
    	super(theBeginIndex);
    }
	
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
	
	/**
	 * @return Returns the expression.
	 */
	public List<NumberLiteral> getExpressions() {
		return expressions;
	}
	/**
	 * @param expression The expression to set.
	 */
	public void setExpressions(List<NumberLiteral> theExpressions) {
		expressions = theExpressions;
	}

}
