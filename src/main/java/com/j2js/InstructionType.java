package com.j2js;

/**
 * @author wolfgang
 */
public class InstructionType {

    private short code;
    private String name;
    private Form[] forms;
    
    public InstructionType(short theCode, String theName, int formCount) {
        code = theCode;
        name = theName;
        forms = new Form[formCount];
    }
    
    public int getFormCount() {
        return forms.length;
    }
    
    public void setForm(Form form, int index) {
        forms[index] = form;
        form.setIndex(index);
    }
    
    public Form getForm(int index) {
        return forms[index];
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
     * @return Returns the code.
     */
    public short getCode() {
        return code;
    }

    /**
     * @param theCode The code to set.
     */
    public void setCode(short theCode) {
        code = theCode;
    }

}
