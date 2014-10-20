package com.zimbra.cs.ml;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
/**
 *
 * @author iraykin
 *
 */
public class DummyClassifier implements Classifier {

	@Override
	public ClassifierResult classify(Message message) {
			return new DummyClassifierResult(Label.NOT_PRIORITY);
	}

	@Override
	public ClassifierResult classify(Message message, FeatureExtractor extractor, boolean determineWhy)
			throws ServiceException {
		return new DummyClassifierResult(Label.NOT_PRIORITY);
	}

}
