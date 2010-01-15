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

package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbSearchConstraints;
import com.zimbra.cs.db.DbSearchConstraintsNode;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;

class DbAndNode implements IConstraints {
   public NodeType getNodeType() { return NodeType.AND; }
   public Iterable<? extends DbSearchConstraintsNode> getSubNodes() { return mSubNodes; }
   public DbSearchConstraints getSearchConstraints() { return null; }

   protected List<IConstraints> mSubNodes = new ArrayList<IConstraints>();

   public Object clone() throws CloneNotSupportedException {
	   DbAndNode toRet = (DbAndNode)super.clone();
	   
	   toRet.mSubNodes = new ArrayList<IConstraints>();  
	   for (IConstraints node : mSubNodes)
		   toRet.mSubNodes.add((IConstraints)node.clone());
	   
	   return toRet;
   }

   DbLeafNode getLeafChild() {
	   for (IConstraints n : mSubNodes)
		   if (n.getNodeType() == NodeType.LEAF)
			   return (DbLeafNode)n;
	   
	   DbLeafNode c = new DbLeafNode();
	   mSubNodes.add(c);
	   return c;
   }

   public IConstraints andIConstraints(IConstraints other) {
	   switch(other.getNodeType()) {
	   case LEAF:
		   DbLeafNode otherLeaf = (DbLeafNode)other;
		   IConstraints ret = getLeafChild().andIConstraints(otherLeaf);
		   assert(mSubNodes.contains(ret));
		   break;
	   case AND:
		   for (IConstraints n : ((DbAndNode)other).mSubNodes) {
			   if (n.getNodeType() == NodeType.LEAF) {
				   IConstraints c = getLeafChild().andIConstraints((DbLeafNode)n);
				   assert(c == this);
			   } else {
				   mSubNodes.add(n);
			   }
		   }
		   break;
	   case OR:
		   mSubNodes.add(other);
		   break;
	   }
	   
	   return this;
   }

   public IConstraints orIConstraints(IConstraints other) 
   {
	   if (other.getNodeType() == NodeType.OR) {
		   return other.orIConstraints(other);
	   } else {
		   IConstraints top = new DbOrNode();
		   top = top.orIConstraints(this);
		   top = top.orIConstraints(other);
		   return top;
	   }
   }

   public void ensureSpamTrashSetting(Mailbox mbox, List<Folder> excludeFolders) throws ServiceException {
	   DbLeafNode c = getLeafChild();
	   c.ensureSpamTrashSetting(mbox, excludeFolders);
   }

   public boolean hasSpamTrashSetting() {
	   for (IConstraints n : mSubNodes) 
		   if (n.hasSpamTrashSetting())
			   return true;
	   return false;
   }

   public void forceHasSpamTrashSetting() {
	   for (IConstraints n : mSubNodes) 
		   if (!n.hasSpamTrashSetting())
			   n.forceHasSpamTrashSetting();
   }

   public boolean hasNoResults() {
	   for (IConstraints n : mSubNodes) 
		   if (n.hasNoResults())
			   return true;
	   return false;
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
			   ret.append(" AND ");
		   
		   ret.append(n.toQueryString());
		   atFirst = false;
	   }
	   
	   ret.append(')');
	   return ret.toString();
   }

   public String toString()
   {
	   StringBuilder toRet = new StringBuilder("AND(");
	   for (IConstraints n : mSubNodes)
		   toRet.append(n.toString()).append(' ');
	   return toRet.append(')').toString();
   }

}