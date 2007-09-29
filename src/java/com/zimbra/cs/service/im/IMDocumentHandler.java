/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.im;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMRouter;
import com.zimbra.cs.mailbox.Mailbox;
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
