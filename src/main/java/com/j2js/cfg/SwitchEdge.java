package com.j2js.cfg;

import java.util.ArrayList;
import java.util.List;

import com.j2js.dom.NumberLiteral;

public class SwitchEdge extends Edge {
    public List<NumberLiteral> expressions = new ArrayList<NumberLiteral>();
    
    SwitchEdge(Graph graph, Node theSource, Node theTarget) {
        super(graph, theSource, theTarget);
    }
}
