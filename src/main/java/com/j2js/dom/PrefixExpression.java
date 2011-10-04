package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * Tagging class for a prefix expression.
 * @author kuehn
 */
public class PrefixExpression extends PStarExpression {
	
	static public Operator NOT = new Operator("!");
//    static public Operator INCREMENT = new Operator("++");
//    static public Operator DECREMENT = new Operator("--");
    static public Operator MINUS = new Operator("-");
    static public Operator PLUS = new Operator("+");
    static public Operator COMPLEMENT = new Operator("~");
    
	public PrefixExpression() {
	    super();
	}
	
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
}