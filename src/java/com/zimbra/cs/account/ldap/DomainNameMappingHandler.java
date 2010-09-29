/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.extension.ExtensionUtil;

public abstract class DomainNameMappingHandler {
    
    private static Map<String, HandlerInfo> sHandlers = new ConcurrentHashMap<String,HandlerInfo>();
    private static Log sLog = LogFactory.getLog(DomainNameMappingHandler.class);
    
    
    /**
     * Given a foreign name and params, return a zimbra account name
     * 
     * @param foreignName
     * @param params
     * @return 
     */
    abstract String mapName(String foreignName, String params) throws ServiceException;
    
    
    private static class HandlerInfo {
        Class<? extends DomainNameMappingHandler> mClass;

        public DomainNameMappingHandler getInstance() {
            DomainNameMappingHandler handler;
            try {
                handler = mClass.newInstance();
            } catch (InstantiationException e) {
                handler = new UnknownDomainNameMappingHandler();
            } catch (IllegalAccessException e) {
                handler = new UnknownDomainNameMappingHandler();
            }
            return handler;
        }
    }
    
    private static HandlerInfo loadHandler(HandlerConfig handlerConfig) {
        HandlerInfo handlerInfo = new HandlerInfo();
        String className = handlerConfig.getClassName();

        try {
            handlerInfo.mClass = ExtensionUtil.findClass(className).asSubclass(DomainNameMappingHandler.class);
        } catch (ClassNotFoundException e) {
            // miss configuration or the extension is disabled
            sLog.warn("Domain name mapping handler %s for application %s not found",
                    className, handlerConfig.getApplicaiton());
            // Fall back to UnknownDomainNameMappingHandler
            handlerInfo.mClass = UnknownDomainNameMappingHandler.class;
        }
        return handlerInfo;
    }
    
    private static DomainNameMappingHandler getHandler(HandlerConfig handlerConfig) {
        String key = handlerConfig.getClassName();
        HandlerInfo handlerInfo = sHandlers.get(key);
        
        if (handlerInfo == null) {
            handlerInfo = loadHandler(handlerConfig);
            sHandlers.put(key, handlerInfo);
        }
        
        return handlerInfo.getInstance();
    }
    
    private static class UnknownDomainNameMappingHandler extends DomainNameMappingHandler {
        @Override
        String mapName(String foreignName, String params) {
            return null; // should never be called
        }
    }
    
    static class HandlerConfig {
        String mApplication;
        String mClassName;
        String mParams;
        
        private HandlerConfig(String application, String className, String params) {
            mApplication = application;
            mClassName = className;
            mParams =params;
        }
        
        private String getApplicaiton() {
            return mApplication;
        }
        
        private String getClassName() {
            return mClassName;
        }
        
        private String getParams() {
            return mParams;
        }
    }
    
    static HandlerConfig getHandlerConfig(Domain domain, String application) {
        Map<String, HandlerConfig> handlers = 
            (Map<String, HandlerConfig>)domain.getCachedData(EntryCacheDataKey.DOMAIN_FOREIGN_NAME_HANDLERS.getKeyName());
        
        if (handlers == null) {
            handlers = new HashMap<String, HandlerConfig>();
            
            String[] handlersRaw = domain.getForeignNameHandler();
            for (String handlerRaw : handlersRaw) {
                // handlers are in the format of {application}:{class name}[:{params}]
                int idx1 = handlerRaw.indexOf(":");
                if (idx1 != -1) {
                    String app = handlerRaw.substring(0, idx1);
                    String className = handlerRaw.substring(idx1+1);
                    String params = null;
                    int idx2 = className.indexOf(":");
                    if (idx2 != -1) {
                        params = className.substring(idx2+1);
                        className = className.substring(0, idx2);
                    }
                    handlers.put(app, new HandlerConfig(app, className, params));
                }
            }
            
            handlers = Collections.unmodifiableMap(handlers);
            domain.setCachedData(EntryCacheDataKey.DOMAIN_FOREIGN_NAME_HANDLERS.getKeyName(), handlers);
        }
        
        return handlers.get(application);
    }

    static String mapName(HandlerConfig handlerConfig, String foreignName) throws ServiceException {
        DomainNameMappingHandler handler = getHandler(handlerConfig);
        
        if (handler instanceof UnknownDomainNameMappingHandler)
            throw ServiceException.FAILURE("unable to load domain name mapping handler " + 
                    handlerConfig.getClassName() + " for application:" + handlerConfig.getApplicaiton(), null);

        return handler.mapName(foreignName, handlerConfig.getParams());
    }


    static class DummyHandler extends DomainNameMappingHandler {
        public String mapName(String foreignName, String params) throws ServiceException{
            return "user2@phoebe.mbp";
        }
    }
    
    public static void main(String[] args) throws Exception {

        Provisioning prov = Provisioning.getInstance();
        
        Domain domain = prov.get(Provisioning.DomainBy.name, "phoebe.mbp");
        domain.addForeignName("app1:name1");
        domain.addForeignName("app2:name2");
        domain.addForeignName("app3:name3");
        
        domain.addForeignNameHandler("app1:com.zimbra.cs.account.ldap.DomainNameMappingHandler$DummyHandler:p1, p2, p3");
        
        Account acct;
        
        acct = prov.getAccountByForeignName("user1@name1", "app1", null);
        acct = prov.getAccountByForeignName("user1@name1", "app1", null); // test cache
        System.out.println(acct.getName() + "(expecting user2@phoebe.mbp)");
        
        acct = prov.getAccountByForeignName("user1@name2", "app2", null);
        System.out.println(acct.getName() + "(expecting user1@phoebe.mbp)");

        acct = prov.getAccountByForeignName("user1", "app3", domain);  // with a supplied domain
        System.out.println(acct.getName() + "(expecting user1@phoebe.mbp)");
        
        acct = prov.getAccountByForeignName("user1@name3", "app2", null);
        System.out.println(acct + "(expecting null)");
        
    }
}
