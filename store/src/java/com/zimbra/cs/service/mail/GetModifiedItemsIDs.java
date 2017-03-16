package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.GetModifiedItemsIDsRequest;
import com.zimbra.soap.account.message.GetModifiedItemsIDsResponse;

public class GetModifiedItemsIDs extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        GetModifiedItemsIDsRequest req = zsc.elementToJaxb(request);
        
        List<Integer> itemIds = mbox.getIdsOfModifiedItemsInFolder(octxt, req.getModSeq(), req.getFolderId());
        GetModifiedItemsIDsResponse resp = new GetModifiedItemsIDsResponse();
        resp.setIds(itemIds);
        return zsc.jaxbToElement(resp);
    }
}
