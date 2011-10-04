package com.j2js.dom;

import com.j2js.Log;
import com.j2js.visitors.AbstractVisitor;
import org.w3c.dom.DOMException;

/*
 * Block.java
 *
 * Created on 21. Mai 2004, 11:38
 */
public class Block extends ASTNode {
    
	public static int TAG = 0;
	
    private String label;
    
    private ASTNode firstChild = null;
    private ASTNode lastChild = null;
    private int childCount = 0;
    
    public Block() {
    	super();
    }
    
    public Block(int theBeginIndex) {
        setBeginIndex(theBeginIndex);
     }
    
    public Block(int theBeginIndex, int theEndIndex) {
        this(theBeginIndex);
        setRange(theBeginIndex, theEndIndex);
    }
    
//    public Block getFooBlock() {
//        Block block = this;
//        while (block!=null) {
//            if (!block.getClass().equals(Block.class)) {
//                return block;
//            }
//            block = block.getParentBlock();
//        }
//        return this;
//    }
    
    public int getTargetPc() {
        if (lastChild instanceof Jump) {
            return ((Jump) lastChild).getTargetIndex();
        }
        return Integer.MAX_VALUE;
    }
    
    public int getTargetIndex() {
        return beginIndex;
    }

    public void setBeginIndex(int theBeginIndex) {
        super.setBeginIndex(theBeginIndex);
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }

    public void appendChildren(ASTNode begin, ASTNode end) {
    	if (begin == null || end == null) {
        	throw new RuntimeException("Illegal null parameters");
        }
        if (begin.getParentBlock() != null)
            (begin.getParentBlock()).removeChildren(begin, end);

        if (firstChild == null) {
            setFirstChildInternal(begin);
        } else {
            ASTNode prev = getLastChild();
            prev.setNextSibling(begin);
            begin.setPreviousSibling(prev);
        }
        setLastChildInternal(end);

        ASTNode node = begin;
        while (node != null) {
            node.setParentNode(this);
            childCount++;
            if (node == end) break;
            node = node.getNextSibling();
        }    
    }
    
    /**
     * Appends all children of the specified block to this block.
     * @param sourceBlock
     */
    public void appendChildren(Block sourceBlock) {
        if (sourceBlock.getChildCount() > 0) {
            appendChildren(sourceBlock.getFirstChild(), sourceBlock.getLastChild());
        }
    }
    
    /**
     * Sets the specified node at the index. Returns the replaced node, or null if the index
     * was equal to the cild count.
     */
    public ASTNode setChildAt(int index, ASTNode newChild) {
        if (index == childCount) {
            appendChild(newChild);
            return null; 
        } else if (index < 0 || index > childCount) {
            throw new RuntimeException("Index " + index + " out of range [0, " + childCount + "]");
        }
        
        return replaceChild(newChild, getChildAt(index));
    }
    
    public ASTNode getChildAt(int index) {
        if (childCount == 0) {
            throw new RuntimeException("Requested child at index " + index + ", but block has no children");
        }
        if (index < 0 || index >= childCount) {
            throw new RuntimeException("Index " + index + " out of range [0, " + (childCount-1) + "]");
        }
        
        if (index == childCount-1) {
            return getLastChild();
        }
        
        ASTNode node = getFirstChild();
        int i = 0;
        while (i < index) {
            i++;
            node = node.getNextSibling();
        }
        
        return node;
    }
    
    public ASTNode appendChild(ASTNode newChild) {
    	Log.getLogger().debug("Appending " + newChild + " to " + this);
    	
    	unlink(newChild);
        
        if (firstChild == null) {
            newChild.setPreviousSibling(null);
            setFirstChildInternal(newChild);
        } else {
            ASTNode prev = getLastChild();
            prev.setNextSibling(newChild);
            newChild.setPreviousSibling(prev);
        }
        setLastChildInternal(newChild);
        newChild.setParentNode(this);
        childCount++;

    	return newChild;
    }
    
