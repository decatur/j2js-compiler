package com.j2js.cfg.transformation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.j2js.cfg.ControlFlowGraph;
import com.j2js.cfg.Edge;
import com.j2js.cfg.EdgeCollections;
import com.j2js.cfg.Node;
import com.j2js.dom.Block;

/**
 * @author wolfgang
 */
public class Merge extends Transformation {

    private Node tail;
    private Set inEdgesForTail;
    
    public Merge() {
    }
    
    public Merge(ControlFlowGraph theGraph) {
        graph = theGraph;
    }
    
    public boolean applies_() {
        HashSet<Node> headerSet = new HashSet<Node>();
        headerSet.add(header);
        
        for (Node child : header.getDomChildren()) {
            if (EdgeCollections.getSources(child.getInEdges()).equals(headerSet)) {
                tail = child;
                return true;
            }
        }
        
        return false;
    }

    public void apply_() {
        // Remove all in-edges to Tail.
        inEdgesForTail = graph.removeInEdges(tail);
        
        // Reroot all out-edges from Tail.
        graph.rerootOutEdges(tail, newNode, false);
        
        //Remove Tail.
        graph.removeNode(tail);
    }
    
    void rollOut_(Block block) {
        Block labeledBlock = block;
        
        Iterator iter = inEdgesForTail.iterator();
        while (iter.hasNext()) {
            Edge edge = (Edge) iter.next();
            if (!edge.isGlobal()) continue;

            if (labeledBlock == block) {
                labeledBlock = new Block();
                block.appendChild(labeledBlock);
            }
            
            produceJump(edge, labeledBlock);
        }
        
        graph.rollOut(header, labeledBlock);
        graph.rollOut(tail, block);
        block.appendChildren(newNode.block);
    }
    
    public String toString() {
        return super.toString() + "(" + header + ", " + tail + ")";
    }
}
