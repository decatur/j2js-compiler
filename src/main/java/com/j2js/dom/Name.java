package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * Copyright by Wolfgang Kuehn 2005
 * Created on Feb 20, 2005
 */
public class Name extends Expression {
    
    private String identifier;

    public Name(String newIdentifier) {
        //super();
        identifier = newIdentifier;
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof Name)) return false;
        return identifier.equals(((Name) obj).identifier);
    }
    
    /**
     * @return Returns the identifier.
     */
    public String getIdentifier() {
        return identifier;
    }
    /**
     * @param theIdentifier The identifier to set.
     */
    public void setIdentifier(String theIdentifier) {
        identifier = theIdentifier;
    }
}
