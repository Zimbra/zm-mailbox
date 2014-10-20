package com.zimbra.cs.ml;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
/**
 *
 * @author iraykin
 *
 */
public interface FeatureSet {

	public void encodeFeatures(Message message, Features features) throws ServiceException;
}
