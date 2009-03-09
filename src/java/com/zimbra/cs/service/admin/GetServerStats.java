/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.stats.JmxServerStatsMBean;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.soap.ZimbraSoapContext;

public class GetServerStats extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        // Assemble list of requested stat names.
        List<Element> eStats = request.listElements(AdminConstants.E_STAT);
        Set<String> requestedNames = new HashSet<String>();
        for (int i = 0; i < eStats.size(); i++) {
            requestedNames.add(eStats.get(i).getAttribute(AdminConstants.A_NAME));
        }
        
        // Get latest values.
        Map<String, Long> stats = new TreeMap<String, Long>();
        boolean allStats = (requestedNames.size() == 0);
        
        try {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(JmxServerStatsMBean.class).getPropertyDescriptors();
            if (properties != null) {
                for (PropertyDescriptor property : properties) {
                    String name = property.getName();
                    if (allStats || requestedNames.contains(name)) {
                        Long value = (Long) property.getReadMethod().invoke(ZimbraPerf.getMonitoringStats(), (Object[]) null);
                        stats.put(name, value);
                        requestedNames.remove(name);
                    }
                }
            }
        } catch (IntrospectionException e) {
            throw ServiceException.FAILURE("Unable to retrieve server stats", e);
        } catch (InvocationTargetException e) {
            throw ServiceException.FAILURE("Unable to retrieve server stats", e);
        } catch (IllegalAccessException e) {
            throw ServiceException.FAILURE("Unable to retrieve server stats", e);
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
        for (String name : stats.keySet()) {
            String stringVal = toString(stats.get(name));
            response.addElement(AdminConstants.E_STAT)
                .addAttribute(AdminConstants.A_NAME, name)
                .setText(stringVal);
        }
        
        return response;
    }

    private static String toString(Long value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
