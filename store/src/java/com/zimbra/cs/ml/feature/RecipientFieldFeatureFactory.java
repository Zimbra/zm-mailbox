package com.zimbra.cs.ml.feature;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;


/**
 * Feature factory that builds a feature based off whether the recipient
 * is on the to/cc field.
 */
public class RecipientFieldFeatureFactory extends FeatureFactory<Message, Integer> {

    public RecipientFieldFeatureFactory() {}

    private String extractEmail(Address addr) {
        return new ParsedAddress(addr.toString()).emailPart;
    }

    private boolean isUserOnField(int msgId, MimeMessage mm, String userEmail, RecipientType type) {
        try {
            Address[] recipients = mm.getRecipients(type);
            if (recipients != null) {
                List<String> addrs = Arrays.asList(recipients).stream().map(addr -> extractEmail(addr)).collect(Collectors.toList());
                return addrs.contains(userEmail);
            } else {
                return false;
            }
        } catch (MessagingException e) {
            ZimbraLog.ml.error("unable to get recipients of type %s for message %d", type, msgId);
            return false;
        }
    }

    @Override
    public Feature<Integer> buildFeature(Message msg) throws ServiceException {
        int msgId = msg.getId();
        Account acct = msg.getAccount();
        String email = acct.getName();
        MimeMessage mm = msg.getMimeMessage();
        int flag;
        if (isUserOnField(msgId, mm, email, RecipientType.TO)) {
            flag = 0;
        } else if (isUserOnField(msgId, mm, email, RecipientType.CC)) {
            flag = 1;
        } else if (isUserOnField(msgId, mm, email, RecipientType.BCC)) {
            flag = 2;
        } else {
            flag = 3; //user is on distribution list?
        }
        return new PrimitiveFeature<Integer>(KnownFeature.RECIPIENT_FIELD, flag);
    }

    @Override
    public void setParams(FeatureParams params) throws ServiceException {
        //no params necessary for this feature
    }
}
