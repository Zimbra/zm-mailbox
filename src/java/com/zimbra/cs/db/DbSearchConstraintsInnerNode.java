/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.util.ArrayList;
import java.util.List;

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
