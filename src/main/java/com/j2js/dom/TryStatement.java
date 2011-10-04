package com.j2js.dom;

import com.j2js.cfg.TryHeaderNode;
import com.j2js.visitors.AbstractVisitor;

/**
 * TryStatement.java
 *
 * Created on 21. Mai 2004, 17:33
 * @author  kuehn
 */
public class TryStatement extends Block {
    
    public TryHeaderNode header;
    
    /** Creates a new instance of TryStatement */
    public TryStatement() {
    	super();
    }
    
    public void addCatchStatement(CatchClause catchStatement) {
    	if (getChildCount() < 2) throw new RuntimeException("Illegal DOM state");
        ((Block) getChildAt(1)).appendChild(catchStatement);
    }
    
    public Block getCatchStatements() {
        return (Block) getChildAt(1);
    }
    
    /** Getter for property finallyBlock.
     * @return Value of property finallyBlock.
     */
    public Block getFinallyBlock() {
        if (getChildCount() < 3) return null;
        return (Block) getChildAt(2);
    }    
    
    /** Setter for property finallyBlock.
     * @param finallyBlock New value of property finallyBlock.
     */
    public void setFinallyBlock(Block finallyBlock) {
    	setChildAt(2, finallyBlock);
    }
    
    /**
     * @return Returns the tryBlock.
     */
    public Block getTryBlock() {
        return (Block) getChildAt(0);
    }

    /**
     * @param tryBlock The tryBlock to set.
     */
    public void setTryBlock(Block tryBlock) {
        setChildAt(0, tryBlock);
        setChildAt(1, new Block());
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
    
    public String toString() {
        return super.toString();
    }

}
