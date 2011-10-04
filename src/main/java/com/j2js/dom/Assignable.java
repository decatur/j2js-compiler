/*
 * Copyright 2005 by Wolfgang Kuehn
 * Created on 16.10.2005
 */
package com.j2js.dom;

/**
 * Tagging interface for all node types which are legal assignment targets. 
 */
public interface Assignable {
    
    /**
     * Returns true if the specified Assignable corresponds to this Assignable.
     */
    public boolean isSame(Object obj);
    
}
