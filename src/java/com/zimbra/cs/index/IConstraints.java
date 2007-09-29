/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbSearchConstraintsNode;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;

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