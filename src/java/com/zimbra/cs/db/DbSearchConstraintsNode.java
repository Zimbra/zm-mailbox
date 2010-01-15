/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
	   Iterable<? extends DbSearchConstraintsNode> getSubNodes();
	   
	   /**
	    * @return The SearchConstraints for this node, if it is a LEAF
	    * node, or NULL if it is not.
	    */
	   DbSearchConstraints getSearchConstraints();
}