package com.zimbra.cs.db;

import java.util.List;

public class DbSearchConstraintsInnerNode implements DbSearchConstraintsNode {
	
	private NodeType mNodeType;
	private List<DbSearchConstraintsNode> mSubNodes;
	
	private DbSearchConstraintsInnerNode(NodeType ntype) {
		mNodeType = ntype;
	}
	
	DbSearchConstraintsInnerNode AND() { 
		return new DbSearchConstraintsInnerNode(NodeType.AND);
	}
	
	DbSearchConstraintsInnerNode OR() {
		return new DbSearchConstraintsInnerNode(NodeType.OR);
	}
	
	public void addSubNode(DbSearchConstraintsInnerNode node) {
		mSubNodes.add(node);
	}
	
	public void removeSubNode(DbSearchConstraintsInnerNode node) {
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
