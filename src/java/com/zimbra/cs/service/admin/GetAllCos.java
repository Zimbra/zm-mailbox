/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.account.Cos;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetAllCos extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();
        List cos = prov.getAllCos();
        
        Element response = lc.createElement(AdminService.GET_ALL_COS_RESPONSE);        
        for (Iterator it = cos.iterator(); it.hasNext(); )
            GetCos.doCos(response, (Cos) it.next());
	    return response;
	}
}
