package com.zimbra.cs.ml;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
/**
 *
 * @author iraykin
 *
 */
public class HeaderFeatures implements FeatureSet {

	@Override
	public void encodeFeatures(Message message, Features features)
			throws ServiceException {
		MimeMessage mm = message.getMimeMessage();
		try {
			encodeHeaderValues(features, mm.getHeader("Priority"), "priorityHeader");
		} catch (MessagingException ignore) {}
		try {
			encodeHeaderValues(features, mm.getHeader("Importance"), "importanceHeader");
		} catch (MessagingException ignore) {}
	}

	private void encodeHeaderValues(Features features, String[] values, String featureName) {
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				features.addCategoricalFeature(featureName, values[i]);
			}
		}
	}
}
