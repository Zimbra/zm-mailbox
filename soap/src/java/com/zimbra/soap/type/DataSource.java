/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
    public String getReplyToAddress();
    public String getReplyToDisplay();
    public String getImportClass();
    public Long getFailingSince();
    public String getLastError();
    public List<String> getAttributes();
    public void setRefreshToken(String refreshToken);
    public String getRefreshToken();
    public void setRefreshTokenUrl(String refreshTokenUrl);
    public String getRefreshTokenUrl();
}
