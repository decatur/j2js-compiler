package com.j2js.visitors;

import com.j2js.J2JSCompiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import com.j2js.Log;
import com.j2js.assembly.ClassUnit;
import com.j2js.assembly.ProcedureUnit;
import com.j2js.assembly.Project;
import com.j2js.assembly.Signature;
import com.j2js.dom.*;
import com.j2js.Utils;

/**
 * @author j2js
 */
public class JavaScriptGenerator extends Generator {

    private final static String DEFINEMETHOD = "dM";
    private final static String DEFINESTATICMETHOD = "dSM";
    private final static String INVOKE = "i";
    private final static String INVOKESUPER = "iSu";
    private final static String INVOKESPECIAL = "iSp";
    private final static String INVOKESTATIC = "iSt";
    private final static String DEFINECLASS = "dfC";
    private final static String NEWINSTANCE = "nI";
    private final static String STATICFIELDREF = "sFR";
    private final static String prefix = "_.";
    
    
    private boolean isFinal;
    
    
    private ASTNode currentNode;
    
    // The code of the currently active method declaration.
    private MethodDeclaration currentMethodDeclaration;
    private Project project;
    
    private ByteArrayOutputStream baStream = new ByteArrayOutputStream();
    
    /** Creates a new instance of Generator */
    public JavaScriptGenerator(Project theProject) {
        super();
        project = theProject;
        setOutputStream(new PrintStream(baStream));
    }
    
    private String reset() {
        flush();
        String s = baStream.toString();
        baStream.reset();
        return s;
    }
    
    private void consume(Object object) {
        object = object==null?object:object;
    }

    /**
     * This method must be called first because it sets the global className.
     */
    public void visit(TypeDeclaration theTypeDecl) {
        //Logger logger = Logger.getLogger();
        //logger.info("Generating JavaScript for " + typeDecl);
        lastChar = '\0';
        currentNode = null;
        depth = 0;
        
        typeDecl = theTypeDecl;
        isFinal = Modifier.isFinal(theTypeDecl.getAccess());
        
        println("var _C = function() {");
        depth++;
        
        List fields = theTypeDecl.getFields();
		for (int i=0; i<fields.size(); i++) {
			VariableDeclaration decl = (VariableDeclaration) fields.get(i);
			// Do not generate static field declaration.
			if (Modifier.isStatic(decl.getModifiers())) continue;
			indent();
			decl.visit(this);
			println(";");
		}
        
		depth--;
		println("}");

        String superType = null;
        // TODO Interface: Interfaces must not have supertype.
        if (theTypeDecl.getSuperType() != null && !Modifier.isInterface(theTypeDecl.getAccess())) {
            superType = Project.getSingleton().getSignature(theTypeDecl.getSuperType().getClassName()).getCommentedId();
        }
        
        print("var _T = j2js.");
        print(DEFINECLASS);
        print("(");
        print(Project.getSingleton().getSignature(theTypeDecl.getClassName()).getCommentedId());
        println(", _C, " + superType + ");");
        
	    // Generate static initializer.
	    println("{");
	    
	    depth++;
		for (int i=0; i<fields.size(); i++) {
			VariableDeclaration decl = (VariableDeclaration) fields.get(i);
			// Only generate static field declaration.
			if (!Modifier.isStatic(decl.getModifiers())) continue;
			indent();
			decl.visit(this);
			println(";");

		}

//		if (typeDecl.getInitializer() != null) {
//		    Block body = type.getInitializer().getBody();
//			if (body.getLastChild() instanceof ReturnStatement) {
//			    body.removeChild(body.getLastChild());
//			}
//			visit_(body);
//		}
		
		depth--;
		println("}");

        ClassUnit classUnit = project.getClassUnit(theTypeDecl.getType());
        classUnit.setData(reset());
        
        
        MethodDeclaration[] methods = theTypeDecl.getMethods();
        
        for (int i=0; i<methods.length; i++) {
            MethodDeclaration method =methods[i];
            currentMethodDeclaration = method;
            try {
                method.visit(this);
            } catch (RuntimeException ex) {
                throw Utils.generateException(ex, method, currentNode);
            }
        }
        
    }
    
