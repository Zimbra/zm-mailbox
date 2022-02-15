package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetModifiedItemsIDsRequest;
import com.zimbra.soap.mail.message.GetModifiedItemsIDsResponse;

public class GetModifiedItemsIDs extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        GetModifiedItemsIDsRequest req = zsc.elementToJaxb(request);
        ItemId iid = new ItemId(req.getFolderId(), mbox.getAccountId());
        List<Integer> itemIds = mbox.getIdsOfModifiedItemsInFolder(octxt, req.getModSeq(), iid.getId());
        GetModifiedItemsIDsResponse resp = new GetModifiedItemsIDsResponse();
        resp.setMids(itemIds);
        return zsc.jaxbToElement(resp);
    }
}
