package com.j2js;

/*
 * Created on May 27, 2004
 */

import java.util.ArrayList;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;

/**
 * @author wolfgang
 */
public class ExceptionHandlers extends ArrayList<ExceptionHandler> {
    
    public ExceptionHandlers(Code code) {
        
        // Join all contiguous CodeExceptions with equal handler PC.
        // This is to eliminate multi-entrant execption handlers.
        CodeException previousCodeException = null;
        for (CodeException codeException : code.getExceptionTable()) {
            if (previousCodeException != null &&
                    previousCodeException.getHandlerPC() == codeException.getHandlerPC()) {
                previousCodeException.setEndPC(codeException.getEndPC());
            } else {
                add(new ExceptionHandler(codeException));
            }
            previousCodeException = codeException;
        }
    }
}
