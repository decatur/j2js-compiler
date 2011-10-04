/*
 * Created on Jan 30, 2005
 */
package com.j2js.visitors;

import java.io.PrintStream;

import com.j2js.dom.ASTNode;
import com.j2js.dom.Block;

/**
 * Copyright by Wolfgang Kuehn 2005
 */
public class SimpleGenerator extends AbstractVisitor {

	private PrintStream out;
	
	public SimpleGenerator(PrintStream theOut) {
		out = theOut;
	}
	
	private String indent = "";
    
    /* (non-Javadoc)
     * @see com.j2js.generators.Generator#generate(com.j2js.dom.ASTNode)
     */
    public void visit(ASTNode node) {
        println(node.toString());
        if (node instanceof Block) {
            String saveIndent = indent;
            indent += "\t";
            ASTNode child = ((Block) node).getFirstChild();
            while (child != null) {
                child.visit(this);
                child = child.getNextSibling();
            }
            indent = saveIndent;
        }
    }
    
    private void println(String s) {
        out.println(indent + s);
    }

}
