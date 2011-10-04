/*
 * Created on Feb 2, 2005
 */
package com.j2js.visitors;

import com.j2js.dom.ASTNode;
import com.j2js.dom.ArrayAccess;
import com.j2js.dom.ArrayCreation;
import com.j2js.dom.ArrayInitializer;
import com.j2js.dom.Assignment;
import com.j2js.dom.Block;
import com.j2js.dom.BooleanLiteral;
import com.j2js.dom.BreakStatement;
import com.j2js.dom.CastExpression;
import com.j2js.dom.CatchClause;
import com.j2js.dom.ClassInstanceCreation;
import com.j2js.dom.ClassLiteral;
import com.j2js.dom.ConditionalExpression;
import com.j2js.dom.ContinueStatement;
import com.j2js.dom.DoStatement;
import com.j2js.dom.FieldAccess;
import com.j2js.dom.IfStatement;
import com.j2js.dom.InfixExpression;
import com.j2js.dom.InstanceofExpression;
import com.j2js.dom.Name;
import com.j2js.dom.PostfixExpression;
import com.j2js.dom.PrefixExpression;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.MethodInvocation;
import com.j2js.dom.NullLiteral;
import com.j2js.dom.NumberLiteral;
import com.j2js.dom.PrimitiveCast;
import com.j2js.dom.ReturnStatement;
import com.j2js.dom.StringLiteral;
import com.j2js.dom.SwitchCase;
import com.j2js.dom.SwitchStatement;
import com.j2js.dom.SynchronizedBlock;
import com.j2js.dom.ThisExpression;
import com.j2js.dom.ThrowStatement;
import com.j2js.dom.TryStatement;
import com.j2js.dom.TypeDeclaration;
import com.j2js.dom.VariableBinding;
import com.j2js.dom.VariableDeclaration;
import com.j2js.dom.WhileStatement;

/**
 * Copyright by Wolfgang Kuehn 2005
 */
public abstract class AbstractVisitor {

 
    public abstract void visit(ASTNode node);
    
    public void visit(TypeDeclaration node) {
        visit((ASTNode) node);
    }

    public void visit(MethodDeclaration node) {
        visit((ASTNode) node);
    }

    public void visit(DoStatement node) {
        visit((ASTNode) node);
    }

    public void visit(WhileStatement node) {
        visit((ASTNode) node);
    }

    public void visit(IfStatement node) {
        visit((ASTNode) node);
    }

    public void visit(TryStatement node) {
        visit((ASTNode) node);
    }

    public void visit(Block node) {
        visit((ASTNode) node);
    }

    public void visit(InfixExpression node) {
        visit((ASTNode) node);
    }
    
    public void visit(PrefixExpression node) {
        visit((ASTNode) node);
    }
    
    public void visit(PostfixExpression node) {
        visit((ASTNode) node);
    }

    public void visit(SwitchStatement node) {
        visit((ASTNode) node);
    }

    public void visit(SwitchCase node) {
        visit((ASTNode) node);
    }
    
    public void visit(CatchClause node) {
        visit((ASTNode) node);
    }

    public void visit(ReturnStatement node) {
        visit((ASTNode) node);
    }

    public void visit(Assignment node) {
        visit((ASTNode) node);
    }

    public void visit(NumberLiteral node) {
        visit((ASTNode) node);
    }

    public void visit(StringLiteral node) {
        visit((ASTNode) node);
    }
    
    public void visit(ClassLiteral node) {
        visit((ASTNode) node);
    }

    public void visit(NullLiteral node) {
        visit((ASTNode) node);
    }

    public void visit(MethodInvocation node) {
        visit((ASTNode) node);
    }

    public void visit(ClassInstanceCreation node) {
        visit((ASTNode) node);
    }

    public void visit(ArrayInitializer node) {
        visit((ASTNode) node);
    }

    public void visit(ArrayCreation node) {
        visit((ASTNode) node);
    }

    public void visit(ArrayAccess node) {
        visit((ASTNode) node);
    }

    public void visit(VariableDeclaration node) {
        visit((ASTNode) node);
    }

    public void visit(VariableBinding node) {
        visit((ASTNode) node);
    }

    public void visit(ThisExpression node) {
        visit((ASTNode) node);
    }

    public void visit(FieldAccess node) {
        visit((ASTNode) node);
    }

    public void visit(BreakStatement node) {
        visit((ASTNode) node);
    }

    public void visit(ContinueStatement node) {
        visit((ASTNode) node);
    }

    public void visit(CastExpression node) {
        visit((ASTNode) node);
    }

    public void visit(BooleanLiteral node) {
        visit((ASTNode) node);
    }
    
    public void visit(ThrowStatement node) {
        visit((ASTNode) node);
    }
    
    public void visit(Name node) {
        visit((ASTNode) node);
    }
    
    public void visit(InstanceofExpression node) {
        visit((ASTNode) node);
    }
    
    public void visit(ConditionalExpression node) {
    	visit((ASTNode) node);
    }
    
    public void visit(SynchronizedBlock node) {
        visit((ASTNode) node);
    }
    
    public void visit(PrimitiveCast node) {
        visit((ASTNode) node);
    }

}