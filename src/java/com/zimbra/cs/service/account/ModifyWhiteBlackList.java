package com.zimbra.cs.service.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyWhiteBlackList extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canModifyOptions(zsc, account))
            throw ServiceException.PERM_DENIED("can not modify options");

        HashMap<String, Object> addrs = new HashMap<String, Object>();
        
        doList(request.getOptionalElement(AccountConstants.E_WHITE_LIST),
               Provisioning.A_amavisWhitelistSender,
               addrs);
        
        doList(request.getOptionalElement(AccountConstants.E_BLACK_LIST),
                Provisioning.A_amavisBlacklistSender,
                addrs);

        // call modifyAttrs and pass true to checkImmutable
        Provisioning.getInstance().modifyAttrs(account, addrs, true, zsc.getAuthToken());

        Element response = zsc.createElement(AccountConstants.MODIFY_WHITE_BLACK_LIST_RESPONSE);
        return response;
    }
    
    private void doList(Element eList, String attrName, HashMap<String, Object> addrs) {
        if (eList == null)
            return;
        
        // empty list, means delete all
        if (eList.getOptionalElement(AccountConstants.E_ADDR) == null) {
            StringUtil.addToMultiMap(addrs, attrName, "");
            return;
        }
        
        for (Element eAddr : eList.listElements(AccountConstants.E_ADDR)) {
            String attr = eAddr.getAttribute(AccountConstants.A_OP, "") + attrName;
            String value = eAddr.getText();
            StringUtil.addToMultiMap(addrs, attr, value);
        }
    }

}

