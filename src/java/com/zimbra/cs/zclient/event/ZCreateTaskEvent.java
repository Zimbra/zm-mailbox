package com.zimbra.cs.zclient.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

public class ZCreateTaskEvent extends ZCreateAppointmentEvent {
    public ZCreateTaskEvent(Element e) throws ServiceException {
        super(e);
    }
}
