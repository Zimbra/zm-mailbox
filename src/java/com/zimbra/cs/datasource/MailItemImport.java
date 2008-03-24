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
package com.zimbra.cs.datasource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;


public interface MailItemImport {

    /**
     * Tests connecting to the specified data source.
     * 
     * @return <code>null</code> or an error message if the test failed. 
     */
    public String test(DataSource ds) throws ServiceException;
    
    /**
     * Imports data from the specified data source.
     */
    public void importData(Account account, DataSource dataSource, boolean fullSync) throws ServiceException;
}
