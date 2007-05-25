package com.zimbra.cs.dav.resource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;

public class CalendarScheduleMessage extends CalendarObject {

	public CalendarScheduleMessage(DavContext ctxt, Message msg) throws ServiceException, DavException {
		super(ctxt, getPath(ctxt, msg), getAssociatedCalendarItem(ctxt, msg));
	}

	private static CalendarItem getAssociatedCalendarItem(DavContext ctxt, Message msg) throws ServiceException, DavException {
		Mailbox mbox = getMailbox(ctxt);
		Message.CalendarItemInfo calItemInfo = msg.getCalendarItemInfo(0);
		return mbox.getCalendarItemById(null, calItemInfo.getCalendarItemId());
	}
	
	private static String getPath(DavContext ctxt, Message msg) throws ServiceException, DavException {
		CalendarItem calItem = getAssociatedCalendarItem(ctxt, msg);
		return getCalendarPath(msg.getPath(), calItem.getUid());
	}
}
