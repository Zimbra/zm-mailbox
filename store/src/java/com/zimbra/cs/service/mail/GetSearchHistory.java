package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.history.SearchHistory;
import com.zimbra.cs.index.history.SearchHistory.SearchHistoryParams;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetSearchHistoryRequest;
import com.zimbra.soap.mail.message.GetSearchHistoryResponse;
import com.zimbra.soap.mail.message.SearchHistoryResponse;

public class GetSearchHistory extends MailDocumentHandler {

    protected static int DEFAULT_SEARCH_HISTORY_RESULT_LIMIT = 5;
    protected static int MAX_SEARCH_HISTORY_RESULT_LIMIT = 100;

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        GetSearchHistoryRequest req = zsc.elementToJaxb(request);
        SearchHistoryParams params = new SearchHistoryParams();
        params.setNumResults(parseLimit(req.getLimit()));
        GetSearchHistoryResponse resp = new GetSearchHistoryResponse();
        putSearchHistory(octxt, zsc,params, resp);
        return zsc.jaxbToElement(resp);
    }

    protected void putSearchHistory(OperationContext octxt, ZimbraSoapContext zsc,
            SearchHistoryParams params, SearchHistoryResponse resp) throws ServiceException {
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        if (SearchHistory.featureEnabled(acct)) {
            List<String> results =  mbox.getSearchHistory(octxt, params);
            resp.setSearches(results);
        } else {
            throw ServiceException.FAILURE("search history is not enabled", null);
        }
    }

    protected int parseLimit(Integer providedLimit) {
        if (providedLimit == null || providedLimit <= 0) {
            return DEFAULT_SEARCH_HISTORY_RESULT_LIMIT;
        } else {
            return Math.min(providedLimit, MAX_SEARCH_HISTORY_RESULT_LIMIT);
        }
    }
}
