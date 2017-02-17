/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.base;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.zimbra.soap.type.KeyValuePair;

@XmlAccessorType(XmlAccessType.NONE)
public interface MessageInfoInterface
extends MessageCommonInterface {
    public MessageInfoInterface createFromId(String id);
    public void setId(String id);
    public void setCalendarIntendedFor(String calendarIntendedFor);
    public void setOrigId(String origId);
    public void setDraftReplyType(String draftReplyType);
    public void setIdentityId(String identityId);
    public void setDraftAccountId(String draftAccountId);
    public void setDraftAutoSendTime(Long draftAutoSendTime);
    public void setSentDate(Long sentDate);
    public void setResentDate(Long resentDate);
    public void setPart(String part);
    public void setFragment(String fragment);
    public void setSubject(String subject);
    public void setMessageIdHeader(String messageIdHeader);
    public void setInReplyTo(String inReplyTo);
    public void setHeaders(Iterable <KeyValuePair> headers);
    public void addHeader(KeyValuePair header);
    public void setContentElems(Iterable <Object> contentElems);
    public void addContentElem(Object contentElem);
    public String getId();
    public String getCalendarIntendedFor();
    public String getOrigId();
    public String getDraftReplyType();
    public String getIdentityId();
    public String getDraftAccountId();
    public Long getDraftAutoSendTime();
    public Long getSentDate();
    public Long getResentDate();
    public String getPart();
    public String getFragment();
    public String getSubject();
    public String getMessageIdHeader();
    public String getInReplyTo();
    public List<KeyValuePair> getHeaders();
    public List<Object> getContentElems();
    public void setEmailInterfaces(Iterable<EmailInfoInterface> emails);
    public void addEmailInterface(EmailInfoInterface email);
    public void setInviteInterface(InviteInfoInterface invite);
    public List<EmailInfoInterface> getEmailInterfaces();
    public InviteInfoInterface getInvitInterfacee();
}
