package com.j2js.cfg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class EdgeCollections {

    public static Set<Node> getSources(Collection<Edge> edges) {
        HashSet<Node> sources = new HashSet<Node>();
        for (Edge edge : edges) {
            sources.add(edge.source);
        }
        return sources;
    }
    
}
