package com.j2js.dom;

import org.apache.bcel.generic.ObjectType;

import com.j2js.assembly.Project;
import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public abstract class FieldAccess extends Expression {

    private String name;
    private ObjectType type;
    //private MethodDeclaration methodDecl;
    
    public FieldAccess() {
    }
    
    public void initialize(MethodDeclaration methodDecl) {
        Project.getSingleton().addReference(methodDecl, this);
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
    
//    if (!faa.getName().equals(fab.getName())) return false;
//    
    public boolean isSame(Object obj) {
        if (!(obj instanceof FieldAccess)) return false;
        FieldAccess other = (FieldAccess) obj;
        if (!name.equals(other.name)) return false;
        if (getExpression() instanceof VariableBinding && other.getExpression() instanceof VariableBinding) {
            VariableBinding vba = (VariableBinding) getExpression();
            VariableBinding vbb = (VariableBinding) other.getExpression();
            return vba.getVariableDeclaration() == vbb.getVariableDeclaration();
        }
        return false;
    }

	/**
	 * @return Returns the expression.
	 */
	public Expression getExpression() {
		return (Expression) getFirstChild();
	}

	/**
	 * @param expression The expression to set.
	 */
	public void setExpression(Expression expression) {
		widen(expression);
		removeChildren();
		appendChild(expression);
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param theName The name to set.
	 */
	public void setName(String theName) {
		name = theName;
	}
	
	public String toString() {
		return super.toString() + " " + name;
	}

    public ObjectType getType() {
        return type;
    }

    public void setType(ObjectType theType) {
        if (type != null) throw new RuntimeException("Type is already set");
        type = theType;
    }

}
