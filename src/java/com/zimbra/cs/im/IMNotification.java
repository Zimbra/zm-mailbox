/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.session.Session;

public abstract class IMNotification extends Session.ExternalEventNotification {
    public void addElement(Element notify) {
        Element eIM = notify.getOptionalElement(ZimbraNamespace.E_IM);
        if (eIM == null) {
            eIM = notify.addUniqueElement(ZimbraNamespace.E_IM);
        }
        try {
            toXml(eIM);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("error serializing IM notification; skipping", e);
        }
    }
    
    abstract public Element toXml(Element parent) throws ServiceException;
    
    protected static Element create(Element parent, String typeName) {
        return parent.addElement("n").addAttribute("type", typeName);
    }
}
