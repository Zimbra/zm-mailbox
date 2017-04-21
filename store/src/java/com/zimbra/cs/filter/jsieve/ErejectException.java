/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.StopException;

/**
 * Class <code>ErejectException</code> indicates that message delivery should be
 * terminated due to a Ereject Command.
 */

public class ErejectException extends SieveException {
    /**
     * Constructor for ErejectException
     */
    public ErejectException() {
        super();
    }

    /**
     * Constructor for ErejectException
     * @param message
     * @param cause
     */
    public ErejectException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for ErejectException
     * @param message
     */
    public ErejectException(String message) {
        super(message);
    }

    /**
     * Constructor for ErejectException
     * @param cause
     */
    public ErejectException(Throwable cause) {
        super(cause);
    }
}
