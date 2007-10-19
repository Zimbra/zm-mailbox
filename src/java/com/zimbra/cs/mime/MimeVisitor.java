/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mime;

import java.util.ArrayList;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import com.zimbra.common.util.ZimbraLog;

/**
 * Walks a JavaMail MIME tree and calls the abstract methods for each node. 
 *   
 * @author bburtin
 */
public abstract class MimeVisitor {

    /** The list of registered MimeVistor classes that convert stored
     *  messages on the fly. */
    private static List<Class<? extends MimeVisitor>> sMimeConverters = new ArrayList<Class<? extends MimeVisitor>>();
    /** The list of registered MimeVistor classes that convert new messages
     *  before storing them to disk. */
    private static List<Class<? extends MimeVisitor>> sMimeMutators   = new ArrayList<Class<? extends MimeVisitor>>();

        static {
            registerConverter(UUEncodeConverter.class);
            registerConverter(TnefConverter.class);
        }

    /** Adds a MimeVisitor class to the list of converters invoked on the fly
     *  when a message is fetched from the store or prepared for indexing.
     *  Note that changes made by these MimeVisitors are not persisted to disk
     *  but instead are executed every time the message is accessed. */
    public static void registerConverter(Class<? extends MimeVisitor> vclass) {
        sMimeConverters.add(vclass);
    }

    /** Retrieves the list of all registered MimeVisitor converter classes.
     * @see #registerConverter(Class) */
    public static List<Class<? extends MimeVisitor>> getConverters() {
        return new ArrayList<Class<? extends MimeVisitor>>(sMimeConverters);
    }

    /** Adds a MimeVisitor class to the list of mutators invoked before a
     *  message is saved to disk or sent via SMTP. */
    public static void registerMutator(Class<? extends MimeVisitor> vclass) {
        sMimeMutators.add(vclass);
    }

    /** Retrieves the list of all registered MimeVisitor mutator classes.
     * @see #registerMutator(Class) */
    public static List<Class<? extends MimeVisitor>> getMutators() {
        return new ArrayList<Class<? extends MimeVisitor>>(sMimeMutators);
    }


    /** This inner interface permits the {@link Mime#accept} caller to be
     *  notified immediately before any changes to the MimeMessage are
     *  performed by a <code>MimeVistor</code>.  Note that when a call to
     *  {@link Mime#accept} results in multiple modifications, the callback
     *  will be invoked multiple times. */
    public static interface ModificationCallback {
        /** A callback function invoked immediately prior to any modification
         *  of the message.  If the callback returns <code>false</code>, the
         *  modification is not performed. */
        public boolean onModification();
    }

    protected ModificationCallback mCallback;

    /** Sets the MimeVisitor's pre-modification callback.  The callback can
     *  be unset by passing <code>null</code> as the argument.
     * @return the <code>MimeVisitor</code> itself */
    public MimeVisitor setCallback(ModificationCallback callback) {
        mCallback = callback;
        return this;
    }
    /** Returns the pre-modification callback currently associated with the
     *  MimeVisitor, or <code>null</code> if there is no such callback. */
    public ModificationCallback getCallback()  { return mCallback; }


    /** The flags passed to the <code>visitXXX</code> methods before and
     *  after a node's children are visited, respectively. */
    protected enum VisitPhase { VISIT_BEGIN, VISIT_END };

    /** Visitor callback for traversing a MimeMessage, either standalone
     *  or as an attachment to another MimeMessage.
     * @return whether any changes were performed during the visit
     * @see VisitPhase */
    protected abstract boolean visitMessage(MimeMessage mm, VisitPhase visitKind) throws MessagingException;

    /** Visitor callback for traversing a Multipart.
     * @return whether any changes were performed during the visit
     * @see VisitPhase */
    protected abstract boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) throws MessagingException;

    /** Visitor callback for traversing a BodyPart.
     * @return whether any changes were performed during the visit
     * @see VisitPhase */
    protected abstract boolean visitBodyPart(MimeBodyPart bp) throws MessagingException;


    /** Walks the mail object tree depth-first, starting at the specified
     *  <code>MimePart</code>.  Invokes the various <code>MimeVisitor</code>
     *  methods in for each visited node.
     * 
     * @param mp the root MIME part at which to start the traversal */
    public synchronized final boolean accept(MimePart mp) throws MessagingException {
        boolean modified = false, multiModified = false;
        MimeMultipart multi = null;

        if (mp instanceof MimeMessage)
            modified |= visitMessage((MimeMessage) mp, VisitPhase.VISIT_BEGIN);

        String ctype = Mime.getContentType(mp);
        boolean isMultipart = ctype.startsWith(Mime.CT_MULTIPART_PREFIX);
        boolean isMessage = !isMultipart && ctype.equals(Mime.CT_MESSAGE_RFC822);

        Object content = null;
        if (isMultipart) {
            try {
                content = Mime.getMultipartContent(mp, ctype);
            } catch (Exception e) {
                ZimbraLog.extensions.warn("could not fetch multipart content; skipping", e);
            }
            if (content instanceof MimeMultipart) {
                multi = (MimeMultipart) content;
                if (visitMultipart(multi, VisitPhase.VISIT_BEGIN))
                    modified = multiModified = true;

                try {
                    for (int i = 0; i < multi.getCount(); i++) {
                        BodyPart bp = multi.getBodyPart(i);
                        if (bp instanceof MimeBodyPart) {
                            if (accept((MimeBodyPart) bp))
                                modified = multiModified = true;
                        } else
                            ZimbraLog.extensions.info("unexpected BodyPart subclass: " + bp.getClass().getName());
                    }
                } catch (MessagingException e) {
                    ZimbraLog.extensions.warn("could not fetch body subpart; skipping remainder", e);
                }

                if (visitMultipart(multi, VisitPhase.VISIT_END))
                    modified = multiModified = true;
                if (multiModified)
                    mp.setContent(multi);
            }
        } else if (isMessage) {
            try {
                content = Mime.getMessageContent(mp);
            } catch (Exception e) {
                ZimbraLog.extensions.warn("could not fetch attached message content; skipping", e);
            }
            if (content instanceof MimeMessage)
                modified |= accept((MimeMessage) content);
        } else if (mp instanceof MimeBodyPart) {
            modified |= visitBodyPart((MimeBodyPart) mp);
        } else if (!(mp instanceof MimeMessage)) {
            ZimbraLog.extensions.info("unexpected MimePart subclass: " + mp.getClass().getName() + " (ctype='" + ctype + "')");
        }

        if (mp instanceof MimeMessage) {
            MimeMessage mm = (MimeMessage) mp;
            modified |= visitMessage(mm, VisitPhase.VISIT_END);

            // commit changes to the message
            if (modified)
                mm.saveChanges();
        }

        return modified;
    }
}