    /**
     */
    public void visit(MethodDeclaration method) {        
        MethodBinding methodBinding = method.getMethodBinding();
        ProcedureUnit unit = project.getProcedureUnit(methodBinding);
        
        // Do not generate abstract or native methods.
        if (method.getBody() == null) {
            if (Modifier.isNative(method.getAccess()) || Modifier.isAbstract(method.getAccess())
            		|| Modifier.isInterface(typeDecl.getAccess())) {
            	return;
            }
            throw new RuntimeException("Method " + method + " with access " + method.getAccess() 
            		+ " may not have empty body");
        }

        if (!J2JSCompiler.compiler.isCompression()) {
            println("/* " + unit.getAbsoluteSignature() + " */");
        }

        String closingString;
        Signature signature = Project.getSingleton().getSignature(methodBinding.toString()).relative();
        
        if (typeDecl.getClassName().equals("java.lang.String") && method.isInstanceConstructor()) {
            // All String constructors are converted to methods returning the argument to the last
            // method invocation.
            
            Block body = method.getBody();
            // Remove call to super constructor.
            body.removeChild(body.getFirstChild());
            
            // Replace call to consume method by returning its argument.
            MethodInvocation consume = (MethodInvocation) body.getLastChild();
            body.removeChild(consume);
            
            ReturnStatement r = new ReturnStatement(0, 0);
            r.setExpression((Expression) consume.getArguments().get(0));
            body.appendChild(r);
            
            print("j2js.StringInit" + signature.getId() + " = function(");
            closingString = "};";
        } else {
            print("_T.");
            if (Modifier.isStatic(method.getAccess())) {
                print(DEFINESTATICMETHOD);
    		} else {
                print(DEFINEMETHOD);
    		}
            print("(");
            print("\"" + signature + "\"");
            print(", " + signature.getId());
            print(", function(");
            closingString = "});";
        }
        
        Iterator<VariableDeclaration> iterator = method.getParameters().iterator();
		while (iterator.hasNext()) {
			VariableDeclaration decl = iterator.next();
			decl.visit(this);
			print(iterator.hasNext() ? ", " : "");
		}
		
		println(") {");
		
		depth = 1;

		// prefix must be _.
        println("var _=j2js;");
        
		// Generate local variable declarations.
		for (VariableDeclaration decl : method.getLocalVariables()) {
		    indent();
			decl.visit(this);
		    println(";");
		}
		
        depth = 0;
        
        visit_(method.getBody());
        
        println(closingString);

        unit.setData(reset());
        Log.getLogger().debug("Generating JavaScript for " + unit);
    }
    
    public void visit(DoStatement doStmt) {
        println("do {");
        visit_(doStmt.getBlock());
        indent("} while (");
        doStmt.getExpression().visit(this);
        print(")");
    }
    
    public void visit(WhileStatement whileStmt) {
        print("while (");
        whileStmt.getExpression().visit(this);
        println(") {");
        visit_(whileStmt.getBlock());
        indent("}");
    }
    
    public void visit(IfStatement ifStmt) {
    	print("if (");
        ifStmt.getExpression().visit(this);
        println(") {");
        visit_(ifStmt.getIfBlock());
        indent("}");
        if (ifStmt.getElseBlock()!=null) {
            println(" else {");
            visit_(ifStmt.getElseBlock());
            indent("}");
        }
    }
    
