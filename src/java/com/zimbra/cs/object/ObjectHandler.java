/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Apr 30, 2004
 */
package com.zimbra.cs.object;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.zimlet.ZimletConfig;
import com.zimbra.cs.zimlet.ZimletException;
import com.zimbra.cs.zimlet.ZimletHandler;
import com.zimbra.cs.zimlet.ZimletUtil;

/**
 * @author schemers
 */
public class ObjectHandler {

    private static Log mLog = LogFactory.getLog(ObjectHandler.class);
    
    private static List<ObjectHandler> mHandlerList;

    private Zimlet        mObjectType;
    private ZimletHandler mHandlerObject;
    private ZimletConfig  mConfigObject;

    private ObjectHandler(Zimlet obj) throws ObjectHandlerException, ZimletException {
        mObjectType = obj;
    	mHandlerObject = (ZimletHandler) ZimletUtil.getHandler(obj.getName());
    	if (mHandlerObject == null) {
    		throw new ObjectHandlerException("null handler for "+obj.getType());
    	}
       	mConfigObject = new ZimletConfig(obj.getHandlerConfig());
       	
        String regex = obj.getServerIndexRegex();
        if (regex != null) {
        	mConfigObject.setRegExValue(regex);
        }
    }
    
    // TODO: this caches handlers for the duration of the VM, which is ok if the Indexer
    // exits/restarts. Might need to add modified column and periodically refresh and/or
    // provide a way to purge the cache
    public static synchronized List getObjectHandlers() throws ServiceException {
        
        if (mHandlerList != null)
            return mHandlerList;
        
        mHandlerList = new ArrayList<ObjectHandler>();
        List dots = Provisioning.getInstance().getObjectTypes();
        for (Iterator it=dots.iterator(); it.hasNext();) {
            Zimlet dot = (Zimlet) it.next();
            ObjectHandler handler = loadHandler(dot);
            if (handler != null)
                mHandlerList.add(handler);
        }
        return mHandlerList;
    }
    
    /**
     * @param type
     * @return
     */
    private static synchronized ObjectHandler loadHandler(Zimlet dot) {
        ObjectHandler handler = null;
        String clazz = dot.getHandlerClassName();
        if (clazz == null)
            return null;

        try {
            handler = new ObjectHandler(dot);
        } catch (Exception e) {
            if (mLog.isErrorEnabled())
                mLog.error("loadHandler caught exception", e);
        }
        return handler;
    }

    public void parse(String text, List matchedObjects, boolean firstMatchOnly)
            throws ObjectHandlerException {
    	try {
    		String[] matchedStrings = mHandlerObject.match(text, mConfigObject);
    		for (int i = 0; i < matchedStrings.length; i++) {
    			MatchedObject mo = new MatchedObject(this, matchedStrings[i]);
    			matchedObjects.add(mo);
    			if (firstMatchOnly)
    				return;
    		}
    	} catch (ZimletException ze) {
    		throw new ObjectHandlerException("error running ZimletHandler " + mObjectType.getType(), ze);
    	}
    }
    
    public String getType() {
        return mObjectType.getType();
    }
    
    public String getHandlerConfig() {
        return mObjectType.getHandlerConfig();
    }
    
    public String getDescription() {
        return mObjectType.getDescription();
    }
    
    public boolean isIndexingEnabled() {
        return mObjectType.isIndexingEnabled();
    }
}
