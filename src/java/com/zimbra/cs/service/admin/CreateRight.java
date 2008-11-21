package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.soap.ZimbraSoapContext;

public class CreateRight extends RightDocumentHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Element eRight = request.getElement(AdminConstants.E_RIGHT);
        String rightName = eRight.getAttribute(AdminConstants.A_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>(); 
        
        RightCommand.XMLToAttrs(eRight, attrs);
        
        Right ldapRight = prov.createRight(rightName, attrs);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateRight","name", rightName}, attrs));         

        Element response = lc.createElement(AdminConstants.CREATE_RIGHT_RESPONSE);
        doRight(response, ldapRight);

        return response;
    }
    

    public static void doRight(Element e, Right r) throws ServiceException {
        Element eRight = e.addElement(AdminConstants.E_RIGHT);
        eRight.addAttribute(AdminConstants.A_NAME, r.getName());
        eRight.addAttribute(AdminConstants.E_ID, r.getId());
        Map attrs = r.getAttrs();
        for (Iterator mit=attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    Element attr = eRight.addElement(AdminConstants.E_A);
                    attr.addAttribute(AdminConstants.A_N, name);
                    attr.setText(sv[i]);
                }
            } else if (value instanceof String){
                Element attr = eRight.addElement(AdminConstants.E_A);
                attr.addAttribute(AdminConstants.A_N, name);
                attr.setText((String) value);
            }
        }
    }

}
