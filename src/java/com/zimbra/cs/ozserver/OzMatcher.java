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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;

/**
 * An OzMatcher are stateful objects used to find the end of a
 * Protocol Data Unit (PDU) in a stream of incoming data. 
 */
public interface OzMatcher {
    
    /**
     * Process the given buffer, advancing its position, until the desired end
     * of PDU is reached.  Never advance the buffer's position beyond the match.
     * This method will be invoked will different buffers, as and when IO occurs.
     */ 
    boolean match(ByteBuffer buffer);
    
    /**
     * Some PDUs have a sequence of bytes at the end that are used to demarcate
     * the end.  Return the number of bytes so they can be trimmed by the caller.
     * This method is never called until a match has occurred, in the interest of
     * matchers that may have variable length PDU terminator. 
     */
    int trailingTrimLength();
    
    /**
     * Has this matcher reached its nirvana?
     */
    boolean matched();
    
    /**
     * Reset matcher state for reuse. 
     */
    void reset();
}
