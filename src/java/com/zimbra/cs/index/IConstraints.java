/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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
 * An interface to a tree of ANDed and ORed search constraints for the 
 * DB-backed data in a search request.
 * 
 */
interface IConstraints extends DbSearchConstraintsNode, Cloneable {
    
	/**
     * Used during query optimization: the optimizer is saying "make sure this query subtree
     * is excluding spam and or trash"
     * 
	 * @param mbox
	 * @param excludeFolders List of spam or trash folders to be excluded
	 * @throws ServiceException
	 */
	void ensureSpamTrashSetting(Mailbox mbox, List<Folder> excludeFolders) throws ServiceException;

    /**
     * Used during query optimization
     * 
     * @return TRUE if we have folder settings (such as "in:foo") that mean we
     * don't need to have constraints added for the implicit spam/trash setting
     */
    boolean hasSpamTrashSetting();
    
    /**
     * A bit of a hack -- when we combine a "all items including trash/spam" term with another query, this
     * API lets us force the "include trash/spam" part in the other query and thereby drop the 1st one. 
     */
    void forceHasSpamTrashSetting();
    
	/**
     * AND another IConstraints into our tree: this returns the new root node
     * (might be us, might be a new toplevel)
     * 
	 * @param other
	 * @return
	 */
	IConstraints andIConstraints(IConstraints other);
    
    /**
     * AND another IConstraints into our tree: this returns the new root node
     * (might be us, might be a new toplevel)
     * 
     * @param other
     * @return
     */
	IConstraints orIConstraints(IConstraints other);
    
    
	/**
     * Helper for query optimization
     * 
	 * @return TRUE if the current set of constraints returns ZERO results
	 */
	boolean hasNoResults();
    
    
	/**
     * Used during query optimization
     * 
	 * @param mbox
	 * @return TRUE if the constraints are such that we think a DB-first plan will be faster.
	 * @throws ServiceException 
	 */
	boolean tryDbFirst(Mailbox mbox) throws ServiceException;
    
    
	/**
     * The allowable query types are added to the constraints tree after it is generated, 
     * using this API
     *  
	 * @param types
	 */
    
	void setTypes(Set<Byte> types);
    
    
	/**
     * Clone is critical for things to work correctly (exploding constraints into multiple trees
     * if the query goes to many target servers).
     * 
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public Object clone() throws CloneNotSupportedException;
    
    
	/**
     * Outputs the constraints tree in a format that is parsable via our QueryParser.  This is
     * used when we have to send a query to a remote server.
     * 
	 * @return
	 */
	String toQueryString();
}