/*
 * Created on Sep 10, 2005
 */
package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class ConditionalExpression extends Expression {
	
	private Expression conditionExpression = null;
	private Expression thenExpression = null;
	private Expression elseExpression = null;
	
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
	
	/**
	 * @return Returns the conditionExpression.
	 */
	public Expression getConditionExpression() {
		return conditionExpression;
	}
	/**
	 * @param theConditionExpression The conditionExpression to set.
	 */
	public void setConditionExpression(Expression theConditionExpression) {
		widen(theConditionExpression);
		conditionExpression = theConditionExpression;
	}
	/**
	 * @return Returns the elseExpression.
	 */
	public Expression getElseExpression() {
		return elseExpression;
	}
	/**
	 * @param theElseExpression The elseExpression to set.
	 */
	public void setElseExpression(Expression theElseExpression) {
		widen(theElseExpression);
		elseExpression = theElseExpression;
	}
	/**
	 * @return Returns the thenExpression.
	 */
	public Expression getThenExpression() {
		return thenExpression;
	}
	/**
	 * @param theThenExpression The thenExpression to set.
	 */
	public void setThenExpression(Expression theThenExpression) {
		widen(theThenExpression);
		thenExpression = theThenExpression;
	}

}
