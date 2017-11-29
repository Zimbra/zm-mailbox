package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.contacts.ContactAffinityStore;
import com.zimbra.cs.contacts.RelatedContactsParams;
import com.zimbra.cs.contacts.RelatedContactsParams.AffinityTarget;
import com.zimbra.cs.contacts.RelatedContactsParams.AffinityType;
import com.zimbra.cs.contacts.RelatedContactsResults;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetRelatedContactsRequest;
import com.zimbra.soap.mail.message.GetRelatedContactsResponse;
import com.zimbra.soap.mail.type.RelatedContactResult;
import com.zimbra.soap.mail.type.RelatedContactsTarget;

public class GetRelatedContacts extends MailDocumentHandler {

    private static final int DEFAULT_CONTACT_SUGGEST_RESULT_LIMIT = 10;
    private static final int MAX_CONTACT_SUGGEST_RESULT_LIMIT = 500;

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        if (!acct.isFeatureRelatedContactsEnabled()) {
            throw ServiceException.FAILURE("Related Contacts feature is not enabled", null);
        }
        GetRelatedContactsRequest req = zsc.elementToJaxb(request);
        RelatedContactsParams params = new RelatedContactsParams(acct.getId());
        List<RelatedContactsTarget> targets = req.getTargets();
        if (targets.isEmpty()) {
            throw ServiceException.INVALID_REQUEST("must specify at least one target contact", null);
        }
        for (RelatedContactsTarget target: targets) {
            params.addTarget(AffinityTarget.fromSOAPTarget(target));
        }
        String requestedAffinity = req.getRequestedAffinity();
        if (!Strings.isNullOrEmpty(requestedAffinity)) {
            params.setRequestedAffinityType(AffinityType.of(requestedAffinity));
        }
        params.setLimit(parseLimit(req.getLimit()));
        long maxAge = acct.getRelatedContactsMaxAge();
        params.setDateCutoff(System.currentTimeMillis() - maxAge);
        params.setMinOccurCount(acct.getRelatedContactsMinConcurrenceCount());
        params.setIncludeIncomingMsgAffinity(acct.isContactAffinityEventLoggingEnabled());
        ContactAffinityStore store = ContactAffinityStore.getFactory().getContactAffinityStore(acct.getId());
        GetRelatedContactsResponse resp = new GetRelatedContactsResponse();
        RelatedContactsResults results = store.getRelatedContacts(params);
        for (RelatedContactResult result: results.getResults()) {
            resp.addRelatedContact(result);
        }
        return zsc.jaxbToElement(resp);
    }

    private int parseLimit(Integer providedLimit) {
        if (providedLimit == null || providedLimit <= 0) {
            return DEFAULT_CONTACT_SUGGEST_RESULT_LIMIT;
        } else {
            return Math.min(providedLimit, MAX_CONTACT_SUGGEST_RESULT_LIMIT);
        }
    }
}
