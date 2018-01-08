package com.zimbra.cs.ml.feature;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.feature.FeatureParam.ParamKey;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;

/**
 * Feature factory that builds a feature representing how many recipients there are on the given field
 */
public class NumRecipientsFeatureFactory extends FeatureFactory<Message, Integer> {

    private RecipientCountType countType;

    public NumRecipientsFeatureFactory() {}

    public NumRecipientsFeatureFactory(RecipientCountType countType) throws ServiceException {
        setParams(new FeatureParams().addParam(new FeatureParam<>(ParamKey.RECIPIENT_TYPE, countType)));
    }

    @Override
    public void setParams(FeatureParams params) throws ServiceException {
        countType = params.get(ParamKey.RECIPIENT_TYPE, RecipientCountType.ALL);
    }

    private int getNumRecipientsForType(MimeMessage mm, RecipientType type, int msgId) throws ServiceException {
        Address[] recipients;
        try {
            recipients = mm.getRecipients(type);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(String.format("unable to get recipients of type %s for message %d", type, msgId), e);
        }
        return recipients == null ? 0 : recipients.length;
    }

    @Override
    public Feature<Integer> buildFeature(Message msg) throws ServiceException {
        MimeMessage mm = msg.getMimeMessage();
        int msgId = msg.getId();
        int numRecipients;
        switch (countType) {
        case TO:
            numRecipients = getNumRecipientsForType(mm, RecipientType.TO, msgId);
            break;
        case CC:
            numRecipients = getNumRecipientsForType(mm, RecipientType.CC, msgId);
            break;
        case ALL:
        default:
            Address[] allRecipients;
            try {
                allRecipients = mm.getAllRecipients();
                numRecipients = allRecipients == null ? 0 : allRecipients.length;
            } catch (MessagingException e) {
                throw ServiceException.FAILURE(String.format("unable to get all recipients of type %s for message %d", msgId), e);
            }
        }
        return new PrimitiveFeature<Integer>(KnownFeature.NUM_RECIPIENTS, numRecipients);
    }

    public static enum RecipientCountType {
        TO, CC, ALL;

        public static RecipientCountType of(String str) throws ServiceException {
            for (RecipientCountType type: RecipientCountType.values()) {
                if (str.equalsIgnoreCase(type.name())) {
                    return type;
                }
            }
            throw ServiceException.INVALID_REQUEST(str + " is not a valid RecpientCountType", null);
        }
    }
}
