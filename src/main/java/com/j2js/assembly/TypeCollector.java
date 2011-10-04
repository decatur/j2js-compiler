package com.j2js.assembly;

import java.util.Collection;
import java.util.HashSet;

public class TypeCollector implements TypeVisitor {
    
    Collection<ClassUnit> collectedTypes = new HashSet<ClassUnit>();
    
    public void visit(ClassUnit clazz) {
        collectedTypes.add(clazz);
    }
}
