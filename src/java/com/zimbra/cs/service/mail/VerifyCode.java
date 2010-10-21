package com.zimbra.cs.service.mail;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Map;

/**
 * Handler for verifying the device code.
 */
public class VerifyCode extends MailDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        String emailAddr = request.getAttribute(MailConstants.A_ADDRESS);
        boolean success = false;
        if (SendVerificationCode.emailToCodeMap.containsKey(emailAddr)) {
            String code = request.getAttribute(MailConstants.A_VERIFICATION_CODE);
            if (SendVerificationCode.emailToCodeMap.get(emailAddr).equals(code)) {
                Account account = getRequestedAccount(zsc);
                account.setPrefCalendarReminderDeviceEmail(emailAddr);
                success = true;
                SendVerificationCode.emailToCodeMap.remove(emailAddr);
            } else {
                ZimbraLog.misc.debug("Invalid verification code");
            }
        } else {
            ZimbraLog.misc.debug("Verification code for %s has either not been generated or has expired", emailAddr);
        }
        return zsc.createElement(MailConstants.VERIFY_CODE_RESPONSE).addAttribute(MailConstants.A_VERIFICATION_SUCCESS, success ? "1" : "0");
    }
}
