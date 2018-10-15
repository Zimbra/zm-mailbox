package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import com.zimbra.cs.lmtpserver.LmtpCallback;
import com.zimbra.cs.lmtpserver.ZimbraLmtpBackend;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.common.util.ZimbraLog;

public class EmailToSMS implements LmtpCallback {

    private static final EmailToSMS sInstance = new EmailToSMS();
    private static final String smsDomain = "esms.gov.in";
    private static final String smsApiUrl = "http://smsgw.sms.gov.in/failsafe/HttpLink";
    private static final String smsUsername = "zimbra.sms";
    private static final String smsPin = "moorxu81";
    private static final String smsSenderId = "NICSMS";

    private EmailToSMS() {
    }

    static {
    	ZimbraLmtpBackend.addCallback(EmailToSMS.getInstance());
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
      String[] splitedRecipients = recipients.split(",");
      String subject = pm.getSubject();
      String message = pm.getFragment(act.getLocale());
      String fullMessage =  encode(subject + "\n" + message);

      for (int i = 0; i < splitedRecipients.length; i++) {
        String rcptAddresses = splitedRecipients[i];
        String[] splitedMobNumber = rcptAddresses.indexOf("<") < 0 ? rcptAddresses.split("@") : rcptAddresses.split("<")[1].replaceAll(">", "").split("@");
        String mobNumber = splitedMobNumber[0];
        String mobNumberDomain = splitedMobNumber[1];

        if ( mobNumberDomain.toLowerCase().equals(smsDomain))  {
          call(fullMessage, mobNumber);
        }
      }
    }

    private void call(String message, String mnumber) throws ServiceException {

        String url = smsApiUrl + "?username="+ smsUsername + "&pin=" + smsPin + "&message="+ message + "&mnumber=" + mnumber + "&signature=" + smsSenderId;
        URL obj;
        String errorMsg = null;

        try {
            obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            int respCode = con.getResponseCode();
            switch (respCode) {
            case 000:
                ZimbraLog.mailbox.info("SMS sent successfully to %s", mnumber);
                break;
            case -2:
                errorMsg = "Invalid credentials";
                break;
            case -3:
            errorMsg = "Empty mobile number";
                break;
            case -4:
                errorMsg = "Empty message";
                break;
            case -5:
                errorMsg = "HTTPS disabled";
                break;
            case -6:
                errorMsg = "HTTP disabled";
                break;
            case -410:
                errorMsg = "Invalid Destination Address";
                break;
            case -201:
                errorMsg = "Email Delivery Disabled";
               break;
            case -404:
                errorMsg = "Invalid MsgType";
                break;
            case -406:
                errorMsg = "Invalid Port";
                break;
            case -407:
                errorMsg = "Invalid Expiry minutes";
                break;
            case -408:
                errorMsg = "Invalid Customer Reference Id";
                break;
            case -433:
                errorMsg = "Invalid Customer Reference Id Length";
                break;
            case -401:
                errorMsg = "Invalid Scheduled Time";
                break;
            case -13:
                errorMsg = "Internal Error";
                break;
            default:
                break;
            }
            if(errorMsg != null && !errorMsg.isEmpty())
              ZimbraLog.mailbox.error("SMS not send to %s with error message %s Error Code: %s ", mnumber, errorMsg, respCode);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            ZimbraLog.mailbox.warn("Unable to send mobile notification MalformedURLException", e);
        } catch (IOException e) {
            e.printStackTrace();
            ZimbraLog.mailbox.warn("Unable to send mobile notification IOException", e);
        }
    }

    public static EmailToSMS getInstance() {
        return sInstance;
    }

    private static boolean validatePhoneNumber(String phoneNo) {
        //validate phone numbers of format "1234567890"
        if (phoneNo.matches("\\d{10}")) return true;
        //validating phone number with -, . or spaces
        else if(phoneNo.matches("\\d{3}[-\\.\\s]\\d{3}[-\\.\\s]\\d{4}")) return true;
        //validating phone number with extension length from 3 to 5
        else if(phoneNo.matches("\\d{3}-\\d{3}-\\d{4}\\s(x|(ext))\\d{3,5}")) return true;
        //validating phone number where area code is in braces ()
        else if(phoneNo.matches("\\(\\d{3}\\)-\\d{3}-\\d{4}")) return true;
        //return false if nothing matches the input
        else return false;
    }

    public static String encode(String url)
    {
      try {
        String encodeURL = URLEncoder.encode( url, "UTF-8" );
        return encodeURL;
      } catch (UnsupportedEncodingException e) {
        return "Issue while encoding" + e.getMessage();
      }
    }
}
