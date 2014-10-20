package com.zimbra.cs.ml;

import org.apache.mahout.classifier.sgd.CrossFoldLearner;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Message;

/** A wrapper around a Mahout classifier trained via an AdaptiveLogisticRegression
 * Can classify messages into Priority or Not Priority.
 * @author iraykin
 *
 */
public class MahoutClassifier implements Classifier {
	private CrossFoldLearner learner;
	private FeatureExtractor defaultExtractor;

	public MahoutClassifier(CrossFoldLearner model) {
		this.learner = model;
		this.defaultExtractor = FeatureExtractor.withAllFeatures();
	}

	@Override
	public MahoutClassifierResult classify(Message message) throws ServiceException {
		return classify(message, defaultExtractor, true);

	}

	@Override
	public MahoutClassifierResult classify(Message message, FeatureExtractor extractor, boolean determineWhy) throws ServiceException {
		Features features = extractor.extractFeatures(message);
		Vector featureVector = features.getVector();
		Vector out = new DenseVector(InternalLabel.numDimensions());
		learner.classifyFull(out, featureVector);
		MahoutClassifierResult result = new MahoutClassifierResult(out);
		ZimbraLog.analytics.debug(String.format("classified message %s as %s with probability %.2f",
				message.getId(), result.getLabel().toString(), result.getValue()));
		if (determineWhy) {
			String reason = determineWhy(message, result, extractor);
			result.setReason(reason);
		}
		return result;
	}

	private String determineWhy(Message message, MahoutClassifierResult classification, FeatureExtractor extractorUsed) throws ServiceException {
		FeatureSet mostContributingFeatures = null;
		double bestValue = -1;
		for (FeatureSet fs: extractorUsed.getFeatureSets()) {
			FeatureExtractor extractor = new FeatureExtractor(fs);
			MahoutClassifierResult result = classify(message, extractor, false);
			double value;
			if (result.getLabel() != classification.getLabel()) {
				value = result.getValue() * -1;
			} else {
				value = result.getValue();
				if (value > bestValue) {
					mostContributingFeatures = fs;
					bestValue = result.getValue();
				}
			}
		}
		return ReasonBuilder.getClassificationReason(mostContributingFeatures, classification.getInternalLabel());
	}

	public CrossFoldLearner getLearner() {
		return learner;
	}

}
