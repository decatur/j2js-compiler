package com.j2js;

import com.j2js.J2JSCompiler;

import java.io.IOException;
import java.lang.reflect.Modifier;

import com.j2js.Pass1;
import com.j2js.ParseException;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.AttributeReader;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import com.j2js.assembly.ClassUnit;
import com.j2js.assembly.Project;
import com.j2js.assembly.Signature;
import com.j2js.dom.*;

/**
 * @author wolfgang
 */
public class Parser {
    
    public static String getResourcePath(String name) {
        name = name.replace('.', '/') + ".class";
        java.net.URL url = Parser.class.getClassLoader().getResource(name);
        if (url == null) throw new RuntimeException("Resource not found: " + name);
        return url.getPath();
    }
    
    private JavaClass jc;
    private ClassUnit fileUnit;
   
    public Parser(ClassUnit theFileUnit) {
        fileUnit = theFileUnit;
        fileUnit.annotations = null;
        
        AttributeReader r = new AnnotationReader(fileUnit);
        Attribute.addAttributeReader("RuntimeVisibleAnnotations", r);
        
        try {
            ClassParser cp = new ClassParser(fileUnit.getClassFile().openInputStream(), fileUnit.getName());
            jc = cp.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public TypeDeclaration parse() {

//        Attribute[] attributes = jc.getAttributes();
//        for (int i=0; i<attributes.length; i++) {
//            Logger.getLogger().info(attributes[i].toString());
//        }
                
        org.apache.bcel.classfile.Method[] bcelMethods = jc.getMethods();
        
        ObjectType type = new ObjectType(jc.getClassName());
        TypeDeclaration typeDecl = new TypeDeclaration(type, jc.getAccessFlags());
        
        fileUnit.isInterface = Modifier.isInterface(typeDecl.getAccess());
        
        if (!type.getClassName().equals("java.lang.Object")) {
            // For an interface, the super class is always java.lang.Object, see 4.1 VM Spec.
            // TODO Interface: Why cant we ignore it?
            //if (!Modifier.isInterface(typeDecl.getAccess())) {
                ObjectType superType = new ObjectType(jc.getSuperclassName());
                typeDecl.setSuperType(superType);
                ClassUnit superUnit = Project.getSingleton().getOrCreateClassUnit(superType.getClassName());
                fileUnit.setSuperUnit(superUnit);
            //}
            
            // TODO: This should be executed also for java.lang.Object.
            String[] interfaceNames = jc.getInterfaceNames();
            for (int i=0; i<interfaceNames.length; i++) {
                ObjectType interfaceType = new ObjectType(interfaceNames[i]);
                ClassUnit interfaceUnit = Project.getSingleton().getOrCreateClassUnit(interfaceType.getClassName());
                fileUnit.addInterface(interfaceUnit);
            }
        }
        
        Field[] fields = jc.getFields();
        for (int i=0; i<fields.length; i++) {
            Field field = fields[i];
            VariableDeclaration variableDecl = new VariableDeclaration(VariableDeclaration.NON_LOCAL);
            variableDecl.setName(field.getName());
            variableDecl.setModifiers(field.getModifiers());
            variableDecl.setType(field.getType());
            
            typeDecl.addField(variableDecl);
        }
        
        for (int i=0; i<bcelMethods.length; i++) {
            Method method = bcelMethods[i];
            
            // Java 5 generates a "bridge synthetic" accessor method for some
            // methods used in a generic context (for example Comparator.compare(Object, Object)
            // will call Comparator.compare(String, String)). Those methods are essential!
            //if (Modifier.isVolatile(method.getAccessFlags())) continue;
            
            MethodBinding binding = MethodBinding.lookup(jc.getClassName(), method.getName(), method.getSignature());
            
            if (J2JSCompiler.compiler.getSingleEntryPoint() != null) {
                Signature signature = Project.getSingleton().getSignature(binding.toString());
                String singleSignature = J2JSCompiler.compiler.getSingleEntryPoint();
                if (!signature.toString().equals(singleSignature)) continue;
            }
            
            MethodDeclaration methodDecl = new MethodDeclaration(binding, method.getAccessFlags(), method.getCode());
            typeDecl.addMethod(methodDecl);

            parseMethod(typeDecl, methodDecl, method);
        }
        
        return typeDecl;
    }

    public void parseMethod(TypeDeclaration typeDecl, MethodDeclaration methodDecl, Method method) {
        Type[] types = method.getArgumentTypes();
        
        int offset;
        if (Modifier.isStatic(methodDecl.getAccess())) {
            offset = 0;
        } else {
            // Reference to this is first argument for member method.
            offset = 1;
        }
        for (int i=0; i<types.length; i++) {
        	VariableDeclaration variableDecl = new VariableDeclaration(VariableDeclaration.LOCAL_PARAMETER);
        	variableDecl.setName(VariableDeclaration.getLocalVariableName(method, offset, 0));
        	variableDecl.setType(types[i]);
        	methodDecl.addParameter(variableDecl);
            offset += types[i].getSize();
        }
        
        if (methodDecl.getCode() == null) return;
        
        Log.getLogger().debug("Parsing " + methodDecl.toString());
        Pass1 pass1 = new Pass1(jc);
        
        try {
            pass1.parse(method, methodDecl);
        } catch (Throwable ex) {
            ASTNode node = null;
            if (ex instanceof ParseException) {
                node = ((ParseException) ex).getAstNode();
            } else {
                node = Pass1.getCurrentNode();
            }

            if (J2JSCompiler.compiler.isFailOnError()) {
                throw Utils.generateException(ex, methodDecl, node);
            } else {
                String msg = Utils.generateExceptionMessage(methodDecl, node);
                J2JSCompiler.errorCount++;
                Log.getLogger().error(msg + "\n" + Utils.stackTraceToString(ex));
            }
            
            Block body = new Block();
            ThrowStatement throwStmt = new ThrowStatement();
            MethodBinding binding = MethodBinding.lookup("java.lang.RuntimeException", "<init>", "(java/lang/String)V;");
            ClassInstanceCreation cic = new ClassInstanceCreation(methodDecl, binding);
            cic.addArgument(new StringLiteral("Unresolved decompilation problem"));
            throwStmt.setExpression(cic);
            body.appendChild(throwStmt);
            methodDecl.setBody(body);

        }
        
        // Remove from body last expressionless return statement.
        if (J2JSCompiler.compiler.optimize && methodDecl.getBody().getLastChild() instanceof ReturnStatement) {
        	ReturnStatement ret = (ReturnStatement) methodDecl.getBody().getLastChild();
        	if (ret.getExpression() == null) {
        		methodDecl.getBody().removeChild(ret);
        	}
        }
        
        Pass1.dump(methodDecl.getBody(), "Body of " + methodDecl.toString());
        
//        if (typeDecl.getClassName().equals("java.lang.String")) {
//           if (methodDecl.isInstanceConstructor()) {
//               
//           }
//        }

        return;
    }
    
    public ConstantPool getConstantPool() {
        return jc.getConstantPool();
    }
    
    public String toString() {
    	return jc.getClassName();
    }
    
}
