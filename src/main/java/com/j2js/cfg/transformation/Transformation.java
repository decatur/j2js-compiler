package com.j2js.cfg.transformation;

import com.j2js.Log;
import com.j2js.Optimizer;
import com.j2js.cfg.ConditionalEdge;
import com.j2js.cfg.ControlFlowGraph;
import com.j2js.cfg.Edge;
import com.j2js.cfg.Graph;
import com.j2js.cfg.Node;
import com.j2js.dom.Block;
import com.j2js.dom.BooleanExpression;
import com.j2js.dom.BreakStatement;
import com.j2js.dom.ContinueStatement;
import com.j2js.dom.Expression;
import com.j2js.dom.IfStatement;

public abstract class Transformation {
    
    static Class[] transformations = new Class[] {
        Switch.class,
        Try.class,
        //If.class,
        //IfElse.class,
        Loop.class,
        // Merge must be applied after Loop.
        Merge.class
    };

    public static Transformation select(Graph graph, Node node) {
        
        for (int i=0; i<transformations.length; i++) {
            Transformation t = fetch(i);
            if (t.applies(node)) {
                return t;
            }
        }
    
        return null;
    }

    static Transformation fetch(int index) {
        Transformation t = null;
        try {
            t = (Transformation) transformations[index].newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    
        return t;
    }
    
    ControlFlowGraph graph;
    public Node header;
    Node newNode;
    
    public Transformation() {
    }
    
    public Node apply() {
        newNode = graph.createNode(Node.class);
        newNode.setInitialPc(header.getInitialPc());
        newNode.trans = this;
        apply_();
        
        // Remove Header.
        graph.replaceNode(header, newNode);
        
        Log.getLogger().debug(toString() + " -> " + newNode);
        return newNode;
    }
    
    public boolean applies(Node node) {
        graph = (ControlFlowGraph) node.getGraph();
        header = node;
        return applies_();
    }
    
    abstract boolean applies_();
    abstract void apply_();
    abstract void rollOut_(Block block);
    
    public void rollOut(Block block) {
        rollOut_(block);
        block.appendChildren(newNode.block);
    }
    
    void produceJump(Edge edge, Block labeledBlock) {
        Node referer = edge.getOrgSource();
        Block breakBlock;
        if (edge instanceof ConditionalEdge) {
            ConditionalEdge condEdge = (ConditionalEdge) edge;
            BooleanExpression condExpr = condEdge.getBooleanExpression();
            Expression expr = Optimizer.simplifyBooleanExpression(condExpr.getExpression(), condEdge.isNegate());
            IfStatement ifStmt = new IfStatement();
            ifStmt.setExpression(expr);
            referer.block.appendChild(ifStmt);
            Block ifBlock = new Block();
            ifStmt.setIfBlock(ifBlock);
            breakBlock = ifBlock;
        } else {
            breakBlock = referer.block;
        }
        
        if (edge.isBackEdge()) {
            breakBlock.appendChild(new ContinueStatement(labeledBlock));
        } else {
            breakBlock.appendChild(new BreakStatement(labeledBlock));
        }
    }
    
    public String toString() {
        return getClass().getName();
    }
}
