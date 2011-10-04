/*
 * Copyright 2005 by Wolfgang Kuehn
 * Created on 12.11.2005
 */
package com.j2js.cfg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.j2js.ASTNodeStack;
import com.j2js.cfg.transformation.Transformation;
import com.j2js.dom.Block;
import com.j2js.dom.Expression;
import com.j2js.dom.IfStatement;

/**
 * @author wolfgang
 */
public class Node {
    
    public class Reference {
        public Node source;
        public boolean isBackward = false;
    }
    
    public static int NON_HEADER = 0;
    public static int REDUCIBLE = 1;
    public static int IRREDUCIBLE = 2;
    
    String id;
    
    Set<Edge> inEdges = new LinkedHashSet<Edge>();
    Set<Edge> outEdges = new LinkedHashSet<Edge>();
    Graph graph;
    private int currentPc = -1;
    private int initialPc = -1;
    public ASTNodeStack stack = new ASTNodeStack();
    public Block block = new Block();
    public boolean closed = false;
    
    protected int preOrderIndex = -1;
    
    private Node domParent;                         // Block that (immediately) dominates this Block
    private Set<Node> domChildren = new HashSet<Node>();        // Blocks that this Block dominates
    
    IfStatement ifStmt;
    public Expression switchExpression;
    
    public Transformation trans;
    public Collection<Node> jsrCallers = new HashSet<Node>();
    
    public boolean isSwitchHeader = false;
       
    public Node(Graph theGraph, int pc) {
        this(theGraph);
        setInitialPc(pc);
    }
    
    public Node(Graph theGraph) {
        graph = theGraph;
    }
     
    public int getComplexity() {
        Node node = this;
        int complexity = 0;
        while (node.trans != null) {
            complexity++;
            node = node.trans.header;
        }
        return complexity;
    }
    
    /**
     * @return Returns the pc.
     */
    public int getCurrentPc() {
        return currentPc;
    }
    /**
     * @param pc The pc to set.
     */
    public void setCurrentPc(int pc) {
        currentPc = pc;
    }
    
    public void close() {
        closed = true;
    }
    
    public void addEdge(Edge e) {
        if (e.source == this) {
            if (!outEdges.add(e)) {
                throw new RuntimeException("\n" + this + "\nalready bound to " + e);
            }
        }
        
        if (e.target == this) {
            if (!inEdges.add(e)) {
                throw new RuntimeException("" + this + " already bound to " + e);
            }
        }
    }
    
    public String toString() {
        String s = getClass().getName();
        // Same as getClass().getSimpleName() in JDK 5.0.
        s = s.replaceFirst(".*\\.", "");

        s += " " + id + "[" + initialPc + ", " + currentPc + "]";
        //if (preOrderIndex >= 0) s += " preIndex=" + preOrderIndex;
        if (domParent != null) s += " dominated by " + domParent.id;
        if (isLoopHeader()) s += " LH";
        return s;
    }
    
    public String describe() {
        String s = toString();
        Iterator iter = outEdges.iterator();
        while (iter.hasNext()) {
            Edge e = (Edge) iter.next();
            s += "\n\t" + e;
        }
        return s;
    }
    
    
    /**
     * @return Returns the inEdges.
     */
    public Set<Edge> getInEdges() {
        return inEdges;
    }
    
    public Edge getSelfEdge() {
        Iterator iterator = inEdges.iterator();
        while (iterator.hasNext()) {
            Edge edge = (Edge) iterator.next();
            if (edge.source == this) return edge;
        }
        return null;
    }
    
//    public Set getGlobalInEdges() {
//        HashSet set = new HashSet();
//        Iterator iterator = inEdges.iterator();
//        while (iterator.hasNext()) {
//            Edge edge = (Edge) iterator.next();
//            if (edge.global) set.add(edge);
//        }
//        return set;
//    }
    
//    public Set getForewardOutEdges() {
//        HashSet set = new HashSet();
//        Iterator iterator = outEdges.iterator();
//        while (iterator.hasNext()) {
//            Edge edge = (Edge) iterator.next();
//            if (!edge.isBackEdge()) set.add(edge);
//        }
//        return set;
//    }
    
    /**
     * @return Returns the outEdges.
     */
    public Set<Edge> getOutEdges() {
        return outEdges;
    }
    
