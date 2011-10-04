package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class BooleanLiteral extends Expression {
    // Note: Never ever use TRUE as part of a DOM.
    public static BooleanLiteral FALSE = new BooleanLiteral(false);
    // Note: Never ever use TRUE as part of a DOM.
    public static BooleanLiteral TRUE = new BooleanLiteral(true);
    
	private boolean value;
	
	public BooleanLiteral(boolean theValue) {
    	value = theValue;
    }

	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
	
	/**
	 * @return Returns the value.
	 */
	public boolean getValue() {
		return value;
	}
	/**
	 * @param theValue The value to set.
	 */
	public void setValue(boolean theValue) {
		value = theValue;
	}
}
