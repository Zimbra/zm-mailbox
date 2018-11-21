/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.client;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import com.zimbra.client.event.ZModifyMessageEvent;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zclient.ZClientException;

public class ZMessage extends ZBaseItem implements ToZJSONObject {

    private final String mSubject;
    private final String mFragment;
    private String mFolderId;
    private String mConversationId;
    private final String mPartName;
    private final long mReceivedDate;
    private final long mSentDate;
    private final String mMessageIdHeader;
    private final List<ZEmailAddress> mAddresses;
    private ZMimePart mMimeStructure;
    private String mContent;
    private String mContentURL;
    private final long mSize;
    private final String mReplyType;
    private final String mInReplyTo;
    private final String mOrigId;
    private ZInvite mInvite;
    private ZShare mShare;
    private final Map<String, String> mReqHdrs;
    private final String mIdentityId;
    private final long mAutoSendTime;
    private final String mSubjectFontSize;
    private final String mLocationFontSize;

    public ZMessage(Element e, ZMailbox zmailbox) throws ServiceException {
        super(e.getAttribute(MailConstants.A_ID),
                e.getAttributeInt(MailConstants.A_IMAP_UID, -1),
                e.getAttributeInt(MailConstants.A_MODIFIED_SEQUENCE, 0));
        mMailbox = zmailbox;
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        mTagIds = e.getAttribute(MailConstants.A_TAGS, null);
        mReplyType = e.getAttribute(MailConstants.A_REPLY_TYPE, null);
        mOrigId = e.getAttribute(MailConstants.A_ORIG_ID, null);
        mSubject = e.getAttribute(MailConstants.E_SUBJECT, null);
        mFragment = e.getAttribute(MailConstants.E_FRAG, null);
        mMessageIdHeader = e.getAttribute(MailConstants.E_MSG_ID_HDR, null);
        mInReplyTo = e.getAttribute(MailConstants.E_IN_REPLY_TO, null);

        mReceivedDate = e.getAttributeLong(MailConstants.A_DATE, 0);
        mSentDate = e.getAttributeLong(MailConstants.A_SENT_DATE, 0);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER, null);
        mConversationId = e.getAttribute(MailConstants.A_CONV_ID, null);
        mPartName = e.getAttribute(MailConstants.A_PART, null);
        mSize = e.getAttributeLong(MailConstants.A_SIZE, -1);
        mIdentityId = e.getAttribute(MailConstants.A_IDENTITY_ID, null);
        mAutoSendTime = e.getAttributeLong(MailConstants.A_AUTO_SEND_TIME, 1);

        Element mMeta = getMetaSection(e, "fontSize");
        mSubjectFontSize = getMetaValueFromKey(mMeta, "subjectFontSize");
        mLocationFontSize = getMetaValueFromKey(mMeta, "locationFontSize");

        Element content = e.getOptionalElement(MailConstants.E_CONTENT);
        if (content != null) {
            mContent = content.getText();
            mContentURL = content.getAttribute(MailConstants.A_URL, null);
        }

        mAddresses = new ArrayList<ZEmailAddress>();
        for (Element emailEl: e.listElements(MailConstants.E_EMAIL)) {
            mAddresses.add(new ZEmailAddress(emailEl));
        }

        //request headers
        mReqHdrs = new HashMap<String,String>();
        List<Element.KeyValuePair> hdrs = e.listKeyValuePairs(MailConstants.A_HEADER, MailConstants.A_ATTRIBUTE_NAME);
        if (hdrs != null) {
            for (Element.KeyValuePair hdr : hdrs) {
                mReqHdrs.put(hdr.getKey(), hdr.getValue());
            }
        }

        Element mp = e.getOptionalElement(MailConstants.E_MIMEPART);
        if (mp != null)
            mMimeStructure = new ZMimePart(null, mp);

        Element inviteEl = e.getOptionalElement(MailConstants.E_INVITE);
        if (inviteEl != null)
            mInvite = new ZInvite(inviteEl);