    public ASTNode replaceChild(ASTNode newChild, ASTNode oldChild) {
    	Log.getLogger().debug("Replacing " + oldChild + " by " + newChild + " in " + this);
        if (oldChild.getParentNode() != this) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "Node " + oldChild + " is not a child of " + this);
        }
        unlink(newChild);
        if (oldChild.getPreviousSibling() != null) {
            oldChild.getPreviousSibling().setNextSibling(newChild);
        }
        if (oldChild.getNextSibling() != null) {
            oldChild.getNextSibling().setPreviousSibling(newChild);
        }
        newChild.setPreviousSibling(oldChild.getPreviousSibling());
	    newChild.setNextSibling(oldChild.getNextSibling());
	    newChild.setParentNode(this);
	    if (getFirstChild() == oldChild) {
	        setFirstChildInternal(newChild);
	    }
	    if (getLastChild() == oldChild) {
	        setLastChildInternal(newChild);
	    }
        oldChild.setPreviousSibling(null);
	    oldChild.setNextSibling(null);
	    oldChild.setParentNode(null);
	    return oldChild;
	}
    
    public ASTNode removeChild(ASTNode oldChild) {
    	Log.getLogger().debug("Removing " + oldChild + " from " + this);
        removeChildren(oldChild, oldChild);
        
	    oldChild.setPreviousSibling(null);
	    oldChild.setNextSibling(null);
        return oldChild;
    }
        
    /**
     * Removes all children from begin to end inclusively.
     */
    private void removeChildren(ASTNode begin, ASTNode end) {
        if (begin == null || end == null) throw new RuntimeException("Illegal null parameters");
    	ASTNode node = begin;
        while (node!= null) {
            if (node.getParentNode() != this) {
                throw new DOMException(DOMException.NOT_FOUND_ERR, "Node " + node + " is not a child of " + this);
            }
            node.setParentNode(null);
            childCount--;
            if (node == end) break;
            node = node.getNextSibling();
        }
        
        
        if (node != end) {
        	throw new RuntimeException("Node " + end + " is not a right-sibling of " + begin);
        }

        if (begin.getPreviousSibling() != null) {
            begin.getPreviousSibling().setNextSibling(end.getNextSibling());
        }
        if (end.getNextSibling() != null) {
            end.getNextSibling().setPreviousSibling(begin.getPreviousSibling());
        }
        if (getFirstChild() == begin) {
	        setFirstChildInternal(end.getNextSibling());
	    }
	    if (getLastChild() == end) {
	        setLastChildInternal(begin.getPreviousSibling());
	    }
    }
    
    public void removeChildren() {
    	if (getFirstChild() != null) {
    		removeChildren(getFirstChild(), getLastChild());
    	}
    }
    
    public int getChildCount() {
        return childCount;
    }
    
    /**
     * Inserts the node newChild after the existing child node refChild.
     * If refChild is null, insert newChild at the beginning of the list of children.  
     * 
     * @param newChild The node to insert
     * @param refChild The reference node, i.e., the node before which the new node must be inserted
     * @return
     */
//    public ASTNode insertAfter(ASTNode newChild, ASTNode refChild) {
//        if (refChild == null) {
//            refChild = getFirstChild();
//        } else {
//            refChild = refChild.getNextSibling();
//        }
//        return insertBefore(newChild, refChild);
//    }

    /**
     * Inserts the node newChild before the existing child node refChild.
     * If refChild is null, insert newChild at the end of the list of children.  
     * 
     * @param newChild The node to insert
     * @param refChild The reference node, i.e., the node before which the new node must be inserted
     * @return
     */
    public ASTNode insertBefore(ASTNode newChild, ASTNode refChild) {
		if (refChild == null) {
            return appendChild(newChild);
        }

    	Log.getLogger().debug("Inserting " + newChild + " before " + refChild + " in " + this);
    	
    	if (refChild.getParentNode() != this) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "Reference " + refChild + " is not a child of " + this);
        }
        
        unlink(newChild);

        if (refChild.getPreviousSibling() != null) {
            refChild.getPreviousSibling().setNextSibling(newChild);
        }
        newChild.setPreviousSibling(refChild.getPreviousSibling());
        newChild.setNextSibling(refChild);
	    newChild.setParentNode(this);
	    childCount++;
        
        refChild.setPreviousSibling(newChild);
        
	    if (getFirstChild() == refChild) {
	        setFirstChildInternal(newChild);
	    }
	    
        return newChild;
    }
    
    public void addStatements(java.util.List statements) {
        for (int i=0; i<statements.size(); i++) {
            appendChild((ASTNode)statements.get(i));
        }
    }
    
    
    public ASTNode getFirstChild() {
        return firstChild;
    }
    
    public ASTNode getLastChild() {
        return lastChild;
    }
    
    /**
     * @param firstChild The firstChild to set.
     */
    private void setFirstChildInternal(ASTNode newFirstChild) {
        firstChild = newFirstChild;
        if (firstChild != null) {
            firstChild.setPreviousSibling(null);
            beginIndex = Math.min(beginIndex, firstChild.getBeginIndex());
        }
    }

    /**
     * @param lastChild The lastChild to set.
     */
    private void setLastChildInternal(ASTNode newLastChild) {
        lastChild = newLastChild;
        if (lastChild != null) {
            lastChild.setNextSibling(null);
            endIndex = Math.max(endIndex, lastChild.getEndIndex());
        }
    }
    
    /**
     * If the specified node is contained in a tree, remove it.
     */
    private void unlink(ASTNode node) {
        if (node.getParentBlock() != null) {
            node.getParentBlock().removeChild(node);
        }
    }
    
    public String getLabel() {
    	if (label == null) throw new RuntimeException("Statement is not labeled");
        return label;
    }
    
    public void setLabel(String theLabel) {
        label = theLabel;
    }
    
    public boolean isLabeled() {
        return label != null;
    }

    public String setLabeled() {
        if (label == null) label = "L" + (++TAG);
        return label;
    }
}
