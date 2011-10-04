/*
 * Created on Oct 31, 2004
 */
package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class ArrayAccess extends Expression implements Assignable {
	
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
	
    public boolean isSame(Object obj) {
        if (!(obj instanceof ArrayAccess)) return false;
        ArrayAccess other = (ArrayAccess) obj;
        if (getArray() instanceof VariableBinding && other.getArray() instanceof VariableBinding) {
            VariableBinding vba = (VariableBinding) getArray();
            VariableBinding vbb = (VariableBinding) other.getArray();
            return vba.getVariableDeclaration() == vbb.getVariableDeclaration();
        }
        return false;
    }
    
	/**
	 * @return Returns the array.
	 */
	public Expression getArray() {
        return (Expression) getChildAt(0);
	}
	/**
	 * @param array The array to set.
	 */
	public void setArray(Expression array) {
		widen(array);
        setChildAt(0, array);
	}
	/**
	 * @return Returns the index.
	 */
	public Expression getIndex() {
        return (Expression) getChildAt(1);
	}
	/**
	 * @param index The index to set.
	 */
	public void setIndex(Expression index) {
        widen(index);
        setChildAt(1, index);
	}
}
