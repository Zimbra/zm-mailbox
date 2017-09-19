package com.zimbra.cs.index.history;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem.Type;

/**
 * Provides information about search strings prompted for saving as a search folder
 */
public abstract class SavedSearchPromptLog {

    /**
     * Returns the status of the provided search string:
     * NOT_PROMPTED - the user has not searched for the query enough times to warrant a prompt
     * PROMPTED - the user has been prompted to create a saved search folder, but has not responded yet
     * CREATED - a saved search folder exists for this search string
     * REJECTED - the user rejected a prompt to create a saved search folder for this string
     */
    public abstract SavedSearchStatus getSavedSearchStatus(String searchString) throws ServiceException;

    /**
     * Set the status of the given search string
     */
    public abstract void setPromptStatus(String searchString, SavedSearchStatus status) throws ServiceException;

    public static enum SavedSearchStatus {
        NOT_PROMPTED((short) 0),
        PROMPTED((short) 1),
        CREATED((short) 2),
        REJECTED((short) 3);

        private short id;
        private static final Map<Short, SavedSearchStatus> MAP;
        static {
            ImmutableMap.Builder<Short, SavedSearchStatus> builder = ImmutableMap.builder();
            for (SavedSearchStatus status : SavedSearchStatus.values()) {
                builder.put(status.id, status);
            }
            MAP = builder.build();
        }
        private SavedSearchStatus(short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static SavedSearchStatus of(short id) throws ServiceException {
            SavedSearchStatus status = MAP.get(id);
            if (status == null) {
                throw ServiceException.FAILURE(String.format("Invalid SavedSearchStatus identifier: %d", id), null);
            } else {
                return status;
            }
        }
     }
}