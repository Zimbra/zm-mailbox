/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.Cos;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.LiquidLog;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class RenameCos extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminService.E_ID);
        String newName = request.getAttribute(AdminService.E_NEW_NAME);

	    Cos cos = prov.getCosById(id);
        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(id);

        String oldName = cos.getName();

        prov.renameCos(id, newName);

        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "RenameCos","name", oldName, "newName", newName})); 

        // get again with new name...

        cos = prov.getCosById(id);
        if (cos == null)
            throw ServiceException.FAILURE("unabled to get renamed cos: "+id, null);
	    Element response = lc.createElement(AdminService.RENAME_COS_RESPONSE);
	    GetCos.doCos(response, cos);
	    return response;
	}

}