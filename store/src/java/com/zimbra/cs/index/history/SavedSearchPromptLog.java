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
     * Returns a boolean representing whether a saved search folder exists for this query
     */
    public boolean savedSearchExists(String searchString) throws ServiceException {
        return getSavedSearchStatus(searchString) == SavedSearchStatus.CREATED;
    }

    /**
     * Returns a boolean representing whether the user has rejected creating a saved search
     * folder for this query in the past
     */
    public boolean promptRejected(String searchString) throws ServiceException {
        return getSavedSearchStatus(searchString) == SavedSearchStatus.REJECTED;
    }

    /**
     * Returns a boolean representing whether the user has been prompted to create a saved
     * search folder for this search query, but has not responded yet
     */
    public boolean prompted(String searchString) throws ServiceException {
        return getSavedSearchStatus(searchString) == SavedSearchStatus.PROMPTED;
    }

    /**
     * Returns a boolean representing whether the user has never been prompted to create a saved
     * search folder for this search query.
     */
    public boolean notPrompted(String searchString) throws ServiceException {
        return getSavedSearchStatus(searchString) == SavedSearchStatus.NOT_PROMPTED;
    }

    /**
     * Set the status of the search string to PROMPTED, meaning that the user
     * has been asked to create a saved search folder but has not responded yet
     */
    public void setPrompted(String searchString) throws ServiceException {
        setPromptStatus(searchString, SavedSearchStatus.PROMPTED);
    }

    /**
     * Set the status of the search string to CREATED, meaning that a saved search
     * folder has been created for this search
     */
    public void setCreated(String searchString) throws ServiceException {
        setPromptStatus(searchString, SavedSearchStatus.CREATED);
    }

    /**
     * Set the status of the search folder as REJECTED, meaning that the user
     * has opted to not create a save search folder for this query
     */
    public void setRejected(String searchString) throws ServiceException {
        setPromptStatus(searchString, SavedSearchStatus.REJECTED);
    }

    /**
     * Returns the status of the provided search string
     */
    protected abstract SavedSearchStatus getSavedSearchStatus(String searchString) throws ServiceException;

    /**
     * Set the status of the given search string
     */
    protected abstract void setPromptStatus(String searchString, SavedSearchStatus status) throws ServiceException;

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