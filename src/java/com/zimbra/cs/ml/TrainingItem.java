package com.zimbra.cs.ml;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;

/** A wrapper around a mail item and its training label, used in training the model.
 * @author iraykin
 **/
public class TrainingItem {
	private Message message;
	private InternalLabel label;

	public TrainingItem(Message message, InternalLabel label) {
		this.message = message;
		this.label = label;
	}

	public Message getMessage() {
		return message;
	}

	public Features getFeatures(FeatureExtractor extractor) throws ServiceException {
		return extractor.extractFeatures(getMessage());
	}

	public InternalLabel getInternalLabel() {
		return label;
	}

	public Label getLabel() {
		return label.getExternalLabel();
	}
}
