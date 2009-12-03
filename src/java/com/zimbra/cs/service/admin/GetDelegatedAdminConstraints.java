package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.AttributeConstraint;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

public class GetDelegatedAdminConstraints extends AdminDocumentHandler {

    private static final String CONSTRAINT_ATTR = Provisioning.A_zimbraConstraint;
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Entry entry = getEntry(request);
        
        AdminAccessControl.GetAttrsRight gar = new AdminAccessControl.GetAttrsRight();
        gar.addAttr(Provisioning.A_zimbraConstraint);
        checkRight(zsc, context, entry, gar);
        
        Map<String, AttributeConstraint> constraints = AttributeConstraint.getConstraint(entry);
        
        Element response = zsc.createElement(AdminConstants.GET_DELEGATED_ADMIN_CONSTRAINTS_RESPONSE);
        
        // return constraints for requested attrs
        boolean hasRequestedAttrs = false;
        for (Element a : request.listElements(AdminConstants.E_A)) {
            hasRequestedAttrs = true;
        
            String attrName = a.getAttribute(AdminConstants.A_NAME);
            AttributeConstraint ac = constraints.get(attrName);
            if (ac != null) {
                Element eAttr = response.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_NAME, attrName);
                ac.toXML(eAttr);
            }
        }
        
        // no attr is specifically requested, return all
        if (!hasRequestedAttrs) {
            for (Map.Entry<String, AttributeConstraint> cons : constraints.entrySet()) {
                String attrName = cons.getKey();
                AttributeConstraint ac = cons.getValue();
                Element eAttr = response.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_NAME, attrName);
                ac.toXML(eAttr);
            }
        }
        
        return response;
    }
    
    
    static Entry getEntry(Element request) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        String typeStr = request.getAttribute(AdminConstants.A_TYPE);
        TargetType type = TargetType.fromCode(typeStr);
        
        if (type == TargetType.config) {
            // cannot specify id or name
            if (request.getAttribute(AdminConstants.A_ID, null) != null)
                throw ServiceException.INVALID_REQUEST("cannot specify id for type " + typeStr, null);
            
            if (request.getAttribute(AdminConstants.A_NAME, null) != null)
                throw ServiceException.INVALID_REQUEST("cannot specify name for type " + typeStr, null);
            
            return prov.getConfig();
        } else if (type == TargetType.cos) {
            String id = request.getAttribute(AdminConstants.A_ID, null);
            String name = request.getAttribute(AdminConstants.A_NAME, null);
            
            if (id != null) {
                Cos cos = prov.get(CosBy.id, id);
                if (cos == null)
                    throw AccountServiceException.NO_SUCH_COS(id);
                
                if (name != null) {
                    if (!name.equals(cos.getName()))
                        throw ServiceException.INVALID_REQUEST("Specified name " + name + " does not match entry name for the specified id " + id, null);
                }
                
                return cos;
            } else if (name != null) {
                Cos cos = prov.get(CosBy.name, name);
                if (cos == null)
                    throw AccountServiceException.NO_SUCH_COS(name);
                
                if (id != null) {
                    if (!id.equals(cos.getId()))
                        throw ServiceException.INVALID_REQUEST("Specified id " + id + " does not match id for the specified name " + name, null);
                }
                
                return cos;
            } else
                throw ServiceException.INVALID_REQUEST("neither id or name is specified", null);
            
        } else
            throw ServiceException.INVALID_REQUEST("invalid type " + typeStr, null);
    }

    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Need set attr right on attribute " + CONSTRAINT_ATTR);
    }
    
}
