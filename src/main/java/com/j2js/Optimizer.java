package com.j2js;

import java.util.Iterator;
import java.util.List;

import org.apache.bcel.generic.Type;

import com.j2js.dom.ASTNode;
import com.j2js.dom.Assignable;
import com.j2js.dom.Assignment;
import com.j2js.dom.Block;
import com.j2js.dom.Expression;
import com.j2js.dom.FieldAccess;
import com.j2js.dom.InfixExpression;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.NumberLiteral;
import com.j2js.dom.PStarExpression;
import com.j2js.dom.PostfixExpression;
import com.j2js.dom.PrefixExpression;
import com.j2js.dom.VariableBinding;
import com.j2js.dom.VariableDeclaration;

public class Optimizer {
    
    private MethodDeclaration methodDecl;
    private List tempDecls;
    
    public Optimizer(MethodDeclaration theMethodDecl, List theTempDecls) {
        methodDecl = theMethodDecl;
        tempDecls = theTempDecls;
    }
    
    public static Expression negate(Expression expr) {
        PrefixExpression pe = new PrefixExpression();
        pe.setOperator(PrefixExpression.NOT);
        pe.setOperand(expr);
        return pe;
    }
    
    /**
     * Simplifies the (possibly negated) expression.
     */
    public static Expression simplifyBooleanExpression(Expression expr, boolean negate) {
        if (expr instanceof PrefixExpression) {
            PrefixExpression pe = (PrefixExpression) expr;
            if (pe.getOperator() != PrefixExpression.NOT) return expr;
            return simplifyBooleanExpression((Expression) pe.getOperand(), !negate);
        }
        
        if (expr instanceof InfixExpression && expr.getTypeBinding() == Type.BOOLEAN) {
            InfixExpression in = (InfixExpression) expr;
            InfixExpression.Operator op = in.getOperator();
            if (negate) {
                op = op.getComplement();
                if (op != InfixExpression.Operator.CONDITIONAL_AND && op != InfixExpression.Operator.CONDITIONAL_OR)
                    negate = false;
            }
            InfixExpression out = new InfixExpression(op);
            out.widen(in);
            out.setOperands(simplifyBooleanExpression(in.getLeftOperand(), negate), simplifyBooleanExpression(in.getRightOperand(), negate));
            return out;
        }
        
        if (negate) {
            PrefixExpression pe = new PrefixExpression();
            pe.setOperator(PrefixExpression.NOT);
            pe.setOperand(expr);
            return pe;
        }
        
        return expr;
    }
    
    private boolean representSameAssignables(Assignable a, Assignable b) {
        if (!(a instanceof FieldAccess && b instanceof FieldAccess)) return false;
        FieldAccess faa = (FieldAccess) a;
        FieldAccess fab = (FieldAccess) b;
        if (!faa.getName().equals(fab.getName())) return false;
        if (faa.getExpression() instanceof VariableBinding && fab.getExpression() instanceof VariableBinding ) {
            VariableBinding vba = (VariableBinding) faa.getExpression();
            VariableBinding vbb = (VariableBinding) fab.getExpression();
            return vba.getVariableDeclaration() == vbb.getVariableDeclaration();
        }
        return false;
    }
    
    /**
     * Reduces occurences of
     *      temp = x;
     *      y = temp;
     * to
     *      y = x;
     */
//    private ASTNode bar1(ASTNode child, ASTNode next) {
//        if (child instanceof Assignment && next instanceof Assignment) {
//            Assignment a1 = (Assignment) child;
//            Assignment a2 = (Assignment) next;
//            if (a1.getLeftHandSide().equals(a2.getRightHandSide())) {
//                a2.setRightHandSide(a1.getRightHandSide());
//                child.getParentBlock().removeChild(child);
//                return next;
//            }
//        }
//        return child;
//    }
    
    private VariableBinding fetchVariableBinding(Expression expr, VariableDeclaration decl) {
        ASTNode child = expr.getFirstChild();
        while (child != null) {
            if (child instanceof VariableBinding && ((VariableBinding) child).getVariableDeclaration() == decl)
                return (VariableBinding) child;
            VariableBinding vb = fetchVariableBinding((Expression) child, decl);
            if (vb != null) return vb;
            child = child.getNextSibling();
        }
        return null;
    }
    
    /**
     * If the specified expression corresponds to
     *      xy + 1 or xy - (-1),
     * returns the INCREMENT operator. If the specified expression corresponds to
     *      xy - 1 or xy + (-1),
     * returns the DECREMENT operator. Otherwise, null is returned.
     * @param expr
     * @return
     */
    private PStarExpression.Operator getOp(InfixExpression expr) {
        NumberLiteral nl;
        if (expr.getRightOperand() instanceof NumberLiteral) {
            nl = (NumberLiteral) expr.getRightOperand();
        } else return null;
        
        PStarExpression.Operator op;
        if (expr.getOperator() == InfixExpression.Operator.PLUS) {
            op = PStarExpression.INCREMENT;
        } else if (expr.getOperator() == InfixExpression.Operator.MINUS) {
            op = PStarExpression.DECREMENT;
        }  else {
            return null;
        }
        
        if (NumberLiteral.isOne(nl)) {
            // We are ok.
        } else if (NumberLiteral.isMinusOne(nl)) {
            op = op.complement();
        } else {
            return null;
        }
        return op;
    }    
    
