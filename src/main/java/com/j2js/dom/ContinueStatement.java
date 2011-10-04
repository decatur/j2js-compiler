package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

public class ContinueStatement extends LabeledJump {
    
	public ContinueStatement(Block block) {
        super(block);
    }
	
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
    
}

