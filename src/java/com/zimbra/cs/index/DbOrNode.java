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
package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbSearchConstraints;
import com.zimbra.cs.db.DbSearchConstraintsNode;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;

class DbOrNode implements IConstraints {
   public NodeType getNodeType() { return NodeType.OR; }

   /**
    * @return The list of ANDed or ORed subnodes, or NULL if 
    * this is a LEAF node.
    */
   public Iterable<? extends DbSearchConstraintsNode> getSubNodes() { return mSubNodes; }

   /**
    * @return The SearchConstraints for this node, if it is a LEAF
    * node, or NULL if it is not.
    */
   public DbSearchConstraints getSearchConstraints() { return null; }

   protected List<IConstraints> mSubNodes = new ArrayList<IConstraints>();

   public Object clone() throws CloneNotSupportedException {
	   DbOrNode toRet = (DbOrNode)super.clone();
	   
	   toRet.mSubNodes = new ArrayList<IConstraints>();  
	   for (IConstraints node : mSubNodes)
		   toRet.mSubNodes.add((IConstraints)node.clone());
	   
	   return toRet;
   }

   public IConstraints andIConstraints(IConstraints other) {
	   if (other.getNodeType() == NodeType.AND) {
		   return other.andIConstraints(this);
	   } else {
		   IConstraints top = new DbAndNode();
		   top = top.andIConstraints(this);
		   top = top.andIConstraints(other);
		   return top;
	   }
   }

   public IConstraints orIConstraints(IConstraints other) {
	   if (other.getNodeType() == NodeType.OR) {
	       // add all of the other node's subnodes to our list of subnodes
	       for (IConstraints n : ((DbOrNode)other).mSubNodes) 
	           mSubNodes.add(n);
	   } else {
	       mSubNodes.add(other);
	   }
	   return this;
   }

   public void ensureSpamTrashSetting(Mailbox mbox, List<Folder> excludeFolders) throws ServiceException {
	   //
	   // push down instead of ANDing this at the toplevel!
	   //
	   // This is important because we exclude (trash spam) and the query is:
	   //
	   //    (tag:foo is:anywhere) or (tag:bar)
	   //
	   // we want the resultant query to be:
	   //
	   //    (tag foo is:anywhere) or (tag:bar -in:trash -in:spam)
	   //
	   //
	   for (IConstraints n : mSubNodes) 
		   n.ensureSpamTrashSetting(mbox, excludeFolders);
   }

   public boolean hasSpamTrashSetting() {
	   for (IConstraints n : mSubNodes) 
		   if (!n.hasSpamTrashSetting())
			   return false;
	   return true;
   }

   public void forceHasSpamTrashSetting() {
	   for (IConstraints n : mSubNodes) 
		   if (!n.hasSpamTrashSetting())
			   n.forceHasSpamTrashSetting();
   }

   public boolean hasNoResults() {
	   for (IConstraints n : mSubNodes) 
		   if (!n.hasNoResults())
			   return false;
	   return true;
   }

   public boolean tryDbFirst(Mailbox mbox) {
	   return false;
   }

   public void setTypes(Set<Byte> types) {
	   for (IConstraints n : mSubNodes) 
		   n.setTypes(types);
   }

   public String toQueryString() {
	   StringBuilder ret = new StringBuilder("(");
	   
	   boolean atFirst = true;
	   
	   for (IConstraints n : mSubNodes) {
		   if (!atFirst)
			   ret.append(" OR ");
		   
		   ret.append(n.toQueryString());
		   atFirst = false;
	   }
	   
	   ret.append(')');
	   return ret.toString();
   }

   public String toString()
   {
	   StringBuilder toRet = new StringBuilder("OR(");
	   for (IConstraints n : mSubNodes)
		   toRet.append(n.toString()).append(' ');
	   return toRet.append(')').toString();
   }

 }