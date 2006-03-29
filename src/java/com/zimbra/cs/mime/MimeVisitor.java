/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import com.zimbra.cs.util.ZimbraLog;

/**
 * A class that implements this interface can be passed to {@link Mime#accept}
 * to walk a MIME node tree.
 *   
 * @author bburtin
 */
public abstract class MimeVisitor {

    /** The list of registered MimeVistor classes that convert stored
     *  messages on the fly. */
    private static List<Class> sMimeConverters = new ArrayList<Class>();
    /** The list of registered MimeVistor classes that convert new messages
     *  before storing them to disk. */
    private static List<Class> sMimeMutators   = new ArrayList<Class>();

        static {
            registerConverter(UUEncodeConverter.class);
            registerConverter(TnefConverter.class);
        }

    /** Adds a MimeVisitor class to the list of converters invoked on the fly
     *  when a message is fetched from the store or prepared for indexing.
     *  Note that changes made by these MimeVisitors are not persisted to disk
     *  but instead are executed every time the message is accessed. */
    public static void registerConverter(Class vclass) {
        try {
            if (vclass.newInstance() instanceof MimeVisitor)
                sMimeConverters.add(vclass);
        } catch (Exception e) { }
    }

    /** Retrieves the list of all registered MimeVisitor converter classes.
     * @see #registerConverter(Class) */
    public static List<Class> getConverters() {
        return new ArrayList<Class>(sMimeConverters);
    }

    /** Adds a MimeVisitor class to the list of mutators invoked before a
     *  message is saved to disk or sent via SMTP. */
    public static void registerMutator(Class vclass) {
        try {
            if (vclass.newInstance() instanceof MimeVisitor)
                sMimeMutators.add(vclass);
        } catch (Exception e) { }
    }

    /** Retrieves the list of all registered MimeVisitor mutator classes.
     * @see #registerMutator(Class) */
    public static List<Class> getMutators() {
        return new ArrayList<Class>(sMimeMutators);
    }

    /** Returns whether the system has any registered MimeVisitor mutator
     *  classes.
     * @see #registerMutator(Class) */
    public static boolean hasMutators() {
        return !sMimeMutators.isEmpty();
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
    protected abstract boolean visitMessage(MimeMessage msg, VisitPhase visitKind) throws MessagingException;

    /** Visitor callback for traversing a Multipart.
     * @return whether any changes were performed during the visit
     * @see VisitPhase */
    protected abstract boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) throws MessagingException;

    /** Visitor callback for traversing a BodyPart.
     * @return whether any changes were performed during the visit
     * @see VisitPhase */
    protected abstract boolean visitBodyPart(MimeBodyPart bp) throws MessagingException;


    /** Determines the "primary/subtype" part of a MimePart's Content-Type
     *  header.  Uses a permissive, RFC2231-capable parser, and defaults
     *  when appropriate. */
    private static final String getContentType(MimePart mp) {
        Map<String, String> ctattrs;
        try {
            ctattrs = Mime.decodeRFC2231(mp.getContentType());
        } catch (MessagingException e) {
            ZimbraLog.extensions.warn("could not fetch part's content-type; defaulting to " + Mime.CT_DEFAULT, e);
            return Mime.CT_DEFAULT;
        }
        String ctype = (ctattrs == null ? null : ctattrs.get(null));
        if (ctype != null)
            ctype = ctype.trim();

        if (ctype == null || ctype.equals(""))
            return Mime.CT_DEFAULT;
        else if (ctype.toLowerCase().equals("text"))
            return Mime.CT_TEXT_PLAIN;
        return ctype;
    }

    private static final String MULTIPART_PREFIX = Mime.CT_MULTIPART + '/';

    /** Walks the mail object tree depth-first, starting at the specified
     *  <code>MimePart</code>.  Invokes the various <code>MimeVisitor</code>
     *  methods in for each visited node.
     * 
     * @param mp the root MIME part at which to start the traversal */
    public synchronized final boolean accept(MimePart mp) throws MessagingException {
        boolean modified = false, msgModified = false;
        MimeMultipart multi = null;

        if (mp instanceof MimeMessage)
            modified |= visitMessage((MimeMessage) mp, VisitPhase.VISIT_BEGIN);

        String ctype = getContentType(mp);
        boolean isMultipart = ctype.startsWith(MULTIPART_PREFIX);
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
                modified |= visitMultipart(multi, VisitPhase.VISIT_BEGIN);
                
                // Make a copy of the parts array and iterate the copy,
                // in case the visitor is adding or removing parts.
                List<BodyPart> parts = new ArrayList<BodyPart>();
                try {
                    for (int i = 0; i < multi.getCount(); i++)
                        parts.add(multi.getBodyPart(i));
                } catch (MessagingException e) {
                    ZimbraLog.extensions.warn("could not fetch body subpart; skipping remainder", e);
                }
                for (BodyPart bp : parts) {
                    if (bp instanceof MimeBodyPart)
                        modified |= accept((MimeBodyPart) bp);
                    else
                        ZimbraLog.extensions.info("unexpected BodyPart subclass: " + bp.getClass().getName());
                }
                modified |= visitMultipart(multi, VisitPhase.VISIT_END);
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
            modified |= (msgModified = visitMessage(mm, VisitPhase.VISIT_END));

            if (modified) {
                // JavaMail oddity: any changes to multipart content require call to setContent()
                if (multi != null && !msgModified)
                    mm.setContent(multi);
                // commit changes to the message
                mm.saveChanges();
            }
        }

        return modified;
    }
}
