/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public interface AttributeCallback {

    /**
     * called before an attribute is modified. If a ServiceException is thrown, no attributes will
     * be modified. The attrsToModify map should not be modified, other then for the current attrName
     * being called.
     * 
     * TODO: if dn/name/type is needed on a create (for whatever reason), we could consider passing
     * them in context with well-known-keys, or having separate *Create callbacks.
     * 
     * @param context place to stash data between invocations of pre/post
     * @param attrName name of the attribute being modified so the callback can be used with multiple attributes.
     * @param attrValue will be null, String, or String[]
     * @param attrsToModify a map of all the attributes being modified
     * @param entry entry object being modified. null if entry is being created.
     * @param isCreate set to true if called during create
     * @throws ServiceException causes the whole transaction to abort.
     */
    void preModify(
            Map context,
            String attrName,
            Object attrValue,
            Map attrsToModify,
            Entry entry,
            boolean isCreate) throws ServiceException;

    /**
     * called after a successful modify of the attributes. should not throw any exceptions.
     * 
     * @param context
     * @param attrName
     * @param entry Set on modify and create.
     * @param isCreate set to true if called during create
     */
    void postModify(
            Map context,
            String attrName,
            Entry entry,
            boolean isCreate);
}
