/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.mail.MailDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Prototype of Priority Inbox. This implementation is only a proof of concept. The performance and the efficiency are
 * not nearly close to the production quality.
 *
 * Priority Inbox automatically identifies your important messages from your entire mailbox based on your past behavior.
 * The current implementation deems that messages you have replied or flagged are important.
 *
 * The system first learns the pattern of your past important messages by scanning your entire mailbox. The pattern
 * consists of sender's addresses (From) and recipients addresses (To). Subject is also a strong factor, but storing
 * tokenized subject strings in a persistent store significantly increases storage demand, which is not realistic to our
 * customers. Then the system calculates score for each addresses using Bayes' theorem. It is important that we need to
 * count not only important messages but also non important messages. Lastly, based on the scores, the system calculates
 * the probability of whether or not the message is important for each messages in your mailbox using Bayes' theorem
 * again. If the probability is greater than 0.9, the messages is tagged with "Important". All those steps above are
 * executed as a batch when PriorityInboxRequest SOAP request is called.
 *
 * Evaluation process:
 * <ol>
 *  <li>Export your entire mailbox from DF/CF.
 *  <li>Import it to your sandbox system. Use a fresh account, not user1.
 *  <li>$ zmsoap -z -m user PriorityInboxRequest
 *  <li>Refresh your browser, and open "Important" tag folder.
 * </ol>
 *
 * The success factor of this proof of concept is that your important messages based on your subjective measure, not
 * necessarily messages that you have actually replied, are reasonably tagged with "Important". The efficiency is out of
 * scope at this point. If we don't get positive results, we will probably switch to more static approach from machine
 * learning techniques.
 *
 * @author ysasaki
 */
public final class PriorityInbox extends MailDocumentHandler {
    private static final String TAG_NAME = "Important";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octx = getOperationContext(zsc, context);
        Account account = getRequestedAccount(zsc);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        List<Integer> ids = getItemIds(mbox);

        Element resp = zsc.createElement("Response");

        Element train = resp.addElement("Train");
        Map<String, Feature> model = train(mbox, ids);

        List<Feature> features = new ArrayList<Feature>(model.values());
        Collections.sort(features);
        for (Feature feature : features) {
            train.addElement("ft").addAttribute("email", feature.email)
                .addAttribute("stat", feature.positive + "/" + feature.total)
                .addAttribute("score", feature.score);
        }

        Element classify = resp.addElement("Classify");
        List<Result> results = classify(mbox, ids, model);
        for (Result result : results) {
            classify.addElement("msg").addAttribute("score", result.score)
                .addAttribute("subject", result.message.getSubject())
                .addAttribute("from", getAddresses(result.message.getSender()).toString())
                .addAttribute("to", getAddresses(result.message.getRecipients()).toString());
        }

        tag(octx, mbox, results);

