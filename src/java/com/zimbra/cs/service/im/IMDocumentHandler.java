package com.zimbra.cs.service.im;

import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMRouter;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

public abstract class IMDocumentHandler extends DocumentHandler {

    Object getLock(ZimbraContext zc) throws ServiceException {
        return super.getRequestedMailbox(zc);
    }
    
    IMPersona getRequestedPersona(ZimbraContext zc, Object lock) throws ServiceException {
        return IMRouter.getInstance().findPersona(zc.getOperationContext(), (Mailbox)lock, true);
    }
}
