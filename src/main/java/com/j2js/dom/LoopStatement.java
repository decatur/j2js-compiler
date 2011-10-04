package com.j2js.dom;

/*
 * LoopStatement.java
 *
 * Created on 21. Mai 2004, 16:51
 */

/**
 *
 * @author  kuehn
 */
public class LoopStatement extends Block {
    
    public LoopStatement() {
    	super();
    }
    
    public LoopStatement(int theBeginIndex) {
    	super(theBeginIndex);
    }
    
    public LoopStatement(int theBeginIndex, int theEndIndex) {
        super(theBeginIndex, theEndIndex);
    }

    public Expression getExpression() {
        return (Expression) getChildAt(1);
    }
    
    public void setExpression(Expression expression) {
        widen(expression);
        setChildAt(1, expression);
    }

    public Block getBlock() {
        return (Block) getChildAt(0);
    }
    
    public void setBlock(Block block) {
        widen(block);
        setChildAt(0, block);
    }
}
