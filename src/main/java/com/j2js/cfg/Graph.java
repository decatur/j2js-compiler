package com.j2js.cfg;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.j2js.Log;
import com.j2js.cfg.Node;

import com.j2js.dom.Block;
import com.j2js.dom.BooleanExpression;
import com.j2js.dom.BreakStatement;
import com.j2js.dom.IfStatement;

public abstract class Graph {

    private Map<String, Node> nodes = new HashMap<String, Node>();
    
    int nodeIdSequence = 0;
    
    Log logger = Log.getLogger();
    
    public Graph() {
    }

    public Node getNodeById(String id) {
        return nodes.get(id);
    }
    
    public Node createNode(Class nodeClass) {
        return createNode(nodeClass, Integer.toString(nodeIdSequence++, 26));
    }
    
    public Node createNode(Class nodeClass, String id) {
        Node node;
        try {
            Constructor constructor = nodeClass.getConstructor(new Class[]{Graph.class});
            node = (Node) constructor.newInstance(new Object[] {this});
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        node.id = id;
        nodes.put(id, node);
        return node;
    }
    
    /**
     * Returns true if node set A is contained in node set B.
     */
    public boolean isContained(Set nodesA, Node[] nodesB) {
        Set<Node> foo = new HashSet<Node>(Arrays.asList(nodesB));
        return foo.containsAll(nodesA);
    }
    
    /**
     * Redirects all inbound edges of a node to another node.
     */
    public void replaceAsTarget(Node oldTarget, Node newTarget) {
        for (Edge edge : new ArrayList<Edge>(oldTarget.getInEdges())) {
            edge.redirect(newTarget);
        }
    }

    public Edge removeEdge(Node source, Node target) {
        Edge edge = getEdge(source, target);
        return removeEdge(edge);
    }
    
    public Edge removeEdge(Edge edge) {
        edge.source.getOutEdges().remove(edge);
        edge.target.getInEdges().remove(edge);
        return edge;
    } 
    
//    public void replaceNodes(Node header, List nodes, Node newNode) {
//        replaceNodes(header, (Node[]) nodes.toArray(new Node[nodes.size()]), newNode);
//    }
    
    public String getEdgeId(Node source, Node target) {
        return source.getId() + "->" + target.getId();
    }
    
    public void addIfElseEdge(Node source, Node ifTarget, Node elseTarget, BooleanExpression be) {
        ConditionalEdge ifEdge = (ConditionalEdge) addEdge(source, ifTarget, ConditionalEdge.class);
        ifEdge.setBooleanExpression(be);
        ConditionalEdge elseEdge = (ConditionalEdge) addEdge(source, elseTarget, ConditionalEdge.class);
        elseEdge.setBooleanExpression(be);
        elseEdge.setNegate(true);
    }
    
    public Edge addEdge(Node source, Node target) {
        return addEdge(source, target, Edge.class);
    }
    
    public Edge addEdge(Node source, Node target, Class clazz) {
        Edge edge = getEdge(source, target);
        if (edge != null) {
            // TODO: Why not allow adding multiple edges? This is possible
            // anyway through reroot or redirect!
            throw new RuntimeException("Edge already exists");
        }
        if (clazz.equals(Edge.class)) {
            edge = new Edge(this, source, target);
        } else if (clazz.equals(SwitchEdge.class)) {
            edge = new SwitchEdge(this, source, target);
        } else if (clazz.equals(ConditionalEdge.class)) {
            edge = new ConditionalEdge(this, source, target);
        } else {
            throw new RuntimeException("Illegal edge class " + clazz);
        }
        source.addEdge(edge);
        if (source != target) {
            target.addEdge(edge);
        }
        return edge;
    }
    
    public Edge getEdge(Node source, Node target) {
        for (Edge edge : source.getOutEdges()) {
            if (edge.target == target) return edge;
        }
        return null;
    }
    	
    public void removeNode(Node node) {
        replaceNode(node, null);
    }
    
    public Set<Edge> removeOutEdges(Node node) {
        Set<Edge> outEdges = new HashSet<Edge>(node.outEdges);
        Iterator iter = outEdges.iterator();
        while (iter.hasNext()) {
            Edge edge = (Edge) iter.next();
            removeEdge(edge);
        }
        
        return outEdges;
    }
    
    /**
     * Removes all in-edges to the specified node.
     *
     * @return the set of removed in-edges
     */
    public Set removeInEdges(Node node) {
        Set<Edge> inEdges = new HashSet<Edge>(node.inEdges);
        Iterator<Edge> iter = inEdges.iterator();
        while (iter.hasNext()) {
            Edge edge = iter.next();
            removeEdge(edge);
        }
        
        return inEdges;
    }
    
    public Set removeSelfEdges(Node node) {
        Set<Edge> selfEdges = new HashSet<Edge>();
        
        for (Edge edge: new HashSet<Edge>(node.outEdges)) {
            if (edge.target != node) continue;
            removeEdge(edge);
            selfEdges.add(edge);
        }
        
        return selfEdges;
    }
    
    public void rerootGlobalOutEdges(Node node, Node newSource) {
        Edge[] edges  = node.getOutEdgesArray();
        for (int i=0; i<edges.length; i++) {
            Edge edge = edges[i];
            if (edge.isGlobal()) {
                edge.reroot(newSource);
            }
        }
    }
    
    public void rerootOutEdges(Node node, Node newSource, boolean localToGlobal) {
        Edge[] edges  = node.getOutEdgesArray();
        for (int i=0; i<edges.length; i++) {
            Edge edge = edges[i];
            edge.reroot(newSource);
            if (localToGlobal && !edge.isGlobal()) {
                edge.setOrgSource(node);
            }
        }
    }
    
    public void replaceNode(Node oldNode, Node newNode) {
        nodes.remove(oldNode.getId());
        
        if (newNode != null) {
            // Redirect all in-edges for oldNode to newNode.
            replaceAsTarget(oldNode, newNode);
            
            // Reroot all out-edges from oldNode at newNode, making local edges global.
            rerootOutEdges(oldNode, newNode, true);
            
            newNode.setDomParent(oldNode.getDomParent());
            for (Node child : new ArrayList<Node>(oldNode.getDomChildren())) {
                child.setDomParent(newNode);
            }
        }
        
        if (oldNode.inEdges.size() > 0 || oldNode.outEdges.size() > 0)
            throw new RuntimeException("Cannot replace node with edges");

        oldNode.setDomParent(null);
    }
	
    public Collection<Node> getNodes() {
        return nodes.values();
    }
    
	public int size() {
		return getNodes().size();
	}

    public Block reduceDumb() {
        Block block = new Block();
        
        for (Node node : getNodes()) {
            block.appendChild(node.block);
            
            if (node.isBranch()) {
                IfStatement ifStmt = new IfStatement();
                ConditionalEdge cEdge = node.getConditionalEdge(true);
                ifStmt.setExpression(cEdge.getBooleanExpression().getExpression());
                ifStmt.setIfBlock(new Block());
                Block targetBlock = cEdge.target.block;
                ifStmt.getIfBlock().appendChild(new BreakStatement(targetBlock));
                ifStmt.setElseBlock(new Block());
                targetBlock = node.getConditionalEdge(false).target.block;
                ifStmt.getElseBlock().appendChild(new BreakStatement(targetBlock));
                block.appendChild(ifStmt);
            } else {
                for (Edge e : node.getOutEdges()) {
                    BreakStatement bStmt = new BreakStatement(e.target.block);
                    node.block.appendChild(bStmt);
                }
            }
        }
        return block;
    }

    boolean isTarget(Node n, Set edgeSet) {
        Iterator iter = edgeSet.iterator();
        while (iter.hasNext()) {
            Edge e = (Edge) iter.next();
            if (n == e.target) return true;
        }
        return false;
    }
    
    public void rollOut(Node node, Block targetBlock) {
        if (node.trans != null) {
            node.trans.rollOut(targetBlock);
        } else {
            targetBlock.appendChildren(node.block);
        }
    }
    
    public void dump(String msg) {
        StringBuffer sb = new StringBuffer();
        sb.append(msg + " ...\n");
        for (Node node : getNodes()) {
            sb.append(node.describe() + "\n");
            //dump(node.block, "Block");
        }
        sb.append("... " + msg);
        logger.debug(sb.toString());
    }
}