    /**
     * A try statement has the following structure:
     *     TryStatement :
	 *		  try Block Catch* (Catch | Finally)
	 *
     * Because ECMAScript3 does not support multiple catch clauses, we will generate the following code.
     *     TryStatement :
     *         try Block catch(ex) { Catch } Finally?
     *		   try Block catch(ex) { if (ex ...) { Catch } else if(ex ...) ... else throw ex } Finally?
     * 
     */
    public void visit(TryStatement tryStmt) {
    	println("try {");
        visit_(tryStmt.getTryBlock());
        indent("} ");
        Block clauses = tryStmt.getCatchStatements();
        CatchClause clause = (CatchClause) clauses.getFirstChild();
        
        // Note: We have to declare variable 'ex_' because we cannot use method local variables due to
        // a JavaScript bug in some clients: var l0 = "foo"; try { ... } catch (l0) { /* Now l0 == "foo" */ }
        String ex = null;
        if (clause != null) {
            ex = clause.getException().getName();
        }
        
        if (clauses.getChildCount()==1) {
            print("catch(" + ex + ") ");
            clause.visit(this);
        } else if (clauses.getChildCount()>1) {
            println("catch(" + ex + ") {");
	        depth++;
	        indent();
	        while (clause != null) {
                if (clause.getException().getType() != null)
                    print("if (" + prefix + "isInstanceof(" + ex 
                            + ", \"" + Utils.getSignature(clause.getException().getType()) + "\")) ");
	            clause.visit(this);
                clause = (CatchClause) clause.getNextSibling();
                if (clause == null) break;
                print(" else ");
	        }
            println("");
	        depth--;
	        indent("}");
        }
        
        Block finallyBlock = tryStmt.getFinallyBlock();
        if (finallyBlock != null) { // There is a finally clause.
            println(" finally {");
            visit_(finallyBlock);
            indent("}");
        }
    }
    
    public void visit(CatchClause clause) {
        visit((Block) clause);
    }
    
    public void visit_(Block block) {
        depth++;
        ASTNode node = block.getFirstChild();
        while (node != null) {
            currentNode = node;
            if (J2JSCompiler.compiler.isGenerateLineNumbers()) {
                int lineNumber = currentMethodDeclaration.getLineNumberCursor().getAndMarkLineNumber(node);
                if (lineNumber != -1) {
                    print(prefix + "ln=" + lineNumber + ";\n");
                }
            }
            
            indent();
            if (node instanceof Block && ((Block) node).isLabeled()) {
                print(((Block) node).getLabel() + ": ");
            }

            node.visit(this);
            
            if (lastChar == '}') {
                println("");
            } else {
                println(";");
            }
            node = node.getNextSibling();
        }
        depth--;
    }
    
    public void visit(Block block) {
    	println("{");
    	visit_(block);
    	indent("}");
    }
    
    public void visit(SynchronizedBlock block) {
        println("{ // Synchronized.");
        visit_(block);
        indent("}");
    }
    
    public void visit(PrefixExpression binOp) {
  		print(binOp.getOperator().toString() + "(");
  		binOp.getOperand().visit(this);
  		print(")");
    }
    
    public void visit(PostfixExpression binOp) {
  		// Note that we do not need parenthese here.
  		binOp.getOperand().visit(this);
  		print(binOp.getOperator().toString());
    }

    private void bracket(ASTNode node, InfixExpression.Operator op) {
        if ((node instanceof InfixExpression && ((InfixExpression) node).getOperator()==op)
        		||
				node instanceof NumberLiteral
				|| 
				node instanceof NullLiteral
                ||
                node instanceof FieldAccess
                ||
                node instanceof VariableBinding) {
            node.visit(this);
        } else {
            print("(");
            node.visit(this);
            print(")");
        }
    }
    
    public void visit(InfixExpression binOp) {
        InfixExpression.Operator op = binOp.getOperator();
        Expression left = binOp.getLeftOperand();
        Expression right = binOp.getRightOperand();
        
        boolean isTruncate = false;
        Type type = binOp.getTypeBinding();
        
        /* There is no integral type division in ECMAScript, so we need to
           truncate the result.
           Note that the % operation works the same in ECMAScript and Java,
           i.e. in accepting floating-points.
         */
        if (op == InfixExpression.Operator.DIVIDE && 
                (type.equals(Type.LONG) || type.equals(Type.INT))) {
            isTruncate = true;
            print(" " + prefix + "trunc(");
        }
        
        bracket(left, op);
        print(" " + op + " ");
        bracket(right, op);
        
        if (isTruncate) {
            print(")");
        }
    }
    
