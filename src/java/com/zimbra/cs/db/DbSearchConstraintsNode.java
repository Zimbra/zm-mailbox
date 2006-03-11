package com.zimbra.cs.db;

/**
 * @author tim
 * 
 * Instead of a single SearchConstraints, DB searches can be done
 * on a tree of SearchConstraints.  Each node of the tree is either
 * an AND, OR, or a leaf node.  
 *
 */
public interface DbSearchConstraintsNode {
	   public static enum NodeType {
		   AND, OR, LEAF;
	   }
	   
	   DbSearchConstraintsNode.NodeType getNodeType();
	   
	   /**
	    * @return The list of ANDed or ORed subnodes, or NULL if 
	    * this is a LEAF node.
	    */
	   Iterable<DbSearchConstraintsNode> getSubNodes();
	   
	   /**
	    * @return The SearchConstraints for this node, if it is a LEAF
	    * node, or NULL if it is not.
	    */
	   DbSearchConstraints getSearchConstraints();
}