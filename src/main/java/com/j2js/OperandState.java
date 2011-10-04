/*
 * Created on 20.10.2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.j2js;

import com.j2js.dom.ASTNode;

/**
 * @author kuehn
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class OperandState {
	
	int code;
	int beginIndex;
	int endIndex;
	
	ASTNode stmt;
	
	OperandState(int theCode) {
		code = theCode;
	}
	
	OperandState(int theCode, int theBeginIndex, ASTNode theStmt) {
		code = theCode;
		beginIndex = theBeginIndex;
		stmt = theStmt;
	}
	
	public String toString() {
		return "State: " + code + " " + stmt;
	}
}
