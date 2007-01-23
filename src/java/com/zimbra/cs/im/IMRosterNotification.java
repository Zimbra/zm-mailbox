package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;

public class IMRosterNotification extends IMNotification {
    
    List<IMNotification> mNots = new ArrayList<IMNotification>();
    
    void addEntry(IMNotification not) {
        mNots.add(not);
    }
    

    public Element toXml(Element parent) throws ServiceException {
        ZimbraLog.im.info("IMRosterNotification:");
        Element e = this.create(parent, "roster");
        
        for (IMNotification n : mNots) {
            n.toXml(e);
        }
        
        return e;
    }

}
