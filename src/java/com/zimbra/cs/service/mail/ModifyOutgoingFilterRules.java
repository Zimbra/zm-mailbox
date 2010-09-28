package com.zimbra.cs.service.mail;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.RuleManager;
import org.dom4j.QName;

/**
 */
public class ModifyOutgoingFilterRules extends ModifyFilterRules {

    @Override
    protected QName getResponseElementName() {
        return MailConstants.MODIFY_OUTGOING_FILTER_RULES_RESPONSE;
    }

    @Override
    protected void setXMLRules(Account account, Element rulesElem) throws ServiceException {
        RuleManager.setOutgoingXMLRules(account, rulesElem);
    }
}
