package com.zimbra.cs.service.account;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.SaveIMAPSubscriptionsRequest;
import com.zimbra.soap.account.message.SaveIMAPSubscriptionsResponse;

public class SaveIMAPSubscriptions extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }

        SaveIMAPSubscriptionsRequest req = JaxbUtil.elementToJaxb(request);
        if(req != null) {
            MetadataList slist = new MetadataList();
            Set<String> subs = req.getSubscriptions();
            if (subs != null && !subs.isEmpty()) {
                for (String sub : subs)
                    slist.add(sub);
            }
            getRequestedMailbox(zsc).setConfig(getOperationContext(zsc, context), AccountUtil.SN_IMAP, new Metadata().put(AccountUtil.FN_SUBSCRIPTIONS, slist));
        }

        SaveIMAPSubscriptionsResponse resp = new SaveIMAPSubscriptionsResponse();
        return JaxbUtil.jaxbToElement(resp);
    }
}