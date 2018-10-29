package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.lmtpserver.LmtpCallback;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class EmailToSMS implements LmtpCallback {

	private static final EmailToSMS sInstance = new EmailToSMS();
	private static final String smsDomain = "esms.gov.in";

	private EmailToSMS() {
	}

	@Override
	public void afterDelivery(Account account, Mailbox mbox, String envelopeSender, String recipientEmail,
			Message newMessage) {
	}

	@Override
	public void forwardWithoutDelivery(Account account, Mailbox mbox, String envelopeSender, String recipientEmail,
			ParsedMessage pm) {
		try {
			sendEmailSMS(pm, account);
		} catch (ServiceException e) {
			ZimbraLog.mailbox.error("Failed to send SMS forwardWithoutDelivery ServiceException  ", e);
		}
	}

	private void sendEmailSMS(ParsedMessage pm, Account act) throws ServiceException {
		String recipients = pm.getRecipients();
		String sender = pm.getSender();
		String[] splitRecipients = recipients.split(",");
		String subject = pm.getSubject();
		String message = pm.getFragment(act.getLocale());
		String fullMessage = encode(subject + "\n" + message);

		for (int i = 0; i < splitRecipients.length; i++) {
			String rcptAddresses = splitRecipients[i];
			String[] splitMobNumber = rcptAddresses.indexOf("<") < 0 ? rcptAddresses.split("@")
					: rcptAddresses.split("<")[1].replaceAll(">", "").split("@");
			String mobNumber = splitMobNumber[0];
			String mobNumberDomain = splitMobNumber[1];

			if (mobNumberDomain.equalsIgnoreCase(smsDomain)) {
				sendsms(fullMessage, mobNumber, sender);
			}
		}
	}

	private void sendsms(String message, String mnumber, String sender) throws ServiceException {
		String smsApiUrl = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraSMSApiUrl);
		String smsUsername = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraSMSApiUsername);
		String smsPin = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraSMSApiPin);
		String smsSenderId = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraSMSApiSenderId);
		String url = smsApiUrl + "?username=" + smsUsername + "&pin=" + smsPin + "&message=" + message + "&mnumber="
				+ mnumber + "&signature=" + smsSenderId;
		URL obj;
		String errorMsg = null;

		try {
			obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			int respCode = con.getResponseCode();

			if(respCode == 200)
				ZimbraLog.mailbox.info("SMS sent successfully to %s, sender %s", mnumber, sender);
			else
				ZimbraLog.mailbox.info("SMS not sent to %s, error code %s, sender %s ", mnumber, respCode, sender);

		} catch (MalformedURLException e) {
			ZimbraLog.mailbox.warn("Unable to send mobile notification MalformedURLException", e);
		} catch (IOException e) {
			ZimbraLog.mailbox.warn("Unable to send mobile notification IOException", e);
		}
	}

	public static EmailToSMS getInstance() {
		return sInstance;
	}

	public static String encode(String url) {
		try {
			String encodeURL = URLEncoder.encode(url, "UTF-8");
			return encodeURL;
		} catch (UnsupportedEncodingException e) {
			return "Issue while encoding" + e.getMessage();
		}
	}

}
