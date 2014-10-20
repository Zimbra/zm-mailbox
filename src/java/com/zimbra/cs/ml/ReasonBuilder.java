package com.zimbra.cs.ml;

import java.util.HashMap;
import java.util.Map;
/**
 *
 * @author iraykin
 *
 */
public class ReasonBuilder {

	private static Map<FeatureSet, String> featureSetDescriptions = new HashMap<FeatureSet, String>();
	static {
		featureSetDescriptions.put(FeatureExtractor.SENDER_FEATURES, "the sender of the email");
		featureSetDescriptions.put(FeatureExtractor.SUBJECT_FEATURES, "the subject of this email");
		featureSetDescriptions.put(FeatureExtractor.HEADER_FEATURES, "the email headers");
		featureSetDescriptions.put(FeatureExtractor.RECIPIENT_FEATURES, "the recipients of this email");
		featureSetDescriptions.put(FeatureExtractor.CONTENT_FEATURES, "the content of this email");
	}

	private static Map<InternalLabel, String> internalLabelDescriptions = new HashMap<InternalLabel, String>();
	static {
		internalLabelDescriptions.put(InternalLabel.PRIORITY_CONTENT, "your interaction with similar emails");
		internalLabelDescriptions.put(InternalLabel.PRIORITY_REPLIED, "your quick replies to similar emails");
	}

	public static String getClassificationReason(FeatureSet mostContributingFeatures, InternalLabel internalLabel)  {
		String labelDescription  = null;
		String featureDescription = null;
		if (featureSetDescriptions.containsKey(mostContributingFeatures)) {
			featureDescription = featureSetDescriptions.get(mostContributingFeatures);
		}
		if (internalLabelDescriptions.containsKey(internalLabel)) {
			labelDescription = internalLabelDescriptions.get(internalLabel);
		}
		if (labelDescription == null && featureDescription == null) {
			return "";
		}
		StringBuilder reason = new StringBuilder("This message is priority because");
		boolean needAnd = false;
		if (labelDescription != null) {
			reason.append(" of ").append(labelDescription);
			needAnd = true;
		}
		if (featureDescription != null) {
			reason.append(needAnd? " and ": " of ");
			reason.append(featureDescription);
		}
		return reason.toString();
	}
}
