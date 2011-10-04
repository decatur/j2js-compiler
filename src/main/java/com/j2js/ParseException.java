package com.j2js;

import com.j2js.dom.ASTNode;

/**
 * @author kuehn
 */
public class ParseException extends RuntimeException {
	
	private ASTNode astNode;

	public ParseException(String msg, ASTNode node) {
		super(msg);
		astNode = node;
	}
	
	public ParseException(Throwable cause, ASTNode node) {
		super(cause);
		astNode = node;
	}
	
	public ASTNode getAstNode() {
		return astNode;
	}
}
