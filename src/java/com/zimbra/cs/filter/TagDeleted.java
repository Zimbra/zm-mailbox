/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.filter;

import org.apache.jsieve.parser.SieveNode;
import org.apache.jsieve.parser.generated.Node;

/**
 * Disables any filter rules that reference a deleted tag.
 */
public class TagDeleted extends SieveVisitor {

    private String mDeletedTagName;
    private Node mIfNode;
    private boolean mModified = false;
    
    public TagDeleted(String deletedTagName) {
        mDeletedTagName = deletedTagName;
    }
    
    public boolean modified() {
        return mModified;
    }

    @Override
    protected void visitNode(Node node, VisitPhase phase, RuleProperties props) {
        if (phase != VisitPhase.begin) {
            return;
        }
        String name = getNodeName(node);
        if ("if".equals(name) || "disabled_if".equals(name)) {
            // Remember the top-level node so we can modify it later.
            mIfNode = node;
        }
    }
    
    @Override
    protected void visitTagAction(Node node, VisitPhase phase, RuleProperties props, String tagName) {
        if (phase != VisitPhase.begin) {
            return;
        }
        String ifNodeName = getNodeName(mIfNode);
        if (tagName.equals(mDeletedTagName) && "if".equals(ifNodeName)) {
            ((SieveNode) mIfNode).setName("disabled_if");
            mModified = true;
        }
    }
}
