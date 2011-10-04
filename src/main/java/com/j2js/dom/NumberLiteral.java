package com.j2js.dom;

import org.apache.bcel.generic.Type;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class NumberLiteral extends Expression {
    
    public static Number ZERO = new Integer(0);
    public static Number ONE = new Integer(1);
    public static Number MINUS_ONE = new Integer(-1);
    
    private Number value;
    
    public static boolean isZero(Expression expr) {
        if (expr == null || !(expr instanceof NumberLiteral)) return false;
        return ((NumberLiteral) expr).getValue().equals(ZERO);
    }
    
    public static boolean isOne(Expression expr) {
        if (expr == null || !(expr instanceof NumberLiteral)) return false;
        return ((NumberLiteral) expr).getValue().equals(ONE);
    }
    
    public static boolean isMinusOne(Expression expr) {
        if (expr == null || !(expr instanceof NumberLiteral)) return false;
        return ((NumberLiteral) expr).getValue().equals(MINUS_ONE);
    }
    
    private NumberLiteral(Number theValue) {
        value = theValue;
        if (theValue instanceof Integer) {
            type = Type.INT;
        } else if (theValue instanceof Byte) {
            type = Type.BYTE;
        } else if (theValue instanceof Float) {
            type = Type.FLOAT;
        } else if (theValue instanceof Double) {
            type = Type.DOUBLE;
        } else if (theValue instanceof Long) {
            type = Type.LONG;
        } else if (theValue instanceof Short) {
            type = Type.SHORT;
        } else
            // TODO: Other types
            throw new RuntimeException("Type not supported: " + theValue.getClass());
    }
    
    public static NumberLiteral create(int i) {
        Number value;
        if (i == 0) value = ZERO;
        else if (i == 1) value = ONE;
        else value = new Integer(i);
        return new NumberLiteral(value);
    }
    
    public static NumberLiteral create(Number value) {
        if (value instanceof Integer) {
            Integer i = (Integer) value;
            if (i.intValue()==0) value = ZERO;
            else if (i.intValue()==1) value = ONE;
        }
        return new NumberLiteral(value);
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
    
	/**
	 * @return Returns the value.
	 */
	public Number getValue() {
		return value;
	}

	/**
	 * @param theValue The value to set.
	 */
	public void setValue(Number theValue) {
		value = theValue;
	}
	
	public String toString() {
		return super.toString() + " value " + value;
	}

}
