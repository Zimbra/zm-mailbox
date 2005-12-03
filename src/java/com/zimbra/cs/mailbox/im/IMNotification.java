package com.zimbra.cs.mailbox.im;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;

public interface IMNotification {
    public Element toXml(Element parent) throws ServiceException;
}