        Element shrEl = e.getOptionalElement("shr");
        if (shrEl != null) {
            String shareContent = shrEl.getAttribute(MailConstants.E_CONTENT);
            if (shareContent != null) {
                mShare = ZShare.parseXml(shareContent);
            }
        }
    }

    //get the metadata off the message, by sectionName
    public Element getMetaSection(Element e, String sectionName) {
        Element meta = new Element.XMLElement("empty");

        for (Element elt : e.listElements("meta")) {
            String metaSection = elt.getAttribute("section", null);

            if (sectionName.equals(metaSection)) {
                meta = elt;
                return meta;
            }
        }

        return meta;
    }

    // for use with Elements returned from getMetaSection
    public String getMetaValueFromKey(Element e, String key) {

        for (Element elt : e.listElements("a")) {
            String attr = elt.getAttribute("n", null);

            if (attr.equals(key)) {
                return elt.getText();
            }
        }

        return "";
    }

    public void modifyNotification(ZModifyMessageEvent mevent) throws ServiceException {
        if (mevent.getId().equals(mId)) {
            mFlags = mevent.getFlags(mFlags);
            mTagIds = mevent.getTagIds(mTagIds);
            mFolderId = mevent.getFolderId(mFolderId);
            mConversationId = mevent.getConversationId(mConversationId);
        }
    }

    public  ZShare getShare() {
        return mShare;
    }

    /**
     *
     * @return invite object if this message contains an invite, null otherwise.
     */
    public ZInvite getInvite() {
        return mInvite;
    }

    /**
     *
     * @return Zimbra id of message we are replying to if this is a draft.
     */
    public String getOriginalId() {
        return mOrigId;
    }

    /**
     *
     * @return message-id header of message we are replying to if this is a draft
     */
    public String getInReplyTo() {
        return mInReplyTo;
    }

    /**
     *
     * @return reply type if this is a draft
     */
    public String getReplyType() {
        return mReplyType;
    }

    public String getSubjectFontSize() {
        return  mSubjectFontSize;
    }

    public String getLocationFontSize() {
        return mLocationFontSize;
    }

    @Override
    public long getSize() {
        return mSize;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", mId);
        zjo.put("flags", mFlags);
        zjo.put("tagIds", mTagIds);
        zjo.put("inReplyTo", mInReplyTo);
        zjo.put("originalId", mOrigId);
        zjo.put("subject", mSubject);
        zjo.put("fragment", mFragment);
        zjo.put("partName", mPartName);
        zjo.put("messageIdHeader", mMessageIdHeader);
        zjo.put("receivedDate", mReceivedDate);
        zjo.put("sentDate", mSentDate);
        zjo.put("folderId", mFolderId);
        zjo.put("conversationId", mConversationId);
        zjo.put("size", mSize);
        zjo.put("content", mContent);
        zjo.put("contentURL", mContentURL);
        zjo.put("addresses", mAddresses);
        zjo.put("mimeStructure", mMimeStructure);
        zjo.put("invite", mInvite);
        zjo.put("share", mShare);
        zjo.put("isInvite", getInvite() != null);
        zjo.put("hasAttachment", hasAttachment());
        zjo.put("hasFlags", hasFlags());
        zjo.put("hasTags", hasTags());
        zjo.put("isDeleted", isDeleted());
        zjo.put("isDraft", isDraft());
        zjo.put("isFlagged", isFlagged());
        zjo.put("isHighPriority", isHighPriority());
        zjo.put("isLowPriority", isLowPriority());
        zjo.put("isForwarded", isForwarded());
        zjo.put("isNotificationSent", isNotificationSent());
        zjo.put("isRepliedTo", isRepliedTo());
        zjo.put("isSentByMe", isSentByMe());
        zjo.put("isUnread", isUnread());
        zjo.put("idnt", mIdentityId);
        zjo.put("subjectFontSize", mSubjectFontSize);
        zjo.put("locationFontSize", mLocationFontSize);
        if (imapUid >= 0) {
            zjo.put("imapUid", imapUid);
        }
        zjo.putMap("requestHeaders", mReqHdrs);
        return zjo;
    }

    @Override
    public String toString() {
        return String.format("[ZMessage %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    /**
     *
     * @return the part name if this message is actually a part of another message
     */
    public String getPartName() {
        return mPartName;
    }

    public String getSubject() {
        return mSubject;
    }

    public String getFragment() {
        return mFragment;
    }

    public String getConversationId() {
        return mConversationId;
    }

    public Map<String,String> getRequestHeader() {
        return mReqHdrs;
    }

    public List<ZEmailAddress> getEmailAddresses() {
        return mAddresses;
    }

    public String getFolderId() {
        return mFolderId;
    }

    @Override
    public int getFolderIdInMailbox() throws ServiceException {
        return getIdInMailbox(getFolderId());
    }

    public String getMessageIdHeader() {
        return mMessageIdHeader;
    }

    public ZMimePart getMimeStructure() {
        return mMimeStructure;
    }

    public long getReceivedDate() {
        return mReceivedDate;
    }

    public long getSentDate() {
        return mSentDate;
    }

    /** content of the message, if raw is specified. if message too big or not ASCII, a content servlet URL is returned */
    public String getContent() {
        return mContent;
    }

    /** if raw is specified and message too big or not ASCII, a content servlet URL is returned */
    public String getContentURL() {
        return mContentURL;
    }

    public String getIdentityId() {
        return mIdentityId;
    }

    public static class ZMimePart implements ToZJSONObject {
        private final String mPartName;
        private final String mName;
        private final String mContentType;
        private final String mContentDisposition;
        private final String mFileName;
        private final String mContentId;
        private final String mContentLocation;
        private final String mContentDescription;
        private final String mContent;
        private final boolean mIsBody;
        private final List<ZMimePart> mChildren;
        private final long mSize;
        private final ZMimePart mParent;
        private final boolean mTruncated;

        public ZMimePart(ZMimePart parent, Element e) throws ServiceException {
            mParent = parent;
            mPartName = e.getAttribute(MailConstants.A_PART);
            mName = e.getAttribute(MailConstants.A_NAME, null);
            mContentType = e.getAttribute(MailConstants.A_CONTENT_TYPE, null);
            mContentDisposition = e.getAttribute(MailConstants.A_CONTENT_DISPOSITION, null);
            mFileName = e.getAttribute(MailConstants.A_CONTENT_FILENAME, null);
            mContentId = e.getAttribute(MailConstants.A_CONTENT_ID, null);
            mContentDescription = e.getAttribute(MailConstants.A_CONTENT_DESCRIPTION, null);
            mContentLocation = e.getAttribute(MailConstants.A_CONTENT_LOCATION, null);
            mIsBody = e.getAttributeBool(MailConstants.A_BODY, false);
            mSize = e.getAttributeLong(MailConstants.A_SIZE, 0);
            mContent = e.getAttribute(MailConstants.E_CONTENT, null);
            mChildren = new ArrayList<ZMimePart>();
            for (Element mpEl: e.listElements(MailConstants.E_MIMEPART)) {
                mChildren.add(new ZMimePart(this, mpEl));
            }
            mTruncated = e.getAttributeBool(MailConstants.A_TRUNCATED_CONTENT, false);
        }

        @Override
        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("partName", mPartName);
            zjo.put("content", mContent);
            zjo.put("contentType", mContentType);
            zjo.put("contentDisposition", mContentDisposition);
            zjo.put("contentId", mContentId);
            zjo.put("contentLocation", mContentLocation);
            zjo.put("contentDescription", mContentDescription);
            zjo.put("isBody", mIsBody);
            zjo.put("size", mSize);
            zjo.put("name", mName);
            zjo.put("fileName", mFileName);
            zjo.put("children", mChildren);
            return zjo;
        }

        @Override
        public String toString() {
            return String.format("[ZMimePart %s]", mPartName);
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }

        public ZMimePart getParent() {
            return mParent;
        }

        /** "" means top-level part, 1 first part, 1.1 first part of a multipart inside of 1. */
        public String getPartName() {
            return mPartName;
        }

        /** name attribute from the Content-Type param list */
        public String getName() {
            return mName;
        }

        /** MIME Content-Type */
        public String getContentType() {
            return mContentType;
        }

        /** MIME Content-Disposition */
        public String getContentDisposition() {
            return mContentDisposition;
        }

        /** filename attribute from the Content-Disposition param list */
        public String getFileName() {
            return mFileName;
        }

        /** MIME Content-ID (for display of embedded images) */
        public String getContentId() {
            return mContentId;
        }

        /** MIME/Microsoft Content-Location (for display of embedded images) */
        public String getContentLocation() {
            return mContentLocation;
        }

        /** MIME Content-Description.  Note cont-desc is not currently used in the code. */
        public String getContentDescription() {
            return mContentDescription;
        }

        /** content of the part, if requested */
        public String getContent() {
            return mContent;
        }

        /** set to 1, if this part is considered to be the "body" of the message for display purposes */
        public boolean isBody() {
            return mIsBody;
        }

        /** get child parts */
        public List<ZMimePart> getChildren() {
            return mChildren;
        }

        public long getSize() {
            return mSize;
        }

        public boolean wasTruncated() {
            return mTruncated;
        }
    }

    public boolean hasAttachment() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.ATTACHED.getFlagChar()) != -1;
    }

    public boolean isDeleted() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.DELETED.getFlagChar()) != -1;
    }

    public boolean isDraft() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.DRAFT.getFlagChar()) != -1;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.FLAGGED.getFlagChar()) != -1;
    }

    public boolean isForwarded() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.FORWARDED.getFlagChar()) != -1;
    }

    public boolean isNotificationSent() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.NOTIFIED.getFlagChar()) != -1;
    }

    public boolean isRepliedTo() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.REPLIED.getFlagChar()) != -1;
    }

    public boolean isSentByMe() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.FROM_ME.getFlagChar()) != -1;
    }

    public boolean isUnread() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.UNREAD.getFlagChar()) != -1;
    }

    public boolean isHighPriority() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.HIGH_PRIORITY.getFlagChar()) != -1;
    }

    public boolean isLowPriority() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.LOW_PRIORITY.getFlagChar()) != -1;
    }

    public void delete() throws ServiceException {
        getMailbox().deleteMessage(getId());
    }

    public void deleteItem() throws ServiceException {
        delete();
    }

    public void trash() throws ServiceException {
        getMailbox().trashMessage(getId());
    }

    public void markRead(boolean read) throws ServiceException {
        getMailbox().markMessageRead(getId(), read);
    }

    public void flag(boolean flag) throws ServiceException {
        getMailbox().flagMessage(getId(), flag);
    }

    public void tag(String nameOrId, boolean tagged) throws ServiceException {
        ZTag tag = mMailbox.getTag(nameOrId);
        if (tag == null)
            throw ZClientException.CLIENT_ERROR("unknown tag: "+nameOrId, null);
        else
           tag(tag, tagged);
    }

    public void tag(ZTag tag, boolean tagged) throws ServiceException {
        mMailbox.tagMessage(mId, tag.getId(), tagged);
    }

    public void move(String pathOrId) throws ServiceException {
        ZFolder destFolder = mMailbox.getFolder(pathOrId);
        if (destFolder == null)
            throw ZClientException.CLIENT_ERROR("unknown folder: "+pathOrId, null);
        else
            move(destFolder);
    }

    public void move(ZFolder destFolder) throws ServiceException {
        mMailbox.moveMessage(mId, destFolder.getId());
    }

    public void markSpam(boolean spam, String pathOrId) throws ServiceException {
        ZFolder destFolder = mMailbox.getFolder(pathOrId);
        if (destFolder == null)
            throw ZClientException.CLIENT_ERROR("unknown folder: "+pathOrId, null);
        else
            markSpam(spam, destFolder);
    }

    public void markSpam(boolean spam, ZFolder destFolder) throws ServiceException {
        getMailbox().markMessageSpam(getId(), spam, destFolder == null ? null : destFolder.getId());
    }

    public void update(String destFolderId, String tagList, String flags) throws ServiceException {
        getMailbox().updateMessage(getId(), destFolderId, tagList, flags); // TODO: simplify tags/folders
    }

    public long getAutoSendTime() {
        return mAutoSendTime;
    }

    @Override
    public long getDate() {
        return this.getReceivedDate();
    }

    /** This should return the same data as MailItem.getContentStream() */
    @Override
    public InputStream getContentStream() throws ServiceException {
        /* Initially thought that if mContent was not null (only true in "raw" mode) we could use that as the basis
         * of the result but it appears that line ending information is lost.
         */
        if (mContentURL != null) {
            ZimbraLog.mailbox.debug("ZMessage getContentStream() based on mContentURL '%s'", mContentURL);
            URI uri = mMailbox.getTransportURI(mContentURL);
            return mMailbox.getResource(uri);
        }
        return super.getContentStream();
    }

    @Override
    public MailItemType getMailItemType() {
        return MailItemType.MESSAGE;
    }

    /**
     * @return the UID the item is referenced by in the IMAP server.  Returns <tt>0</tt> for items that require
     * renumbering because of moves.
     * The "IMAP UID" will be the same as the item ID unless the item has been moved after the mailbox owner's first
     * IMAP session. */
    @Override
    public int getImapUid() {
        if (imapUid >= 0) {
            return imapUid;
        }
        ZimbraLog.mailbox.debug("ZMessage getImapUid() - regetting UID");
        ZMessage zm = null;
        try {
            /* Perhaps, this ZMessage object was not created in a way which included Imap UID information (or
             * a renumber is under way).  Using this mechanism guarantees that the Imap UID will be asked for,
             * or if there is a cache hit, the mechanism that put the entry in the cache should have ensured
             * Imap UID info was provided. */
            zm = getMailbox().getMessageById(mId);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.debug("ZMessage getImapUid() - getMessageById failed", e);
            return 0;
        }
        if (null == zm) {
            return 0;
        }
        imapUid = (zm.imapUid <=0 ) ? 0 : zm.imapUid;
        return imapUid;
    }
}