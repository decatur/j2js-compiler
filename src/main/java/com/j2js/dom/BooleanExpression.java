package com.j2js.dom;

public class BooleanExpression implements Cloneable {
    private Expression expression;
    
    public BooleanExpression(Expression newExpression) {
        expression = newExpression;
    }

    public Expression getExpression() {
        return expression;
    }


}
