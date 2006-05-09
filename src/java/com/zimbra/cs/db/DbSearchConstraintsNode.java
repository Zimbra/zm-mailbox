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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
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