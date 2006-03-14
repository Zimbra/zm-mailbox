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

package com.zimbra.cs.service.admin;

import org.dom4j.QName;

import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.soap.Element;

/**
 * @author jhahm
 */
public class GetAllCalendarResources extends GetAllAccounts {

    protected QName getResponseQName() {
        return AdminService.GET_ALL_CALENDAR_RESOURCES_RESPONSE;
    }
    
    protected void doDomain(final Element e, Domain d)
    throws ServiceException {
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            public void visit(com.zimbra.cs.account.NamedEntry entry)
            throws ServiceException {
                ToXML.encodeCalendarResource(e, (CalendarResource) entry, true);
            }
        };
        d.getAllCalendarResources(visitor);
    }
}
