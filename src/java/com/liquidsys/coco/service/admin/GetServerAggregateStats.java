/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.db.DbAggregateStat;
import com.liquidsys.coco.db.DbPool;
import com.liquidsys.coco.db.DbPool.Connection;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;

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
