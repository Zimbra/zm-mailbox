/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;


public interface DataSource {

    public enum ConnectionType {
        cleartext,
        ssl,
        tls,
        tls_if_available;

        public static ConnectionType fromString(String s) throws ServiceException {
            try {
                return ConnectionType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid type: " + s + ", valid values: " + Arrays.asList(ConnectionType.values()), e); 
            }
        }
    };
    
    public void copy(DataSource from);
    
    public String getId();
    
    public void setId(String id);
    
    public String getName();
    
    public void setName(String name);
    
    public String getFolderId();
    
    public void setFolderId(String folderId);
    
    public Boolean isEnabled();
    
    public void setEnabled(Boolean isEnabled);
    
    public Boolean isImportOnly();
    
    public void setImportOnly(Boolean isImportOnly);
    
    public String getHost();
    
    public void setHost(String host);
    
    public Integer getPort();
    
    public void setPort(Integer port);
    
    public ConnectionType getConnectionType();
    
    public void setConnectionType(ConnectionType connectionType);
    
    public String getUsername();
    
    public void setUsername(String username);
    
    public String getPassword();
    
    public void setPassword(String password);
    
    public String getPollingInterval();
    
    public void setPollingInterval(String pollingInterval);
    
    public String getEmailAddress();
    
    public void setEmailAddress(String emailAddress);
    
    public Boolean isUseAddressForForwardReply();
    
    public void setUseAddressForForwardReply(Boolean useAddressForForwardReply);
    
    public String getDefaultSignature();
    
    public void setDefaultSignature(String defaultSignature);
    
    public String getFromDisplay();
    
    public void setFromDisplay(String fromDisplay);
    
    public String getFromAddress();
    
    public void setFromAddress(String fromAddress);
    
    public String getReplyToAddress();
    
    public void setReplyToAddress(String replyToAddress);
    
    public String getReplyToDisplay();
    
    public void setReplyToDisplay(String replyToDisplay);
    
    public Long getFailingSince();
    
    public void setFailingSince(Long failingSince);
    
    public String getLastError();
    
    public void setLastError(String lastError);
}
