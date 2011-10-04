package com.j2js.dom;

import java.util.HashMap;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author kuehn
 */
public class Assignment extends Expression {
	
    static public class Operator {
        
        static private HashMap<String, Operator> opsByToken = new HashMap<String, Operator>();
        
        static public Operator lookup(String token) {
            return opsByToken.get(token);
        }
        
        static public Operator ASSIGN = new Operator("=");
        static public Operator PLUS_ASSIGN = new Operator("+=");
        static public Operator MINUS_ASSIGN = new Operator("-=");
        static public Operator TIMES_ASSIGN = new Operator("*=");
        static public Operator DIVIDE_ASSIGN = new Operator("/=");
        static public Operator BIT_AND_ASSIGN = new Operator("&=");
        static public Operator BIT_OR_ASSIGN = new Operator("|=");
        static public Operator BIT_XOR_ASSIGN = new Operator("^=");
        static public Operator REMAINDER_ASSIGN = new Operator("%=");
        static public Operator LEFT_SHIFT_ASSIGN = new Operator("<<=");
        static public Operator RIGHT_SHIFT_SIGNED_ASSIGN = new Operator(">>=");
        static public Operator RIGHT_SHIFT_UNSIGNED_ASSIGN = new Operator(">>>=");
        
        private String token;
        
        Operator(String theToken) {
            token = theToken;
            opsByToken.put(theToken, this);
        }
        
        public String toString() {
            return token;
        }
    }
    
    private Operator operator;
    
	public Assignment(Operator theOperator) {
		super();
		operator = theOperator;
	}
    
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }

	/**
	 * @param rightHandSide The rightHandSide to set.
	 */
	public void setRightHandSide(Expression rightHandSide) {
        widen(rightHandSide);
        setChildAt(1, rightHandSide);
	}

	/**
	 * @return Returns the rightHandSide.
	 */
	public Expression getRightHandSide() {
        return (Expression) getChildAt(1);
	}

	/**
	 * @param leftHandSide The leftHandSide to set.
	 */
	public void setLeftHandSide(Expression leftHandSide) {
        setChildAt(0, leftHandSide);
	}

	/**
	 * @return Returns the leftHandSide.
	 */
	public Expression getLeftHandSide() {
        return (Expression) getChildAt(0);
	}
    /**
     * @return Returns the operator.
     */
    public Operator getOperator() {
        return operator;
    }
    /**
     * @param theOerator The operator to set.
     */
    public void setOperator(Operator theOperator) {
        operator = theOperator;
    }
}
