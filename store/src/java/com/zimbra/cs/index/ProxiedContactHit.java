/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
