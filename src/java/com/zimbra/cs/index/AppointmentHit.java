/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Feb 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import org.apache.lucene.document.Document;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author tim
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AppointmentHit extends MessageHit {

    /**
     * @param results
     * @param mbx
     * @param d
     * @param score
     */
    public AppointmentHit(ZimbraQueryResultsImpl results, Mailbox mbx, Document d,
            float score) {
        super(results, mbx, d, score);
    }

    /**
     * @param results
     * @param mbx
     * @param id
     * @param score
     */
    public AppointmentHit(ZimbraQueryResultsImpl results, Mailbox mbx, int id,
            float score) {
        super(results, mbx, id, score);
    }

}
