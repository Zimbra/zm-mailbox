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

package com.zimbra.soap.admin.type;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.zimbra.common.service.ServiceException;

@XmlEnum
public enum RequestAttr {
    @XmlEnumValue("zimbraMailRecipient") mailRecipient,
    @XmlEnumValue("zimbraAccount") account,
    @XmlEnumValue("zimbraAlias") alias,
    @XmlEnumValue("zimbraDistributionList") distributionList,
    @XmlEnumValue("zimbraCOS") cos,
    @XmlEnumValue("zimbraGlobalConfig") globalConfig,
    @XmlEnumValue("zimbraDomain") domain,
    @XmlEnumValue("zimbraSecurityGroup") securityGroup,
    @XmlEnumValue("zimbraServer") server,
    @XmlEnumValue("zimbraMimeEntry") mimeEntry,
    @XmlEnumValue("zimbraObjectEntry") objectEntry,
    @XmlEnumValue("zimbraTimeZone") timeZone,
    @XmlEnumValue("zimbraZimletEntry") zimletEntry,
    @XmlEnumValue("zimbraCalendarResource") calendarResource,
    @XmlEnumValue("zimbraIdentity") identity,
    @XmlEnumValue("zimbraDataSource") dataSource,
    @XmlEnumValue("zimbraPop3DataSource") pop3DataSource,
    @XmlEnumValue("zimbraImapDataSource") imapDataSource,
    @XmlEnumValue("zimbraRssDataSource") rssDataSource,
    @XmlEnumValue("zimbraLiveDataSource") liveDataSource,
    @XmlEnumValue("zimbraGalDataSource") galDataSource,
    @XmlEnumValue("zimbraSignature") signature,
    @XmlEnumValue("zimbraXMPPComponent") xmppComponent,
    @XmlEnumValue("zimbraAclTarget") aclTarget,
    @XmlEnumValue("zimbraGroup") group;
    
    public static RequestAttr fromString(String s) throws ServiceException {
        try {
            return RequestAttr.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST(
                    "invalid attrs: \""+ s + "\", valid values: " + 
                    Arrays.asList(RequestAttr.values()), e);
        }
    }
}
