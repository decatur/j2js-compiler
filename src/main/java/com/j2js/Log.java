/*
 * Copyright 2005 by Wolfgang Kuehn
 * Created on 04.10.2005
 */
package com.j2js;


/**
 * @author wolfgang
 */
public class Log {
	
    public static Log logger;
    
    public static Log getLogger() {
        return logger;
    }
    
    private int state = INFO;
    
    /**
     * @return Returns the state.
     */
    public int getState() {
        return state;
    }

    /**
     * @param state The state to set.
     */
    public void setState(int state) {
        this.state = state;
    }

    public static final int DEBUG = 3;
    public static final int INFO = 2;
    public static final int WARN = 1;
    public static final int ERROR = 0;
    
    public void debug(CharSequence arg0, Throwable arg1) {
        if (isDebugEnabled()) {
            System.out.println("[DEBUG] " + arg0);
            arg1.printStackTrace();
        }
    }

    public void debug(CharSequence arg0) {
        if (isDebugEnabled()) {
            System.out.println("[DEBUG] " + arg0);
        }
    }

    public void debug(Throwable arg0) {
        if (isDebugEnabled()) {
            arg0.printStackTrace();
        }
    }

    public void error(CharSequence arg0, Throwable arg1) {
        if (isErrorEnabled()) {
            System.out.println("[ERROR] " + arg0);
            arg1.printStackTrace();
        }
    }

    public void error(CharSequence arg0) {
        if (isErrorEnabled()) {
            System.out.println("[ERROR] " + arg0);
        }
    }

    public void error(Throwable arg0) {
        if (isErrorEnabled()) {
            arg0.printStackTrace();
        }
    }

    public void info(CharSequence arg0, Throwable arg1) {
        if (isInfoEnabled()) {
            System.out.println("[INFO] " + arg0);
            arg1.printStackTrace();
        }
    }

    public void info(CharSequence arg0) {
        if (isInfoEnabled()) {
            System.out.println("[INFO] " + arg0);
        }
    }

    public void info(Throwable arg0) {
        if (isInfoEnabled()) {
            arg0.printStackTrace();
        }
    }

    public boolean isDebugEnabled() {
        return state >= DEBUG;
    }

    public boolean isErrorEnabled() {
        return state >= ERROR;
    }

    public boolean isInfoEnabled() {
        return state >= INFO;
    }

    public boolean isWarnEnabled() {
        return state >= WARN;
    }

    public void warn(CharSequence arg0, Throwable arg1) {
        if (isWarnEnabled()) {
            System.out.println("[WARNING] " + arg0);
            arg1.printStackTrace();
        }
    }

    public void warn(CharSequence arg0) {
        if (isWarnEnabled()) {
            System.out.println("[WARNING] " + arg0);
        }
    }

    public void warn(Throwable arg0) {
        if (isWarnEnabled()) {
            arg0.printStackTrace();
        }
    }
    
}
