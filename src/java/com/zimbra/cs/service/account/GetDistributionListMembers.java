package com.zimbra.cs.service.account;

import java.util.Arrays;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.soap.ZimbraSoapContext;

public class GetDistributionListMembers extends AccountDocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        int limit = (int) request.getAttributeLong(AdminConstants.A_LIMIT, 0);
        if (limit < 0) {
            throw ServiceException.INVALID_REQUEST("limit" + limit + " is negative", null);
        }
        
        int offset = (int) request.getAttributeLong(AdminConstants.A_OFFSET, 0);
        if (offset < 0) {
            throw ServiceException.INVALID_REQUEST("offset" + offset + " is negative", null);
        }
        
        Element d = request.getElement(AdminConstants.E_DL);
        String value = d.getText();
        
        // TODO: use GAL for bug 11017 (for external groups)
        DistributionList distributionList = prov.get(DistributionListBy.name, value);
        
        if (!AccessManager.getInstance().canDo(zsc.getAuthToken(), distributionList, User.R_viewDistList, false))
            throw ServiceException.PERM_DENIED("can not access dl");
        
        if (distributionList == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);
            
        Element response = zsc.createElement(AccountConstants.GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE);
        encodeMembers(response, distributionList, offset, limit);

        return response;
    }
    
    public static Element encodeDistributionList(Element e, DistributionList d) throws ServiceException {
        Element distributionList = e.addElement(AccountConstants.E_DL);
        distributionList.addAttribute(AccountConstants.A_NAME, d.getUnicodeName());
        distributionList.addAttribute(AccountConstants.A_ID,d.getId());
        return distributionList;
    }
    
    private void encodeMembers(Element response, DistributionList distributionList, 
            int offset, int limit) throws ServiceException {
        String[] members = distributionList.getAllMembers();
        if (offset > 0 && offset >= members.length) {
            throw ServiceException.INVALID_REQUEST("offset " + offset + " greater than size " + members.length, null);
        }
        int stop = offset + limit;
        if (limit == 0) {
            stop = members.length;
        }
        if (stop > members.length) {
            stop = members.length;
        }
        
        Arrays.sort(members);
        
        Provisioning prov = Provisioning.getInstance();
        for (int i = offset; i < stop; i++) {
            Element eMember = response.addElement(AccountConstants.E_DLM).setText(members[i]);
            if (prov.isDistributionList(members[i]))
                eMember.addAttribute(AccountConstants.A_isDL, true);
        }
        
        response.addAttribute(AccountConstants.A_MORE, stop < members.length);
        response.addAttribute(AccountConstants.A_TOTAL, members.length);
    }
}
