/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
    
    public interface ResultCallback {
        public void add(String field, String cert);
        public boolean continueWithNextConfig();
    }
    
    public abstract void lookupPublicKeys(Account acct, String email, ResultCallback resultCallback) 
    throws ServiceException;
};