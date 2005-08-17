/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.db.DbTableMaintenance;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author bburtin
 */
public class MaintainTables extends WriteOpDocumentHandler {
    
	public Element handle(Element request, Map context) throws ServiceException {
        int numTables = DbTableMaintenance.runMaintenance();
        
        LiquidContext lc = getLiquidContext(context);
        Element response = lc.createElement(AdminService.MAINTAIN_TABLES_RESPONSE);
        response.addAttribute(AdminService.A_NUM_TABLES, numTables);
    	return response;
	}
}

