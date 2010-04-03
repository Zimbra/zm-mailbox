/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.soap.ZimbraSoapContext;

public class GetServerStats extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Server localServer = Provisioning.getInstance().getLocalServer();
        checkRight(zsc, context, localServer, Admin.R_getServerStats);
        
        // Assemble list of requested stat names.
        List<Element> eStats = request.listElements(AdminConstants.E_STAT);
        Set<String> requestedNames = new HashSet<String>();
        for (int i = 0; i < eStats.size(); i++) {
            requestedNames.add(eStats.get(i).getAttribute(AdminConstants.A_NAME));
        }
        
        // Get latest values.
        Map<String, Object> allStats = ZimbraPerf.getStats();
        Map<String, Object> returnedStats = new TreeMap<String, Object>();
        boolean returnAllStats = (requestedNames.size() == 0);
        
        for (String name : allStats.keySet()) {
            if (returnAllStats || requestedNames.contains(name)) {
                returnedStats.put(name, allStats.get(name));
                requestedNames.remove(name);
            }
        }
        
        if (requestedNames.size() != 0) {
            StringBuilder buf = new StringBuilder("Invalid stat name");
            if (requestedNames.size() > 1) {
                buf.append("s");
            }
            buf.append(": ").append(StringUtil.join(", ", requestedNames));
            throw ServiceException.FAILURE(buf.toString(), null);
        }
        
        // Send response.
        Element response = zsc.createElement(AdminConstants.GET_SERVER_STATS_RESPONSE);
        for (String name : returnedStats.keySet()) {
            String stringVal = toString(returnedStats.get(name));
            response.addElement(AdminConstants.E_STAT)
                .addAttribute(AdminConstants.A_NAME, name)
                .setText(stringVal);
        }
        
        return response;
    }

    private static String toString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double || value instanceof Float) {
            return String.format("%.2f", value);
        } else {
            return value.toString();
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getServerStats);
    }
}
