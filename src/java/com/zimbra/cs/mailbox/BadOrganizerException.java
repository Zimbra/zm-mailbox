/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;

public class BadOrganizerException extends ServiceException {
    private static final long serialVersionUID = 5950670701876236413L;

    private BadOrganizerException(String message, Throwable cause) {
        super(message, INVALID_REQUEST, SENDERS_FAULT, cause);
    }

    /**
     * CalDAV has specific errors for this situation
     */
    public static class DiffOrganizerInComponentsException extends BadOrganizerException {
        private static final long serialVersionUID = -1061008479818171529L;

        public DiffOrganizerInComponentsException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * @param compDescription - either {@code "an appointment"} or {@code "a task"}
     */
    public static BadOrganizerException ADD_ORGANIZER_NOT_ALLOWED(String newOrganizer,
            String compDescription) {
        return new BadOrganizerException(String.format(
                "Adding ORGANIZER '%s' for %s is not allowed", newOrganizer, compDescription), null);
    }

    /**
     * @param compDescription - either {@code "an appointment"} or {@code "a task"}
     */
    public static BadOrganizerException DEL_ORGANIZER_NOT_ALLOWED(String origOrganizer,
            String compDescription) {
        return new BadOrganizerException(String.format(
                "Removing ORGANIZER '%s' for %s is not allowed", origOrganizer, compDescription), null);
    }

    /**
     * @param compDescription - either {@code "an appointment"} or {@code "a task"}
     */
    public static BadOrganizerException CHANGE_ORGANIZER_NOT_ALLOWED(String oldOrganizer, String newOrganizer,
            String compDescription) {
        return new BadOrganizerException(String.format(
                "Changing ORGANIZER of %s is not allowed: old='%s' new='%s'",
                compDescription, oldOrganizer, newOrganizer), null);
    }

    /**
     * @param compDescription - either {@code "an appointment"} or {@code "a task"}
     */
    public static BadOrganizerException ORGANIZER_INTRODUCED_FOR_EXCEPTION(String newOrganizer, String compDescription) {
        return new DiffOrganizerInComponentsException(String.format(
                "For %s, instance with ORGANIZER '%s' is not allowed.  Main rule has no ORGANIZER",
                compDescription, newOrganizer), null);
    }

    /**
     * @param compDescription - either {@code "an appointment"} or {@code "a task"}
     */
    public static BadOrganizerException DIFF_ORGANIZER_IN_COMPONENTS(String oldOrganizer, String newOrganizer,
            String compDescription) {
        return new DiffOrganizerInComponentsException(String.format(
                "For %s, instance ORGANIZER '%s' should be same as main rule ORGANIZER '%s'",
                compDescription, newOrganizer, oldOrganizer), null);
    }

    /**
     * @param compDescription - either {@code "an appointment"} or {@code "a task"}
     */
    public static BadOrganizerException MISSING_ORGANIZER_IN_SINGLE_INSTANCE(String oldOrganizer,
            String compDescription) {
        return new DiffOrganizerInComponentsException(String.format(
                "For %s, instance is missing an ORGANIZER.  Must be same as main rule ORGANIZER '%s'",
                compDescription, oldOrganizer), null);
    }
}
