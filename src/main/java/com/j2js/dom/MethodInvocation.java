package com.j2js.dom;

import java.util.List;

import org.apache.bcel.generic.Type;

import com.j2js.ASTNodeStack;
import com.j2js.assembly.Project;
import com.j2js.visitors.AbstractVisitor;

/**
 * @author kuehn
 */
public class MethodInvocation extends Expression {
	
	private Expression expression;
    private MethodDeclaration methodDecl;

	/**
     * Special handling for superclass, private, and instance initialization method invocations.
	 */
	public boolean isSpecial = false;
	private MethodBinding methodBinding;
    
    public MethodInvocation() {
    }
    
    public MethodInvocation(MethodDeclaration theMethodDecl) {
        methodDecl = theMethodDecl;
    }
    
    public MethodInvocation(MethodDeclaration theMethodDecl, MethodBinding theMethodBinding) {
        methodDecl = theMethodDecl;
        setMethodBinding(theMethodBinding);
    }
    
	public Type getTypeBinding() {
        if (methodBinding == null) return super.getTypeBinding();
        return methodBinding.getReturnType();
    }
    
    /**
     * Returns true if this method invocation applies to a super class of the specified class.
     */
    public boolean isSuper(String currentClassName) {
        if (!isSpecial) return false;
        // Use resolved class unless
        // 1) the resolved method is not an instance initialization method,
        if (methodBinding.isConstructor()) return false;
        // 2) and the class of the resolved method is a superclass of the current class and
        String name = methodBinding.getDeclaringClass().getClassName();
        if (currentClassName.equals(name)) return false;
        
        // TODO: The resolved class is different from the current class, but this does not imply that
        // the resolved class is a superclass! Problem: How do we get this information without loading
        // the class hierarchy? 
        return true;
    }

    /**
	 * @return Returns the arguments.
	 */
	public List getArguments() {
        ASTNodeStack stack = new ASTNodeStack();
        ASTNode node = getFirstChild();
        if (expression != null) {
            node = node.getNextSibling();
        }
        
        while (node != null) {
            stack.add(node);
            node = node.getNextSibling();
        }
        
        return stack;
        
	}

	/**
	 * @param arguments The arguments to set.
	 */
	public void addArgument(Expression argument) {
		widen(argument);
		appendChild(argument);
	}

	/**
	 * @return Returns the expression.
	 */
	public Expression getExpression() {
		return expression;
	}

	/**
	 * @param expression The expression to set.
	 */
	public void setExpression(Expression targetExpression) {
		if (expression != null) {
			throw new RuntimeException("Expression is already set");
		}
		expression = targetExpression;
		widen(expression);
		insertBefore(expression, getFirstChild());
	}

	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }

    public MethodBinding getMethodBinding() {
        return methodBinding;
    }

    public void setMethodBinding(MethodBinding theMethodBinding) {
        methodBinding = theMethodBinding;
        Project.getSingleton().addReference(methodDecl, this);
    }
}
