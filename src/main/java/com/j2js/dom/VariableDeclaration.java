package com.j2js.dom;

import com.j2js.J2JSCompiler;

import java.util.List;
import java.util.ArrayList;

import com.j2js.visitors.AbstractVisitor;

import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

/**
 * @author wolfgang
 */
public class VariableDeclaration extends ASTNode {
	public static int LOCAL = 0;
	public static int NON_LOCAL = 1;
	public static int LOCAL_PARAMETER = 2;
    
    private String name;
	private Type type;
	private int modifiers;
	private int location;
	private boolean isInitialized;
    public List<VariableBinding> vbs = new ArrayList<VariableBinding>();
    
    public static String getLocalVariableName(Method method, int slot, int pc) {
        if (false && method.getLocalVariableTable() != null) {
            // TODO: Use source variable name as comment only.
            LocalVariable[] table = method.getLocalVariableTable().getLocalVariableTable();
            LocalVariable lvar = null;
            for (int i=0; i<table.length; i++) {
                lvar = table[i];
                if (lvar.getIndex() == slot 
                        && lvar.getStartPC() <= pc && pc <= lvar.getStartPC()+lvar.getLength()) {
                    return lvar.getName();
                }
            }
            // if (name.equals("this")) return "l" + slot;
        }
        return "l" + slot;
    }
	
	public VariableDeclaration(boolean theIsInitialized) {
	    location = VariableDeclaration.LOCAL;
	    isInitialized = theIsInitialized;
	}
	
	public VariableDeclaration(int theLocation) {
	    if (theLocation != VariableDeclaration.NON_LOCAL && theLocation != VariableDeclaration.LOCAL_PARAMETER) {
	    	throw new RuntimeException("Illegal location specified: " + theLocation);
	    }
		location = theLocation;
	    isInitialized = (theLocation == VariableDeclaration.NON_LOCAL);
	}
	
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
	
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param theName The name to set.
	 */
	public void setName(String theName) {
		name = theName;
	}
	/**
	 * @return Returns the modifiers.
	 */
	public int getModifiers() {
		return modifiers;
	}
	/**
	 * @param theModifiers The modifiers to set.
	 */
	public void setModifiers(int theModifiers) {
		modifiers = theModifiers;
	}
    /**
     * @return Returns the type.
     */
    public Type getType() {
        return type;
    }
    /**
     * @param theType The type to set.
     */
    public void setType(Type theType) {
        type = theType;
    }
    /**
     * @return Returns the location.
     */
    public int getLocation() {
        return location;
    }
	/**
	 * Returns true if the variable is initialized during declaration.
	 */
	public boolean isInitialized() {
		return isInitialized;
	}
}
