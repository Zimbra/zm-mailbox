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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mime.ParsedMessage;

public abstract class MetadataCallback {
    private static Set<String> sCallbackKeys = new CopyOnWriteArraySet<String>();
    private static List<MetadataCallback> sCallbacks = new CopyOnWriteArrayList<MetadataCallback>();

    /** Adds an instance of an callback class that will be triggered when a
     *  message is added to a user mailbox and when an item is serialized.  */
    public synchronized static void addCallback(MetadataCallback callback) {
        if (callback == null) {
            ZimbraLog.mailbox.error("", new IllegalStateException("MetadataCallback cannot be null"));
        } else if (sCallbackKeys.contains(callback.getMetadataSectionKey())) {
            ZimbraLog.mailbox.error("", new IllegalStateException("second MetadataCallback for key " + callback.getMetadataSectionKey()));
        } else {
            ZimbraLog.mailbox.info("Adding metadata callback: %s", callback.getClass().getName());
            sCallbacks.add(callback);
            sCallbackKeys.add(callback.getMetadataSectionKey());
        }
    }

    public static boolean isSectionRegistered(String key) {
        return sCallbackKeys.contains(key);
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


    private static String CONVERSATION_METADATA;
        static {
            CustomMetadata meta = new CustomMetadata("ignored");
            meta.put("_exists", "1");
            CONVERSATION_METADATA = meta.getSerializedValue();
        }

    /** Combines all custom message metadata into a set of conversation
     *  metadata.  This method starts with the existing conversation metadata
     *  (<tt>null</tt> at first, augmented subsequently via calls to this
     *  method) and incorporates data from a single <code>Message</code>.<p>
     *  The resulting combined custom conversation metadata contains a
     *  single entry for each unique <b>section</b> on either the message
     *  or already on the conversation.  Those stubbed entries contain only
     *  a single key (<tt>"_exists"</tt>), having the value <tt>"1"</tt>.  
     * @param extended  The custom metadata already associated with the
     *                  <code>Conversation</code>.
     * @param msg       The <code>Message</code> being added.
     * @return a <code>CustomMetadataList</code> containing stubs for all
     *         custom metadata sections on either the conversation or the
     *         message, or <tt>null</tt> if there is no metadata to combine. */
    public static CustomMetadataList duringConversationAdd(CustomMetadataList extended, final Message msg) {
        if (msg == null)
            return extended;

        List<String> msgSections = msg.getCustomDataSections();
        if (msgSections != null && !msgSections.isEmpty()) {
            for (String key : msgSections) {
                if (extended == null)
                    extended = new CustomMetadataList();
                extended.addSection(key, CONVERSATION_METADATA);
            }
        }
        return extended;
    }
}
