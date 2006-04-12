/**
 * Copyright (C) 2004  Free Software Foundation, Inc.
 *
  * Author: Oliver Hitz
 *
 * This file is part of GNU Libidn.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 */

package org.jivesoftware.stringprep;

public class Stringprep {

    /**
     * Additional characters prohibited by nodeprep that aren't defined as part of the
     * RFC3454 tables.
     */
    private static final char [] NODEPREP_PROHIBIT = new char [] { '\u0022', '\u0026', '\'',
            '\u002F', '\u003A', '\u003C', '\u003E', '\u003E', '\u0040'};

    public static String nameprep(String input, boolean allowUnassigned)
            throws StringprepException
    {
        if (input == null) {
            return null;
        }
        StringBuilder s = new StringBuilder(input);

        if (allowUnassigned) {
            if (contains(s, RFC3454.A1)) {
                throw new StringprepException(StringprepException.CONTAINS_UNASSIGNED);
            }
        }

        filter(s, RFC3454.B1);
        map(s, RFC3454.B2search, RFC3454.B2replace);

        s = new StringBuilder(NFKC.normalizeNFKC(s.toString()));
        // B.3 is only needed if NFKC is not used, right?
        // map(s, RFC3454.B3search, RFC3454.B3replace);

        if (contains(s, RFC3454.C1_2) ||
                contains(s, RFC3454.C2_2) ||
                contains(s, RFC3454.C3) ||
                contains(s, RFC3454.C4) ||
                contains(s, RFC3454.C5) ||
                contains(s, RFC3454.C6) ||
                contains(s, RFC3454.C7) ||
                contains(s, RFC3454.C8)) {
            // Table C.9 only contains code points > 0xFFFF which Java
            // doesn't handle
            throw new StringprepException(StringprepException.CONTAINS_PROHIBITED);
        }

        // Bidi handling
        boolean r = contains(s, RFC3454.D1);
        boolean l = contains(s, RFC3454.D2);

        // RFC 3454, section 6, requirement 1: already handled above (table C.8)

        // RFC 3454, section 6, requirement 2
        if (r && l) {
            throw new StringprepException(StringprepException.BIDI_BOTHRAL);
        }

        // RFC 3454, section 6, requirement 3
        if (r) {
            if (!contains(s.charAt(0), RFC3454.D1) ||
                    !contains(s.charAt(s.length() - 1), RFC3454.D1)) {
                throw new StringprepException(StringprepException.BIDI_LTRAL);
            }
        }

        return s.toString();
    }

    public static String nodeprep(String input) throws StringprepException {
        if (input == null) {
            return null;
        }
        StringBuilder s = new StringBuilder(input);

        if (contains(s, RFC3454.A1)) {
            throw new StringprepException(StringprepException.CONTAINS_UNASSIGNED);
        }

        filter(s, RFC3454.B1);
        map(s, RFC3454.B2search, RFC3454.B2replace);

        s = new StringBuilder(NFKC.normalizeNFKC(s.toString()));

        if (contains(s, RFC3454.C1_1) ||
            contains(s, RFC3454.C1_2) ||
            contains(s, RFC3454.C2_1) ||
            contains(s, RFC3454.C2_2) ||
            contains(s, RFC3454.C3) ||
            contains(s, RFC3454.C4) ||
            contains(s, RFC3454.C5) ||
            contains(s, RFC3454.C6) ||
            contains(s, RFC3454.C7) ||
            contains(s, RFC3454.C8) ||
            contains(s, NODEPREP_PROHIBIT))
        {
            // Table C.9 only contains code points > 0xFFFF which Java
            // doesn't handle
            throw new StringprepException(StringprepException.CONTAINS_PROHIBITED);
        }

        // Bidi handling
        boolean r = contains(s, RFC3454.D1);
        boolean l = contains(s, RFC3454.D2);

        // RFC 3454, section 6, requirement 1: already handled above (table C.8)

        // RFC 3454, section 6, requirement 2
        if (r && l) {
            throw new StringprepException(StringprepException.BIDI_BOTHRAL);
        }

        // RFC 3454, section 6, requirement 3
        if (r) {
            if (!contains(s.charAt(0), RFC3454.D1) ||
                    !contains(s.charAt(s.length() - 1), RFC3454.D1)) {
                throw new StringprepException(StringprepException.BIDI_LTRAL);
            }
        }

        return s.toString();
    }

