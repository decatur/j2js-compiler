package com.j2js.dom;

import org.apache.bcel.generic.ObjectType;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class ClassInstanceCreation extends MethodInvocation {
	
    public ClassInstanceCreation(ObjectType theType) {
        type = theType;
    }
    
    public ClassInstanceCreation(MethodDeclaration methodDecl, MethodBinding methodBinding) {
        super(methodDecl, methodBinding);
    }
    
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }

}
