/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.account.ldap;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.extension.ExtensionUtil;

public abstract class LdapSMIMEConfig {
    
    public static LdapSMIMEConfig getInstance() throws ServiceException {
        return getInstance(null);
    }
    
    public static LdapSMIMEConfig getInstance(Entry entry) throws ServiceException {
        String className = "com.zimbra.cs.account.ldap.LdapSMIMEConfigImpl";
        LdapSMIMEConfig instance = null;
   
        try {
            if (entry == null) {
                instance = (LdapSMIMEConfig)ExtensionUtil.findClass(className).newInstance();
            } else {
                instance = (LdapSMIMEConfig)ExtensionUtil.findClass(className).getConstructor(Entry.class).newInstance(entry);
            }
        } catch (ClassNotFoundException e) {     
            throw ServiceException.FAILURE("cannot instantiate " + className, e);
        } catch (IllegalArgumentException e) {
            throw ServiceException.FAILURE("cannot instantiate " + className, e);
        } catch (SecurityException e) {
            throw ServiceException.FAILURE("cannot instantiate " + className, e);
        } catch (InstantiationException e) {
            throw ServiceException.FAILURE("cannot instantiate " + className, e);
        } catch (IllegalAccessException e) {
            throw ServiceException.FAILURE("cannot instantiate " + className, e);
        } catch (InvocationTargetException e) {
            throw ServiceException.FAILURE("cannot instantiate " + className, e);
        } catch (NoSuchMethodException e) {
            throw ServiceException.FAILURE("cannot instantiate " + className, e);
        }
           
        if (instance == null) {
            throw ServiceException.FAILURE("cannot instantiate " + className, null);
        }
            
        return instance;
    }
        
    protected LdapSMIMEConfig() {}
    public abstract Set<String> getAllSMIMEAttributes();
    public abstract Map<String, Map<String, Object>> get(String configName) throws ServiceException;
    public abstract void modify(String configName, Map<String, Object> attrs) throws ServiceException;
    public abstract void remove(String configName) throws ServiceException;
    
    public abstract List<String> lookupPublicKeys(Account acct, String email) throws ServiceException;
};