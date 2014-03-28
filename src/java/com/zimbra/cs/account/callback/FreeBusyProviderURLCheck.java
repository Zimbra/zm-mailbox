/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

/**
 * @author zimbra
 *
 */
public class FreeBusyProviderURLCheck extends AttributeCallback {

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.AttributeCallback#preModify(com.zimbra.cs.account.callback.CallbackContext, java.lang.String, java.lang.Object, java.util.Map, com.zimbra.cs.account.Entry)
     */
    @Override
    public void preModify(CallbackContext context, String attrName,
            Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        String value = (String) attrValue;
        if (StringUtil.isNullOrEmpty(value)) {
            // we are unsetting the either the zimbraFreebusyExchangeURL or
            // zimbraFreebusyExternalZimbraURL
            return;
        }

        Provisioning prov = Provisioning.getInstance();

        if (attrsToModify.containsKey(Provisioning.A_zimbraFreebusyExchangeURL) &&
                (attrsToModify.containsKey(Provisioning.A_zimbraFreebusyExternalZimbraURL))) {
            throw ServiceException.INVALID_REQUEST("Setting both " +
                    Provisioning.A_zimbraFreebusyExchangeURL + " and " +  Provisioning.A_zimbraFreebusyExternalZimbraURL +
                    " is not allowed", null);
        } else if (attrsToModify.containsKey(Provisioning.A_zimbraFreebusyExchangeURL)) {
            String[] zimbraFbExtUrl = prov.getConfig().getFreebusyExternalZimbraURL();
            if (zimbraFbExtUrl != null && zimbraFbExtUrl.length > 0) {
                 throw ServiceException.INVALID_REQUEST("Setting both " +
                         Provisioning.A_zimbraFreebusyExchangeURL + " and " +  Provisioning.A_zimbraFreebusyExternalZimbraURL +
                         " is not allowed", null);
            }
        } else if (attrsToModify.containsKey(Provisioning.A_zimbraFreebusyExternalZimbraURL)) {
            String exchngFbUrl = prov.getConfig().getFreebusyExchangeURL();
            if (!StringUtil.isNullOrEmpty(exchngFbUrl)) {
                 throw ServiceException.INVALID_REQUEST("Setting both " +
                         Provisioning.A_zimbraFreebusyExchangeURL + " and " +  Provisioning.A_zimbraFreebusyExternalZimbraURL +
                         " is not allowed", null);
            }

        }

    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.AttributeCallback#postModify(com.zimbra.cs.account.callback.CallbackContext, java.lang.String, com.zimbra.cs.account.Entry)
     */
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {

    }

}
