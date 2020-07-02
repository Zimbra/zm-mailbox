package com.zimbra.cs.contacts;

import com.zimbra.common.service.ServiceException;

/**
 * Interface to calculate the contact affinity. This should not be called for each request, as it
 * may involve intensive computation.
 */
public interface ComputationBackend {

    public RelatedContactsResults calculateContactAffinity(RelatedContactsParams query) throws ServiceException;
}
