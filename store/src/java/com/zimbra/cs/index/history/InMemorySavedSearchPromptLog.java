package com.zimbra.cs.index.history;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;

public class InMemorySavedSearchPromptLog extends SavedSearchPromptLog {

    private Map<String, SavedSearchStatus> map;

    public InMemorySavedSearchPromptLog() {
        map = new HashMap<String, SavedSearchStatus>();
    }

    @Override
    protected SavedSearchStatus getSavedSearchStatus(String searchString)
            throws ServiceException {
        SavedSearchStatus status = map.get(searchString);
        return status == null ? SavedSearchStatus.NOT_PROMPTED : status;
    }

    @Override
    public void setPromptStatus(String searchString,
            SavedSearchStatus status) throws ServiceException {
        map.put(searchString, status);
    }
}
