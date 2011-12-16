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

package com.zimbra.doc.soap;

public enum OccurrenceSpec {
    REQUIRED, // one and only one 1:1 = ""
    OPTIONAL, // zero or one 0:1 = "?"
    REQUIRED_MORE, // one or more 1:* = "+"
    OPTIONAL_MORE;  // zero or more 0:* = "*"

    private static final String OCCURRENCE_REQUIRED_STR = ""; // one and only one 1:1 = ""
    private static final String OCCURRENCE_OPTIONAL_STR = "?"; // zero or one 0:1 = "?"
    private static final String OCCURRENCE_REQUIRED_MORE_STR = "+"; // one or more 1:* = "+"
    private static final String OCCURRENCE_OPTIONAL_MORE_STR = "*"; // zero or more 0:* = "*"

    public String getOccurrenceAsString() {
        switch(this) {
            case OPTIONAL: {
                return OCCURRENCE_OPTIONAL_STR;
            }
            case REQUIRED_MORE: {
                return OCCURRENCE_REQUIRED_MORE_STR;
            }
            case OPTIONAL_MORE: {
                return OCCURRENCE_OPTIONAL_MORE_STR;
            }
        }
        return OCCURRENCE_REQUIRED_STR;
    }
}
