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

package com.zimbra.cs.gal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.directory.SearchResult;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.extension.ExtensionUtil;

public abstract class GalGroupHandler {

    public abstract boolean isGroup(SearchResult sr);
    
    public abstract String[] getMembers(ZimbraLdapContext zlc, SearchResult sr);
    
    
    private static Map<String, HandlerInfo> sHandlers = new ConcurrentHashMap<String,HandlerInfo>();
    
    private static class HandlerInfo {
        Class<? extends GalGroupHandler> mClass;

        public GalGroupHandler getInstance() {
            GalGroupHandler handler;
            try {
                handler = mClass.newInstance();
            } catch (InstantiationException e) {
                handler = newDefaultHandler();
            } catch (IllegalAccessException e) {
                handler = newDefaultHandler();
            }
            return handler;
        }
    }
    
    private static GalGroupHandler newDefaultHandler() {
        return new ZimbraGalGroupHandler();
    }
    
    private static HandlerInfo loadHandler(String className) {
        HandlerInfo handlerInfo = new HandlerInfo();

        try {
            handlerInfo.mClass = ExtensionUtil.findClass(className).asSubclass(GalGroupHandler.class);
        } catch (ClassNotFoundException e) {
            // miss configuration or the extension is disabled
            ZimbraLog.gal.warn("GAL group handler %s not found, default to ZimbraGalGroupHandler", className);
            // Fall back to ZimbraGalGroupHandler
            handlerInfo.mClass = ZimbraGalGroupHandler.class;
        }
        return handlerInfo;
    }
    
    public static GalGroupHandler getHandler(String className) {
        if (StringUtil.isNullOrEmpty(className)) {
            return newDefaultHandler();
        }
        
        HandlerInfo handlerInfo = sHandlers.get(className);
        
        if (handlerInfo == null) {
            handlerInfo = loadHandler(className);
            sHandlers.put(className, handlerInfo);
        }
        
        return handlerInfo.getInstance();
    }
}
