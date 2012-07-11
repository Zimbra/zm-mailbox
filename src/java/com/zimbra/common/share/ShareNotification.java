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
package com.zimbra.common.share;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimePart;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants.ShareConstants;

public class ShareNotification {

    public static ShareNotification fromMimePart(MimePart part)
    throws ServiceException, IOException, MessagingException {
        return new ShareNotification(part.getInputStream());
    }

    private ShareNotification(InputStream in) throws ServiceException {
        this(Element.parseXML(in));
    }

    private ShareNotification(Element xml) throws ServiceException {
        Element grantor = xml.getElement(ShareConstants.E_GRANTOR);
        Element grantee = xml.getElement(ShareConstants.E_GRANTEE);
        Element link = xml.getElement(ShareConstants.E_LINK);
        Element n = xml.getOptionalElement(ShareConstants.E_NOTES);

        grantorId = grantor.getAttribute(ShareConstants.A_ID);
        grantorEmail = grantor.getAttribute(ShareConstants.A_EMAIL);
        grantorName = grantor.getAttribute(ShareConstants.A_NAME);

        granteeId = grantee.getAttribute(ShareConstants.A_ID, null);
        granteeEmail = grantee.getAttribute(ShareConstants.A_EMAIL);
        granteeName = grantee.getAttribute(ShareConstants.A_NAME);

        itemId = (int)link.getAttributeLong(ShareConstants.A_ID);
        itemName = link.getAttribute(ShareConstants.A_NAME);
        view = link.getAttribute(ShareConstants.A_VIEW);

        if (xml.getQName().equals(ShareConstants.SHARE)) {
            permissions = link.getAttribute(ShareConstants.A_PERM);
        } else if (xml.getAttributeBool(ShareConstants.A_EXPIRE, false)) {
            expire = true;
        } else {
            revoke = true;
        }

        if (n != null) {
            notes = n.getText();
        }
    }

    public String getGrantorId() {
        return grantorId;
    }

    public String getGrantorName() {
        return grantorName;
    }

    public String getGrantorEmail() {
        return grantorEmail;
    }

    public String getGranteeId() {
        return granteeId;
    }

    public String getGranteeName() {
        return granteeName;
    }

    public String getGranteeEmail() {
        return granteeEmail;
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getView() {
        return view;
    }

    public String getPermissions() {
        return permissions;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isRevoke() {
        return revoke;
    }

    public boolean isExpire() {
        return expire;
    }

    private final String grantorId;
    private final String grantorName;
    private final String grantorEmail;

    private final String granteeId;
    private final String granteeName;
    private final String granteeEmail;

    private final int itemId;
    private final String itemName;
    private final String view;
    private String permissions;

    private String notes;
    private boolean revoke;
    private boolean expire;
}
