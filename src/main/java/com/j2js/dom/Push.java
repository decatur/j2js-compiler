/*
 * Copyright 2005 by Wolfgang Kuehn
 * Created on 19.10.2005
 */
package com.j2js.dom;

/**
 * @author wolfgang
 */
public class Push extends Expression {
    
    public Push(Expression e) {
        widen(e);
    }

}
