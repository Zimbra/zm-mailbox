package com.zimbra.cs.service.im;

import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMRouter;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class IMDocumentHandler extends DocumentHandler {

    Object getLock(ZimbraSoapContext zc) throws ServiceException {
        return super.getRequestedMailbox(zc);
    }
    
    IMPersona getRequestedPersona(ZimbraSoapContext zc, Object lock) throws ServiceException {
        return IMRouter.getInstance().findPersona(zc.getOperationContext(), (Mailbox)lock, true);
    }
}
