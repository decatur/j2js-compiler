package com.j2js.dom;

import org.apache.bcel.generic.Type;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class StringLiteral extends Expression {
    
    private String value;
    
    public StringLiteral(String theValue) {
        value = theValue;
        type = Type.STRING;
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
    
	/**
	 * @return Returns the value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param theValue The value to set.
	 */
	public void setValue(String theValue) {
		value = theValue;
	}

}
