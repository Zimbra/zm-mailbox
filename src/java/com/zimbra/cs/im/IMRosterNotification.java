/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2010 Zimbra, Inc.
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
package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;

public class IMRosterNotification extends IMNotification {
    
    List<IMNotification> mNots = new ArrayList<IMNotification>();
    
    void addEntry(IMNotification not) {
        mNots.add(not);
    }
    
    public Element toXml(Element parent) throws ServiceException {
        ZimbraLog.im.info("IMRosterNotification:");
        Element e = this.create(parent, "roster");
        
        for (IMNotification n : mNots) {
            n.toXml(e);
        }
        
        return e;
    }

}
