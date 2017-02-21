/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
