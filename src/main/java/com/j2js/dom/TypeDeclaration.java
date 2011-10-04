/*
 * Created on 22.10.2004
 */
package com.j2js.dom;

import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.generic.ObjectType;

import com.j2js.assembly.Project;
import com.j2js.visitors.AbstractVisitor;


/**
 * @author kuehn
 */
public class TypeDeclaration extends ASTNode {
	
	private ObjectType type;
    private ObjectType superType;
    
	private ArrayList<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
	//private MethodDeclaration initializer;
	private List<VariableDeclaration> fields = new ArrayList<VariableDeclaration>();
    private int accessFlags;
    //private String superClassName;

    public TypeDeclaration(ObjectType theType, int theAccessFlags) {
        type = theType;
        accessFlags = theAccessFlags;
    }
    
    public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
	}
	
	/**
	 * @return Returns the methods.
	 */
	public MethodDeclaration[] getMethods() {
	    MethodDeclaration[] a = new MethodDeclaration[methods.size()];
	    return methods.toArray(a);
	}

	public int getAccess() {
        return accessFlags;
    }
    
    /**
	 * @param methods The methods to set.
	 */
	public void addMethod(MethodDeclaration method) {
	    method.setParentNode(this);
	    methods.add(method);
	}

	/**
	 * @return Returns the name.
	 */
	public ObjectType getType() {
		return type;
	}
	
	/**
	 * @return Returns the package portion of this types name.
	 */
	public String getPackageName() {
        String name = type.getClassName();
        int index = name.lastIndexOf('.');
        if (index != -1)
            return name.substring(0, index);
        else
            return name;
	}
    
    public String getClassName() {
        return type.getClassName();
    }
    
    public String getUnQualifiedName() {
        String name = type.getClassName();
        int index = name.lastIndexOf('.');
        if (index != -1)
            return name.substring(index+1);
        else
            return name;
    }

	/**
	 * @return Returns the fields.
	 */
	public List<VariableDeclaration> getFields() {
		return fields;
	}
	/**
	 * @param fields The fields to set.
	 */
	public void addField(VariableDeclaration field) {
		fields.add(field);
        Project.getSingleton().getOrCreateFieldUnit(type, field.getName());
	}
    /**
     * @return Returns the superClassName.
     */
    public ObjectType getSuperType() {
        return superType;
    }

    /**
     * Sets the super type.
     */
    public void setSuperType(ObjectType newSuperType) {
        superType = newSuperType;
    }
    
    public String toString() {
        return type.getClassName();
    }

}
