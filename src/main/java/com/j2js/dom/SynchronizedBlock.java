/*
 * Copyright 2005 by Wolfgang Kuehn
 * Created on 18.10.2005
 */
package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author wolfgang
 */
public class SynchronizedBlock extends Block {
    public Expression monitor;
    
    public void visit(AbstractVisitor visitor) {
        visitor.visit(this);
    }
}