    /**
     * Reduces
     *      vb = x.y;      (1)
     *      x.y = vb + 1;  (2)
     *      ...
     *      expr(vb);
     * to incremental form
     *       expr(x.y++);
     * Likewise decrement.
     */
    private boolean reduceXCrement(VariableDeclaration decl) {
        Assignment a1 = null;
        Assignment a2 = null;
        VariableBinding vb1 = null;
        VariableBinding vb2 = null;
        
        Assignable fa1 = null;
        Assignable fa2 = null;
        
        InfixExpression sum = null;
        
        
        Iterator iter = decl.vbs.iterator();

        while (iter.hasNext()) {
            VariableBinding vb = (VariableBinding) iter.next();
            
            if (vb.getParentNode() instanceof Assignment) {
                Assignment a = (Assignment) vb.getParentNode();
                if (a.getLeftHandSide() == vb && a.getRightHandSide() instanceof Assignable) {
                    vb1 = vb;
                    a1 = a;
                    fa1 = (Assignable) a.getRightHandSide();
                    continue;
                }
            }

            if (vb.getParentNode() instanceof InfixExpression) {
                InfixExpression infix = (InfixExpression) vb.getParentNode();
                if (infix.getParentNode() instanceof Assignment) {
                    Assignment a = (Assignment) infix.getParentNode();
                    if (a.getLeftHandSide() instanceof Assignable) {
                        vb2 = vb;
                        fa2 = (Assignable) a.getLeftHandSide();
                        a2 = a;
                        sum = infix;
                        continue;
                    }
                }
            }
        }

        if (a1 == null || a2 == null) return false;
        if (!fa1.isSame(fa2)) return false;

        PStarExpression.Operator operator = getOp(sum);
        if (operator == null) return false;
        
        PStarExpression p = new PostfixExpression();
        p.setOperand((Expression) fa1);
        p.setOperator(operator);

        decl.vbs.remove(vb1);
        decl.vbs.remove(vb2);
        VariableBinding vb = decl.vbs.get(0);
        vb.getParentBlock().replaceChild(p, vb);

        Block b = a1.getParentBlock();
        b.removeChild(a1);
        b.removeChild(a2);
        return true;
    }
    
    /**
     * Reduces
     *      vb = x.y op z;  (3)
     *      x.y = vb;       (4)
     *      ...
     *      expr(vb);
     * to incremental form
     *      expr(x.y op= z) or expr(++x.y);
     * Likewise decrement.
     */
    private boolean reduceYCrement(VariableDeclaration decl) {
        Assignment a1 = null;
        Assignment a2 = null;
        VariableBinding vb1 = null;
        VariableBinding vb2 = null;
        
        Assignable fa1 = null;
        Assignable fa2 = null;
        
        InfixExpression infixExpr = null;
        
        Iterator iter = decl.vbs.iterator();

        while (iter.hasNext()) {
            VariableBinding vb = (VariableBinding) iter.next();
            
            if (!(vb.getParentNode() instanceof Assignment)) continue;
        
            Assignment a = (Assignment) vb.getParentNode();
            if (a.getRightHandSide() == vb && a.getLeftHandSide() instanceof Assignable) {
                vb2 = vb;
                a2 = a;
                fa2 = (Assignable) a.getLeftHandSide();
                continue;
            }
            
            if (a.getLeftHandSide() == vb && a.getRightHandSide() instanceof InfixExpression) {
                InfixExpression infix = (InfixExpression) a.getRightHandSide();
                if (!(infix.getLeftOperand() instanceof Assignable)) continue;
                vb1 = vb;
                a1 = a;
                fa1 = (Assignable) infix.getLeftOperand();
                infixExpr = infix;
                continue;
            }
        }

        if (a1 == null || a2 == null) return false;
        if (!fa1.isSame(fa2)) return false;

        decl.vbs.remove(vb1);
        decl.vbs.remove(vb2);
        VariableBinding vb = decl.vbs.get(0);
        Expression replacement = null;
        
        PStarExpression.Operator operator = getOp(infixExpr);
        if (operator != null) {
            PrefixExpression   p = new PrefixExpression();
            p.setOperand((Expression) fa1);
            p.setOperator(operator);
            replacement = p;
        } else {
            InfixExpression.Operator op = infixExpr.getOperator();
            Assignment.Operator opp = Assignment.Operator.lookup(op.toString() + '=');
            Assignment a = new Assignment(opp);
            a.setLeftHandSide((Expression) fa2);
            a.setRightHandSide(infixExpr.getRightOperand());
            replacement = a;
        }
        
        vb.getParentBlock().replaceChild(replacement, vb);
        
        Block b = a1.getParentBlock();
        b.removeChild(a1);
        b.removeChild(a2);
        return true;
    }
    
    public void optimize() {
        if (false) return;
        // Review this code. For example j2js.fa(obj, name)++ is illegal!
        
        Iterator iter = tempDecls.iterator();
        while (iter.hasNext()) {
            VariableDeclaration decl = (VariableDeclaration) iter.next();
            int count = decl.vbs.size();
            if (count == 3) {
                if (reduceXCrement(decl)) {
                    iter.remove();
                    methodDecl.removeLocalVariable(decl.getName());
                } else if (reduceYCrement(decl)) {
                    iter.remove();
                    methodDecl.removeLocalVariable(decl.getName());
                }
            }
        }
    }

}
