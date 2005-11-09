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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 30, 2004
 */
package com.zimbra.cs.object;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public abstract class ObjectHandler {

    private static Log mLog = LogFactory.getLog(ObjectHandler.class);
    
    private static Map mHandlers = new HashMap();
    private static List mHandlerList;
    
    private ObjectType mObjectType;
    
    // TODO: this caches handlers for the duration of the VM, which is ok if the Indexer
    // exits/restarts. Might need to add modified column and periodically refresh and/or
    // provide a way to purge the cache
    public static synchronized List getObjectHandlers() throws ServiceException {
        
        if (mHandlerList != null)
            return mHandlerList;
        
        mHandlerList = new ArrayList();
        List dots = Provisioning.getInstance().getObjectTypes();
        for (Iterator it=dots.iterator(); it.hasNext();) {
            ObjectType dot = (ObjectType) it.next();
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
    private static synchronized ObjectHandler loadHandler(ObjectType dot) {
        ObjectHandler handler = null;
        String clazz = dot.getHandlerClass();
        if (clazz == null)
            return null;
        if (clazz.indexOf('.') == -1)
            clazz = "com.zimbra.cs.object.handler." + clazz;
        try {
            handler = (ObjectHandler) Class.forName(clazz).newInstance();
            handler.mObjectType = dot;
            mHandlers.put(dot.getType(), handler);
        } catch (Exception e) {
            if (mLog.isErrorEnabled())
                mLog.error("loadHandler caught exception", e);
        }
        return handler;
    }

    public abstract void parse(String text, List matchedObjects, boolean firstMatchOnly)
            throws ObjectHandlerException; 
    
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
    
    public boolean storeMatched() {
        return mObjectType.isStoreMatched();
    }
}
