package com.zimbra.cs.ml.classifier;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.feature.ComputedFeatures;
import com.zimbra.cs.ml.feature.FeatureSet;
import com.zimbra.cs.ml.query.MessageClassificationQuery;
import com.zimbra.cs.ml.schema.ClassifierInfo;
import com.zimbra.cs.ml.schema.MessageClassificationInput;

public class MessageClassifier extends Classifier<Message> {

    public MessageClassifier(String id, String label, FeatureSet<Message> featureSet, ClassifierInfo info) {
        super(id, label, ClassifiableType.MESSAGE, featureSet, info);
    }

    public MessageClassifier(String id, String label, FeatureSet<Message> featureSet) {
        super(id, label, ClassifiableType.MESSAGE, featureSet);
    }

    @Override
    protected MessageClassificationQuery buildQuery(ComputedFeatures<Message> features) throws ServiceException {
        Message msg = features.getItem();
        return new MessageClassificationQuery(getId(), msg.getRecipients(), new MessageClassificationInput(features));
    }
}
