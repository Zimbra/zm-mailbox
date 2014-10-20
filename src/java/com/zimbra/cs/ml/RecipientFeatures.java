package com.zimbra.cs.ml;

import java.util.HashSet;
import java.util.Set;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
/**
 *
 * @author iraykin
 *
 */
public class RecipientFeatures extends AddressFeatures {

	@Override
	public void encodeFeatures(Message message, Features features) throws ServiceException {
		MimeMessage mm = message.getMimeMessage();
		InternetAddress[] cc = Mime.parseAddressHeader(mm, "Cc");
		InternetAddress[] to = Mime.parseAddressHeader(mm, "To");
		encodeAddrs(cc, "cc", features);
		features.addContinuousFeature("numTo", String.valueOf(to.length));
		features.addContinuousFeature("numCc", String.valueOf(cc.length));
		//Set<String> myAddrs = message.getMailbox().getAccount().getAllAddrsSet();
		Set<String>myAddrs = new HashSet<String>();
		myAddrs.add("raykini@zimbra.com"); //for the hackathon, since we're running on the user2 dev account
		Set<String> toAddrs = new HashSet<String>();
		for (int i = 0; i < to.length; i++) {
			toAddrs.add(to[i].getAddress());
		}
		Set<String> ccAddrs = new HashSet<String>();
		for (int i = 0; i < cc.length; i++) {
			ccAddrs.add(cc[i].getAddress());
		}
		boolean isCCd = Sets.intersection(ccAddrs, myAddrs).size() > 0;
		boolean isToMe = Sets.intersection(toAddrs, myAddrs).size() > 0;
		/* No time during hackathon to figure out how to expand distribution lists.
		 * Instead, if your address is not in the to or cc fields, assume that
		 * one of them is a distribution list and you are on it.
		 */
		String receivedBecause = isToMe? "recipient": isCCd? "ccd": "dl";
		features.addCategoricalFeature("RecievedBecause", receivedBecause);
	}
}
