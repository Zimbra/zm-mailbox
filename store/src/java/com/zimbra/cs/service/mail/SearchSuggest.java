package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.index.history.SearchHistory.SearchHistoryParams;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SearchSuggestRequest;
import com.zimbra.soap.mail.message.SearchSuggestResponse;

public class SearchSuggest extends GetSearchHistory {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        SearchSuggestRequest req = zsc.elementToJaxb(request);
        SearchHistoryParams params = new SearchHistoryParams();
        params.setNumResults(parseLimit(req.getLimit()));
        params.setPrefix(req.getQuery());
        SearchSuggestResponse resp = new SearchSuggestResponse();
        putSearchHistory(octxt, zsc,params, resp);
        return zsc.jaxbToElement(resp);
    }
}
