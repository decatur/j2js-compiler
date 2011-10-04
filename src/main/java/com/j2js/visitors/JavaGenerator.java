package com.j2js.visitors;

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.Iterator;

import com.j2js.Log;
import com.j2js.dom.*;
import com.j2js.Utils;

/**
 * @author  kuehn
 */
public class JavaGenerator extends Generator {
    
    /** Creates a new instance of Generator */
    public JavaGenerator() {
    }
    
    public JavaGenerator(PrintStream targetStream) {
        setOutputStream(targetStream);
    }
   
    public void visit(ASTNode node) {}
    
    /**
     * This method must be call first because it sets the global className.
     */
    public void visit(TypeDeclaration theTypeDecl) {
        Log logger = Log.getLogger();
        logger.info("Generating Java for " + theTypeDecl);
        
        depth = 0;
        typeDecl = theTypeDecl;
        
        println("package " + theTypeDecl.getPackageName() + ";");
        println("");
        print("public ");
        if (Modifier.isFinal(theTypeDecl.getAccess())) print("final ");
            
        println("class " + theTypeDecl.getUnQualifiedName() + " {");
        
        depth++;
        
        MethodDeclaration[] methods = theTypeDecl.getMethods();
        for (int i=0; i<methods.length; i++) {
            MethodDeclaration method =methods[i];
            try {
                method.visit(this);
            } catch (RuntimeException ex) {
                throw Utils.generateException(ex, method, null);
            }
        }
        
        depth--;
        
        println("}");
        
    }
    
    /**
     */
    public void visit(MethodDeclaration method) {
        MethodBinding methodBinding = method.getMethodBinding();
        
        println("");
        indent("");
        
        if (Modifier.isPrivate(method.getAccess())) print("private ");
        if (Modifier.isProtected(method.getAccess())) print("protected ");
        if (Modifier.isPublic(method.getAccess())) print("public ");
        if (Modifier.isStatic(method.getAccess())) print("static ");
        if (Modifier.isNative(method.getAccess())) print("native ");
        if (Modifier.isAbstract(method.getAccess())) print("abstract ");
        if (Modifier.isFinal(method.getAccess())) print("final ");
        
        if (method.isInstanceConstructor()) {
            print(typeDecl.getUnQualifiedName());
        } else {
            print(methodBinding.getReturnType().toString() + " " + methodBinding.getName());
        }
        
        print("(");
        
        Iterator iterator = method.getParameters().iterator();
        while (iterator.hasNext()) {
            VariableDeclaration decl = (VariableDeclaration) iterator.next();
            decl.visit(this);
            print(iterator.hasNext() ? ", " : "");
        }
        
        print(")");
        
        if (Modifier.isNative(method.getAccess()) || Modifier.isAbstract(method.getAccess()))
            println(";");
        else
            println(" {}");
        
    }
    
    public void visit(VariableDeclaration decl) {
        print(decl.getType().toString() + " " + decl.getName());
    }
           
}