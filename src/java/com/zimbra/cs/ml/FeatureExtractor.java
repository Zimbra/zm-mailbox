package com.zimbra.cs.ml;

import java.util.LinkedList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;

/** Class for extracting data from a MailItem into a Features object
 *
 * @author iraykin
 *
 */
public class FeatureExtractor {

	public static final FeatureSet SENDER_FEATURES = new SenderFeatures();
	public static final FeatureSet RECIPIENT_FEATURES = new RecipientFeatures();
	public static final FeatureSet CONTENT_FEATURES = new ContentFeatures();
	public static final FeatureSet SUBJECT_FEATURES = new SubjectFeatures();
	public static final FeatureSet HEADER_FEATURES = new HeaderFeatures();

	private List<FeatureSet> featureSets = new LinkedList<FeatureSet>();

	public FeatureExtractor() {}

	public FeatureExtractor(FeatureSet featureSet) {
		addFeatureSets(featureSet);
	}

	public List<FeatureSet> getFeatureSets() {
		return featureSets;
	}

	public FeatureExtractor addFeatureSets(FeatureSet featureSet, FeatureSet... more) {
		featureSets.add(featureSet);
		for (int i = 0; i < more.length; i++) {
			featureSets.add(more[i]);
		}
		return this;
	}

	public Features extractFeatures(MailItem item) throws ServiceException {
		if (item instanceof Message) {
			return extractFeaturesFromMessage((Message) item);
		}
		return null;
	}

	private Features extractFeaturesFromMessage(Message item) throws ServiceException {
		Features features = new Features();
		for (FeatureSet featureSet: featureSets) {
			featureSet.encodeFeatures(item, features);
		}
		return features;
	}

	public static FeatureExtractor withAllFeatures() {
		FeatureExtractor extractor = new FeatureExtractor()
		.addFeatureSets(SENDER_FEATURES, RECIPIENT_FEATURES, SUBJECT_FEATURES, HEADER_FEATURES);
		return extractor;
	}
}
