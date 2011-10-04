package com.j2js;

import org.apache.bcel.Constants;
//import org.apache.bcel.classfile.*;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Utility;

import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.ByteSequence;

import com.j2js.cfg.ControlFlowGraph;
import com.j2js.cfg.Node;
import com.j2js.cfg.SwitchEdge;
import com.j2js.cfg.TryHeaderNode;
import com.j2js.dom.*;
import com.j2js.visitors.SimpleGenerator;
import com.j2js.assembly.Project;
import com.j2js.assembly.Signature;

import org.apache.bcel.generic.InstructionList;

import com.j2js.J2JSCompiler;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class Pass1 {
    
    private ConstantPool constantPool;

    private ByteSequence bytes;
    private static ASTNode currentNode;
    
    private ASTNodeStack stack;
    
    private Code code;
    private MethodDeclaration methodDecl;
    // The currently parsed method.
    private Method method;
    
    private List<TryStatement> tryStatements = new ArrayList<TryStatement>();
    private ControlFlowGraph graph = new ControlFlowGraph(tryStatements);
    
    // Not used anymore.
    private int depth;
    
    private static Log logger = Log.getLogger();
    
    
    /*
     * The `WIDE' instruction is used in the byte code to allow 16-bit wide
     * indices for local variables. This opcode may precede one of
     * iload, fload, aload, lload, dload, istore, fstore, astore, lstore, dstore, ret or iinc. 
     * The opcode immediately following takes an extra byte which is combined with
     * the following byte to form a 16-bit value.
     * In case of iinc, the default two 8 bit operants are widened to two 16 bit aperants accordingly.
     */
    private boolean wide = false;

    public static ASTNode getCurrentNode() {
        return currentNode;
    }

    public Pass1(JavaClass jc) {
        constantPool = jc.getConstantPool();
    }

    private CatchClause createCatchClause(TryStatement tryStmt, ExceptionHandler handle) {
        CatchClause cStmt = new CatchClause(handle.getHandlerPC());
        VariableDeclaration decl = new VariableDeclaration(VariableDeclaration.LOCAL_PARAMETER);
        decl.setName("_EX_");
        decl.setType(handle.getCatchType(constantPool));
        cStmt.setException(decl);
        tryStmt.addCatchStatement(cStmt);
        return cStmt;
    }

    private void makeTryFrames() {
        for (int i=0; i<tryStatements.size(); i++) {
            TryStatement tryStmt = tryStatements.get(i);
            makeTryFrame(tryStmt);
        }
    }

    private void makeTryFrame(TryStatement stmt) {
        TryHeaderNode header = stmt.header;
        
        Node tryNode = graph.getOrCreateNode(stmt.getBeginIndex());
        tryNode.stack = new ASTNodeStack();
        header.setTryBody(tryNode);
        
        CatchClause clause = (CatchClause) stmt.getCatchStatements().getFirstChild();
        while (clause != null) {
            // Push implicit exception.
            Node catchNode = graph.createNode(clause.getBeginIndex());
            //catchNode.type = NodeType.CATCH;
            catchNode.stack = new ASTNodeStack(new VariableBinding(clause.getException()));
            header.addCatchNode(catchNode);
            
            clause = (CatchClause) clause.getNextSibling();
        }
    }
    
    private void compileCodeException() {
        ExceptionHandlers handlers = new ExceptionHandlers(code);
        
        Iterator<ExceptionHandler> handleIterator = handlers.iterator();
        
        ExceptionHandler handle = handleIterator.hasNext()?handleIterator.next():null;
        while (handle != null) {
            boolean hasFinally = false;
            int start = handle.getStartPC();
            int end = handle.getEndPC();
            
            TryStatement tryStmt = new TryStatement();
            tryStmt.header = (TryHeaderNode) graph.createNode(TryHeaderNode.class);
            tryStmt.header.tryStmt = tryStmt;

            Block tryBlock = new Block(start, end);
            tryStmt.setTryBlock(tryBlock);

            //tryStmt.setBeginIndex(start);

            tryStatements.add(tryStmt);

            CatchClause cStmt = null;
            
            // Collect all non-default handlers. The range of each handler is from the 'store'-instruction to the beginning of the next handler.
            while (handle != null && !handle.isDefault() && handle.getStartPC()==start && handle.getEndPC()==end) {
                if (cStmt!=null) {
                    cStmt.setEndIndex(handle.getHandlerPC()-1);
                }
                cStmt = createCatchClause(tryStmt, handle);
                handle = handleIterator.hasNext()?handleIterator.next():null;
            } 
            
            int foo = -1;
            if (handle != null && handle.isDefault() && handle.getStartPC()==start) {
                // Collect first default handler.
                hasFinally = true;
                if (cStmt!=null) {
                    cStmt.setEndIndex(handle.getHandlerPC()-1);
                    tryStmt.setEndIndex(handle.getHandlerPC()-1);
                    // Warning: We only set a lower bound for the end index. The correct index is set later
                    // when the finally statement is analysed.
                }
                cStmt = createCatchClause(tryStmt, handle);
                foo = handle.getHandlerPC();
                handle = handleIterator.hasNext()?handleIterator.next():null;
            }

            // Last catch stmt has no endIndex, yet!
            
            while (handle != null && handle.isDefault() && (handle.getHandlerPC()==foo)) {
                // Skip all remaining default handlers.
                throw new RuntimeException("remaining default handlers");
                //handle = handleIterator.hasNext()?handleIterator.next():null;
            }

            Block catches = tryStmt.getCatchStatements();
            if (catches.getChildCount() == 0) {
                throw new ParseException("A try clause must have at least one (possibly default) catch clause", tryStmt);
            }
            cStmt = (CatchClause) catches.getChildAt(0);
            tryBlock.setEndIndex(cStmt.getBeginIndex()-1);
            cStmt = (CatchClause) catches.getLastChild();
            if (cStmt.getEndIndex() == Integer.MIN_VALUE) {
                cStmt.setEndIndex(cStmt.getBeginIndex()+1);
            }
            tryStmt.setEndIndex(cStmt.getEndIndex());
            
            if (hasFinally) {
                // Can't say yet where finally block is located.
            }
            
            if (logger.isDebugEnabled()) {
                dump(tryStmt, "Try");
            }
        }
    }
    
    /**
     * Dumps instructions and exeption table.
     */
    private void dumpCode() {
        InstructionList il = new InstructionList(code.getCode());
        InstructionHandle[] handles = il.getInstructionHandles();
        
        for (InstructionHandle handle : handles) {
            System.out.println(handle.toString(true));
            InstructionTargeter[] targeters = handle.getTargeters();
            if (targeters != null) {
                for (InstructionTargeter targeter : handle.getTargeters()) {
                    System.out.println("    Targeter: " + targeter.toString() + " " + targeter.getClass());
                }
            }
        }
        
        for (CodeException ce : code.getExceptionTable()) {
            String exceptionType;
            if (ce.getCatchType() > 0) {
                Constant constant = constantPool.getConstant(ce.getCatchType());
                exceptionType = Pass1.constantToString(constant, constantPool);
            } else {
                exceptionType = "Default";
            }
            System.out.println(ce.toString() + " " + exceptionType);
        }
    }
    
    public void parse(Method theMethod, MethodDeclaration theMethodDecl) throws IOException {
        method = theMethod;
        methodDecl = theMethodDecl;
        
        code = method.getCode();
        
        if (logger.isDebugEnabled()) {
            dumpCode();
        }
        
        Block.TAG = 0;
        
        compileCodeException();
        
        bytes = new ByteSequence(code.getCode());

        graph.createNode(0);
        
        makeTryFrames();
        
        parseStatement();
        
        try {
            Optimizer optimizer = new Optimizer(methodDecl, tempDecls);
            optimizer.optimize();
        } catch (Error e) {
            J2JSCompiler.errorCount++;
            if (logger.isDebugEnabled()) {
                logger.debug("In Expression Optimizer:\n" + e + "\n" + Utils.stackTraceToString(e));
            } else {
                logger.error("In Expression Optimizer:\n " + e);
            }
        }

        Block block;
        if (J2JSCompiler.compiler.reductionLevel == 0) {
            block = graph.reduceDumb();
        } else {
            block = graph.reduce();
        }
        
        methodDecl.setBody(block);
 
    }

    private boolean isProcedure(ASTNode stmt) {
        if (stmt instanceof MethodInvocation) {
            MethodInvocation mi = (MethodInvocation) stmt;
            return mi.getTypeBinding().equals(Type.VOID);
        }
        return false;
    }

    Node cNode;
    Node lastCurrentNode;
    
    private void setCurrentNode(Node theNode) {
        if (cNode == theNode) return;
        cNode = theNode;
        if (cNode != null && cNode != lastCurrentNode) {
            logger.debug("Switched to " + cNode);
            lastCurrentNode = cNode;
        }
    }
    
    private void joinNodes(Node node) {        
        Collection<Node> nodes = node.preds();
        Iterator iter = nodes.iterator();
        while (iter.hasNext()) {
            Node n = (Node) iter.next();
            if (n.stack.size() == 0) iter.remove();
        }
        if (nodes.size() > 0) {
            mergeStacks(nodes, node.stack);
        }
    }
    
    /**
     * Selects a single node as currently active node.
     */
    private void selectActiveNode(int pc) {
        // Find all nodes currently active at pc.
        List<Node> activeNodes = new ArrayList<Node>();
        for (Node node : graph.getNodes()) {
            if (node.getCurrentPc() == pc) {
                activeNodes.add(node);
            }
        }
        
        if (activeNodes.size() == 0) {
            // No active node: Create one.
            Node node = graph.createNode(pc);
            setCurrentNode(node);
            return;
        }
        
        if (activeNodes.size() == 1) {
            // Return single active node.
            Node node = activeNodes.get(0);
            setCurrentNode(node);            
            return;
        }
        
        // Multiple nodes. Select the one starting at pc. 
        
        Node node = graph.getNode(pc);
        if (node ==  null) throw new RuntimeException("No node found at " + pc);
        
        setCurrentNode(node);
        
        // Add edges from all other active nodes to the selected node.
        activeNodes.remove(node);
        for (Node n : activeNodes) {
            graph.addEdge(n, node);
        }
    }
    
    /**
     * If the stack of the specified node is [s0, s1, ..., sn], then [t1=s1; ... tn=sn} is
     * appended to its block, and the stack is replaced by [t1, t2, ..., tn].
     */
    private void expressionsToVariables(Node node, boolean clone) {
        if (node.stack.size() == 0) return;
        
        logger.debug("expressionsToVariables ...");
        
        for (int i=0; i<node.stack.size(); i++) {
            Expression expr = (Expression) node.stack.get(i);
            
            
            if (expr instanceof VariableBinding && 
                    (((VariableBinding) expr).isTemporary())) {
                // Skip temporary variables.
                continue;
            }

            VariableBinding vb = methodDecl.createAnonymousVariableBinding(expr.getTypeBinding(), true);
            logger.debug("\t" + expr + ' ' + vb.getName());
            Assignment a = new Assignment(Assignment.Operator.ASSIGN);
            a.setLeftHandSide(vb);
            a.setRightHandSide(expr);
            //a.setRange(pc, pc);
            node.block.appendChild(a);
            node.stack.set(i, clone?(VariableBinding)vb.clone():vb);
        }
        
        logger.debug("... expressionsToVariables");
    }
    
    /**
     * Returns the top element of all specified stacks if identical, otherwise the type binding (which
     * must be identical for all elements).
     */
    private Object stacksIdentical(Collection sources, int index) {
        Expression expr = null;
        Iterator iter = sources.iterator();
        while (iter.hasNext()) {
            Node node = (Node) iter.next();
            Expression e = (Expression) node.stack.get(index);
            if (expr == null) {
                expr = e;
            } else if (e != expr) {
                return expr.getTypeBinding();
            }
        }
        return expr;
    }
    
    /**
     * Merges all source stacks into one target stack. If a layer over all stacks contains the identical
     * element, then this element is propagated.
     * For example, if there are two stacks [a, b, c] and [d, b, e], we append to the source nodes 
     * {t1=a; t2=c} and {t1=d; t2=e}, and populate the specified target stack with [t1, b, t2]. 
     */
    private void mergeStacks(Collection sources, ASTNodeStack target) {
        logger.debug("Merging ...");
        
        Iterator iter = sources.iterator();
        while (iter.hasNext()) {
            Node pred = (Node) iter.next();
            dump(pred.stack, "Stack for " + pred);
        }
        
        
        int stackSize = -1;
        //String msg = "";
        // Find the common size of all source stacks.
        iter = sources.iterator();
        while (iter.hasNext()) {
            Node pred = (Node) iter.next();
            //msg += pred + ", ";
            if (stackSize == -1) {
                stackSize = pred.stack.size(); 
            } else if (stackSize != pred.stack.size()){
                dump(sources);
                throw new RuntimeException("Stack size mismatch");
            }
        }

        for (int index=0; index < stackSize; index++) {
            Object obj = stacksIdentical(sources, index);
            if (obj instanceof Expression) {
                target.add((Expression) ((Expression) obj).clone());
                logger.debug("\tIdentical: " + obj);
            } else {
                // Generate variable binding tempX.
                VariableBinding vb = methodDecl.createAnonymousVariableBinding((Type) obj, true);
                // Append binding to target stack.
                target.add(vb);
                
                iter = sources.iterator();
                while (iter.hasNext()) {
                    Node node = (Node) iter.next();
                    Expression expr = (Expression) node.stack.get(index);
                    // Generate assignment tempX = expr.
                    Assignment a = new Assignment(Assignment.Operator.ASSIGN);
                    a.setLeftHandSide((VariableBinding) vb.clone());
                    if (expr instanceof VariableBinding) expr = (VariableBinding) expr.clone();
                    a.setRightHandSide(expr);
                    // Append assignment to source node.
                    node.block.appendChild(a);
                }
                logger.debug("\t" + vb.getName());
            }
        }
        logger.debug("... Merging stacks");
    }

    public void parseStatement() throws IOException {
        depth = 0;
        
        // TODO: Check that all nodes get closed and that each closed node has empty stack!
        
        while (bytes.available() > 0) {
            
            int pc = bytes.getIndex();
            //Logger.getLogger().finer("@" + pc);
            
            if (cNode != null) {
                cNode.setCurrentPc(pc);
            }

            selectActiveNode(pc);
            
            if (cNode.getInitialPc() == pc) {
                joinNodes(cNode);
            }
            
            stack = cNode.stack;
            
            ASTNode stmt = parseInstruction();
            //InstructionHandle handle = il.findHandle(stmt.getBeginIndex());
            //System.out.println(">>>" + handle);
            
            if (stmt instanceof NoOperation) continue;
            
            depth += stmt.getStackDelta();
            
            if (stmt instanceof VariableBinding) {
                depth = depth;
            }

            logger.debug(" -> " + stmt + " @ " 
                    + methodDecl.getLineNumberCursor().getLineNumber(stmt)
                    + ", depth:" + depth + ", delta:" + stmt.getStackDelta());
    
            if (stmt instanceof JumpSubRoutine) {
                JumpSubRoutine jsr = (JumpSubRoutine) stmt;
                cNode.block.setEndIndex(jsr.getEndIndex());
                
                Node finallyNode = graph.getNode(jsr.getTargetIndex());
                
                if (finallyNode == null) {
                    // Found finally clause.
                    finallyNode = graph.createNode(jsr.getTargetIndex());
                    // Generate dummy expression for the astore instruction of the finally block.
                    finallyNode.stack = new ASTNodeStack(new Expression());

                    //finallyNode.jsrCallers.add(cNode);
                }
                finallyNode.jsrCallers.add(cNode);
                if (cNode.preds().size() == 1 && finallyNode.preds().size() == 0 && cNode.getPred() instanceof TryHeaderNode) {
                    // Current node must be the default handler.
                    // TODO: This only works if default handler is a single node!!
                    TryHeaderNode tryHeaderNode = (TryHeaderNode) cNode.getPred();
                    // Attach finally to its try header node.
                    tryHeaderNode.setFinallyNode(finallyNode);
                }
                
            } else if (stmt instanceof ConditionalBranch) {
                ConditionalBranch cond = (ConditionalBranch) stmt;
                
                if (bytes.getIndex() == cond.getTargetIndex()) {
                    // This is a conditional branch with an empty body, i.e. not a branch at all. We ignore it.
                } else {
                    Node elseNode = graph.getOrCreateNode(bytes.getIndex());
                    
                    Node ifNode;
                    if (cond.getTargetIndex() <= pc) {
                        Node[] nodes = graph.getOrSplitNodeAt(cNode, cond.getTargetIndex());
                        cNode = nodes[0];
                        ifNode = nodes[1];
                    } else {
                        ifNode = graph.getOrCreateNode(cond.getTargetIndex());
                    }
                    
                    BooleanExpression be = new BooleanExpression(cond.getExpression());
                    
                    graph.addIfElseEdge(cNode, ifNode, elseNode, be);
                    expressionsToVariables(cNode, false);
                    cNode = null;
                }
            } else if (stmt instanceof Jump) {
                int targetPc = ((Jump) stmt).getTargetIndex();
                Node targetNode;

                if (targetPc <= pc) {
                    // Backward jump.
                    Node[] nodes = graph.getOrSplitNodeAt(cNode, targetPc);
                    cNode = nodes[0];
                    targetNode = nodes[1];
                } else {
                    targetNode = graph.getOrCreateNode(targetPc);
                }
                graph.addEdge(cNode, targetNode);
                cNode = null;
            } else if (stmt instanceof SynchronizedBlock || isProcedure(stmt)) {
                cNode.block.appendChild(stmt);
            } else if (stmt instanceof Assignment) {
              expressionsToVariables(cNode, true);
              cNode.block.appendChild(stmt);
            } else if (stmt instanceof ThrowStatement || stmt instanceof ReturnStatement) {
                cNode.block.appendChild(stmt);
                cNode.close();
                cNode = null;
            } else {
                stack.push(stmt);
            }
        }
        
    }
    

    void dump(Collection nodes) {
        if (!logger.isDebugEnabled()) return;
        
        Iterator iter = nodes.iterator();
        while (iter.hasNext()) {
            Node node = (Node) iter.next();
            dump(node.stack, node.toString());
        }
    }
    
    public static void dump(ASTNode node, String msg) {
        if (!logger.isDebugEnabled()) return;
        
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(ba);
        out.println(msg);
        SimpleGenerator generator = new SimpleGenerator(out);
        generator.visit(node);
        out.close();
        try { 
            ba.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.debug(ba.toString());
    }
    
    static void dump(List list, String msg) {
        if (!logger.isDebugEnabled()) return;
        
        StringBuffer sb = new StringBuffer();
        sb.append("Begin dumping " + msg + "...\n");
        for (int i=0; i<list.size(); i++) {
            ASTNode node = (ASTNode) list.get(i);
            sb.append("    " + i + ": " + node + "\n");
        }
        sb.append("... end of dump");
        
        logger.debug(sb.toString());
    }
    
    private VariableBinding createVariableBinding(int slot, Type type, boolean isWrite) {
        return methodDecl.createVariableBinding(VariableDeclaration.getLocalVariableName(method, slot, bytes.getIndex()), type, isWrite);
    }
    
    private InfixExpression createInfixRightLeft(InfixExpression.Operator op, Expression right, Expression left, Type type) {
        InfixExpression binOp = new InfixExpression(op);
        binOp.setTypeBinding(type);
        binOp.setOperands(left, right);
        return binOp;
    }
    
    private PrefixExpression createPrefix(PrefixExpression.Operator op, ASTNode operand, Type type) {
        PrefixExpression pe = new PrefixExpression();
        pe.setOperator(op);
        pe.setTypeBinding(type);
        pe.setOperand(operand);
        return pe;
    }
    
    private Form selectForm(InstructionType instructionType) {
        if (instructionType.getFormCount()==1) {
            return instructionType.getForm(0);
        }
        FormLoop: 
        for (int i=0; i<instructionType.getFormCount(); i++) {
            Form form = instructionType.getForm(i);
            for (int j=0; j<form.getIns().length; j++) {
                Form.Value in = form.getIns()[form.getIns().length - 1 - j];
                if (stack.peek(j).getCategory() != in.getCategory()) continue FormLoop;
            }
            return form;
        }
        throw new RuntimeException("Could not determine correct form for " + instructionType);
    }
    
    private List<VariableDeclaration> tempDecls = new ArrayList<VariableDeclaration>();
    
    private Expression[] duplicate(Expression e) {
        if (e instanceof NumberLiteral || e instanceof ThisExpression || e instanceof StringLiteral) {
            // Refered values will never change nor will they have side effects,
            // so we do not need to create an intermediate variable.
            return new Expression[] {e, (Expression) e.clone()};
        }
        
        if (e instanceof VariableBinding && ((VariableBinding) e).isTemporary()) {
            VariableBinding vb1 = (VariableBinding) e;
            VariableBinding vb2 = (VariableBinding) vb1.clone();
            return new VariableBinding[] {vb1, vb2};
        }
        
        Assignment a = new Assignment(Assignment.Operator.ASSIGN);
        a.setRange(bytes.getIndex(), bytes.getIndex());
        VariableBinding vb1 = methodDecl.createAnonymousVariableBinding(e.getTypeBinding(), true);
        VariableBinding vb2 = (VariableBinding) vb1.clone();
        VariableBinding vb3 = (VariableBinding) vb1.clone();
        tempDecls.add(vb1.getVariableDeclaration());
        vb1.getVariableDeclaration().setParentNode(methodDecl);
        a.setLeftHandSide(vb1);
        a.setRightHandSide(e);
        cNode.block.appendChild(a);
        return new VariableBinding[] {vb2, vb3};
    }
    
    /**
     * Creates a new case group at the specified pc if it does not yet exist.
     * 
     * @param header the switch header
     * @param switchEdges all currently existing switch edges
     * @param startPc the pc at which the case group starts 
     * @return the switch edge to the case group. 
     */
    private SwitchEdge getOrCreateCaseGroup(Node header, Map<Integer, SwitchEdge> switchEdges, int startPc) {
        SwitchEdge switchEdge = switchEdges.get(startPc);
        if (switchEdge == null) {
            Node caseGroupNode = graph.createNode(startPc);
            switchEdge = (SwitchEdge) graph.addEdge(header, caseGroupNode, SwitchEdge.class);
            switchEdges.put(startPc, switchEdge);
        }
        
        return switchEdge;
    }

    private int readUnsigned() throws IOException {
        int index;
        if (wide) {
            index = bytes.readUnsignedShort();
            wide = false; // Clear flag
        } else {
            index = bytes.readUnsignedByte();
        }
        return index;
    }
    
    private int readSigned() throws IOException {
        int index;
        if (wide) {
            index = bytes.readShort();
            wide = false; // Clear flag
        } else {
            index = bytes.readByte();
        }
        return index;
    }
    
    private void dup1() {
        Expression[] value1 = duplicate(stack.pop());
        stack.push(value1[0]);
        stack.push(value1[1]);
    }
    
    private void dup2() {
        Expression[] value1 = duplicate(stack.pop());
        Expression[] value2 = duplicate(stack.pop());
        stack.push(value2[0]);
        stack.push(value1[0]);
        stack.push(value2[1]);
        stack.push(value1[1]);
    }
    
    private ASTNode parseInstruction() throws IOException {
        int currentIndex = bytes.getIndex();
        short opcode = (short) bytes.readUnsignedByte();

        InstructionType instructionType = Const.instructionTypes[opcode];

        Form form = selectForm(instructionType);

        int opStackDelta = form.getOpStackDelta();
        
        ASTNode instruction = null;
        
        logger.debug(currentIndex + " " + instructionType.getName() + "[" + opcode + "] ");

        switch (opcode) {
            /*
             * Switches have variable length arguments.
             */
            case Const.TABLESWITCH :
                // Format: tableswitch, padding(byte), padding(byte), padding(byte), default(int), low(int), high(int)
                // Operand stack: ..., index(int) -> ...
            case Const.LOOKUPSWITCH : {
                // Format: lookupswitch, padding(byte), padding(byte), padding(byte), default(int), npairs(int)
                // Operand stack: ..., key() -> ...
                Node switchNode = graph.createNode(currentIndex);
                switchNode.isSwitchHeader = true;
                graph.addEdge(cNode, switchNode);
                cNode = null;

                int defaultOffset;
                int npairs;
                int offset;
                int remainder = bytes.getIndex() % 4;
                int noPadBytes = (remainder == 0) ? 0 : 4 - remainder;

                // Skip (0-3) padding bytes, i.e., the following bytes are 4-byte-aligned.
                for (int i = 0; i < noPadBytes; i++) {
                    byte b;

                    if ((b = bytes.readByte()) != 0) {
                        logger.warn("Padding byte != 0 in " + instructionType.getName() + ":" + b);
                    }
                }

                defaultOffset = bytes.readInt();

                int low = 0;
                if (opcode==Const.LOOKUPSWITCH) {
                    npairs = bytes.readInt();
                    offset = bytes.getIndex() - 8 - noPadBytes - 1;
                } else {
                    low = bytes.readInt();
                    int high = bytes.readInt();
                    npairs = high - low + 1;
                    offset = bytes.getIndex() - 12 - noPadBytes - 1;
                }

                defaultOffset += offset;

                switchNode.switchExpression = stack.pop();

                TreeMap<Integer, SwitchEdge> caseGroups = new TreeMap<Integer, SwitchEdge>();

                // Add all cases.
                for (int i = 0; i < npairs; i++) {
                    int key;
                    if (opcode==Const.LOOKUPSWITCH) {
                        key = bytes.readInt();
                    } else {
                        key = low + i;
                    }

                    SwitchEdge switchEdge = getOrCreateCaseGroup(switchNode, caseGroups, offset + bytes.readInt());
                    switchEdge.expressions.add(NumberLiteral.create(new Integer(key)));
                }
                
                Node defaultNode = graph.createNode(defaultOffset);
                graph.addEdge(switchNode, defaultNode);
                //getOrCreateCaseGroup(switchNode, caseGroups, defaultOffset);
                
                instruction = new NoOperation();
                break;
            }

            case Const.CHECKCAST : {
                // Format: checkcast, index(short)
                // Operand stack: ..., objectref() -> ..., objectref(object)
                CastExpression cast = new CastExpression();
                int index = bytes.readUnsignedShort();
                ConstantClass c = (ConstantClass) constantPool.getConstant(index);
                ObjectType type = new ObjectType(c.getBytes(constantPool).replace('/', '.'));
                cast.setTypeBinding(type);
                cast.setExpression(stack.pop());
                instruction = cast;
                break;
            }
            
            
            case Const.INSTANCEOF : {
                // Format: instanceof, index(short)
                // Operand stack: ..., objectref() -> ..., result(int)
                int index = bytes.readUnsignedShort();
                InstanceofExpression ex = new InstanceofExpression();
                Expression objectref = stack.pop();
                ex.setLeftOperand(objectref);
                ConstantClass c = (ConstantClass) constantPool.getConstant(index);
                ObjectType type = new ObjectType(c.getBytes(constantPool).replace('/', '.'));
                ex.setRightOperand(type);
                ex.widen(objectref);
                instruction = ex;
                break;
            }
        
            
            case Const.ACONST_NULL :
                // Format: aconst_null
                // Operand stack: ... -> ..., null(object)
                instruction = new NullLiteral();
                break;
            
            case Const.JSR : {
                // Format: jsr, branch(short)
                // Operand stack: ... -> ..., address(returnAddress)
                instruction = new JumpSubRoutine(currentIndex + bytes.readShort());
                opStackDelta = 0;
                break;
            }
                
            case Const.JSR_W : {
                // Format: jsr_w, branch(int)
                // Operand stack: ... -> ..., address()
                instruction = new JumpSubRoutine(currentIndex + bytes.readInt());
                break;
            }
            
            case Const.IFEQ :
                // Format: ifeq, branch(short)
                // Operand stack: ..., value() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.EQUALS, NumberLiteral.create(0));
                break;
            case Const.IFNE :
                // Format: ifne, branch(short)
                // Operand stack: ..., value() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.NOT_EQUALS, NumberLiteral.create(0));
                break;
            case Const.IFGE :
                // Format: ifge, branch(short)
                // Operand stack: ..., value() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.GREATER_EQUALS, NumberLiteral.create(0));
                break;
            case Const.IFGT :
                // Format: ifgt, branch(short)
                // Operand stack: ..., value() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.GREATER, NumberLiteral.create(0));
                break;
            case Const.IFLE :
                // Format: ifle, branch(short)
                // Operand stack: ..., value() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.LESS_EQUALS, NumberLiteral.create(0));
                break;
            case Const.IFLT :
                // Format: iflt, branch(short)
                // Operand stack: ..., value() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.LESS, NumberLiteral.create(0));
                break;
            case Const.IFNONNULL :
                // Format: ifnonnull, branch(short)
                // Operand stack: ..., value() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.NOT_EQUALS, new NullLiteral());
                break;
            case Const.IFNULL :
                // Format: ifnull, branch(short)
                // Operand stack: ..., value() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.EQUALS, new NullLiteral());
                break;
            
            case Const.IF_ACMPEQ :
                // Format: if_acmpeq, branch(short)
                // Operand stack: ..., value1(), value2() -> ...
            case Const.IF_ICMPEQ :
                // Format: if_icmpeq, branch(short)
                // Operand stack: ..., value1(), value2() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.EQUALS);
                break;
            case Const.IF_ACMPNE :
                // Format: if_acmpne, branch(short)
                // Operand stack: ..., value1(), value2() -> ...
            case Const.IF_ICMPNE :
                // Format: if_icmpne, branch(short)
                // Operand stack: ..., value1(), value2() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.NOT_EQUALS);
                break;
            case Const.IF_ICMPGE :
                // Format: if_icmpge, branch(short)
                // Operand stack: ..., value1(), value2() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.GREATER_EQUALS);
                break;
            case Const.IF_ICMPGT :
                // Format: if_icmpgt, branch(short)
                // Operand stack: ..., value1(), value2() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.GREATER);
                break;
            case Const.IF_ICMPLE :
                // Format: if_icmple, branch(short)
                // Operand stack: ..., value1(), value2() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.LESS_EQUALS);
                break;
            case Const.IF_ICMPLT :
                // Format: if_icmplt, branch(short)
                // Operand stack: ..., value1(), value2() -> ...
                instruction = createConditional(currentIndex, InfixExpression.Operator.LESS);
                break;

            case Const.LCMP :
                // Format: lcmp
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.FCMPL :
                // Format: fcmpl
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.FCMPG :
                // Format: fcmpg
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.DCMPL :
                // Format: dcmpl
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.DCMPG : {
                // Format: dcmpg
                // Operand stack: ..., value1(), value2() -> ..., result(int)
                MethodBinding binding = MethodBinding.lookup("javascript.Utils", "cmp", "(DDI)I");
                MethodInvocation mi = new MethodInvocation(methodDecl, binding);
                
                Expression value2 = stack.pop();
                mi.addArgument(stack.pop());
                mi.addArgument(value2);
                
                int gORl = 0;
                if (instructionType.getName().endsWith("g")) gORl = 1;
                else if (instructionType.getName().endsWith("l")) gORl = -1;
                mi.addArgument(NumberLiteral.create(gORl));
                
                instruction = mi;
                
                break;
            }
                
            case Const.GOTO : {
                // Format: goto, branch(short)
                // Operand stack: ... -> ...
                instruction = new Jump(currentIndex + bytes.readShort());
                break;
            }

            case Const.GOTO_W : {
                // Format: goto_w, branch(int)
                // Operand stack: ... -> ...
                instruction = new Jump(currentIndex + bytes.readInt());
                break;
            }
            
            case Const.NEW: {
                // Format: new, index(short)
                // Operand stack: ... -> ..., objectref(object)
                ConstantClass c = (ConstantClass) constantPool.getConstant(bytes.readUnsignedShort());
                ObjectType type = new ObjectType(c.getBytes(constantPool).replace('/', '.'));
                
                instruction = new ClassInstanceCreation(type);
            }
            break;
            
            /**
             * Create new array with component type being primitiv.
             */
            case Const.NEWARRAY: {
                // Format: newarray, atype(byte)
                // Operand stack: ..., count(int) -> ..., arrayref(object)
                
                String componentSignature = BasicType.getType(bytes.readByte()).getSignature();

                // Create a new array with components of type componentType.
                List<ASTNode> dimensions = new ArrayList<ASTNode>();
                dimensions.add(stack.pop());
                ArrayCreation ac = new ArrayCreation(methodDecl, new ObjectType("[" + componentSignature), dimensions);
                instruction = ac;
                break;
            }
            
            /*
             * Create new array of reference of non-primitiv type.
             */
            case Const.ANEWARRAY: {
                // Format: anewarray, index(short)
                // Operand stack: ..., count(int) -> ..., arrayref(object)
                                
                ConstantClass c = (ConstantClass) constantPool.getConstant(bytes.readUnsignedShort());
                String componentSignature = c.getBytes(constantPool).replace('/', '.');
                Type arrayType;
                if (componentSignature.startsWith("[")) {
                    arrayType = new ObjectType("[" + componentSignature);
                } else {
                    arrayType = new ObjectType("[L" + componentSignature + ";");
                }

                // Create a new array with components of type componentType.
                List<ASTNode> dimensions = new ArrayList<ASTNode>();
                dimensions.add(stack.pop());
                ArrayCreation ac = new ArrayCreation(methodDecl, arrayType, dimensions);
                instruction = ac;
                break;
            }
            
            /*
             * Multidimensional array of references.
             */
            case Const.MULTIANEWARRAY : {
                // Format: multianewarray, index(short), dimension N(byte)
                // Operand stack: ..., count1(), ...(), countN() -> ..., arrayref(object)
                
                ConstantClass c = (ConstantClass) constantPool.getConstant(bytes.readUnsignedShort());
                ObjectType arrayType = new ObjectType(c.getBytes(constantPool).replace('/', '.'));
                
                // Create a new multidimensional array of the array type arrayType.
                // The array type must be an array class type of dimensionality greater than or equal to dimensions.
                
                int dimCount = bytes.readUnsignedByte();
                opStackDelta = 1 - dimCount;
                List<ASTNode> dimensions = new ArrayList<ASTNode>();
                for (int i=0; i<dimCount; i++) {
                    // Add dimension in reverse order.
                    dimensions.add(0, stack.pop());
                }
                ArrayCreation ac = new ArrayCreation(methodDecl, arrayType, dimensions);
                instruction = ac;
                break;
             }
                
            case Const.PUTSTATIC :
                // Format: putstatic, index(short)
                // Operand stack: ..., value() -> ...
            case Const.PUTFIELD : {
                // Format: putfield, index(short)
                // Operand stack: ..., objectref(), value() -> ...
                Assignment a = new Assignment(Assignment.Operator.ASSIGN);
                Expression rhs = stack.pop();
                
                int index = bytes.readUnsignedShort();
                ConstantFieldref fieldRef = (ConstantFieldref) constantPool.getConstant(index, Constants.CONSTANT_Fieldref);

                FieldAccess fa = new FieldWrite();
                fa.setName(getFieldName(fieldRef));
                fa.setType(new ObjectType(fieldRef.getClass(constantPool)));
                fa.initialize(methodDecl);
                
                if (opcode==Const.PUTFIELD) {
                    fa.setExpression(stack.pop());
                }

                a.setLeftHandSide(fa);
                a.setRightHandSide(rhs);
                
                instruction = a;
                break;
            }
            
            case Const.GETFIELD : {
                // Format: getfield, index(short)
                // Operand stack: ..., objectref() -> ..., value()
                int index = bytes.readUnsignedShort();
                ConstantFieldref fieldRef = (ConstantFieldref) constantPool.getConstant(index, Constants.CONSTANT_Fieldref);

                Expression ex = stack.pop();
                FieldAccess fa = new FieldRead();
                fa.setType(new ObjectType(fieldRef.getClass(constantPool)));
                fa.setName(getFieldName(fieldRef));
                fa.setExpression(ex);
                fa.initialize(methodDecl);
                instruction = fa;
                break;
            }
            
            case Const.GETSTATIC : {
                // Format: getstatic, index(short)
                // Operand stack: ... -> ..., value()
                int index = bytes.readUnsignedShort();
                ConstantFieldref fieldRef = (ConstantFieldref) constantPool.getConstant(index, Constants.CONSTANT_Fieldref);
                
                FieldAccess fa = new FieldRead();
                fa.setType(new ObjectType(fieldRef.getClass(constantPool)));
                fa.setName(getFieldName(fieldRef));
                fa.initialize(methodDecl);
                //Name e = new Name(fieldRef.getClass(constantPool));
                //fa.setExpression(e);
                instruction = fa;
                break;
            }
            
            case Const.DUP : {
                // Format: dup
                // Operand stack: ..., value(cat1) -> ..., value(cat1), value(cat1)
                dup1();
                instruction = stack.pop();
                break;
            }
                
            case Const.DUP2 :
                // (0) Format: dup2
                // (0) Operand stack: ..., value2(cat1), value1(cat1) -> ..., value2(cat1), value1(cat1), value2(cat1), value1(cat1)
                // (1) Format: dup2
                // (1) Operand stack: ..., value(cat2) -> ..., value(cat2), value(cat2)
                if (form.getIndex() == 0) {
                    dup2();
                    instruction = stack.pop();
                } else {
                    dup1();
                    instruction = stack.pop();
                }
                break;
                
            case Const.DUP_X1 : {
                // Format: dup_x1
                // Operand stack: ..., value2(cat1), value1(cat1) -> ..., value1(cat1), value2(cat1), value1(cat1)
                dup1();
                stack.rotate(2);
                instruction = stack.pop();
                break;
            }
            
            case Const.DUP_X2 : {
                // (0) Format: dup_x2
                // (0) Operand stack: ..., value3(cat1), value2(cat1), value1(cat1) -> ..., value1(cat1), value3(cat1), value2(cat1), value1(cat1)
                // (1) Format: dup_x2
                // (1) Operand stack: ..., value2(cat2), value1(cat1) -> ..., value1(cat1), value2(cat2), value1(cat1)
                if (form.getIndex()==0) {
                    dup1();
                    stack.rotate(3);
                } else {
                    dup1();
                    stack.rotate(2);
                }
                instruction = stack.pop();
                break;
            }
            
            case Const.DUP2_X1 :
                // (0) Format: dup2_x1
                // (0) Operand stack: ..., value3(cat1), value2(cat1), value1(cat1) -> ..., value2(cat1), value1(cat1), value3(cat1), value2(cat1), value1(cat1)
                // (1) Format: dup2_x1
                // (1) Operand stack: ..., value2(cat1), value1(cat2) -> ..., value1(cat2), value2(cat1), value1(cat2)
                if (form.getIndex()==0) {
                    dup2();
                    stack.rotate(4);
                    stack.rotate(4);
                } else {
                    dup1();
                    stack.rotate(2);
                }
                instruction = stack.pop();
                break;
                
            case Const.DUP2_X2 :
                // (0) Format: dup2_x2
                // (0) Operand stack: ..., value4(cat1), value3(cat1), value2(cat1), value1(cat1) -> ..., value2(cat1), value1(cat1), value4(cat1), value3(cat1), value2(cat1), value1(cat1)
                // (1) Format: dup2_x2
                // (1) Operand stack: ..., value3(cat1), value2(cat1), value1(cat2) -> ..., value1(cat2), value3(cat1), value2(cat1), value1(cat2)
                // (2) Format: dup2_x2
                // (2) Operand stack: ..., value3(cat2), value2(cat1), value1(cat1) -> ..., value2(cat1), value1(cat1), value3(cat2), value2(cat1), value1(cat1)
                // (3) Format: dup2_x2
                // (3) Operand stack: ..., value2(cat2), value1(cat2) -> ..., value1(cat2), value2(cat2), value1(cat2)
                if (form.getIndex()==0) {
                    dup2();
                    stack.rotate(5);
                    stack.rotate(5);
                } else if (form.getIndex()==1) {
                    dup1();
                    stack.rotate(3);
                } else if (form.getIndex()==2) {
                    dup2();
                    stack.rotate(4);
                    stack.rotate(4);
                } else {
                    dup1();
                    stack.rotate(2);
                }
                
                instruction = stack.pop();
                break;
                
            case Const.SWAP : {
                // Format: swap
                // Operand stack: ..., value2(cat1), value1(cat1) -> ..., value1(cat1), value2(cat1)
                stack.rotate(1);
                instruction = new NoOperation();
                break;
            }
            
            case Const.I2S : 
                // Format: i2s
                // Operand stack: ..., value() -> ..., result(short)
            case Const.I2F :
                // Format: i2f
                // Operand stack: ..., value() -> ..., result(float)
            case Const.L2I :
                // Format: l2i
                // Operand stack: ..., value() -> ..., result(int)
            case Const.F2I :
                // Format: f2i
                // Operand stack: ..., value(float) -> ..., result(int)
            case Const.F2L :
                // Format: f2l
                // Operand stack: ..., value() -> ..., result(long)
            case Const.L2F :
                // Format: l2f
                // Operand stack: ..., value() -> ..., result(float)
            case Const.L2D :
                // Format: l2d
                // Operand stack: ..., value() -> ..., result(double)
            case Const.D2I :
                // Format: d2i
                // Operand stack: ..., value() -> ..., result(int)
            case Const.D2L :
                // Format: d2l
                // Operand stack: ..., value() -> ..., result(long)
            case Const.D2F :
                // Format: d2f
                // Operand stack: ..., value() -> ..., result(float)
            case Const.I2B :
                // Format: i2b
                // Operand stack: ..., value() -> ..., result(byte)
            case Const.I2C :
                // Format: i2c
                // Operand stack: ..., value() -> ..., result(byte)
                // Operation i2f may result in a loss of precision because values of type float have only 24 significand bits.
                // Operation l2d may lose precision because values of type double have only 53 significand bits
                instruction = new PrimitiveCast(opcode, stack.pop(), form.getResultType());
                break;
            
            case Const.I2L :
                // Format: i2l
                // Operand stack: ..., value() -> ..., result(long)
                stack.peek().setTypeBinding(Type.LONG);
                instruction = new NoOperation();
                break;
            case Const.I2D :
                // Format: i2d
                // Operand stack: ..., value() -> ..., result(double)
            case Const.F2D :
                // Format: f2d
                // Operand stack: ..., value() -> ..., result(double)
                // Widening preserves the numeric value exactly, see VM Spec2, Section 2.6.2.
                // For operation f2d there may be a rounding related to value set conversion. 
                stack.peek().setTypeBinding(Type.DOUBLE);
                instruction = new NoOperation();
                break;
                
            case Const.INEG:
                // Format: ineg
                // Operand stack: ..., value() -> ..., result(int)
            case Const.LNEG:
                // Format: lneg
                // Operand stack: ..., value() -> ..., result(long)
            case Const.FNEG:
                // Format: fneg
                // Operand stack: ..., value() -> ..., result(float)
            case Const.DNEG:
                // Format: dneg
                // Operand stack: ..., value() -> ..., result(double)
                instruction = createPrefix(PrefixExpression.MINUS, stack.pop(), form.getResultType());
                break;
            
            case Const.ISHR :
                // Format: ishr
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LSHR : 
                // Format: lshr
                // Operand stack: ..., value1(), value2() -> ..., result(long)
                instruction = createInfixRightLeft(InfixExpression.Operator.RIGHT_SHIFT_SIGNED, stack.pop(), stack.pop(), form.getResultType());
                break;
            
            case Const.ISHL :
                // Format: ishl
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LSHL :
                // Format: lshl
                // Operand stack: ..., value1(), value2() -> ..., result(long)
                instruction = createInfixRightLeft(InfixExpression.Operator.LEFT_SHIFT, stack.pop(), stack.pop(), form.getResultType());
                break;
            
            case Const.IUSHR :
                // Format: iushr
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LUSHR :
                // Format: lushr
                // Operand stack: ..., value1(), value2() -> ..., result(long)
                instruction = createInfixRightLeft(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, stack.pop(), stack.pop(), form.getResultType());
                break;
     
            case Const.IADD :
                // Format: iadd
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LADD :
                // Format: ladd
                // Operand stack: ..., value1(), value2() -> ..., result(long)
            case Const.FADD :
                // Format: fadd
                // Operand stack: ..., value1(), value2() -> ..., result(float)
            case Const.DADD :
                // Format: dadd
                // Operand stack: ..., value1(double), value2(double) -> ..., result(double)
                instruction = createInfixRightLeft(InfixExpression.Operator.PLUS, stack.pop(), stack.pop(), form.getResultType());
                break;

            case Const.ISUB :
                // Format: isub
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LSUB :
                // Format: lsub
                // Operand stack: ..., value1(), value2() -> ..., result(long)
            case Const.FSUB :
                // Format: fsub
                // Operand stack: ..., value1(), value2() -> ..., result(float)
            case Const.DSUB :
                // Format: dsub
                // Operand stack: ..., value1(double), value2(double) -> ..., result(double)
                instruction = createInfixRightLeft(InfixExpression.Operator.MINUS, stack.pop(), stack.pop(), form.getResultType());
                break;
                
            case Const.IMUL :
                // Format: imul
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LMUL :
                // Format: lmul
                // Operand stack: ..., value1(), value2() -> ..., result(long)
            case Const.FMUL :
                // Format: fmul
                // Operand stack: ..., value1(), value2() -> ..., result(float)
            case Const.DMUL :
                // Format: dmul
                // Operand stack: ..., value1(), value2() -> ..., result(double)
                instruction = createInfixRightLeft(InfixExpression.Operator.TIMES, stack.pop(), stack.pop(), form.getResultType());
                break;
                
            case Const.IDIV :
                // Format: idiv
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LDIV :
                // Format: ldiv
                // Operand stack: ..., value1(), value2() -> ..., result(long)
            case Const.FDIV :
                // Format: fdiv
                // Operand stack: ..., value1(), value2() -> ..., result(float)
            case Const.DDIV :
                // Format: ddiv
                // Operand stack: ..., value1(), value2() -> ..., result(double)
                instruction = createInfixRightLeft(InfixExpression.Operator.DIVIDE, stack.pop(), stack.pop(), form.getResultType());
                break;
                
            case Const.IREM :
                // Format: irem
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LREM :
                // Format: lrem
                // Operand stack: ..., value1(), value2() -> ..., result(long)
            case Const.FREM :
                // Format: frem
                // Operand stack: ..., value1(), value2() -> ..., result(float)
            case Const.DREM :
                // Format: drem
                // Operand stack: ..., value1(), value2() -> ..., result(double)
                instruction = createInfixRightLeft(InfixExpression.Operator.REMAINDER, stack.pop(), stack.pop(), form.getResultType());
                break;
                
            case Const.IXOR :
                // Format: ixor
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LXOR :
                // Format: lxor
                // Operand stack: ..., value1(), value2() -> ..., result(long)
                instruction = createInfixRightLeft(InfixExpression.Operator.XOR, stack.pop(), stack.pop(), form.getResultType());
                break;
            
            case Const.IAND :
                // Format: iand
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LAND :
                // Format: land
                // Operand stack: ..., value1(), value2() -> ..., result(long)
                instruction = createInfixRightLeft(InfixExpression.Operator.AND, stack.pop(), stack.pop(), form.getResultType());
                break;
            
            case Const.IOR :
                // Format: ior
                // Operand stack: ..., value1(), value2() -> ..., result(int)
            case Const.LOR :
                // Format: lor
                // Operand stack: ..., value1(), value2() -> ..., result(long)
                instruction = createInfixRightLeft(InfixExpression.Operator.OR, stack.pop(), stack.pop(), form.getResultType());
                break;
            
            case Const.IINC : {
                // Format: iinc, index(byte), const(int)
                // Operand stack: ... -> ...
                boolean isWide = wide;
                int index = readUnsigned();
                // Reset wide flag.
                wide = isWide;
                int constByte = readSigned();

                VariableBinding reference = createVariableBinding(index, Type.INT, true);
                reference.setField(false);
                
//                if (stack.peek() instanceof VariableBinding) {
//                    if (reference.isSame(reference) && Math.abs(constByte) == 1) {
//                        stack.pop();
//                        PostfixExpression pe = new PostfixExpression();
//                        pe.setOperator(constByte==1?PStarExpression.INCREMENT:PStarExpression.DECREMENT);
//                        pe.setOperand(reference);
//                        instruction = pe;
//                        break;
//                    }
//                }
                
                Assignment assign = new Assignment(Assignment.Operator.PLUS_ASSIGN);
                assign.setLeftHandSide(reference);
                assign.setRightHandSide(NumberLiteral.create(new Integer(constByte)));
                instruction = assign;
                break;
            }
            
            case Const.ARRAYLENGTH : {
                // Format: arraylength
                // Operand stack: ..., arrayref() -> ..., length(int)
                Expression arrayRef = stack.pop();
                FieldAccess access = new FieldRead();
                access.setExpression(arrayRef);
                access.setName("length");
                //access.initialize(methodDecl);
                instruction = access;
                break;
            }
                
                /*
                 * Remember wide byte which is used to form a 16-bit address in
                 * the following instruction. Relies on that the method is
                 * called again with the following opcode.
                 */
            case Const.WIDE :
                // Format: wide
                // Operand stack: ... -> ...
                wide = true;
                return new NoOperation();

            case Const.ILOAD_0 :
                // Format: iload_0
                // Operand stack: ... -> ..., value(int)
            case Const.ILOAD_1 :
                // Format: iload_1
                // Operand stack: ... -> ..., value(int)
            case Const.ILOAD_2 :
                // Format: iload_2
                // Operand stack: ... -> ..., value(int)
            case Const.ILOAD_3 : {
                // Format: iload_3
                // Operand stack: ... -> ..., value(int)
                VariableBinding reference = createVariableBinding(opcode - Const.ILOAD_0, Type.INT, false);
                reference.setField(false);
                instruction = reference;
                break;
            }
            
            case Const.LLOAD_0 :
                // Format: lload_0
                // Operand stack: ... -> ..., value(long)
            case Const.LLOAD_1 :
                // Format: lload_1
                // Operand stack: ... -> ..., value(long)
            case Const.LLOAD_2 :
                // Format: lload_2
                // Operand stack: ... -> ..., value(long)
            case Const.LLOAD_3 : {
                // Format: lload_3
                // Operand stack: ... -> ..., value(long)
                VariableBinding reference = createVariableBinding(opcode - Const.LLOAD_0, Type.LONG, false);
                reference.setField(false);
                instruction = reference;
                break;
            }
            
            case Const.FLOAD_0 :
                // Format: fload_0
                // Operand stack: ... -> ..., value(float)
            case Const.FLOAD_1 :
                // Format: fload_1
                // Operand stack: ... -> ..., value(float)
            case Const.FLOAD_2 :
                // Format: fload_2
                // Operand stack: ... -> ..., value(float)
            case Const.FLOAD_3 : {
                // Format: fload_3
                // Operand stack: ... -> ..., value(float)
                VariableBinding reference = createVariableBinding(opcode - Const.FLOAD_0, Type.FLOAT, false);
                reference.setField(false);
                instruction = reference;
                break;
            }
            
            case Const.DLOAD_0 :
                // Format: dload_0
                // Operand stack: ... -> ..., value(double)
            case Const.DLOAD_1 :
                // Format: dload_1
                // Operand stack: ... -> ..., value(double)
            case Const.DLOAD_2 :
                // Format: dload_2
                // Operand stack: ... -> ..., value(double)
            case Const.DLOAD_3 : {
                // Format: dload_3
                // Operand stack: ... -> ..., value(double)
                VariableBinding reference = createVariableBinding(opcode - Const.DLOAD_0, Type.DOUBLE, false);
                reference.setField(false);
                instruction = reference;
                break;
            }
            
            case Const.ALOAD_0 :
                // Format: aload_0
                // Operand stack: ... -> ..., objectref(object)
            case Const.ALOAD_1 :
                // Format: aload_1
                // Operand stack: ... -> ..., objectref(object)
            case Const.ALOAD_2 :
                // Format: aload_2
                // Operand stack: ... -> ..., objectref(object)
            case Const.ALOAD_3 : {
                // Format: aload_3
                // Operand stack: ... -> ..., objectref(object)
                if (opcode == Const.ALOAD_0 && !Modifier.isStatic(methodDecl.getAccess())) {
                    ThisExpression reference = new ThisExpression();
                    instruction = reference;
                } else {
                    VariableBinding reference = createVariableBinding(opcode - Const.ALOAD_0, Type.OBJECT, false);
                    reference.setField(true);
                    instruction = reference;
                }
                break;
            }
            
            case Const.ILOAD :
                // Format: iload, index(byte)
                // Operand stack: ... -> ..., value(int)
            case Const.LLOAD :
                // Format: lload, index(byte)
                // Operand stack: ... -> ..., value(long)
            case Const.FLOAD :
                // Format: fload, index(byte)
                // Operand stack: ... -> ..., value(float)
            case Const.DLOAD : {
                // Format: dload, index(byte)
                // Operand stack: ... -> ..., value(double)
                VariableBinding reference = createVariableBinding(readUnsigned(), form.getResultType(), false);
                reference.setField(false);
                instruction = reference;
                break;
            }
            
            case Const.ALOAD : {
                // Format: aload, index(byte)
                // Operand stack: ... -> ..., objectref(object)
                VariableBinding reference = createVariableBinding(readUnsigned(), Type.OBJECT, false);
                reference.setField(true);
                instruction = reference;
                break;
            }
            
            case Const.BALOAD :
                // Format: baload
                // Operand stack: ..., arrayref(), index() -> ..., value(int)
            case Const.CALOAD :
                // Format: caload
                // Operand stack: ..., arrayref(), index() -> ..., value(int)
            case Const.SALOAD :
                // Format: saload
                // Operand stack: ..., arrayref(), index() -> ..., value(short)
            case Const.IALOAD :
                // Format: iaload
                // Operand stack: ..., arrayref(), index() -> ..., value(int)
            case Const.LALOAD :
                // Format: laload
                // Operand stack: ..., arrayref(), index() -> ..., value(long)
            case Const.FALOAD :
                // Format: faload
                // Operand stack: ..., arrayref(), index() -> ..., value(float)
            case Const.DALOAD :
                // Format: daload
                // Operand stack: ..., arrayref(), index() -> ..., value(double)
            case Const.AALOAD : {
                // Format: aaload
                // Operand stack: ..., arrayref(), index() -> ..., objectref(object)
                Expression index = stack.pop();
                Expression arrayRef = stack.pop();
                ArrayAccess aa;
                aa = new ArrayAccess();
                aa.setTypeBinding(form.getResultType());
                aa.setArray(arrayRef);
                aa.setIndex(index);

                instruction = aa;
                break;
            }
            
            case Const.BASTORE :
                // Format: bastore
                // Operand stack: ..., arrayref(), index(), value() -> ...
            case Const.CASTORE :
                // Format: castore
                // Operand stack: ..., arrayref(), index(), value() -> ...
            case Const.SASTORE :
                // Format: sastore
                // Operand stack: ..., array(), index(), value() -> ...
            case Const.IASTORE :
                // Format: iastore
                // Operand stack: ..., arrayref(), index(), value() -> ...
            case Const.LASTORE :
                // Format: lastore
                // Operand stack: ..., arrayref(), index(), value() -> ...
            case Const.FASTORE :
                // Format: fastore
                // Operand stack: ..., arrayref(), index(), value() -> ...
            case Const.DASTORE :                
                // Format: dastore
                // Operand stack: ..., arrayref(), index(), value() -> ...
            case Const.AASTORE : {
                // Format: aastore
                // Operand stack: ..., arrayref(), index(), value() -> ...
                Expression value = stack.pop();
                Expression index = stack.pop();
                Expression arrayRef = stack.pop();
                if (arrayRef instanceof ArrayCreation) {
                    ArrayCreation ac = (ArrayCreation) arrayRef;
                    if (ac.getInitializer() == null) {
                        ac.setInitializer(new ArrayInitializer());
                    }
                    ac.getInitializer().getExpressions().add(value);
                    instruction = new NoOperation();
                    break;
                }
                Assignment a = new Assignment(Assignment.Operator.ASSIGN);
                
                ArrayAccess aa;
                aa = new ArrayAccess();
                aa.setArray(arrayRef);
                aa.setIndex(index);
                
                a.setLeftHandSide(aa);
                a.setRightHandSide(value);
                instruction = a;
                break;
            }
                
            
            case Const.DSTORE :
                // Format: dstore, index(byte)
                // Operand stack: ..., value(double) -> ...
            case Const.DSTORE_0 :
                // Format: dstore_0
                // Operand stack: ..., value(double) -> ...
            case Const.DSTORE_1 :
                // Format: dstore_1
                // Operand stack: ..., value(double) -> ...
            case Const.DSTORE_2 :
                // Format: dstore_2
                // Operand stack: ..., value(double) -> ...
            case Const.DSTORE_3 : {
                // Format: dstore_3
                // Operand stack: ..., value(double) -> ...
                int index;
                if (opcode == Const.DSTORE) {
                    index = readUnsigned();
                } else {
                    index = opcode - Const.DSTORE_0;
                }
                Assignment a = new Assignment(Assignment.Operator.ASSIGN);
                VariableBinding reference = createVariableBinding(index, Type.DOUBLE, true);
                reference.setField(false);
                a.setLeftHandSide(reference);
                a.setRightHandSide(stack.pop());
                instruction = a;
                break;
            }
            
            case Const.FSTORE :
                // Format: fstore, index(byte)
                // Operand stack: ..., value() -> ...
            case Const.FSTORE_0 :
                // Format: fstore_0
                // Operand stack: ..., value(float) -> ...
            case Const.FSTORE_1 :
                // Format: fstore_1
                // Operand stack: ..., value(float) -> ...
            case Const.FSTORE_2 :
                // Format: fstore_2
                // Operand stack: ..., value(float) -> ...
            case Const.FSTORE_3 : {
                // Format: fstore_3
                // Operand stack: ..., value(float) -> ...
                int index;
                if (opcode == Const.FSTORE) {
                    index = readUnsigned();
                } else {
                    index = opcode - Const.FSTORE_0;
                }
                Assignment a = new Assignment(Assignment.Operator.ASSIGN);
                VariableBinding reference = createVariableBinding(index, Type.FLOAT, true);
                reference.setField(false);
                a.setLeftHandSide(reference);
                a.setRightHandSide(stack.pop());
                instruction = a;
                break;
            }
            
            case Const.ISTORE :
                // Format: istore, index(byte)
                // Operand stack: ..., value() -> ...
            case Const.ISTORE_0 :
                // Format: istore_0
                // Operand stack: ..., value() -> ...
            case Const.ISTORE_1 :
                // Format: istore_1
                // Operand stack: ..., value() -> ...
            case Const.ISTORE_2 :
                // Format: istore_2
                // Operand stack: ..., value() -> ...
            case Const.ISTORE_3 : {
                // Format: istore_3
                // Operand stack: ..., value() -> ...
                int index;
                if (opcode == Const.ISTORE) {
                    index = readUnsigned();
                } else {
                    index = opcode - Const.ISTORE_0;
                }
                Assignment a = new Assignment(Assignment.Operator.ASSIGN);
                VariableBinding reference = createVariableBinding(index, Type.INT, true);
                reference.setField(false);
                a.setLeftHandSide(reference);
                a.setRightHandSide(stack.pop());
                instruction = a;
                break;
            }

            case Const.LSTORE :
                // Format: lstore, index(byte)
                // Operand stack: ..., value() -> ...
            case Const.LSTORE_0 :
                // Format: lstore_0
                // Operand stack: ..., value() -> ...
            case Const.LSTORE_1 :
                // Format: lstore_1
                // Operand stack: ..., value() -> ...
            case Const.LSTORE_2 :
                // Format: lstore_2
                // Operand stack: ..., value() -> ...
            case Const.LSTORE_3 : {
                // Format: lstore_3
                // Operand stack: ..., value() -> ...
                int index;
                if (opcode == Const.LSTORE) {
                    index = readUnsigned();
                } else {
                    index = opcode - Const.LSTORE_0;
                }
                Assignment a = new Assignment(Assignment.Operator.ASSIGN);
                VariableBinding reference = createVariableBinding(index, Type.LONG, true);
                reference.setField(false);
                a.setLeftHandSide(reference);
                a.setRightHandSide(stack.pop());
                instruction = a;
                break;
            }

            case Const.ASTORE :
                // Format: astore, index(byte)
                // Operand stack: ..., objectref() -> ...
            case Const.ASTORE_0 :
                // Format: astore_0
                // Operand stack: ..., objectref() -> ...
            case Const.ASTORE_1 :
                // Format: astore_1
                // Operand stack: ..., objectref() -> ...
            case Const.ASTORE_2 :
                // Format: astore_2
                // Operand stack: ..., objectref() -> ...
            case Const.ASTORE_3 : {
                // Format: astore_3
                // Operand stack: ..., objectref() -> ...

                Assignment a = new Assignment(Assignment.Operator.ASSIGN);
                int index;
                if (opcode==Const.ASTORE) {
                    index = readUnsigned();
                } else {
                    index = (opcode - Const.ASTORE_0);
                }
                VariableBinding reference = createVariableBinding(index, Type.OBJECT, true);
                a.setLeftHandSide(reference);
                
                // This may be the first instruction of an exception handler.
                // It will store the implied exception. This exception is pushed onto the
                // stack by makeTryFrames(). However, if the handler is unreachable, the
                // Java Compiler may not generate an exception handler, and the stack is therefore
                // empty.
                // TODO: Refactor this Class by using org.apache.bcel.generic.InstructionList()
                // and having two passes, not just one!
                if (stack.size() > 0) {
                    a.setRightHandSide(stack.pop());
                }
                instruction = a;
                break;
            }
                            
            case Const.ATHROW: {
                // Format: athrow
                // Operand stack: ..., objectref() -> ..., objectref(object)
                ThrowStatement throwStmt = new ThrowStatement();
                throwStmt.setExpression(stack.pop());
                instruction = throwStmt;
                break;
            }
            
            case Const.ICONST_M1 :
                // Format: iconst_m1
                // Operand stack: ... -> ..., i(int)
            case Const.ICONST_0 :
                // Format: iconst_0
                // Operand stack: ... -> ..., i(int)
            case Const.ICONST_1 :
                // Format: iconst_1
                // Operand stack: ... -> ..., i(int)
            case Const.ICONST_2 :
                // Format: iconst_2
                // Operand stack: ... -> ..., i(int)
            case Const.ICONST_3 :
                // Format: iconst_3
                // Operand stack: ... -> ..., i(int)
            case Const.ICONST_4 :
                // Format: iconst_4
                // Operand stack: ... -> ..., i(int)
            case Const.ICONST_5 :
                // Format: iconst_5
                // Operand stack: ... -> ..., i(int)
                instruction = NumberLiteral.create(new Integer(-1 + opcode - Const.ICONST_M1));
                break;
                
            case Const.LCONST_0 :
                // Format: lconst_0
                // Operand stack: ... -> ..., l(long)
            case Const.LCONST_1 :
                // Format: lconst_1
                // Operand stack: ... -> ..., l(long)
                // Push the long constant 0 or 1 onto the operand stack.
                instruction = NumberLiteral.create(new Long(opcode - Const.LCONST_0));
                break;
                
            case Const.FCONST_0 :
                // Format: fconst_0
                // Operand stack: ... -> ..., f(float)
            case Const.FCONST_1 :
                // Format: fconst_1
                // Operand stack: ... -> ..., f(float)
            case Const.FCONST_2 :
                // Format: fconst_2
                // Operand stack: ... -> ..., f(float)
                // Push the float constant 0.0, 1.0 or 2.0 onto the operand stack.
                instruction = NumberLiteral.create(new Float(opcode - Const.FCONST_0));
                break;
                
            case Const.DCONST_0 :
                // Format: dconst_0
                // Operand stack: ... -> ..., d(double)
            case Const.DCONST_1 :
                // Format: dconst_1
                // Operand stack: ... -> ..., d(double)
                // Push the double constant 0.0 or 1.0 onto the operand stack.
                instruction = NumberLiteral.create(new Double(opcode - Const.DCONST_0));
                break;
            
            case Const.BIPUSH : {
                // Format: bipush, byte(byte)
                // Operand stack: ... -> ..., value(int)
                NumberLiteral literal = NumberLiteral.create(new Byte(bytes.readByte()));
                instruction = literal;
                break;
            }
            
            case Const.SIPUSH : {
                // Format: sipush
                // Operand stack: ... -> ..., value(short)
                NumberLiteral il = NumberLiteral.create(new Short(bytes.readShort()));
                instruction = il;
                break;
            }
 
            case Const.LDC :
                // Format: ldc, index(byte)
                // Operand stack: ... -> ..., value(cat1)
            case Const.LDC_W :
                // Format: ldc_w
                // Operand stack: ... -> ..., value(cat1)
            case Const.LDC2_W : {
                // Format: ldc2_w
                // Operand stack: ... -> ..., value(cat1)
                int index;
                if (opcode == Const.LDC) {
                    index = bytes.readUnsignedByte();
                } else {
                    index = bytes.readUnsignedShort();
                }
                Constant constant = constantPool.getConstant(index);
                
                if (opcode==Const.LDC2_W && (constant.getTag()!=Constants.CONSTANT_Double && constant.getTag()!=Constants.CONSTANT_Long))
                    throw new RuntimeException("LDC2_W must load long or double");
                
                if (constant.getTag() == Constants.CONSTANT_Integer) {
                    instruction = NumberLiteral.create(new Integer(((ConstantInteger) constant).getBytes()));
                } else if (constant.getTag() == Constants.CONSTANT_Float) {
                    instruction = NumberLiteral.create(new Float(((ConstantFloat) constant).getBytes()));
                } else if (constant.getTag() == Constants.CONSTANT_Long) {
                    instruction = NumberLiteral.create(new Long(((ConstantLong) constant).getBytes()));
                } else if (constant.getTag() == Constants.CONSTANT_Double) {
                    instruction = NumberLiteral.create(new Double(((ConstantDouble) constant).getBytes()));
                } else if (constant.getTag() == Constants.CONSTANT_Utf8) {
                    instruction = new StringLiteral(((ConstantUtf8) constant).getBytes());
                } else if (constant.getTag() == Constants.CONSTANT_String) {
                    int k = ((ConstantString) constant).getStringIndex();
                    constant = constantPool.getConstant(k, Constants.CONSTANT_Utf8);
                    instruction = new StringLiteral(((ConstantUtf8) constant).getBytes());
                } else if (constant.getTag() == Constants.CONSTANT_Class) {
                    Signature signature = Project.getSingleton().getSignature(((ConstantClass) constant).getBytes(constantPool));
                    instruction = new ClassLiteral(signature);
                } else {
                    throw new RuntimeException("Cannot handle constant tag: " + constant.getTag());
                }
                break;
            }
            

            case Const.RET: {
                // Format: ret, index(byte)
                // Operand stack: ... -> ...
                int index = readUnsigned();
                ReturnStatement r = new ReturnStatement(currentIndex, currentIndex);
                r.setExpression(createVariableBinding(index, Type.INT, false));
                instruction = r;
                break;
            }
            
            case Const.RETURN:
                // Format: return
                // Operand stack: ... -> ...
            case Const.IRETURN:
                // Format: ireturn
                // Operand stack: ..., value() -> ...
            case Const.FRETURN:
                // Format: freturn
                // Operand stack: ..., value() -> ...
            case Const.LRETURN:
                // Format: lreturn
                // Operand stack: ..., value() -> ...
            case Const.DRETURN:
                // Format: dreturn
                // Operand stack: ..., value(double) -> ...
            case Const.ARETURN: {
                // Format: areturn
                // Operand stack: ..., objectref() -> ...
                ReturnStatement r = new ReturnStatement(currentIndex, currentIndex);
                if (opcode != Const.RETURN) {
                    r.setExpression(stack.pop());
                }
                instruction = r;
                break;
            }
            
            case Const.POP :
                // Format: pop
                // Operand stack: ..., value(cat1) -> ...
            case Const.POP2: {
                // (0) Format: pop2
                // (0) Operand stack: ..., value(cat2) -> ...
                // (1) Format: pop2
                // (1) Operand stack: ..., value2(cat1), value1(cat1) -> ...
                
                if (opcode == Const.POP2 && form.getIndex() == 1) {
                    throw new UnsupportedOperationException("InstructionType " + instructionType.getName() + " not supported");
                }
                
                //instruction = stack.pop();
                // TODO: Which is correct?
                // Node is probably an invokation of a method with unused result.
                ASTNode a = stack.pop();
                if (!(a instanceof VariableBinding)) {
                    // Can't have binding on stack! TODO: VariableBinding must not be an ASTNode,
                    // because it has no location.
                    cNode.block.appendChild(a);
                }
                instruction = new NoOperation();
                break;                
            }
            
            case Const.NOP :
                // Format: nop
                // Operand stack: ... -> ...
                return new NoOperation();
            
            case Const.XXXUNUSEDXXX :
                // Format: xxxunusedxxx
                // Operand stack: ... -> ...
                logger.info("Byte code contains unused operation (Ignored)");
                return new NoOperation();
                
            case Const.INVOKEINTERFACE :
                // Format: invokeinterface, index(short), count(byte), 0(byte)
                // Operand stack: ..., objectref(), arg1(), ...(), argN() -> ...
            case Const.INVOKESPECIAL :
                // Format: invokespecial
                // Operand stack: ..., objectref(), arg1(), ...(), argN() -> ...
            case Const.INVOKEVIRTUAL :
                // Format: invokevirtual
                // Operand stack: ..., objectref(), arg1(), ...(), argN() -> ...
            case Const.INVOKESTATIC : {
                // Format: invokestatic, index(short)
                // Operand stack: ..., arg1(), ...(), argN() -> ...
                int index = bytes.readUnsignedShort();
                MethodBinding methodBinding = MethodBinding.lookup(index, constantPool);
                MethodInvocation invocation = new MethodInvocation(methodDecl, methodBinding);
                
                //Processor.getLogger().finer(method.getName() + "->" + invocation.binding);

                int nArgs = methodBinding.getParameterTypes().length;
                int kk = stack.size() - nArgs;
                for (int i=0; i<nArgs; i++) {
                    Expression arg = (Expression) stack.get(kk);
                    stack.remove(kk);
                    invocation.addArgument(arg);
                }
                
                opStackDelta = -nArgs;
                
                if (opcode==Const.INVOKEVIRTUAL || opcode==Const.INVOKESPECIAL || opcode==Const.INVOKEINTERFACE) {
                    opStackDelta--;
                    invocation.setExpression(stack.pop());
                } else {  // INVOKESTATIC
                    //Name name = new Name(method.getClassName());
                    //invocation.setExpression(name);
                }
                
                if (methodBinding.getReturnType() != Type.VOID) {
                    opStackDelta++;
                }
                
                if (opcode==Const.INVOKEINTERFACE) {
                    bytes.readUnsignedByte(); // historical, redundant number of arguments.
                    bytes.readUnsignedByte(); // Last byte is a reserved space.
                } else if (opcode == Const.INVOKESPECIAL) {
                    invocation.isSpecial = true;
                }
                
//                if (opcode==Const.INVOKESPECIAL && stack.size() > 0 && stack.peek() instanceof ClassInstanceCreation && methodBinding.getName().equals("<init>")) {
//                    ClassInstanceCreation cic = (ClassInstanceCreation) stack.pop();
//                    List args = invocation.getArguments();
//                    for (int i=0; i<args.size(); i++)
//                        cic.addArgument((Expression) args.get(i));
//                    cic.signature = method.getSignature();
//                    //invocation.setExpression(cic.getType());
//                    instruction = cic;
//                } else {
//                    instruction = invocation;
//                }
                instruction = invocation;
                break;
            }

            case Const.MONITORENTER : {
                // Format: monitorenter
                // Operand stack: ..., objectref() -> ...
                SynchronizedBlock sb = new SynchronizedBlock();
                sb.monitor = stack.pop();
                sb.widen(sb.monitor);
                sb.setEndIndex(currentIndex);
                instruction = sb;
                break;
            }
            case Const.MONITOREXIT :
                // Format: monitorexit
                // Operand stack: ..., objectref() -> ...
                instruction = new NoOperation();
                instruction.widen(stack.pop());
                instruction.setEndIndex(currentIndex);
                break;
            
            default :
                throw new UnsupportedOperationException("InstructionType " + instructionType.getName() + " not supported");
                //break;
        }
        
        if (opcode!=Const.WIDE && wide) throw new RuntimeException("Expected wide operation");
        
        instruction.setStackDelta(opStackDelta);
        if (opcode < Const.DUP || opcode > Const.DUP2_X2) {
            instruction.leftWiden(currentIndex);
            instruction.rightWiden(bytes.getIndex()-1);
        }
        currentNode = instruction;
        return instruction;
    }
    
    ConditionalBranch createConditional(int currentIndex, InfixExpression.Operator operator) throws IOException {
        ConditionalBranch c = new ConditionalBranch(currentIndex + bytes.readShort());
        InfixExpression be = new InfixExpression(operator);
        Expression rightOperand = stack.pop();
        be.setOperands(stack.pop(),rightOperand);
        c.setExpression(be);
        return c;
    }
    
    ConditionalBranch createConditional(int currentIndex, InfixExpression.Operator operator,
            Expression rightOperand) throws IOException {
        ConditionalBranch c = new ConditionalBranch(currentIndex + bytes.readShort());
        Expression leftOperand = stack.pop();
       
        if (leftOperand.getTypeBinding()!=null && leftOperand.getTypeBinding()==Type.BOOLEAN) {
            if (operator == InfixExpression.Operator.EQUALS && NumberLiteral.isZero(rightOperand)) {
                c.setExpression(Optimizer.negate(leftOperand));
            } else {
                c.setExpression(leftOperand);
            }
        } else {
            InfixExpression be = new InfixExpression(operator);
            be.setOperands(leftOperand, rightOperand);
            c.setExpression(be);
        }

        return c;
    }
    
    private String getFieldName(ConstantFieldref fieldRef) {
        ConstantNameAndType nameAndType = (ConstantNameAndType) constantPool.getConstant(fieldRef.getNameAndTypeIndex());
        return nameAndType.getName(constantPool);
    }
    
    public static String constantToString(Constant c, ConstantPool constantPool) throws ClassFormatException {
        String str;
        byte tag = c.getTag();

        switch (tag) {
        case Constants.CONSTANT_Class :
            str = Utility.compactClassName(((ConstantClass) c).getBytes(constantPool), false);
            break;

        case Constants.CONSTANT_String :
            str = "\"" + Utils.escape(((ConstantString) c).getBytes(constantPool)) + "\"";
            break;

        case Constants.CONSTANT_Utf8 :
            str = ((ConstantUtf8) c).getBytes();
            break;
        case Constants.CONSTANT_Double :
            str = "" + ((ConstantDouble) c).getBytes();
            break;
        case Constants.CONSTANT_Float :
            str = "" + ((ConstantFloat) c).getBytes();
            break;
        case Constants.CONSTANT_Long :
            str = "" + ((ConstantLong) c).getBytes();
            break;
        case Constants.CONSTANT_Integer :
            str = "" + ((ConstantInteger) c).getBytes();
            break;

        case Constants.CONSTANT_NameAndType :
            str = ((ConstantNameAndType) c).getName(constantPool);
            break;

        case Constants.CONSTANT_InterfaceMethodref :
        case Constants.CONSTANT_Methodref :
        case Constants.CONSTANT_Fieldref :
            str = ((ConstantCP) c).getClass(constantPool);
            break;

        default : // Never reached
            throw new RuntimeException("Unknown constant type " + tag);
        }

        return str;
    }

}
