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
package com.zimbra.cs.index.query;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query messages tagged with Forwarded.
 *
 * @author tim
 * @author ysasaki
 */
public final class ForwardedQuery extends TagQuery {

    public ForwardedQuery(Mailbox mailbox, int mod, boolean truth)
        throws ServiceException {

        super(mailbox, mod, "\\Forwarded", truth);
    }

    @Override
    public StringBuilder dump(StringBuilder out) {
        super.dump(out);
        return out.append(getBool() ? ",FORWARDED)" : ",UNFORWARDED)");
    }
}
