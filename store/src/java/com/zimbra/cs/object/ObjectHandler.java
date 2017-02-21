/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Apr 30, 2004
 */
package com.zimbra.cs.object;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
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

    private final Zimlet        mObjectType;
    private final ZimletHandler mHandlerObject;
    private final ZimletConfig  mConfigObject;

    private ObjectHandler(Zimlet obj) throws ObjectHandlerException, ZimletException {
        mObjectType = obj;
        mHandlerObject = ZimletUtil.getHandler(obj.getName());
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
    public static synchronized List<ObjectHandler> getObjectHandlers() throws ServiceException {

        if (mHandlerList != null)
            return mHandlerList;

        mHandlerList = new ArrayList<ObjectHandler>();
        List<Zimlet> dots = Provisioning.getInstance().listAllZimlets();
        for (Iterator<Zimlet> it=dots.iterator(); it.hasNext();) {
            Zimlet dot = it.next();
            ObjectHandler handler = loadHandler(dot);
            if (handler != null) {
                mHandlerList.add(handler);
            }
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

    public void parse(String text, List<MatchedObject> matchedObjects, boolean firstMatchOnly)
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
