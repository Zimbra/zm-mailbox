package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

public class GetWhiteBlackList extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
         ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        String[] senders = account.getMultiAttr(Provisioning.A_amavisWhitelistSender);
        
        Element response = zsc.createElement(AccountConstants.GET_WHITE_BLACK_LIST_RESPONSE);
        
        // white list
        senders = account.getMultiAttr(Provisioning.A_amavisWhitelistSender);
        doList(response, AccountConstants.E_WHITE_LIST, senders);
        
        // black list
        senders = account.getMultiAttr(Provisioning.A_amavisBlacklistSender);
        doList(response, AccountConstants.E_BLACK_LIST, senders);
        
        return response;
    }

    private void doList(Element response, String list, String[] senders) {
        Element eList = response.addElement(list);
        
        for (String sender : senders)
            eList.addElement(AccountConstants.E_ADDR).setText(sender);
    }
}
