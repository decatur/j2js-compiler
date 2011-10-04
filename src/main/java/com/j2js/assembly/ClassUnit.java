package com.j2js.assembly;

import com.j2js.J2JSCompiler;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.j2js.FileObject;
import com.j2js.Log;

/**
 * Instances of ClassUnit represent classes and interfaces managed by a project.
 * 
 * @author wolle
 */
public class ClassUnit extends Unit {
    
    static final long serialVersionUID = 1;
    
    // Time stamp at which unit was last compiled.
    private long lastCompiled;

    /**
     * Returns the time stamp at which the class file was last modified.
     */
    public long getLastModified() {
        return getClassFile().getLastModified();
    }

    // All members declared by this class, mapped by relative signature.
    private Map<String, MemberUnit> declaredMembers;

    // The super class.
    private ClassUnit superUnit;
    
    // All interfaces implemented by this unit.
    private Collection<ClassUnit> interfaces;
    
    // All derived classes.
    private Collection<ClassUnit> subUnits;

    public boolean isInterface = false;
    
    public boolean isConstructorTainted = false;
    
    public Map<String, String>[] annotations;
    
    private Project project;
    
    // Transient fields start here.
    private transient boolean isResolved = false;
    
    // The class file of this unit.
    private transient FileObject classFile;
    
    public ClassUnit() {
    }
    
    public ClassUnit(Project theProject, Signature theSignature) {
        project = theProject;
        
        interfaces = new HashSet<ClassUnit>();
        declaredMembers = new HashMap<String, MemberUnit>();
        subUnits = new HashSet<ClassUnit>();
        lastCompiled = -1;
        setSignature(theSignature);
    }

    /**
     * Reset all fields filled by compilation.
     */
    void clear() {
        // Do not remove registered subunits!
        lastCompiled = -1;
        removeInterfaces();
        setSuperUnit(null);
        declaredMembers.clear();
    }
    
    /**
     * Returns true if this unit is up to date.
     */
    public boolean isUpToDate() {
        return lastCompiled >= getLastModified();
    }

    public Collection<ClassUnit> getInterfaces() {
        return interfaces;
    }

    public void addInterface(ClassUnit interfaceUnit) {
        interfaces.add(interfaceUnit);
        //interfaceUnit.addSubUnit(this);
    }
    
    private void removeInterfaces() {
        Iterator iter = interfaces.iterator();
        while (iter.hasNext()) {
            ClassUnit interfaceUnit = (ClassUnit) iter.next();
            interfaceUnit.removeSubUnit(this);
            iter.remove();
        }
    }
    
    public void addSubUnit(ClassUnit subUnit) {
        if (subUnit == null) throw new NullPointerException();
        subUnits.add(subUnit);
    }
    
    public void removeSubUnit(ClassUnit subUnit) {
        if (subUnit == null) throw new NullPointerException();
        subUnits.remove(subUnit);
    }

    public Collection<ClassUnit> getSubUnits() {
        return subUnits;
    }
    
    /**
     * Returns the declared member with the specified signature. 
     */
    public MemberUnit getDeclaredMember(String signature) {
        if (signature == null) throw new NullPointerException();
        return declaredMembers.get(signature);
    }
    
    /**
     * Returns all types to which this class can be converted, i.e., the collection of
     * all supertypes and implemented interfaces and the class itself.
     */
    public Collection<ClassUnit> getSupertypes() {
        TypeCollector collector = new TypeCollector();
        project.visitSuperTypes(this, collector);
        return collector.collectedTypes;
    }
    
    /**
     * Returns a member object that reflects the specified public member method of the class
     * or interface represented by this Class object.
     */
//    public MemberUnit getMember(String signature) {
//        if (signature == null) throw new NullPointerException();
//        
//        ClassUnit clazz = this;
//        do {
//            MemberUnit member = clazz.getDeclaredMember(signature);
//            if (member != null) return member;
//            clazz = clazz.getSuperUnit();
//        } while (clazz != null);
//        
//        return null;
//    }
    
    public Collection<MemberUnit> getMembers(String signature) {
        if (signature == null) throw new NullPointerException();
        ArrayList<MemberUnit> list = new ArrayList<MemberUnit>();
        
        for (ClassUnit clazz : getSupertypes()) {
            MemberUnit member = clazz.getDeclaredMember(signature);
            if (member != null) list.add(member);
        }
        
        return list;
    }
    
    public Collection<MemberUnit> getDeclaredMembers() {
        if (!isResolved) throw new RuntimeException("Class is not yet resolved: " + getName());
        return declaredMembers.values();
    }
    
    public void addMemberUnit(MemberUnit unit) {
        declaredMembers.put(unit.getSignature().toString(), unit);
    }

    public ClassUnit getSuperUnit() {
        return superUnit;
    }

    public void setSuperUnit(ClassUnit theSuperUnit) {
        if (superUnit != null) {
            superUnit.removeSubUnit(this);
        }
        superUnit = theSuperUnit;
        if (superUnit != null) {
            superUnit.addSubUnit(this);
        }
    }
   
    public void write(int depth, Writer writer) throws IOException {
        if (!isTainted()) return;
        
        Log.getLogger().debug(getIndent(depth) + this);

        if (getData() != null) {
            writer.write(getData());
        } else {
            // TODO: Is it correct to return so soon?
            return;
        }
        
        if (interfaces.size() > 0) {
            //Logger.getLogger().info("Class + " + this.getName() + " has interfaces: ");
            
            writer.write("_T.interfaces = [");
            int i = 0;
            for (ClassUnit interFace : interfaces) {
                //Logger.getLogger().info(">>>" + interFace.getName());
                if (i++ > 0) writer.write(", ");
                writer.write(String.valueOf(interFace.getSignature().getId()));
            }
            writer.write("];\n");
        }
        
        if (annotations != null) {
            writer.write("_T.annotations = ");
            Serializer serializer = new Serializer(writer);
            serializer.serialize(annotations);
            writer.write(";\n");
        }

        for (MemberUnit member : getDeclaredMembers()) {
            if (member.isTainted()) {
                member.write(depth + 1, writer);
                if (member instanceof ProcedureUnit) {
                    project.currentGeneratedMethods++;
                    writer.flush();
                }
            }
        }

        for (ClassUnit child : getSubUnits()) {
            // TODO Interface: Interfaces must not extend java.lang.Object!
            //if (!child.isInterface) {
                child.write(depth + 1, writer);
            //}
        }
    }

    void setSignature(Signature theSignature) {
        super.setSignature(theSignature);
    }
    
    public FileObject getClassFile() {
        if (classFile == null) {
            classFile = J2JSCompiler.compiler.fileManager.getFileForInput(
                    getSignature().toString().replaceAll("\\.", "/") + ".class"); 
        }
        return classFile;
    }

    public void setLastCompiled(long theLastCompiled) {
        lastCompiled = theLastCompiled;
    }
    
    public boolean isResolved() {
        return isResolved;
    }
    
    public void setSuperTainted() {
        ClassUnit clazz = this;
        do {
            clazz.setTainted();
            clazz = clazz.getSuperUnit();
        } while (clazz != null);
        
        for (ClassUnit i : interfaces) {
            i.setSuperTainted();
        }
    }

    public void setResolved(boolean theIsResolved) {
        isResolved = theIsResolved;
    }
    public String getName() {
        return getSignature().className();
    }

    public Project getProject() {
        return project;
    }
  
}
