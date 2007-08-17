package com.zimbra.cs.zclient.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

public class ZModifyTaskEvent extends ZModifyAppointmentEvent {

    public ZModifyTaskEvent(Element e) throws ServiceException {
        super(e);
    }

}
