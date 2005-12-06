package com.zimbra.cs.im;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;

public interface IMNotification {
    public Element toXml(Element parent) throws ServiceException;
}
