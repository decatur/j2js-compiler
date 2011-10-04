package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * Tagging class for a postfix expression.
 * @author kuehn
 */
public class PostfixExpression extends PStarExpression {
	
	public PostfixExpression() {
	    super();
	}
	
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
}
