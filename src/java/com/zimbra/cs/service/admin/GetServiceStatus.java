/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.db.DbPool;
import com.liquidsys.coco.db.DbServiceStatus;
import com.liquidsys.coco.db.DbPool.Connection;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetServiceStatus extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);

        Element response = lc.createElement(AdminService.GET_SERVICE_STATUS_RESPONSE);
	    Connection conn = null;
        try { 
            conn = DbPool.getConnection();
            List stats = DbServiceStatus.getStatus(conn.getConnection());
            doServiceStatus(response, stats);            
        } finally {
            DbPool.quietClose(conn);
        }
	    return response;
	}

	// <status server="..." service="..." t="...">{status}<status/>
    public static void doServiceStatus(Element e, List stats) throws ServiceException {
        for (Iterator it = stats.iterator(); it.hasNext(); ) {
            DbServiceStatus stat = (DbServiceStatus) it.next();
            Element s = e.addElement(AdminService.E_STATUS);
            s.addAttribute(AdminService.A_SERVER, stat.getServer());
            s.addAttribute(AdminService.A_SERVICE, stat.getService());
            s.addAttribute(AdminService.A_T, stat.getTime());
            s.setText(Integer.toString(stat.getStatus()));
        }
    }
}
