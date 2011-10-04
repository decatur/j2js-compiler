package com.j2js.cfg;

import java.util.*;

import com.j2js.Log;

/**
 * Class to build the dominator tree of a given control flow graph.
 * The algorithm is according Purdum-Moore, which isn't as fast as Lengauer-Tarjan, but a lot simpler.
 */
public class DominatorTree {
  
    private ControlFlowGraph graph;
    
    public DominatorTree(ControlFlowGraph theGraph) {
        graph = theGraph;
    }
    
    /**
     * Sets the pre-order index of a node.
     */
    private void visit(Node node, Collection<Node> visited) {
        // Establish preorder index.
        node.setPreOrderIndex(visited.size());
        visited.add(node);
        
        for (Node succ : node.succs()) {
            if (! visited.contains(succ)) {
                visit(succ, visited);
            }
        }

    }
    
    /**
     * Builds the dominator tree and store it in the respective nodes. 
     * It will remove all unreachable nodes on the way!
     */  
    public void build() {

        // Construct list of nodes in pre-order order.
        ArrayList<Node> preOrder = new ArrayList<Node>();
      
        visit(graph.getSource(), preOrder);

        // Remove unreachable nodes.
        for (Node node :  new ArrayList<Node>(graph.getNodes())) {
            if (!preOrder.contains(node)) {
                Log.getLogger().warn("Unreachable code detected and removed");
                //Logger.getLogger().info("Removed " + node);
                graph.removeInEdges(node);
                graph.removeOutEdges(node);
                graph.removeNode(node);
            } else if (node.getPreOrderIndex() == -1) {
                throw new RuntimeException("Pre-order not set for " + node);
            }
        }
      
      
        int size = graph.size();          // The number of vertices in the cfg
    
        Map snkPreds = new HashMap();     // The predacessor vertices from the sink
    

        // Determine the predacessors of the cfg's sink node
        //insertEdgesToSink(graph, snkPreds, reverse);
    
        // Get the index of the root
        int rootIndex = graph.getSource().getPreOrderIndex();
    
        if (rootIndex < 0 || rootIndex >= size) throw new RuntimeException("Root index out of range");

        // Bit matrix indicating the dominators of each node.
        // If bit j of dom[i] is set, then node j dominates node i.
        BitSet[] domMatrix = new BitSet[size];

        // Initially, all the bits in the dominance matrix are set, except
        // for the root node. The root node is initialized to have itself
        // as an immediate dominator.
        for (int i = 0; i < size; i++) {
              BitSet domVector = new BitSet(size);
        
              if (i == rootIndex) {
                  // Only root dominates root.
                  domVector.set(rootIndex);
              }
              else {
                  // Assume that all nodes dominate non-root node i. 
                  domVector.set(0, size);
              }
              
              domMatrix[i] = domVector;
        }

        // Did the dominator bit vector array change?
        boolean changed;
    
        do {
            changed = false;
        
            // Fetch all nodes in pre-order.
            Iterator nodes = preOrder.iterator();
        
            // Compute the dominators of each node in the cfg.  We iterate 
            // over every node in the cfg.  The dominators of a node N are
            // found by taking the intersection of the dominator bit vectors
            // of each predacessor of N and unioning that with N.  This 
            // process is repeated until no changes are made to any dominator
            // bit vector.
      
            while (nodes.hasNext()) {
                Node node = (Node) nodes.next();

                int i = node.getPreOrderIndex();

                if (i < 0 || i >= size) throw new RuntimeException("Unreachable node " + node);

                // We already know the dominators of the root, keep looking
                if (i == rootIndex) {
                    continue;
                }

                BitSet oldSet = domMatrix[i];
                
                // domVector := intersection of dom(pred) for all pred(node).
                BitSet domVector = new BitSet(size);
                domVector.or(oldSet);

                
                Collection preds = node.preds();

                Iterator e = preds.iterator();

                // Find the intersection of the dominators of node's 
                // predacessors.
                while (e.hasNext()) {
                    Node pred = (Node) e.next();

                    int j = pred.getPreOrderIndex();
                    if (j == -1) throw new RuntimeException("Unreachable node " + pred);

                    domVector.and(domMatrix[j]);
                }

                // Don't forget to account for the sink node if node is a
                // leaf node.  Appearantly, there are not edges between 
                // leaf nodes and the sink node!
                preds = (Collection) snkPreds.get(node);

                if (preds != null) {
                    e = preds.iterator();

                    while (e.hasNext()) {
                        Node pred = (Node) e.next();

                        int j = pred.getPreOrderIndex();

                        domVector.and(domMatrix[j]);
                    }
                }

                // Include yourself in your dominators?!
                domVector.set(i);

                // If the set changed, set the changed bit.
                if (!domVector.equals(oldSet)) {
                    changed = true;
                    domMatrix[i] = domVector;
                }
            }
        } while (changed);

        // Once we have the predacessor bit vectors all squared away, we can
        // determine which vertices dominate which vertices.

        // Initialize each node's (post)dominator parent and children
        for (Node node : graph.getNodes()) {
            node.setDomParent(null);
            node.getDomChildren().clear();
        }

        // A node's immediate dominator is its closest dominator.  So, we
        // start with the dominators, dom(b), of a node, b.  To find the 
        // imediate dominator of b, we remove all nodes from dom(b) that
        // dominate any node in dom(b).

        for (Node node : graph.getNodes()) {
            int i = node.getPreOrderIndex();
    
            if (i < 0 || i >= size) throw new RuntimeException("Unreachable node " + node);
    
            if (i == rootIndex) {
                continue;
            }

            // Find the immediate dominator
            // idom := dom(node) - dom(dom(node)) - node
            BitSet domVector = domMatrix[i];
    
            BitSet idom = new BitSet(size);
            idom.or(domVector);
            idom.clear(i);
    
            for (int j = 0; j < size; j++) {
                if (i != j && domVector.get(j)) {
                    // idom = idom - (domMatrix[j] - {j})
                    BitSet b = new BitSet(size);
                    // Complement of domMatrix[j].
                    b.or(domMatrix[j]); b.flip(0, size);
                    b.set(j);
                    idom.and(b);
                }
            }
    
            Node parent = null;
    
            // A node should only have one immediate dominator.
            for (int j = 0; j < size; j++) {
                if (idom.get(j)) {
                    Node p = preOrder.get(j);
                    if (parent != null) 
                        throw new RuntimeException(node + " has more than one immediate dominator: " + parent + " and " + p);
                    parent = p;
                }
            }
    
            if (parent == null) 
                throw new RuntimeException(node + " has 0 immediate dominators");
    
            node.setDomParent(parent);
        }
    }
}
