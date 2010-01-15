/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.util.ArrayList;
import java.util.List;

/**
 * A single superclass for both AND and OR nodes in a tree of search constraints
 * 
 * This class is only used for testing and can probably go away: it has been
 * subsumed by cs.index.DbAndNode and cs.index.DbOrNode
 * 
 */
public class DbSearchConstraintsInnerNode implements DbSearchConstraintsNode {
	
	private NodeType mNodeType;
	private List<DbSearchConstraintsNode> mSubNodes;
	
	private DbSearchConstraintsInnerNode(NodeType ntype) {
		mNodeType = ntype;
	}
	
	public static DbSearchConstraintsInnerNode AND() { 
		return new DbSearchConstraintsInnerNode(NodeType.AND);
	}
	
    public static DbSearchConstraintsInnerNode OR() {
		return new DbSearchConstraintsInnerNode(NodeType.OR);
	}
	
	public void addSubNode(DbSearchConstraintsNode node) {
        if (mSubNodes == null)
            mSubNodes = new ArrayList<DbSearchConstraintsNode>();
		mSubNodes.add(node);
	}
	
	public void removeSubNode(DbSearchConstraintsNode node) {
        if (mSubNodes != null)
            mSubNodes.remove(node);
	}
	
	public NodeType getNodeType() {
		return mNodeType;
	}

	public Iterable<DbSearchConstraintsNode> getSubNodes() {
		return mSubNodes;
	}

	public DbSearchConstraints getSearchConstraints() {
		return null;
	}

}
