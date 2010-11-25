package com.zimbra.cs.service.mail;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Map;

/**
 * Unsets the zimbraCalendarReminderDeviceEmail account attr
 */
public class InvalidateReminderDevice extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        String emailAddr = request.getAttribute(MailConstants.A_ADDRESS);
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        if (emailAddr.equals(account.getCalendarReminderDeviceEmail()))
            account.unsetCalendarReminderDeviceEmail();
        else
            throw ServiceException.INVALID_REQUEST(
                    "Email address is not same as the " + Provisioning.A_zimbraCalendarReminderDeviceEmail + " attr value", null);
        return zsc.createElement(MailConstants.INVALIDATE_REMINDER_DEVICE_RESPONSE);
    }
}
