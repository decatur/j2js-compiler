package com.j2js.dom;

import org.apache.bcel.generic.Type;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author kuehn
 */
public class ThisExpression extends VariableBinding {

	private static VariableDeclaration vd;
    
    static {
        vd = new VariableDeclaration(VariableDeclaration.NON_LOCAL);
        vd.setName("this");
        vd.setType(Type.OBJECT);
    }
    
    public ThisExpression() {
		super(vd);
	}
	
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
}