        return resp;
    }

    private List<Integer> getItemIds(Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        DbConnection conn = DbPool.getConnection();
        try {
            stmt = conn.prepareStatement("SELECT id FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE mailbox_id = ? AND type = " + MailItem.Type.MESSAGE.toByte() + " AND folder_id NOT IN (" +
                    Mailbox.ID_FOLDER_DRAFTS + "," + Mailbox.ID_FOLDER_SENT + "," + Mailbox.ID_FOLDER_SPAM + ")");
            stmt.setInt(1, mbox.getId());
            rs = stmt.executeQuery();
            List<Integer> ids = new ArrayList<Integer>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            return ids;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to fetch item IDs", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    private Map<String, Feature> train(Mailbox mbox, List<Integer> ids) {
        Map<String, Feature> model = new HashMap<String, Feature>();

        for (int id: ids) {
            Message msg;
            try {
                msg = (Message) mbox.getItemById(null, id, MailItem.Type.MESSAGE);
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("Faild to fetch message", e);
                continue;
            }

            boolean positive = ((msg.getFlagBitmask() & (Flag.BITMASK_REPLIED | Flag.BITMASK_FLAGGED)) > 0);

            for (String addr : getAddresses(msg)) {
                Feature feature = model.get(addr);
                if (feature == null) {
                    feature = new Feature(addr);
                    model.put(addr, feature);
                }
                feature.total++;
                if (positive) {
                    feature.positive++;
                }
            }
        }

        int totalFrom = 0;
        int positiveFrom = 0;
        int totalTo = 0;
        int positiveTo = 0;

        for (Feature feature : model.values()) {
            if (feature.email.startsWith("from:")) {
                totalFrom += feature.total;
                positiveFrom += feature.positive;
            } else if (feature.email.startsWith("to:")) {
                totalTo += feature.total;
                positiveTo += feature.positive;
            }
        }

        ZimbraLog.misc.info("Trained from=%d/%d,to=%d/%d", positiveFrom, totalFrom, positiveTo, totalTo);

        for (Feature feature : model.values()) {
            if (feature.total >= 5) {
                if (feature.email.startsWith("from:")) {
                    float pos = (float) feature.positive / positiveFrom;
                    float neg = (float) (feature.total - feature.positive) / (totalFrom - positiveFrom);
                    feature.score = (int) (1000F * pos / (pos + neg));
                } else if (feature.email.startsWith("to:")) {
                    float pos = (float) feature.positive / positiveTo;
                    float neg = (float) (feature.total - feature.positive) / (totalTo - positiveTo);
                    feature.score = (int) (1000F * pos / (pos + neg));
                }
            } else {
                feature.score = 0;
            }
        }

        return model;
    }

    private List<Result> classify(Mailbox mbox, List<Integer> ids, Map<String, Feature> model) {
        int total = 0;
        List<Result> result = new ArrayList<Result>();
        for (int id : ids) {
            Message msg;
            try {
                msg = (Message) mbox.getItemById(null, id, MailItem.Type.MESSAGE);
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("Faild to fetch message", e);
                continue;
            }

            total++;

            float positive = 1.0f;
            float negative = 1.0f;
            for (String addr : getAddresses(msg)) {
                Feature feature = model.get(addr);
                if (feature != null && feature.score > 0) {
                    positive *= feature.score / 1000f;
                    negative *= (1000 - feature.score) / 1000f;
                } else {
                    positive *= 0.1f;
                    negative *= 0.9f;
                }
            }
            float score = positive / (positive + negative);
            if (score > 0.9f) {
                result.add(new Result(msg, (int) (score * 1000f)));
            }
        }
        ZimbraLog.misc.info("Classified %d/%d", result.size(), total);
        Collections.sort(result);
        return result;
    }

    private void tag(OperationContext octx, Mailbox mbox, List<Result> results) throws ServiceException {
        Tag tag;
        try {
            tag = mbox.getTagByName(TAG_NAME);
            mbox.delete(octx, tag.getId(), tag.getType());
        } catch (ServiceException ignore) {
        }

        tag = mbox.createTag(octx, TAG_NAME, MailItem.DEFAULT_COLOR_RGB);

        int[] ids = new int[results.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = results.get(i).message.getId();
        }
        mbox.alterTag(octx, ids, MailItem.Type.MESSAGE, tag.getId(), true, null);
    }

    private List<String> getAddresses(Message message) {
        List<String> result = new ArrayList<String>();

        String sender = message.getSender();
        if (!Strings.isNullOrEmpty(sender)) {
            for (InternetAddress addr : InternetAddress.parseHeader(sender)) {
                if (addr.getAddress() != null) {
                    result.add("from:" + addr.getAddress().toLowerCase());
                }
            }
        }

        String rcpts = message.getRecipients();
        if (!Strings.isNullOrEmpty(rcpts)) {
            for (InternetAddress addr : InternetAddress.parseHeader(rcpts)) {
                if (addr.getAddress() != null) {
                    result.add("to:" + addr.getAddress().toLowerCase());
                }
            }
        }

        return result;
    }

    private List<String> getAddresses(String raw) {
        List<String> result = new ArrayList<String>();
        if (!Strings.isNullOrEmpty(raw)) {
            for (InternetAddress addr : InternetAddress.parseHeader(raw)) {
                if (addr.getAddress() != null) {
                    result.add(addr.getAddress().toLowerCase());
                }
            }
        }
        return result;
    }

    private static final class Feature implements Comparable<Feature> {
        final String email;
        int positive = 0;
        int total = 0;
        int score;

        Feature(String email) {
            this.email = email;
        }

        @Override
        public int compareTo(Feature other) {
            return other.score - score;
        }
    }

    private static final class Result implements Comparable<Result> {
        final Message message;
        final int score;

        Result(Message message, int score) {
            this.message = message;
            this.score = score;
        }

        @Override
        public int compareTo(Result other) {
            return other.score - score;
        }
    }

}
