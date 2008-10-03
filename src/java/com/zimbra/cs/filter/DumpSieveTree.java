/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.filter;

import org.apache.jsieve.parser.SieveNode;
import org.apache.jsieve.parser.generated.Node;

/**
 * Implementation of {@link SieveVisitor} that dumps
 * all the nodes in the tree.  Used for debugging.
 */
public class DumpSieveTree extends SieveVisitor {

    private int mIndentLevel = 0;
    StringBuilder mBuf = new StringBuilder();

    @Override
    protected void visitAction(Node actionNode, VisitPhase phase, RuleProperties props) {
    }

    @Override
    protected void visitNode(Node node, VisitPhase phase, RuleProperties props) {
        if (phase == VisitPhase.begin) {
            indent();
            Object value = getValue(node);
            String valueClass = (value == null ? "" : String.format(" (%s)", value.getClass().getSimpleName()));
            mBuf.append(String.format("%s, %s=%s%s\n", node.getClass().getSimpleName(), getName(node), value, valueClass));
            mIndentLevel++;
        } else {
            mIndentLevel--;
        }
    }

    @Override
    protected void visitRule(Node ruleNode, VisitPhase phase, RuleProperties props) {
    }

    @Override
    protected void visitTest(Node testNode, VisitPhase phase, RuleProperties props) {
    }

    private String getName(Node node) {
        return ((SieveNode) node).getName();
    }
    
    private Object getValue(Node node) {
        return ((SieveNode) node).getValue();
    }

    private void indent() {
        for (int i = 0; i < mIndentLevel; i++) {
            mBuf.append(" ");
        }
    }
    
    public String toString() {
        return mBuf.toString();
    }
}
