package com.j2js.dom;

import java.util.List;

import org.apache.bcel.generic.Type;

import com.j2js.assembly.Project;
import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class ArrayCreation extends Expression {
	
	private List<ASTNode> dimensions;
	private ArrayInitializer initializer;
	
    public ArrayCreation(MethodDeclaration methodDecl, Type theType, List<ASTNode> theDimensions) {
        type = theType;
        dimensions = theDimensions;
        for (ASTNode dimension : dimensions) {
            this.widen(dimension);
        }
        Project.getSingleton().addReference(methodDecl, this);
    }
    
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }

	/**
	 * @return Returns the initializer.
	 */
	public ArrayInitializer getInitializer() {
		return initializer;
	}
	/**
	 * @param theInitializer The initializer to set.
	 */
	public void setInitializer(ArrayInitializer theInitializer) {
		initializer = theInitializer;
	}
	/**
	 * @return Returns the dimensions.
	 */
	public List<ASTNode> getDimensions() {
		return dimensions;
	}

}
