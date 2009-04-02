/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mime.ParsedMessage;

public abstract class MetadataCallback {
    private static List<MetadataCallback> sCallbacks = new CopyOnWriteArrayList<MetadataCallback>();

    /** Adds an instance of an callback class that will be triggered when a
     *  message is added to a user mailbox and when an item is serialized.  */
    public static void addCallback(MetadataCallback callback) {
        if (callback == null) {
            ZimbraLog.mailbox.error("", new IllegalStateException("MetadataCallback cannot be null"));
            return;
        }
        ZimbraLog.mailbox.info("Adding metadata callback: %s", callback.getClass().getName());
        sCallbacks.add(callback);
    }

    private final String mSectionKey;

    protected MetadataCallback(String key) {
        if (key == null || key.trim().equals(""))
            throw new IllegalArgumentException("metadata callback section key cannot be blank");
        mSectionKey = key;
    }

    /** Returns the "section key" for the callback.  This string identifies
     *  the data associated with the callback, is used as the "section key"
     *  for all <code>CustomMetadata</code> objects passed to and from the
     *  callback, is part of the key in the <tt>METADATA</tt> serialization
     *  of the callback's associated metadata, and is included as an attribute
     *  on the element when returning the metadata in a JSON/XML response. */
    protected String getMetadataSectionKey() {
        return mSectionKey;
    }


    /** Invokes all <code>MetadataCallback</code>s on the about-to-be-delivered
     *  message to generate a set of custom metadata.  This metadata will be
     *  attached to the newly-created <code>Message</code> and subsequently
     *  fetchable via {@link MailItem#getCustomData(String)}.
     * @see #analyzeMessage(ParsedMessage)
     * @return a <code>CustomMetadataList</code> containing all the metadata
     *         generated from all registered callbacks, or <tt>null</tt> if no
     *         callback needed to associate metadata. */
    public static CustomMetadataList preDelivery(ParsedMessage pm) {
        if (pm == null || sCallbacks.isEmpty())
            return null;

        CustomMetadataList extended = null;
        for (MetadataCallback callback : sCallbacks) {
            CustomMetadata custom = callback.analyzeMessage(pm);
            if (custom != null) {
                if (extended == null)
                    extended = custom.asList();
                else
                    extended.addSection(custom);
            }
        }
        return extended;
    }

    /** Scans a message about to be added to the mailbox and generates a hunk
     *  of custom metadata to be associated with the message in the database.
     * @see #getMetadataSectionKey()
     * @see #preDelivery(ParsedMessage)
     * @return a <code>CustomMetadata</code> object matching this callback's
     *         section key, or <tt>null</tt> if the callback has no metadata
     *         to associate with the message */
    protected abstract CustomMetadata analyzeMessage(ParsedMessage pm);


    /** Invokes all <code>MetadataCallback</code>s to combine custom message
     *  metadata into a set of conversation metadata.  This method starts with
     *  the existing conversation metadata (<tt>null</tt> at first, augmented
     *  subsequently via calls to this method) and incorporates data from a
     *  single <code>Message</code>.
     * @param extended  The custom metadata already associated with the
     *                  <code>Conversation</code>.
     * @param msg       The <code>Message</code> being added.
     * @see #accumulatesOnConversation()
     * @see #addToConversation(CustomMetadata, CustomMetadata)
     * @return a <code>CustomMetadataList</code> containing all the metadata
     *         generated from all registered callbacks, or <tt>null</tt> if
     *         there is no metadata to combine. */
    public static CustomMetadataList duringConversationAdd(CustomMetadataList extended, Message msg) {
        if (msg == null || sCallbacks.isEmpty())
            return extended;

        for (MetadataCallback callback : sCallbacks) {
            if (!callback.accumulatesOnConversation())
                continue;

            String key = callback.getMetadataSectionKey();
            try {
                CustomMetadata fromMsg = msg.getCustomData(key);
                if (fromMsg == null)
                    continue;
                CustomMetadata fromConv = extended == null ? null : extended.getSection(key);

                CustomMetadata custom = callback.addToConversation(fromConv, fromMsg);

                if (custom == null || custom.isEmpty()) {
                    if (extended != null)
                        extended.removeSection(key);
                } else if (extended == null) {
                    extended = custom.asList();
                } else {
                    extended.addSection(custom);
                }
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("error adding message to conversation metadata; skipping this callback", e);
            }
        }
        return extended;
    }

    /** Returns whether this <code>MetadataCallback</code> aggregates data
     *  from <code>Message</code>s onto their <code>Conversation</code>.
     * @see #duringConversationAdd(CustomMetadataList, Message)
     * @see #addToConversation(CustomMetadata, CustomMetadata) */
    protected abstract boolean accumulatesOnConversation();

    /** Callback invoked when a <code>Message</code>'s custom metadata needs
     *  to be folded into its <code>Conversation</code>'s.  If the message has
     *  no metadata matching the callback's section key, the callback is not
     *  invoked.<p>
     *  The invoker prefers that you modify and return <code>fromConv</code>
     *  when it is not null.  This is solely a performance-related issue and
     *  does not affect the resulting metadata.
     * @param fromConv  The set of metadata already accumulated on the
     *                  <code>Conversation</code>.
     * @param fromMsg   The set of metadata from the <code>Message</code>
     *                  in question (guaranteed non-<tt>null</tt>).
     * @see #duringConversationAdd(CustomMetadataList, Message)
     * @return metadata incorporating both the existing conversation state and
     *         the just-added message.  Do <u>not</u> return <tt>null</tt> when
     *         no change is necessary; return <code>fromConv</code> instead. */
    protected abstract CustomMetadata addToConversation(CustomMetadata fromConv, CustomMetadata fromMsg);
}
