package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/*
 * Statement.java
 *
 * Created on 21. Mai 2004, 11:45
 */

/**
 *
 * @author  kuehn
 */
public class ASTNode {
    
    public static final int BEFORE = 0;
    public static final int AFTER = 1;
    public static final int SAME = 2;
    public static final int CONTAINS = 3;
    public static final int ISCONTAINED = 4;
	
	int beginIndex = Integer.MAX_VALUE;
    int endIndex = Integer.MIN_VALUE;
    private ASTNode parent = null;
    private ASTNode previousSibling = null;
    private ASTNode nextSibling = null;
    
    private int stackDelta = 0;
    
    public ASTNode() {
    	super();
    }
    
    public ASTNode(int theBeginIndex, int theEndIndex) {
        setRange(theBeginIndex, theEndIndex);
    }
    
    /**
     * @return Returns the stackDelta.
     */
    public int getStackDelta() {
        return stackDelta;
    }

    /**
     * @param theStackDelta The stackDelta to set.
     */
    public void setStackDelta(int theStackDelta) {
        stackDelta = theStackDelta;
    }
    
    public void widen(ASTNode node) {
    	leftWiden(node.beginIndex);
    	rightWiden(node.endIndex);
    }
    
    public void leftWiden(int targetBeginIndex) {
    	if (targetBeginIndex < beginIndex) beginIndex = targetBeginIndex;
    }
    
    public void rightWiden(int targetEndIndex) {
    	if (targetEndIndex > endIndex) endIndex = targetEndIndex;
    }
    
    public void setRange(int theBeginIndex, int theEndIndex) {
    	setBeginIndex(theBeginIndex);
        setEndIndex(theEndIndex);
    }

//    private void checkRange() {
//    	if (endIndex!=Integer.MIN_VALUE && beginIndex!=Integer.MAX_VALUE && endIndex<beginIndex) {
//    		throw new RuntimeException("Begin index greater than end index: " + beginIndex + ">" + endIndex);
//    	}
//    }
    
    /** Getter for property beginIndex.
     * @return Value of property beginIndex.
     */
    public int getBeginIndex() {
        return beginIndex;
    }    
    
    /** Setter for property beginIndex.
     * @param theBeginIndex New value of property beginIndex.
     */
    public void setBeginIndex(int theBeginIndex) {
    	beginIndex = theBeginIndex;
    }
    
    /** Getter for property endIndex.
     * @return Value of property endIndex.
     */
    public int getEndIndex() {
        return endIndex;
    }
    
    /** Setter for property endIndex.
     * @param endIndex New value of property endIndex.
     */
    public void setEndIndex(int theEndIndex) {
    	endIndex = theEndIndex;
    }

    public boolean isRightSiblingOf(ASTNode leftSibling) {
        for (ASTNode node=this; node!=null; node=node.getPreviousSibling()) {
	        if (node == leftSibling) {
	            return true;
	        }
	    }
        return false;
    }
    
    public ASTNode rightMostSibling() {
        for (ASTNode node=this;;) {
	        if (node.getNextSibling() == null) {
	            return node;
	        }
	        node = node.getNextSibling();
	    }
    }
	
    public boolean isAncestorOf(ASTNode node) {
    	do {
    		node = node.getParentNode();
    		if (node == this) {
    			return true;
    		}
    	} while (node != null);
    	
    	return false;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        if (getBeginIndex() != Integer.MAX_VALUE) {
            sb.append("[");
            sb.append(getBeginIndex());
            sb.append(", ");
            sb.append(getEndIndex());
            sb.append("]");
        }
        return sb.toString();
    }
    
	/**
	 * @return Returns the parent.
	 */
	public ASTNode getParentNode() {
		return parent;
	}
	
	public Block getParentBlock() {
		return (Block) parent;
	}
    
    public Block getLogicalParentBlock() {
        if (parent != null && parent.parent instanceof IfStatement) {
            return (Block) parent.parent;
        }
        return (Block) parent;
    }

	/**
	 * @param theParent The parent to set.
	 */
	public void setParentNode(ASTNode theParent) {
		parent = theParent;
	}
    
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
	
	/**
     * @return Returns the nextSibling.
     */
    public ASTNode getNextSibling() {
        return nextSibling;
    }
    /**
     * @param theNextSibling The nextSibling to set.
     */
    public void setNextSibling(ASTNode theNextSibling) {
        nextSibling = theNextSibling;
    }
    /**
     * @return Returns the previousSibling.
     */
    public ASTNode getPreviousSibling() {
        return previousSibling;
    }
    /**
     * @param thePreviousSibling The previousSibling to set.
     */
    public void setPreviousSibling(ASTNode thePreviousSibling) {
        previousSibling = thePreviousSibling;
    }
}
