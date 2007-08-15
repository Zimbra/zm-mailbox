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
package com.zimbra.cs.service.im;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.cs.im.IMAddr;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.PrivacyList;
import com.zimbra.cs.im.PrivacyListEntry;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * 
 */
public class IMSetPrivacyList extends IMDocumentHandler {

    /* @see com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element, java.util.Map) */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element list = request.getElement(IMConstants.E_LIST);
        
        PrivacyList pl = new PrivacyList(list.getAttribute(IMConstants.A_NAME));
        
        for (Iterator<Element> iter = list.elementIterator(IMConstants.E_ITEM); iter.hasNext();) {
            Element item = iter.next();
            
            String actionStr = item.getAttribute(IMConstants.A_ACTION);
            PrivacyListEntry.Action action = PrivacyListEntry.Action.valueOf(actionStr);
            int order = (int)item.getAttributeLong(IMConstants.A_ORDER);
            if (order <= 0)
                throw ServiceException.INVALID_REQUEST("Order must be >= 0 in item "+item.toString(), null);
            String addr = item.getAttribute(IMConstants.A_ADDRESS);
            
            PrivacyListEntry entry = new PrivacyListEntry(new IMAddr(addr), order, action, PrivacyListEntry.BLOCK_ALL);
            
            try { 
                pl.addEntry(entry);
            } catch (PrivacyList.DuplicateOrderException e) {
                throw ServiceException.INVALID_REQUEST("Order must be unique for every item within list.  Problem at entry: "+item, e);
            }
        }
        
        Object lock = super.getLock(zsc);
        synchronized (lock) {
            IMPersona persona = getRequestedPersona(zsc, context, lock);
            assert(persona.getLock() == lock);
            persona.setPrivacyList(pl);
        }
        
        return zsc.createElement(IMConstants.IM_UPDATE_PRIVACY_LIST_RESPONSE);
    }
}