    public void visit(ConditionalExpression ce) {
        ce.getConditionExpression().visit(this);
        print("?");
        ce.getThenExpression().visit(this);
        print(":");
        ce.getElseExpression().visit(this);
    }
    
    public void visit(InstanceofExpression node) {
        print(prefix + "isInstanceof(");
        node.getLeftOperand().visit(this);
        print(", \"");
        Signature signature = Project.getSingleton().getArraySignature(node.getRightOperand());
        print(signature.toString());
        print("\")");
    }
    
    public void visit(SwitchStatement switchStmt) {
    	print("switch (");
    	switchStmt.getExpression().visit(this);
    	println(") {");
    	ASTNode node = switchStmt.getFirstChild();
    	while (node != null) {
    		SwitchCase sc = (SwitchCase) node;
    		sc.visit(this);
    		node = node.getNextSibling();
    	}
    	indentln("}");
    }
    
    public void visit(SwitchCase switchCase) {
    	Iterator<NumberLiteral> iter = switchCase.getExpressions().iterator();
        if (iter.hasNext()) {
            while (iter.hasNext()) {
                NumberLiteral expression = iter.next();
                indent("case ");
                expression.visit(this);
                println(":");
            }
        } else {
            indentln("default:");
        }
        visit_(switchCase);	// Generate switchCase as block!
    }
    
    public void visit(ASTNode stmt) {
        print(stmt.toString());
    }
    
    public void visit(ReturnStatement r) {
    	print("return");
    	if (r.getExpression() != null) {
    		print(" ");
    		r.getExpression().visit(this);
    	}
    }
    
    public void visit(Assignment a) {
        Expression rhs = a.getRightHandSide();
        
        if (rhs instanceof ClassInstanceCreation) {
            ClassInstanceCreation cic = (ClassInstanceCreation) rhs;
            if (cic.getTypeBinding().toString().equals("java.lang.String")) {
                // Do not generate String creation.
                // TODO: Move this to the optimize phase.
                return;
            }
        }

        a.getLeftHandSide().visit(this);
        print(" " + a.getOperator() + " ");
        if (VariableBinding.isBoolean(a.getLeftHandSide())) {
    		if (NumberLiteral.isZero(rhs)) {
    			print("false");
    		} if (NumberLiteral.isOne(rhs)) {
    			print("true");
    		} else {
                rhs.visit(this);
            }
        } else {
            rhs.visit(this);
        }
    }
    
    public void visit(NumberLiteral literal) {
        print("" + literal.getValue());
    }
    
    public void visit(StringLiteral literal) {
    	print(Utils.escape(literal.getValue()));
    }
    
