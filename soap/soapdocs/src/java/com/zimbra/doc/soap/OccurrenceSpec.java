/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
