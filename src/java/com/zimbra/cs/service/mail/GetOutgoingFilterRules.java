package com.zimbra.cs.service.mail;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.RuleManager;
import org.dom4j.QName;

/**
 */
public class GetOutgoingFilterRules extends GetFilterRules {

    @Override
    protected QName getResponseElementName() {
        return MailConstants.GET_OUTGOING_FILTER_RULES_RESPONSE;
    }

    @Override
    protected Element getRulesAsXML(Account account, Element.ElementFactory elementFactory) throws ServiceException {
        return RuleManager.getOutgoingRulesAsXML(elementFactory, account);
    }
}
