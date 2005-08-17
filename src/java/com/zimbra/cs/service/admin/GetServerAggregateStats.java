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
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetServerAggregateStats extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);

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
