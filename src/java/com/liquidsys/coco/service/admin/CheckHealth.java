/*
 * Created on 2005. 1. 26.
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.db.DbStatus;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;

/**
 * @author jhahm
 */
public class CheckHealth extends AdminDocumentHandler {

    /* (non-Javadoc)
     * @see com.liquidsys.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element document, Map context)
            throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
        Element response = lc.createElement(AdminService.CHECK_HEALTH_RESPONSE);

        boolean dir = Provisioning.getInstance().healthCheck();
        boolean db = DbStatus.healthCheck();
        boolean healthy = dir && db;

        response.addAttribute(AdminService.A_HEALTHY, healthy);
        return response;
    }

    public boolean needsAuth(Map context) {
        // Must return false to leave the auth decision entirely up to
        // needsAdminAuth().
    	return false;
    }

    /**
     * No auth required if client is localhost.  Otherwise, admin auth is
     * required.
     * @param context
     * @return
     */
    public boolean needsAdminAuth(Map context) {
        return !clientIsLocal(context);
    }
}