    public Edge[] getOutEdgesArray() {
        return outEdges.toArray(new Edge[outEdges.size()]);
    }
    
    public Edge[] getInEdgesArray() {
        return inEdges.toArray(new Edge[inEdges.size()]);
    }
    
    public int getPreOrderIndex() {
        return preOrderIndex;
    }

    public void setPreOrderIndex(int thePreOrderIndex) {
        preOrderIndex = thePreOrderIndex;
    }
    
    public Set<Node> succs() {
        Set<Node> list = new LinkedHashSet<Node>();
        Edge[] edges = getOutEdgesArray();
        for (int i=edges.length-1; i>=0; i--) {
            list.add(edges[i].target);
        }
        return list;
    }
    
    public Set<Node> preds() {
        Set<Node> list = new LinkedHashSet<Node>();
        Iterator iter = inEdges.iterator();
        while (iter.hasNext()) {
            Edge e = (Edge) iter.next();
            list.add(e.source);
        }
        return list;
    }
    
    public Node getPred() {
        int count = inEdges.size();
        if (count != 1) throw new RuntimeException("Requested unique predecessor, found " + count);
        return inEdges.iterator().next().source;
    }
    
    public Node getSucc() {
        return getOutEdge().target;
    }
    
    public Edge getLocalOutEdgeOrNull() {
        Edge outEdge = null;
        Iterator iter = outEdges.iterator();
        while (iter.hasNext()) {
            Edge edge = (Edge) iter.next();
            if (outEdge != null) new RuntimeException("Found multiple local out-edges");
            outEdge = edge;
        }
        return outEdge;
    }
    
    public Edge getOutEdge() {
        int count = outEdges.size();
        if (count != 1) throw new RuntimeException("Requested unique successor, found " + count);
        return outEdges.iterator().next();
    }

    /**
     * Return true if this node is equal to or dominates the specified node.
     */
    public boolean isDomAncestor(Node node) {
        do {
            if (node == null) return false;
            if (node == this) return true;
            node = node.getDomParent();
        } while (true);
    }
    
    public Set<Node> getDomChildren() {
        return domChildren;
    }
    
    public Node getDomChild() {
        if (domChildren.size() != 1) throw new RuntimeException("Node must have single child");
        return getDomChildren().iterator().next();
    }

    public Node getDomParent() {
        return domParent;
    }

    public void setDomParent(Node newDomParent) {
        // If this Block already had a dominator specified, remove
        // it from its dominator's children.
        if (domParent != null) {
            domParent.domChildren.remove(this);
        }

        domParent = newDomParent;

        // Add this Block to its new dominator's children.
        if (domParent != null) {
            domParent.domChildren.add(this);
        }
    }

    public boolean isBranch() {
        Edge[] edges = getOutEdgesArray();
        if (edges.length != 2) return false;
        if (edges[0] instanceof ConditionalEdge && edges[1] instanceof ConditionalEdge) return true;
        if ((edges[0] instanceof ConditionalEdge) || (edges[1] instanceof ConditionalEdge))
            throw new RuntimeException("Node must not have mixed edges");
        return false;
    }

    /**
     * Returns either the true or false edge.
     */
    public ConditionalEdge getConditionalEdge(boolean trueFalse) {
        if (!isBranch()) throw new RuntimeException("Node must be a branch");
        Iterator<Edge> iter = outEdges.iterator();
        Edge edge = iter.next();
        if (!trueFalse) edge = iter.next();
        return (ConditionalEdge) edge;
    }

    public String getId() {
        return id;
    }

    public int getInitialPc() {
        return initialPc;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setInitialPc(int theInitialPc) {
        initialPc = theInitialPc;
        currentPc = theInitialPc;
    }

    public boolean isLoopHeader() {
        Iterator iter = inEdges.iterator();
        while (iter.hasNext()) {
            Edge edge = (Edge) iter.next();
            if (edge.isBackEdge()) return true;
        }
        return false;
    }
    
    public boolean hasSelfEdges() {
        Iterator iter = outEdges.iterator();
        while (iter.hasNext()) {
            Edge edge = (Edge) iter.next();
            if (edge.target == this) return true;
        }
        return false;
    }
    
//    public void replaceEdge(Edge oldEdge, Edge newEdge) {
//        source.outEdges.remove(this);
//        newSource.outEdges.add(this);
//        source = newSource;
//    }
}
