package com.zimbra.cs.ml;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
/**
 *
 * @author iraykin
 *
 */
public class SenderFeatures extends AddressFeatures {

	@Override
	public void encodeFeatures(Message message, Features features) throws ServiceException {
		MimeMessage mm = message.getMimeMessage();
		InternetAddress[] from = Mime.parseAddressHeader(mm, "From");
		encodeAddrs(from, "from", features);
	}
}
