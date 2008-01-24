/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.account.ToXML;

/**
 * @author jhahm
 */
public class GetAllCalendarResources extends GetAllAccounts {

    protected QName getResponseQName() {
        return AdminConstants.GET_ALL_CALENDAR_RESOURCES_RESPONSE;
    }
    
    /*
     * server s is not used, need to use the same signature as GetAllAccounts.doDomain 
     * so the overridden doDomain is called.
     */
    protected void doDomain(final Element e, Domain d, Server s)
    throws ServiceException {
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            public void visit(com.zimbra.cs.account.NamedEntry entry) {
                ToXML.encodeCalendarResourceOld(e, (CalendarResource) entry, true);
            }
        };
        Provisioning.getInstance().getAllCalendarResources(d, visitor);
    }
}
