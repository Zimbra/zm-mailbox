/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.index;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;

/**
 * A {@link ZimbraHit} which is being proxied from another server: i.e. we did a SOAP request somewhere else and are now
 * wrapping results we got from request.
 */
public final class ProxiedContactHit extends ProxiedHit  {

    /**
     * @param sortValue - typically A_FILE_AS_STR rather than A_SORT_FIELD (the value for general ProxiedHits)
     */
    public ProxiedContactHit(ZimbraQueryResultsImpl results, Element elt, String sortValue) {
        super(results, elt, sortValue);
    }

    @Override
    String getName() throws ServiceException {
        return super.getElement().getAttribute(MailConstants.A_FILE_AS_STR);
    }
}
