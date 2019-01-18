package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.*;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.lmtpserver.LmtpCallback;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

public class EmailToSMS implements LmtpCallback {

	private static final EmailToSMS sInstance = new EmailToSMS();
	private static final String smsDomain = "esms.gov.in";
	static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();

	private EmailToSMS() {
	}

	@Override
	public void afterDelivery(Account account, Mailbox mbox, String envelopeSender, String recipientEmail,
			Message newMessage) {
				try {
					sendEmailSMS(recipientEmail, newMessage);
				} catch (ServiceException | MessagingException e) {
					ZimbraLog.mailbox.error("Failed to send SMS afterDelivery ServiceException  ", e);
				}
	}

	@Override
	public void forwardWithoutDelivery(Account account, Mailbox mbox, String envelopeSender, String recipientEmail,
			ParsedMessage pm) {
	}

	private void sendEmailSMS(String recipient, Message zMsg) throws ServiceException,MessagingException{
		Address[] recipients = zMsg.getParsedMessage().getMimeMessage().getAllRecipients();
		String[] recipientEmail = recipient.split("@");
		String recipientEmailDomain = recipientEmail[1];
		String sender = zMsg.getSender();
		String subject = zMsg.getSubject();
		MimeMessage mimeMsg =  zMsg.getMimeMessage(false);
		Pair<String,String> fullTextMessage = getTextBody(mimeMsg, false);
		String bodyMsg = fullTextMessage.getFirst();
		Boolean isASCIIString = isPureAscii(bodyMsg);
		String fullMessage = "";
		if(isASCIIString) {
			fullMessage = encode(subject + "\n" + bodyMsg);
		} else {
			fullMessage = fullMessage + "FEFF";
			fullMessage = fullMessage + convertStringToUnicode(subject);
			fullMessage = fullMessage + convertStringToUnicode("\n");
			fullMessage = fullMessage + convertStringToUnicode(bodyMsg);
		}
		Set<String> uniqueMobileSet = new HashSet<String>();
		for (int i = 0; i < recipients.length; i++) {
			String rcptAddresses = recipients[i].toString();
			String[] splitMobNumber = rcptAddresses.indexOf("<") < 0 ? rcptAddresses.split("@")
					: rcptAddresses.split("<")[1].replaceAll(">", "").split("@");
			String mobNumber = splitMobNumber[0];
			String mobNumberWithoutSymbol = splitMobNumber[0].replaceAll("[()\\-+. ]", "");
			String mobNumberDomain = splitMobNumber[1];
			if (mobNumberDomain.equalsIgnoreCase(smsDomain) && recipientEmailDomain.equalsIgnoreCase(smsDomain) && StringUtils.isNumeric(mobNumberWithoutSymbol) && !uniqueMobileSet.contains(mobNumber)) {
				uniqueMobileSet.add(mobNumber);
				sendsms(fullMessage, mobNumber, sender, isASCIIString);
			}
		}
	}

	private void sendsms(String message, String mnumber, String sender, Boolean isASCIIString) throws ServiceException {
		String smsApiUrl = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraSMSApiUrl);
		String smsUsername = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraSMSApiUsername);
		String smsPin = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraSMSApiPin);
		String smsSenderId = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraSMSApiSenderId);
		String msgType = isASCIIString ?  "" : "&msgType=UC";
		String url = smsApiUrl + "?username=" + smsUsername + "&pin=" + smsPin + "&message=" + message + "&mnumber="
				+ mnumber + "&signature=" + smsSenderId + msgType;
		URL obj;

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

	public static Pair<String, String> getTextBody(MimeMessage mimeMsg, boolean preferHtml) {
		String body = null;
		String contentType = null;
		StringBuilder cb = new StringBuilder();
		try {
			List<MPartInfo> parts = Mime.getParts(mimeMsg);
			Set<MPartInfo> mpiSet = Mime.getBody(parts, preferHtml);
			for (MPartInfo mpi : mpiSet) {
				if (contentType == null || (hasChanged(contentType, mpi.getContentType())
					&& !contentType.startsWith(MimeConstants.CT_TEXT_HTML))) {
						contentType = mpi.getContentType();
				}
				if (contentType != null
					&& (contentType.startsWith(MimeConstants.CT_TEXT_PLAIN) || contentType
						.startsWith(MimeConstants.CT_TEXT_HTML))) {
					MimePart mp = mpi.getMimePart();
					String charSet = mpi.getContentTypeParameter(MimeConstants.P_CHARSET);
					cb.append(Mime.getStringContent(mp, charSet));
				}
			}
		} catch (Exception e) {
			ZimbraLog.ews.errorQuietly("Error fetching message content", e);
			return null;
		}
		body = cb.toString();
		return new Pair<String, String>(body, contentType);
	}

	private static boolean hasChanged(String contentType, String newContentType) {
		boolean changed = false;
		if (!StringUtil.isNullOrEmpty(newContentType)) {
			return !contentType.equalsIgnoreCase(newContentType);
		}
		return changed;
	}

	public static String convertStringToUnicode(String s) {
		String out = null;
		out = StringEscapeUtils.escapeJava(s);
		out = out.replace("\\u", "");
		out = out.replace(" ", "0020");
		out = out.replace("\\r", "000a");
		out = out.replace("\\n", "000a");
		return out;
	}

	public static boolean isPureAscii(String v) {
		return asciiEncoder.canEncode(v);
	}
}
