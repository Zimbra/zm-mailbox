package com.zimbra.cs.service.account;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

public class SubscribeDistributionList extends AccountDocumentHandler {
    
    private enum SubscribeOp {
        subscribe,
        unsubscribe;
        
        private static SubscribeOp fromString(String str) throws ServiceException {
            try {
                return SubscribeOp.valueOf(str);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid op: " + str, e);
            }
        }
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        SubscribeOp op = SubscribeOp.fromString(request.getAttribute(AccountConstants.A_OP));

        Element d = request.getElement(AccountConstants.E_DL);
        String key = d.getAttribute(AccountConstants.A_BY);
        String value = d.getText();
        
        Group group = prov.getGroup(Key.DistributionListBy.fromString(key), value);
        
        if (group == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);
        }
        
        // TODO: check subscribe policy
        
        Account acct = getRequestedAccount(zsc);
        String[] members = new String[]{acct.getName()};
        
        if (op == SubscribeOp.subscribe) {
            prov.addGroupMembers(group, members);
        } else {
            prov.removeGroupMembers(group, members);
        }
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "SubscribeDistributionList","name", group.getName(), 
                    "op", op.name(),        
                    "member", Arrays.deepToString(members)})); 
        
        Element response = zsc.createElement(AccountConstants.SUBSCRIBE_DISTRIBUTION_LIST_RESPONSE);

        return response;
    }
}
