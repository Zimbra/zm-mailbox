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
