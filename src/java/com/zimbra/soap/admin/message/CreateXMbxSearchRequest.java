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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.XMbxSearchConstants;
import com.zimbra.soap.admin.type.AdminKeyValuePairs;

// Note: ZimbraXMbxSearch/docs/soap.txt documented a non-existent <searchtask> sub-element.
//       This is not used - the attributes are direct children of <CreateXMbxSearchRequest>
/**
 * @zm-api-command-description Creates a search task
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=XMbxSearchConstants.E_CREATE_XMBX_SEARCH_REQUEST)
public class CreateXMbxSearchRequest extends AdminKeyValuePairs {

    public CreateXMbxSearchRequest() {
    }

}
