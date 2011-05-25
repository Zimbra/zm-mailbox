/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

package com.zimbra.soap.type;

import java.util.Arrays;
import java.util.List;

import com.zimbra.common.service.ServiceException;

public interface DataSource {

    public enum ConnectionType {
        cleartext,
        ssl,
        tls,
        tls_if_available;

        public static ConnectionType fromString(String s)
        throws ServiceException {
            try {
                return ConnectionType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid type: " +
                        s + ", valid values: " +
                        Arrays.asList(ConnectionType.values()), e);
            }
        }
    };

    public void copy(DataSource from);

    public void setId(String id);
    public void setName(String name);
    public void setFolderId(String folderId);
    public void setEnabled(Boolean enabled);
    public void setImportOnly(Boolean importOnly);
    public void setHost(String host);
    public void setPort(Integer port);
    /* Interface interested in ConnectionType, not MdsConnectionType */
    public void setConnectionType(ConnectionType connectionType);
    public void setUsername(String username);
    public void setPassword(String password);
    public void setPollingInterval(String pollingInterval);
    public void setEmailAddress(String emailAddress);
    public void setUseAddressForForwardReply(Boolean useAddressForForwardReply);
    public void setDefaultSignature(String defaultSignature);
    public void setForwardReplySignature(String forwardReplySignature);
    public void setFromDisplay(String fromDisplay);
    public void setFromAddress(String fromAddress);
    public void setReplyToAddress(String replyToAddress);
    public void setReplyToDisplay(String replyToDisplay);
    public void setImportClass(String importClass);
    public void setFailingSince(Long failingSince);
    public void setLastError(String lastError);
    public void setAttributes(Iterable <String> attributes);
    public void addAttribute(String attribute);

    public String getId();
    public String getName();
    public String getFolderId();
    public Boolean isEnabled();
    public Boolean isImportOnly();
    public String getHost();
    public Integer getPort();
    /* Interface interested in ConnectionType, not MdsConnectionType */
    public ConnectionType getConnectionType();
    public String getUsername();
    public String getPassword();
    public String getPollingInterval();
    public String getEmailAddress();
    public Boolean isUseAddressForForwardReply();
    public String getDefaultSignature();
    public String getForwardReplySignature();
    public String getFromDisplay();
    public String getFromAddress();
    public String getReplyToAddress();
    public String getReplyToDisplay();
    public String getImportClass();
    public Long getFailingSince();
    public String getLastError();
    public List<String> getAttributes();
}
