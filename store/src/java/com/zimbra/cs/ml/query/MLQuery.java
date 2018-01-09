package com.zimbra.cs.ml.query;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ml.MachineLearningBackend;

/**
 * Base class for queries to the machine learning server
 */
public abstract class MLQuery<T> {

    public T execute() throws ServiceException {
        ZimbraLog.ml.debug("executing %s", this.getClass().getSimpleName());
        return MachineLearningBackend.getFactory().getMachineLearningBackend().executeQuery(this);
    }

}

