package com.j2js.assembly;

import com.j2js.J2JSCompiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import com.j2js.Log;
import com.j2js.Utils;
import com.j2js.dom.ArrayCreation;
import com.j2js.dom.FieldAccess;
import com.j2js.dom.MethodBinding;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.MethodInvocation;
import com.j2js.dom.TypeDeclaration;

public class Project implements Serializable {
    
    static final long serialVersionUID = 0;
    
    private static Project singleton;
    
    // All managed classes mapped by class name.
    private Map<String, ClassUnit> classesByName;
    private ClassUnit javaLangObject;
    private boolean compressed;
    private boolean generateLineNumbers;
    private Map<String, Signature> signatures;
    
    private transient Stack<Integer> ids;
    private transient int currentId;
    private transient int currentIndex;

    public transient int currentGeneratedMethods;
    
    public static Project getSingleton() {
        if (singleton == null) throw new NullPointerException();
        return singleton;
    }
    
    public static Project createSingleton(File cacheFile) {
        
        if (cacheFile != null && cacheFile.exists()) { 
            Log.getLogger().info("Using cache " + cacheFile);
            try {
                read(cacheFile);
            } catch (Exception e) {
                Log.getLogger().warn("Could not read cache:\n" + e.getMessage());
            }
        }
        
        if (singleton == null || 
                singleton.compressed != J2JSCompiler.compiler.isCompression() ||
                singleton.generateLineNumbers != J2JSCompiler.compiler.isGenerateLineNumbers()) {
            // Cache does not exist, could not be read, or compression does not match.
            singleton = new Project();
            singleton.clear();
        }

        return singleton;
    }
    
    private static void read(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        singleton = (Project) ois.readObject();
        ois.close();
    }
    
    public static void write() throws IOException {
        File file = J2JSCompiler.compiler.getCacheFile();
        if (file.exists() && !file.canWrite()) {
            throw new IOException("Cannot write " + file);
        }
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(singleton);
        oos.close();
    }
    
    public Signature getArraySignature(Type type) {
        String signatureString = type.getSignature();
        /* Examples:
         * L[java.lang.Integer;; -> [java.lang.Integer;
         * L[I; -> [I
         */
        if (!signatureString.startsWith("L") || !signatureString.endsWith(";")) {
            throw new RuntimeException("Not a class signature: " + signatureString);
        }
        signatureString = signatureString.substring(1, signatureString.length()-1);
        return getSignature(signatureString);
    }
    
    /**
     * All request for a signature delegate to this method.
     */
    public Signature getSignature(String signatureString) {
        if (signatureString.endsWith(";")) {
            //throw new RuntimeException("Invalid signature: " + signatureString);
        }
        signatureString = signatureString.replaceAll("/", ".");
        
        Signature signature = signatures.get(signatureString);
        if (signature == null) {
            signature = new Signature(signatureString, getUniqueId());
            signatures.put(signatureString, signature);
        }
        
        return signature;
    }
    
    public Signature getSignature(String className, String relativeSignature) {
        return getSignature(className + '#' + relativeSignature);
    }
    
    public Signature getSignature(FieldAccess fa) {
        return getSignature(fa.getType().getClassName(), fa.getName());
    }
    
    private int getUniqueId() {
        if (ids == null) {
            ids = new Stack<Integer>();
            for (Signature signature : signatures.values()) {
               ids.add(signature.getId());
            }
            Collections.sort(ids);
        }
        
        while (currentIndex<ids.size() && ids.get(currentIndex) == currentId) {
            currentId += 1;
            currentIndex += 1;
        }
        
        currentId++;
        return currentId - 1;
    }
    
    private void clear() {
        classesByName = new HashMap<String, ClassUnit>();
        javaLangObject = null;

        signatures = new HashMap<String, Signature>();
        ids = null;
        currentId = 0;
        currentIndex = 0;
        compressed = J2JSCompiler.compiler.isCompression();
        generateLineNumbers = J2JSCompiler.compiler.isGenerateLineNumbers();
    }
    
    public void remove(ClassUnit clazz) {
        classesByName.remove(clazz);
    }
    
    void visitSuperTypes(ClassUnit clazz, TypeVisitor visitor) {
        visitor.visit(clazz);
        ClassUnit superClass = clazz.getSuperUnit();
        if (superClass != null) {
            visitSuperTypes(superClass, visitor);
        }

        for (ClassUnit interfaceUnit : clazz.getInterfaces()) {
            visitor.visit(interfaceUnit);
            visitSuperTypes(interfaceUnit, visitor);
        }
    }
    
    public ClassUnit getJavaLangObject() {
        return javaLangObject;
    }

    public ClassUnit getClassUnit(String className) {
        ClassUnit clazz = classesByName.get(className);
        if (clazz != null) return clazz;
        
        throw new RuntimeException("No such unit: " + className);
    }
    
