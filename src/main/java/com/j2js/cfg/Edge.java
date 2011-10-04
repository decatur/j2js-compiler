/*
 * Copyright 2005 by Wolfgang Kuehn
 * Created on 12.11.2005
 */
package com.j2js.cfg;

/**
 * @author wolfgang
 */
public class Edge {
    
    private Graph graph;
    public Node source;
    public Node target;
    public EdgeType type;
    private Node orgSource;
    
    Edge(Graph theGraph, Node theSource, Node theTarget) {
        graph = theGraph;
        source = theSource;
        target = theTarget;
    }
    
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Edge)) return false;
        Edge otherEdge = (Edge) other;
        return source.getId().equals(otherEdge.source.getId()) && target.getId().equals(otherEdge.target.getId());
    }
    
    /**
     * Replace the target of this edge.
     */
    public void reroot(Node newSource) {
        source.outEdges.remove(this);
        newSource.outEdges.add(this);
        source = newSource;
    }
    
    /**
     * Replace the target of this edge.
     */
    public void redirect(Node newTarget) {
        target.inEdges.remove(this);
        newTarget.inEdges.add(this);
        target = newTarget;
    }

    /**
     * Returns true if this edge is a backward (i.e. loop) edge.
     */
    public boolean isBackEdge() {
        return target.isDomAncestor(source);
    }
    
    public boolean isGlobal() {
        return orgSource != null;
    }
    
    public String toString() {
        String s = getClass().getName();
        // Extract unqualified class name.
        s = s.substring(s.lastIndexOf('.')+1);
        return s + " " + source.getId() + " -> " + target.getId();
    }

    public Node getOrgSource() {
        if (orgSource == null) {
            return source;
        }
        return orgSource;
    }

    public void setOrgSource(Node theOrgSource) {
        orgSource = theOrgSource;
    }

   
}
