package com.j2js.dom;

import org.apache.bcel.generic.Type;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class VariableBinding extends Expression implements Assignable {
    private boolean field;
	private VariableDeclaration decl;
	private boolean isTemporary = false;
    
    public static boolean isBoolean(Expression expr) {
        if (expr == null || !(expr instanceof VariableBinding)) return false;
        if (((VariableBinding) expr).getVariableDeclaration().getType() != Type.BOOLEAN) return false;
        return true;
    }
    
	public VariableBinding(VariableDeclaration theDecl) {
		super();
		decl = theDecl;
        decl.vbs.add(this);
		setTypeBinding(theDecl.getType());
	}
    
    public Object clone() {
        // TODO: Make cloning explicit.
        VariableBinding other = (VariableBinding) super.clone();
        decl.vbs.add(other);
        return other;
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
    
    public boolean isSame(Object other) {
    	if (other == null || !(other instanceof VariableBinding)) return false;
    	return decl == ((VariableBinding) other).decl;
    }

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return decl.getName();
	}

	/**
	 * @return Returns the field.
	 */
	public boolean isField() {
		return field;
	}

	/**
	 * @param theField The field to set.
	 */
	public void setField(boolean theField) {
		field = theField;
	}

	/**
	 * @return Returns the decl.
	 */
	public VariableDeclaration getVariableDeclaration() {
		return decl;
	}
    
    public String toString() {
        return super.toString() + ' ' + decl.getName();
    }

    public boolean isTemporary() {
        return isTemporary;
    }

    public void setTemporary(boolean isTemporary) {
        this.isTemporary = isTemporary;
    }
}
