/*
 * Created on Feb 13, 2005
 */
package com.j2js.dom;

/**
 * Copyright by Wolfgang Kuehn 2005
 */
public class ConditionalBranch extends Branch {
    
    private Expression expression;
    
    public ConditionalBranch(int targetIndex) {
        super(targetIndex);
    }
    
    /**
     * This constructor is for testing purposes only!!
     */
    public ConditionalBranch(int theBeginIndex, int theEndIndex, int targetIndex) {
        super(targetIndex);
        setExpression(new Expression(theBeginIndex, theEndIndex));
    }
    
    /**
     * @return Returns the condition.
     */
    public Expression getExpression() {
        return expression;
    }
    
    /**
     * @param condition The condition to set.
     */
    public void setExpression(Expression theExpression) {
        expression = theExpression;
        widen(theExpression);
        appendChild(theExpression);
    }

}
