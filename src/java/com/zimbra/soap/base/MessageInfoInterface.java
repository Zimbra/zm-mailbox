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
