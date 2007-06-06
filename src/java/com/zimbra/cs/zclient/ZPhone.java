/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;

public class ZPhone {

	private String mName;

	public ZPhone(String name) throws ServiceException {
		mName = name;
	}

	public String getName() {
		return mName;
	}

	public String getDisplay() {
        // Handles familiar usa-style numbers only for now...
        int offset = 0;
        boolean doIt = false;
        if (mName.length() == 10) {
            doIt = true;
        } else if ((mName.length() == 11) && (mName.charAt(0) == '1')) {
            doIt = true;
            offset = 1;
        }
        if (doIt) {
            StringBuilder builder = new StringBuilder();
            builder.append('(');
            builder.append(mName, offset, offset + 3);
            builder.append(") ");
            builder.append(mName, offset + 3, offset + 6);
            builder.append('-');
            builder.append(mName, offset + 6, offset + 10);
            return builder.toString();
        } else {
            return mName;
        }
	}
}
