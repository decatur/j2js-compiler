package com.j2js.cfg.transformation;

import java.util.Iterator;
import java.util.Set;

import com.j2js.cfg.Edge;
import com.j2js.dom.Block;
import com.j2js.dom.BooleanLiteral;
import com.j2js.dom.WhileStatement;


public class Loop extends Transformation {
    
    private Set selfEdges;
    
    public boolean applies_() {
        return header.hasSelfEdges();
    }
    
    public void apply_() {
        // Remove self edges.
        selfEdges = graph.removeSelfEdges(header);
    }
    
    void rollOut_(Block block) {
        WhileStatement loopStmt = new WhileStatement();
        Block loopBody = new Block();
        loopStmt.setBlock(loopBody);
        loopStmt.setExpression(new BooleanLiteral(true));

        block.appendChild(loopStmt);
        
        Iterator iter = selfEdges.iterator();
        while (iter.hasNext()) {
            Edge edge = (Edge) iter.next();
            if (!edge.isGlobal()) continue;
            loopStmt.isLabeled();
            produceJump(edge, loopStmt);
        }
        
        graph.rollOut(header, loopBody);
    }
    
    public String toString() {
        return super.toString() + "(" + header + ")";
    }
}
