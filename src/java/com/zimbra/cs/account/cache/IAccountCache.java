/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 VMware, Inc.
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
package com.zimbra.cs.account.cache;

import com.zimbra.cs.account.Account;

public interface IAccountCache extends IEntryCache {
    public void clear();
    public void remove(Account entry);
    public void put(Account entry);
    public void replace(Account entry);
    public Account getById(String key);
    public Account getByName(String key);
    public Account getByForeignPrincipal(String key);
}