    public void visit(ClassLiteral literal) {
        MethodBinding binding = MethodBinding.lookup("java.lang.Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        MethodInvocation mi = new MethodInvocation(currentMethodDeclaration, binding);
        mi.addArgument(new StringLiteral(literal.getSignature().toString()));
        visit(mi);
     }
    
    public void visit(NullLiteral literal) {
        consume(literal);
        print("null");
    }
    
    private void generateList(List arguments) {
    	for (int i=0; i<arguments.size(); i++) {
    		print(i==0?"":", ");
    		((ASTNode) arguments.get(i)).visit(this);
    	}
    }
    

	/*
	 We have to substitute each setter and getter method by the appropriate field access.
	 According the first 3 characters and the number of arguments of the method (set*(arg) for setter, get*() for getter),
	 we can deduce whether we are looking at a setter/getter, with the following exceptions: 
	     NamedNodeMap.setNamedItem(arg)
	     NamedNodeMap.setNamedItemNS(arg)
	     Element.setAttributeNode(arg)
	     Element.setAttributeNodeNS(arg)
	 */
    private boolean isW3C(MethodInvocation invocation) {
        MethodBinding methodBinding = invocation.getMethodBinding();
        
        String name = methodBinding.getName();
        int argCount = invocation.getArguments().size();
    	boolean isSetter = name.startsWith("set") && argCount==1;
    	boolean isGetter = name.startsWith("get") && argCount==0;
    	
        if (!isSetter && !isGetter) return false;
    	
        if (methodBinding.equals("org.w3c.dom5.NamedNodeMap")) {
            if (name.equals("setNamedItemNS") || name.equals("setNamedItem")) return false;
        }
    	if (methodBinding.equals("org.w3c.dom5.Element")) {
            if (name.equals("setAttributeNode") || name.equals("setAttributeNodeNS")) return false;
        }
        // TODO: Also check class name.
        if (name.equals("getContentDocument")) return false;
        if (name.equals("getButton")) return false;
        
        return true;

	}
    
    private void generateScriptCode(MethodInvocation invocation) {
        MethodBinding methodBinding = invocation.getMethodBinding();
        String name = methodBinding.getName();
        List args = invocation.getArguments();
        if (!(args.get(0) instanceof StringLiteral)) {
            throw new RuntimeException("First argument to " + methodBinding + " must be a string literal");
        }
        String firstArg = ((StringLiteral) args.get(0)).getValue();
        
        if (name.equals("put")) {
            // In the HTML document object model the window property of the global object
            // is the global object itself.
            // NOTE: The key must be a string literal for this to work!
            if (firstArg.indexOf('.') == -1) {
                print("var ");
            }
            print(firstArg + "=");
            ((ASTNode) args.get(1)).visit(this);
        } else if (name.startsWith("eval")) {
            print(firstArg);
        } else
            throw new IllegalArgumentException("Cannot handle method " + name);
    }
    
    private void generateArguments(MethodInvocation invocation) {
        MethodBinding methodBinding = invocation.getMethodBinding();
        Signature signature = Project.getSingleton().getSignature(methodBinding.getDeclaringClass().getClassName());
        print(signature.getCommentedId());
        print(", ");
        signature = Project.getSingleton().getSignature(methodBinding.getRelativeSignature());
        print(signature.getCommentedId());
        
        print(", [");
        generateList(invocation.getArguments());
        print("]");
    }
    
    public void visit(MethodInvocation invocation) {
        MethodBinding methodBinding = invocation.getMethodBinding();
        String name = methodBinding.getName();
        String className = methodBinding.getDeclaringClass().getClassName();
        
        if (className.equals("javascript.ScriptHelper")) {
            generateScriptCode(invocation);
            return;
        }
        
        if (className.equals("javax.script.ScriptEngine") &&
                (name.equals("put") || name.equals("eval"))) {
            generateScriptCode(invocation);
            return;
        }
        
        ASTNode expression = invocation.getExpression();
        // TODO: Runtime check on null for expression in all cases.
        
//        if (className.startsWith("[")) {
//            if (methodBinding.getRelativeSignature().equals("clone()")) {
//                // TODO: Don't do nothing; Clone the array.
//                expression.visit(this);
//            } else {
//                //throw new UnsupportedOperationException("Invocation of " + methodBinding + " not supported");
//            }
//            //return;
//        }
        
        if (className.equals("java.lang.String") && methodBinding.isConstructor()) {
            if (expression instanceof VariableBinding) {
                expression.visit(this);
                print(" = ");
            } else {
                assert expression instanceof ClassInstanceCreation;
            }
            
            Signature signature = Project.getSingleton().getSignature(methodBinding.toString()).relative();
            print("j2js.StringInit" + signature.getId() + "(");
            generateList(invocation.getArguments());
            print(")");
            return;
        }

        if ( className.startsWith("org.w3c.dom5.") && !className.equals("org.w3c.dom5.views.Window") ) {
            if (name.equals("addEventListener")) {
                // Example: createDelegate(tr, "click", listener, false);
                print(prefix + "createDelegate(");
                expression.visit(this);
                print(", ");
                generateList(invocation.getArguments());
                print(")");
                return;
            }
            
            if (name.equals("removeEventListener")) {
                // Example: createDelegate(tr, "click", listener, false);
                print(prefix + "removeDelegate(");
                expression.visit(this);
                print(", ");
                generateList(invocation.getArguments());
                print(")");
                return;
            }
            
            if (name.equals("setProperty")) {
                expression.visit(this);
                List args = invocation.getArguments();
                
                ASTNode property = (ASTNode) args.get(0); 
                if (property instanceof StringLiteral) {
                    print(".");
                    print(((StringLiteral) property).getValue());
                } else {
                    print("[");
                    property.visit(this);
                    print("]");
                }
                print(" = ");
                ((ASTNode) args.get(1)).visit(this);
                return;
            }
            
            if (name.equals("getPropertyValue")) {
                expression.visit(this);
                List args = invocation.getArguments();
                print(".");
                ASTNode property = (ASTNode) args.get(0); 
                if (property instanceof StringLiteral) {
                    print(((StringLiteral) property).getValue());
                } else {
                    print("]");
                    property.visit(this);
                    print("]");
                }
                return;
            }
                       
            if (isW3C(invocation)) {
                String property = name.substring(3);
                property = property.substring(0, 1).toLowerCase() + property.substring(1);
                
                //if (methodBinding.equals("org.w3c.dom.html.HTMLFrameElement") && property.equals("contentDocument"))
                //  property = "document";
                
                if (name.startsWith("get")) {
                    FieldAccess fa = new FieldRead();
                    fa.setName(property);
                    fa.setExpression((Expression) expression);
                    visit(fa);
                } else {
                    expression.visit(this);
                    print("." + property + " = ");
                    ((ASTNode) invocation.getArguments().get(0)).visit(this);
                }
            } else if (name.equals("getContentDocument")) {
                print(prefix + "getContentDocument(");
                expression.visit(this);
                print(")");
            } else {
                print(prefix + "cn(");
                expression.visit(this);
                print(")");
                print("." + name + "(");
                generateList(invocation.getArguments());
                print(")");
            }
            return;
        }
        
        if (invocation.isSuper(typeDecl.getClassName())) {
            print(prefix);
            print(INVOKESUPER);
            print("(");
            if (expression == null) {
                print("null");
            } else {
                expression.visit(this);
            }
            print(", ");
        } else if(invocation.isSpecial) {
              print(prefix);
              print(INVOKESPECIAL);
              print("(");
              //print(getSignatureReference(Signature.getSignature(className)));
              //print(", ");
              expression.visit(this);
              print(", ");
        } else if(expression == null) {
            print(prefix);
            print(INVOKESTATIC);
            print("(");
        } else {
            print(prefix);
            print(INVOKE);
            print("(");
            expression.visit(this);
            print(", ");
        }
        
        generateArguments(invocation);
        print(")");
	}
    
    public void visit(ClassInstanceCreation cic) {
        print(prefix);
        print(NEWINSTANCE);
        print("(");
        print(Project.getSingleton().getSignature(((ObjectType) cic.getTypeBinding()).getClassName()).getCommentedId());
        
        if (cic.getMethodBinding() != null) {
            // We never get here!
            print(", ");
            generateArguments(cic);
        }
        
        print(")");
    }
    
    public void visit(ArrayInitializer ai) {
    	print("[");
    	for (int i=0; i<ai.getExpressions().size(); i++) {
    		print(i==0?"":", ");
    		((ASTNode) ai.getExpressions().get(i)).visit(this);
    	}
    	print("]");
    }
    
    public void visit(ArrayCreation ac) {
    	if (ac.getDimensions().size()<=0) {
    		throw new RuntimeException("Expected array dimension > 0, but was" + ac.getDimensions().size());
    	}
    	
		if (ac.getInitializer() != null) {
    		ac.getInitializer().visit(this);
    	} else {
    		print("j2js.newArray('");
            Signature signature = Project.getSingleton().getArraySignature(ac.getTypeBinding());
            print(signature.toString());
            print("', [");
    		for (int i=0; i<ac.getDimensions().size(); i++) {
    			print(i==0?"":", ");
    			ac.getDimensions().get(i).visit(this);
    		}
    		print("])");
    	}
    }
    
    public void visit(ArrayAccess aa) {
    	aa.getArray().visit(this);
    	print("[");
    	aa.getIndex().visit(this);
    	print("]");
    }
    
    private String normalizeAccess(String name) {
        if (!name.matches("\\w*")) {
            // Name contains non-word characters, for example generated by AspectJ.
            return "[\"" + name + "\"]";
        }
        return "." + name;
    }
    
    public void visit(VariableDeclaration decl) {
    	if (decl.getLocation() == VariableDeclaration.LOCAL_PARAMETER) {
            print(decl.getName());
            return;
    	}
    	
        if (decl.getLocation() == VariableDeclaration.NON_LOCAL) {
        	if (Modifier.isStatic(decl.getModifiers())) {
                print("_T.constr.prototype");
        	} else {
        		print("this");
        	}
            print(normalizeAccess(decl.getName()));
        } else {
            if (decl.getLocation() != VariableDeclaration.LOCAL)
                throw new RuntimeException("Declaration must be local");
        	print("var " + decl.getName());
        }

        if (!decl.isInitialized()) return;
        
        print(" = ");
        
        switch (decl.getType().getType()) {
			case Constants.T_INT:
			case Constants.T_SHORT:
			case Constants.T_BYTE:
			case Constants.T_LONG:
			case Constants.T_DOUBLE:
			case Constants.T_FLOAT:
			case Constants.T_CHAR: 
			    print("0");
				break;
			case Constants.T_BOOLEAN: 
			    print("false");
				break;
			default:
			    print("null");
			break;
		}
    }
    
    public void visit(VariableBinding reference) {
//        if (!reference.isField()) {
//        	print("l");
//        }
        print(reference.getName());
    }
    
    public void visit(ThisExpression reference) {
        consume(reference);
        print("this");
    }
    
    public void visit(FieldAccess fr) {
    	ASTNode expression = fr.getExpression();
        if (expression == null) {
            // Static access.
            print(prefix);
            print(STATICFIELDREF);
            print("(" + Project.getSingleton().getSignature(fr.getType().getClassName()).getCommentedId());
            print(")");
        } else if (expression instanceof ThisExpression) {
            expression.visit(this);
        } else {
            print(prefix + "cn(");
            expression.visit(this);
            print(")");
        }
        
        print(normalizeAccess(fr.getName()));
    }

    public void visit(BreakStatement stmt) {
        print("break");
        if (stmt.getLabel() != null) {
            print(" " + stmt.getLabel());
        }
    }
    
    public void visit(ContinueStatement stmt) {
        print("continue");
        if (stmt.getLabel() != null) {
            print(" " + stmt.getLabel());
        }
    }
    
    public void visit(CastExpression cast) {
        if (false && cast.getTypeBinding() != Type.VOID) { // && J2JSCompiler.compiler.isCheckCast()) {
            print(prefix + "checkCast(");
            cast.getExpression().visit(this);
            print(",'" + cast.getTypeBinding().toString() + "')");
        } else {
            // TODO: Is it correct to remove casts to void (i.e. pop)?
            //print("void ");
            cast.getExpression().visit(this);
        }
    }
    
    public void visit(BooleanLiteral be) {
    	print(Boolean.toString(be.getValue()));
    }
     
    public void visit(ThrowStatement node) {
        print("throw j2js.nullSaveException(");
        node.getExpression().visit(this);
        print(")");
    }
    
    public void visit(Name node) {
        if (false && node.getIdentifier().equals("javascript.Global")) {
    	    print("self");
    	} else {
    	    print(node.getIdentifier());
    	}
    }
    
    public void visit(PrimitiveCast node) {
        // TODO: Review cast to long.
        Type type = node.getTypeBinding();
        if (type.equals(Type.LONG)) {
            print(prefix + "trunc(");
            node.getExpression().visit(this);
            print(")");
        } else if (type.equals(Type.INT)) {
            print(prefix + "narrow(");
            node.getExpression().visit(this);
            print(", 0xffffffff)");
        } else if (type.equals(Type.SHORT)) {
            print(prefix + "narrow(");
            node.getExpression().visit(this);
            print(", 0xffff)");
        } else if (type.equals(Type.BYTE)) {
            print(prefix + "narrow(");
            node.getExpression().visit(this);
            print(", 0xff)");
        } else node.getExpression().visit(this);
    }
        
}