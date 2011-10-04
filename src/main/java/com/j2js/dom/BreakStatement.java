package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

public class BreakStatement extends LabeledJump {
    
    public BreakStatement(String theLabel) {
        super(theLabel);
    }
    
    public BreakStatement(Block block) {
        super(block);
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
    
}
