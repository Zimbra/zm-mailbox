/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.Config;
import com.liquidsys.coco.account.Cos;
import com.liquidsys.coco.account.Provisioning;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetCos extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
	public Element handle(Element request, Map context) throws ServiceException {
	    
        LiquidContext lc = getLiquidContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element d = request.getElement(AdminService.E_COS);
	    String key = d.getAttribute(AdminService.A_BY);
        String value = d.getText();

	    Cos cos = null;

        if (key.equals(BY_NAME)) {
            cos = prov.getCosByName(value);
        } else if (key.equals(BY_ID)) {
            cos = prov.getCosById(value);
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }
	    
        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(value);

	    Element response = lc.createElement(AdminService.GET_COS_RESPONSE);
        doCos(response, cos);

	    return response;
	}

	public static void doCos(Element e, Cos c) throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        Element cos = e.addElement(AdminService.E_COS);
        cos.addAttribute(AdminService.A_NAME, c.getName());
        cos.addAttribute(AdminService.E_ID, c.getId());
        Map attrs = c.getAttrs();
        for (Iterator mit=attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            boolean isCosAttr = !config.isInheritedAccountAttr(name);
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    Element attr = cos.addElement(AdminService.E_A);
                    attr.addAttribute(AdminService.A_N, name);
                    if (isCosAttr) 
                        attr.addAttribute(AdminService.A_C, "1");
                    attr.setText(sv[i]);
                }
            } else if (value instanceof String){
                Element attr = cos.addElement(AdminService.E_A);
                attr.addAttribute(AdminService.A_N, name);
                if (isCosAttr) 
                    attr.addAttribute(AdminService.A_C, "1");                
                attr.setText((String) value);
            }
        }
    }
}
