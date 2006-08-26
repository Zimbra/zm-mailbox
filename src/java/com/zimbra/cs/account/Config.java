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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Config extends Entry {
    
    private Map<String, Object> mDomainDefaults = new HashMap<String, Object>();
    private Map<String, Object> mServerDefaults = new HashMap<String, Object>();    

    public Config(Map<String, Object> attrs) {
        super(attrs, null);
        resetData();
    }

    @Override
    public void resetData() {
        super.resetData();
        try {
            getDefaults(AttributeFlag.domainInherited, mDomainDefaults);
            getDefaults(AttributeFlag.serverInherited, mServerDefaults);            
        } catch (ServiceException e) {
            // TODO log?
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDomainDefaults() {
        return mDomainDefaults;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getServerDefaults() {
        return mServerDefaults;
    }
}
