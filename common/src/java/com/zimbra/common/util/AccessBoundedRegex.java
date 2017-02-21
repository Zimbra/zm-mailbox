/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;
import java.util.regex.Pattern;

import com.zimbra.common.service.ServiceException;

/**
 * For use with regex expressions where you want to place a limit on the computational resources allowed
 * to execute a match.
 * Assumption: Pattern.matcher uses the supplied CharSequence during its computation (rather than taking a copy)
 */
public class AccessBoundedRegex {
    final Pattern pattern;
    final int maxAccesses; // maximum allowed accesses to target string when resolving a regex match

    /**
     * @param patt
     * @param maxAccesses - The maximum number of accesses to characters in the <b>target</b>
     * when invoking <b>matches</b>
     */
    public AccessBoundedRegex(Pattern patt, int maxAccesses) {
        this.pattern = patt;
        this.maxAccesses = maxAccesses;
    }

    /**
     * @throws TooManyAccessesToMatchTargetException if consumed too many resources attempting to match <b>target</b>
     */
    public boolean matches(CharSequence target)
    throws TooManyAccessesToMatchTargetException {

        AccessBoundedCharSequence boundedTarget = new AccessBoundedCharSequence(target, maxAccesses);
        try {
            boolean matches = pattern.matcher(boundedTarget).matches();
            ZimbraLog.misc.trace("AccessBoundedRegex matches=%s pattern='%s' target='%s' charAt() calls=%s of %s",
                    matches, pattern, target, boundedTarget.accessCount, boundedTarget.maxAccesses);
            return matches;
        } catch (AccessBoundedCharSequence.TooManyAccessesToUnderlyingCharSequenceException ex) {
            throw new TooManyAccessesToMatchTargetException(boundedTarget.maxAccesses, pattern, target);
        }
    }

    /**
     * @param maxAccesses - The maximum number of accesses to characters in <b>target</b>
     * @throws TooManyAccessesToMatchTargetException if consumed too many resources attempting to match <b>target</b>
     */
    public static boolean matches(String target, Pattern patt, int maxAccesses)
    throws TooManyAccessesToMatchTargetException {
        AccessBoundedRegex regex = new AccessBoundedRegex(patt, maxAccesses);
        return regex.matches(target);
    }

    public static class TooManyAccessesToMatchTargetException extends ServiceException {
        private static final long serialVersionUID = -7786253993299128868L;

        public TooManyAccessesToMatchTargetException(int maxAccesses, Pattern pattern, CharSequence target) {
            super(String.format(
                    "regular expression match involved more than %d accesses for pattern '%s' against string '%s'",
                    maxAccesses, pattern, target), ServiceException.INTERRUPTED, RECEIVERS_FAULT);
        }
    }

}