    public ClassUnit getClassUnit(ReferenceType type) {
        String signature;
        if (type instanceof ArrayType) {
            ArrayType aType = (ArrayType) type;
            signature = Utils.getSignature(aType.getBasicType());
            for (int i = 0; i<aType.getDimensions(); i++) {
                signature += "[]";
            }
        } else {
            signature = Utils.getSignature(type);
        }

        return getClassUnit(signature);
    }
    
    public ClassUnit getOrCreateClassUnit(String className) {
        ClassUnit classUnit = classesByName.get(className);
        if (classUnit != null) return classUnit;
        
        Signature signature = Project.singleton.getSignature(className);
        classUnit = new ClassUnit(this, signature);
        classesByName.put(className, classUnit);
        
        if (className.equals("java.lang.Object")) {
            javaLangObject = classUnit;
        }

        return classUnit;
    }

    private MemberUnit getMemberUnitOrNull(String className, Signature signature) {
        ClassUnit classUnit = getOrCreateClassUnit(className);
        if (classUnit == null) return null;
        return classUnit.getDeclaredMember(signature.toString());
    }
    
    private MemberUnit getMemberUnit(String className, Signature signature) {
        MemberUnit unit = getMemberUnitOrNull(className, signature);
        if (unit == null) {
            throw new RuntimeException("No such unit: " + className + "#"+ signature);
        }
        
        return unit;
    }
    
    public ProcedureUnit getProcedureUnit(MethodBinding methodBinding) {
        Signature signature = Project.singleton.getSignature(methodBinding.getRelativeSignature());
        String className = methodBinding.getDeclaringClass().getClassName();
        return (ProcedureUnit) getMemberUnit(className, signature);
    }
    
    public ProcedureUnit getOrCreateProcedureUnit(MethodBinding methodBinding) {
        Signature signature = Project.singleton.getSignature(methodBinding.getRelativeSignature());
        String className = methodBinding.getDeclaringClass().getClassName();
        return (ProcedureUnit) getOrCreateMemberUnit(className, signature);
    }
    
    private MemberUnit getOrCreateMemberUnit(String className, Signature signature) {
        MemberUnit member = getMemberUnitOrNull(className, signature);
                
        if (member == null) {
            ClassUnit clazz = getClassUnit(className);
            if (signature.isMethod()) {
                member = new MethodUnit(signature, clazz);
            } else if (signature.isConstructor()) {
                member = new ConstructorUnit(signature, clazz);
            } else {
                member = new FieldUnit(signature, clazz);
            }
            
            
        }
        
        return member;
    }
    
//    public Unit getOrCreateUnit(String signature) {
//        String array[] = signature.split("#");
//        Unit unit;
//        if (array.length == 1) {
//            unit = getOrCreateClassUnit(signature);
//        } else {
//            unit = getOrCreateMemberUnit(array[0], Project.singleton.getSignature(array[1]));
//        }
//        return unit;
//    }
    
    public FieldUnit getOrCreateFieldUnit(ObjectType type, String name) {
        return (FieldUnit) getOrCreateMemberUnit(type.getClassName(), Project.singleton.getSignature(name));
    }
    
    public void addReference(MethodDeclaration decl, FieldAccess fa) {
        ProcedureUnit source = getOrCreateProcedureUnit(decl.getMethodBinding());
        source.addTarget(Project.singleton.getSignature(fa));
    }
    
    public void addReference(MethodDeclaration decl, MethodInvocation invocation) {
        ProcedureUnit source = getOrCreateProcedureUnit(decl.getMethodBinding());
        source.addTarget(Project.singleton.getSignature(invocation.getMethodBinding().toString()));
    }
    
    public void addReference(MethodDeclaration decl, ArrayCreation ac) {
        ProcedureUnit source = getOrCreateProcedureUnit(decl.getMethodBinding());
        Signature signature = Project.getSingleton().getArraySignature(ac.getTypeBinding());
        for (int i = 0; i<ac.getDimensions().size(); i++) {
            // TODO: Target must be a field or method. Is length the right way?
            source.addTarget(Project.singleton.getSignature(signature.toString().substring(i)+"#length"));
        }
    }
    
    /**
     * Returns a live collection of all classes managed by this project.
     */
    public Collection<ClassUnit> getClasses() {
        return classesByName.values();
    }
    
    public void resolve(ClassUnit clazz) {
        if (clazz.isResolved()) return;
        
        if (clazz.getName().startsWith("[")) {
            // This is an array type and not a class.
            clazz.setSuperUnit(getJavaLangObject());
            //clazz.setTainted();
            clazz.setResolved(true);
            // We need a member (and we chose length) in addReference(..) to taint the class.
            new FieldUnit(getSignature("length"), clazz);
            
            TypeDeclaration typeDecl = new TypeDeclaration(new ObjectType(clazz.getName()), 0);
            typeDecl.setSuperType(Type.OBJECT);
            typeDecl.visit(J2JSCompiler.compiler.generator);
        } else {
            TypeResolver resolver = new TypeResolver(this, J2JSCompiler.compiler.generator);
            visitSuperTypes(clazz, resolver);
        }
    }
    
}