    public static String resourceprep(String input) throws StringprepException {
        if (input == null) {
            return null;
        }
        StringBuilder s = new StringBuilder(input);

        if (contains(s, RFC3454.A1)) {
            throw new StringprepException(StringprepException.CONTAINS_UNASSIGNED);
        }

        filter(s, RFC3454.B1);

        s = new StringBuilder(NFKC.normalizeNFKC(s.toString()));

        if (contains(s, RFC3454.C1_2) &&
            contains(s, RFC3454.C2_1) &&
            contains(s, RFC3454.C2_2) &&
            contains(s, RFC3454.C3) &&
            contains(s, RFC3454.C4) &&
            contains(s, RFC3454.C5) &&
            contains(s, RFC3454.C6) &&
            contains(s, RFC3454.C7) &&
            contains(s, RFC3454.C8))
        {
            // Table C.9 only contains code points > 0xFFFF which Java
            // doesn't handle
            throw new StringprepException(StringprepException.CONTAINS_PROHIBITED);
        }

        // Bidi handling
        boolean r = contains(s, RFC3454.D1);
        boolean l = contains(s, RFC3454.D2);

        // RFC 3454, section 6, requirement 1: already handled above (table C.8)

        // RFC 3454, section 6, requirement 2
        if (r && l) {
            throw new StringprepException(StringprepException.BIDI_BOTHRAL);
        }

        // RFC 3454, section 6, requirement 3
        if (r) {
            if (!contains(s.charAt(0), RFC3454.D1) ||
                    !contains(s.charAt(s.length() - 1), RFC3454.D1)) {
                throw new StringprepException(StringprepException.BIDI_LTRAL);
            }
        }

        return s.toString();
    }

    private static boolean contains(StringBuilder s, char[] p) {
        for (int i = 0; i < p.length; i++) {
            char c = p[i];
            for (int j = 0; j < s.length(); j++) {
                if (c == s.charAt(j)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean contains(StringBuilder s, char[][] p) {
        for (int i = 0; i < p.length; i++) {
            char[] r = p[i];
            if (1 == r.length) {
                char c = r[0];
                for (int j = 0; j < s.length(); j++) {
                    if (c == s.charAt(j)) {
                        return true;
                    }
                }
            } else if (2 == r.length) {
                char f = r[0];
                char t = r[1];
                for (int j = 0; j < s.length(); j++) {
                    if (f <= s.charAt(j) && t >= s.charAt(j)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean contains(char c, char[][] p) {
        for (int i = 0; i < p.length; i++) {
            char[] r = p[i];
            if (1 == r.length) {
                if (c == r[0]) {
                    return true;
                }
            } else if (2 == r.length) {
                char f = r[0];
                char t = r[1];
                if (f <= c && t >= c) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void filter(StringBuilder s, char[] f) {
        for (int i = 0; i < f.length; i++) {
            char c = f[i];

            int j = 0;
            while (j < s.length()) {
                if (c == s.charAt(j)) {
                    s.deleteCharAt(j);
                } else {
                    j++;
                }
            }
        }
    }

    private static void filter(StringBuilder s, char[][] f) {
        for (int i = 0; i < f.length; i++) {
            char[] r = f[i];

            if (1 == r.length) {
                char c = r[0];

                int j = 0;
                while (j < s.length()) {
                    if (c == s.charAt(j)) {
                        s.deleteCharAt(j);
                    } else {
                        j++;
                    }
                }
            } else if (2 == r.length) {
                char from = r[0];
                char to = r[1];

                int j = 0;
                while (j < s.length()) {
                    if (from <= s.charAt(j) && to >= s.charAt(j)) {
                        s.deleteCharAt(j);
                    } else {
                        j++;
                    }
                }
            }
        }
    }

    private static void map(StringBuilder s, char[] search, String[] replace) {
        for (int i = 0; i < search.length; i++) {
            char c = search[i];

            int j = 0;
            while (j < s.length()) {
                if (c == s.charAt(j)) {
                    s.deleteCharAt(j);
                    if (null != replace[i]) {
                        s.insert(j, replace[i]);
                        j += replace[i].length() - 1;
                    }
                } else {
                    j++;
                }
            }
        }
    }
}