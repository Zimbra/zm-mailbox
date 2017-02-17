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

    /**
     * Wraps a normal CharSequence, but keeps track of how many accesses to chars in the sequence have
     * been made and throws a {@link RunTimeException} if the maximum limit has been hit.
     *
     * Useful for example to guard against too much computational resource being expended on complex regex
     * expressions - see {@link AccessBoundedRegex}
     */
class AccessBoundedCharSequence implements CharSequence {
    final CharSequence sequence;
    final public int maxAccesses;
    public Integer accessCount;

    /**
     * @param sequence - the base {@link CharSequence} being wrapped by this class
     * @param accessCount - the current number of accesses that have been made to <b>sequence</b>
     * @param maxAccesses - The maximum number of accesses to characters in <b>target</b>
     */
    public AccessBoundedCharSequence(CharSequence sequence, Integer accessCount, int maxAccesses) {
       super();
       this.sequence = sequence;
       this.maxAccesses = maxAccesses;
       this.accessCount = accessCount;
    }

    /**
     * @param sequence - the base {@link CharSequence} being wrapped by this class
     * @param accessCount - the current number of accesses that have been made to <b>sequence</b>
     * @param maxAccesses - The maximum number of accesses to characters in <b>target</b>
     */
    public AccessBoundedCharSequence(CharSequence sequence, int maxAccesses) {
        this(sequence, Integer.valueOf(0), maxAccesses);
    }

    /**
     * @throws RuntimeException if consumed too many computational resources accessing this {@link CharSequence}
     */
    @Override
    public char charAt(int index) {
       if (accessCount >= maxAccesses) {
          throw new TooManyAccessesToUnderlyingCharSequenceException(accessCount);
       }
       accessCount++;
       return sequence.charAt(index);
    }

    @Override
    public int length() {
       return sequence.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
       return new AccessBoundedCharSequence(sequence.subSequence(start, end), accessCount, maxAccesses);
    }

    @Override
    public String toString() {
       return sequence.toString();
    }

    public static class TooManyAccessesToUnderlyingCharSequenceException extends RuntimeException {
        private static final long serialVersionUID = -6976402430873538076L;

        public TooManyAccessesToUnderlyingCharSequenceException(int accessCount) {
            super(String.format(
                  "Thread processing CharSequence called charAt more than maximum allowed number of times (%d)",
                  accessCount));
        }
    }

 }
