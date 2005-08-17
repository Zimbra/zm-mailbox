/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class NoOp extends DocumentHandler  {

	public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
	    return lc.createElement(MailService.NO_OP_RESPONSE);
	}
}
