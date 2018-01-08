package com.zimbra.cs.ml.feature;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;

/**
 * Feature factory that builds a boolean feature that is TRUE if the message
 * is part of existing conversation thread
 */
public class ConversationFeatureFactory extends FeatureFactory<Message, Boolean> {

    public ConversationFeatureFactory() {}

    @Override
    public Feature<Boolean> buildFeature(Message msg) {
        return new PrimitiveFeature<>(KnownFeature.IS_PART_OF_CONVERSATION, msg.getConversationId() > 0);
    }

    @Override
    public void setParams(FeatureParams params) throws ServiceException {
        //nothing to do here
    }
}
