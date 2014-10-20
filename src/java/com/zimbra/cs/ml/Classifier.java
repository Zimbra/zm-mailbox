package com.zimbra.cs.ml;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
/**
 *
 * @author iraykin
 *
 */
public interface Classifier {

	public ClassifierResult classify(Message message) throws ServiceException;

	public ClassifierResult classify(Message message, FeatureExtractor extractor, boolean determineWhy) throws ServiceException;
}
