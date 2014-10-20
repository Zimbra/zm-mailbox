package com.zimbra.cs.ml;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
/**
 *
 * @author iraykin
 *
 */
public class ContentFeatures implements FeatureSet {

	@Override
	public void encodeFeatures(Message message, Features features)
			throws ServiceException {
		byte[] content = message.getContent();
		features.addTextFeature("body", new String(content));
	}

}
