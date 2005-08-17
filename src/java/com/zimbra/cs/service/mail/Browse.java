/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.mailbox.BrowseResult;
import com.liquidsys.coco.mailbox.BrowseResult.DomainItem;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.stats.StopWatch;
import com.liquidsys.soap.DocumentHandler;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class Browse extends DocumentHandler  {
    
    private StopWatch sWatch = StopWatch.getInstance("BrowseMsg");
    
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        
        try {
            LiquidContext lc = getLiquidContext(context);
            Mailbox mbox = getRequestedMailbox(lc);

            String browseBy = request.getAttribute("browseby", null);
            if (browseBy == null)
                browseBy = request.getAttribute(MailService.A_BROWSE_BY);

            BrowseResult browse;
            try {
                browse = mbox.browse(browseBy);
            } catch (IOException e) {
                throw ServiceException.FAILURE("IO error", e);
            }
            
            Element response = lc.createElement(MailService.BROWSE_RESPONSE);
            
            List result = browse.getResult();
            if (result != null) {
                for (Iterator mit = result.iterator(); mit.hasNext();) {
                    Object o = mit.next();
                    if (o instanceof String) {
                        response.addElement(MailService.E_BROWSE_DATA).setText((String) o);
                    } else if (o instanceof DomainItem) {
                        DomainItem domain = (DomainItem) o;
                        Element e = response.addElement(MailService.E_BROWSE_DATA).setText(domain.getDomain());
                        String flags = domain.getHeaderFlags();
                        if (!flags.equals(""))
                            e.addAttribute(MailService.A_BROWSE_DOMAIN_HEADER, flags);
                    } else {
                        throw new RuntimeException("unknown browse item: " + o.getClass().getName() + " " + o);
                    }
                }
            }
            return response;
        } finally {
            sWatch.stop(startTime);
        }
    }
}
