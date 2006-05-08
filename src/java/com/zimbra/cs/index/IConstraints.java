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
package com.zimbra.cs.index;

import java.util.List;
import java.util.Set;

import com.zimbra.cs.db.DbSearchConstraintsNode;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 *
 * An interface to constraints for the DB-backed data in a search
 * request.
 */
interface IConstraints extends DbSearchConstraintsNode, Cloneable {
	void ensureSpamTrashSetting(Mailbox mbox, List<Folder> excludeFolders) throws ServiceException;
	IConstraints andIConstraints(IConstraints other);
	IConstraints orIConstraints(IConstraints other);
	boolean hasSpamTrashSetting();
	void forceHasSpamTrashSetting();
	boolean hasNoResults();
	boolean tryDbFirst(Mailbox mbox);
	void setTypes(Set<Byte> types);
	public Object clone() throws CloneNotSupportedException;
	String toQueryString();
}