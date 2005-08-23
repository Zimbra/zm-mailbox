/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.db.DbAggregateStat;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class GetServerAggregateStats extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        ZimbraContext lc = getZimbraContext(context);

        String statName = request.getAttribute(AdminService.E_STAT_NAME);
        long startTime = request.getAttributeLong(AdminService.E_START_TIME);
        long endTime = request.getAttributeLong(AdminService.E_END_TIME);
        int period = (int) request.getAttributeLong(AdminService.E_PERIOD);

	    Element response = lc.createElement(AdminService.GET_SERVER_AGGREGATE_STATS_RESPONSE);
	    Connection conn = null;
        try { 
            conn = DbPool.getConnection();
            List stats = DbAggregateStat.getStats(conn.getConnection(), statName, startTime, endTime, period);
            doServerAggregateStats(response, stats);            
        } finally {
            DbPool.quietClose(conn);
        }

	    return response;
	}

    public static void doServerAggregateStats(Element e, List stats) throws ServiceException {
        for (Iterator it = stats.iterator(); it.hasNext(); ) {
            DbAggregateStat stat = (DbAggregateStat) it.next();
            Element s = e.addElement(AdminService.E_S);
            s.addAttribute(AdminService.A_N, stat.getName());
            s.addAttribute(AdminService.A_T, stat.getTime());
            s.setText(stat.getValue());
        }
    }
}
