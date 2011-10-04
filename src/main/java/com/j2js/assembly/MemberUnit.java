package com.j2js.assembly;

/**
 * The MemberUnit class is the base class for Field, Method and Constructor objects.
 * 
 * @author wolle
 */
public abstract class MemberUnit extends Unit {
    
    // The class to which this method belongs. 
    ClassUnit declaringClass;

    MemberUnit(Signature theSignature, ClassUnit theDeclaringClazz) {
        setSignature(theSignature);
        declaringClass = theDeclaringClazz;
        declaringClass.addMemberUnit(this);
    }
    
    public ClassUnit getDeclaringClass() {
        return declaringClass;
    }
    
    public Signature getAbsoluteSignature() {
        Signature s = Project.getSingleton().getSignature(declaringClass.toString(), getSignature().toString());
        return s;
    }
    
    public String toString() {
        return declaringClass.getName() + "#" + super.toString();
    }

}
