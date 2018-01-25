package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SearchActionRequest;
import com.zimbra.soap.mail.message.SearchActionResponse;
import com.zimbra.soap.mail.message.SearchRequest;
import com.zimbra.soap.mail.type.BulkAction;
import com.zimbra.soap.type.SearchHit;

public class SearchAction extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        SearchActionRequest req = zsc.elementToJaxb(request);
        SearchRequest searchRequest = req.getSearchRequest();
        Account acct = mbox.getAccount();
        Server server = Provisioning.getInstance().getServer(acct);
        String url = URLUtil.getSoapURL(server, false);
        com.zimbra.soap.mail.message.SearchResponse resp = null;
        Element searchResponse;
        SoapHttpTransport transport = new SoapHttpTransport(url);
        transport.setTargetAcctId(acct.getId());
        try {
            transport.setAuthToken(octxt.getAuthToken().getEncoded());
            searchResponse = transport.invokeWithoutSession(JaxbUtil.jaxbToElement(searchRequest));
            resp = (com.zimbra.soap.mail.message.SearchResponse) JaxbUtil
                .elementToJaxb(searchResponse);
        } catch (AuthTokenException | IOException e) {
            throw ServiceException.FAILURE("Failed to execute search request", e);
        }
        List<SearchHit> searchHits = resp.getSearchHits();
        BulkAction action = req.getBulkAction();
        performAction(action, searchRequest, searchHits, mbox, octxt);
        SearchActionResponse searchActionResponse = new SearchActionResponse();
        return zsc.jaxbToElement(searchActionResponse);
    }
    
    public static void performAction(BulkAction action, SearchRequest searchRequest, List<SearchHit> searchHits, Mailbox mbox, OperationContext octxt) throws ServiceException {
        switch(action.getOp()) {
        case move :
            performMoveAction(action, searchRequest,searchHits,mbox, octxt);
            break;
        default :
            throw ServiceException.INVALID_REQUEST("Unsupported action", null);
        }
    }

    private static void performMoveAction(BulkAction action, SearchRequest searchRequest,
        List<SearchHit> searchHits, Mailbox mbox, OperationContext octxt) throws ServiceException {
        Folder folder = null;
        if (action.getFolder() != null) {
            folder = mbox.getFolderByPath(octxt, action.getFolder());
        } else {
            throw ServiceException.INVALID_REQUEST("Target folder name not provided", null);
        }
        for (SearchHit searchHit : searchHits) {
            String id = searchHit.getId();
            if ("message".equalsIgnoreCase(searchRequest.getSearchTypes())
                && folder.getDefaultView() == MailItem.Type.MESSAGE) {
                mbox.move(octxt, Integer.parseInt(id), MailItem.Type.MESSAGE, folder.getId());
            } else if ("appointment".equalsIgnoreCase(searchRequest.getSearchTypes())
                && folder.getDefaultView() == MailItem.Type.APPOINTMENT) {
                mbox.move(octxt, Integer.parseInt(id), MailItem.Type.APPOINTMENT, folder.getId());
            } else if ("task".equalsIgnoreCase(searchRequest.getSearchTypes())
                && folder.getDefaultView() == MailItem.Type.TASK) {
                mbox.move(octxt, Integer.parseInt(id), MailItem.Type.TASK, folder.getId());
            } else if ("contact".equalsIgnoreCase(searchRequest.getSearchTypes())
                && folder.getDefaultView() == MailItem.Type.CONTACT) {
                mbox.move(octxt, Integer.parseInt(id), MailItem.Type.CONTACT, folder.getId());
            } else if ("conversation".equalsIgnoreCase(searchRequest.getSearchTypes())
                && folder.getDefaultView() == MailItem.Type.CONVERSATION) {
                mbox.move(octxt, Integer.parseInt(id), MailItem.Type.CONVERSATION, folder.getId());
            } else if ("wiki".equalsIgnoreCase(searchRequest.getSearchTypes())
                && folder.getDefaultView() == MailItem.Type.WIKI) {
                mbox.move(octxt, Integer.parseInt(id), MailItem.Type.WIKI, folder.getId());
            } else if ("document".equalsIgnoreCase(searchRequest.getSearchTypes())
                && folder.getDefaultView() == MailItem.Type.DOCUMENT) {
                mbox.move(octxt, Integer.parseInt(id), MailItem.Type.DOCUMENT, folder.getId());
            } else if (folder.getDefaultView() == MailItem.Type.UNKNOWN) {
                mbox.move(octxt, Integer.parseInt(id), MailItem.Type.UNKNOWN, folder.getId());
            } else {
                throw ServiceException
                    .INVALID_REQUEST("Target folder type does not match search item type", null);
            }
        }
    }
}
