package com.j2js.dom;

/**
 * @author wolfgang
 */
public class Branch extends Expression {
    
    private int targetIndex = -1;
    private ASTNode target;
    
    public Branch() {
        super();
    }
    
    public Branch(int theTargetIndex) {
        setTargetIndex(theTargetIndex);
    }

    public boolean isBackward() {
        return getTargetIndex() <= getBeginIndex();
    }
    
    /**
     * @return Returns the targetIndex.
     */
    public int getTargetIndex() {
        return targetIndex;
    }

    /**
     * @param theTargetIndex The targetIndex to set.
     */
    public void setTargetIndex(int theTargetIndex) {
        targetIndex = theTargetIndex;
    }
    
    public String toString() {
        String s = getClass().getName()
				+ "[" + getBeginIndex() + ", " + getEndIndex() + ", " + targetIndex + "] -> ";
        if (target != null) {
            Exception e = new Exception();
            if (target == this) {
                s += "self";
            } else if (e.getStackTrace().length>20) {
                s += "...";
            } else {
                s += "" + target.toString();
            }
        } else {
            s += "null";
        }
        return s;
    }
    /**
     * @return Returns the target.
     */
    public ASTNode getTarget() {
        return target;
    }
    /**
     * @param theTarget The target to set.
     */
    public void setTarget(ASTNode theTarget) {
        target = theTarget;
        if (theTarget != null) targetIndex = theTarget.getBeginIndex();
    }
}
