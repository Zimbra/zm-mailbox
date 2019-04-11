/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.EmailAddrInfo;
import com.zimbra.soap.type.Id;

import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.types.GraphQLType;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Send share notification
 * <br />
 * The client can list the recipient email addresses for the share, along with the itemId of the item being shared.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SEND_SHARE_NOTIFICATION_REQUEST)
public class SendShareNotificationRequest {

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Item ID
     */
    @XmlElement(name=MailConstants.E_ITEM /* item */, required=false)
    private Id item;

    /**
     * @zm-api-field-tag email-addrs
     * @zm-api-field-description Email addresses
     */
    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    private final List<EmailAddrInfo> emailAddresses = Lists.newArrayList();

    /**
     * @zm-api-field-tag notes
     * @zm-api-field-description Notes
     */
    @XmlElement(name=MailConstants.E_NOTES /* notes */, required=false)
    private String notes;

    /**
     * @zm-api-field-tag action
     * @zm-api-field-description Set to "revoke" if it is a grant revoke notification. It is set to "expire"
     *   by the system to send notification for a grant expiry.
     */
    @XmlAttribute(name=MailConstants.A_ACTION /* action */, required=false)
    private Action action;

    public SendShareNotificationRequest() {
    }

    public static SendShareNotificationRequest create(Id id, Action action, String notes,
            List<EmailAddrInfo> emailAddresses) {
        SendShareNotificationRequest req = new SendShareNotificationRequest();
        req.setItem(id);
        req.setAction(action);
        req.setNotes(notes);
        req.setEmailAddresses(emailAddresses);
        return req;
    }

    public static SendShareNotificationRequest create(Integer id, Action action, String notes,
            List<EmailAddrInfo> emailAddresses) {
        SendShareNotificationRequest req = new SendShareNotificationRequest();
        req.setItem(new Id(id));
        req.setAction(action);
        req.setNotes(notes);
        req.setEmailAddresses(emailAddresses);
        return req;
    }

    public void setItem(Id item) { this.item = item; }
    public void setEmailAddresses(Iterable <EmailAddrInfo> emailAddresses) {
        this.emailAddresses.clear();
        if (emailAddresses != null) {
            Iterables.addAll(this.emailAddresses,emailAddresses);
        }
    }
    public void addEmailAddress(EmailAddrInfo emailAddress) {
        this.emailAddresses.add(emailAddress);
    }
    public void setNotes(String notes) { this.notes = notes; }

    public Id getItem() { return item; }
    public List<EmailAddrInfo> getEmailAddresses() {
        return Collections.unmodifiableList(emailAddresses);
    }
    public String getNotes() { return notes; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("item", item)
            .add("email", emailAddresses)
            .add("notes", notes)
            .add("action", action);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    @XmlEnum
    @GraphQLType(name = GqlConstants.CLASS_ACTION, description = "sharing action")
    public static enum Action {
        @GraphQLEnumValue(description = "edit share") edit,
        @GraphQLEnumValue(description = "revoke share") revoke,
        @GraphQLEnumValue(description = "share expired") expire;

        public static Action fromString(String value) throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return Action.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST(
                        "Invalid value: " + value + ", valid values: " + Arrays.asList(Action.values()), null);
            }
        }
    }
}
