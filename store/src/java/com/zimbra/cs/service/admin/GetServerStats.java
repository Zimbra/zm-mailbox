/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
            Element eStat = response.addElement(AdminConstants.E_STAT)
                .addAttribute(AdminConstants.A_NAME, name)
                .setText(stringVal);
            
            String description = ZimbraPerf.getDescription(name);
            if (description != null) {
                eStat.addAttribute(AdminConstants.A_DESCRIPTION, description);
            }
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
