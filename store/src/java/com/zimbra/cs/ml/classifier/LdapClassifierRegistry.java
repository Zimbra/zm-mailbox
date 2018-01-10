package com.zimbra.cs.ml.classifier;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

/**
 * Stores classifier info in a multivalued LDAP attribute
 */
class LdapClassifierRegistry extends ClassifierRegistry {

    Config config;

    public LdapClassifierRegistry() throws ServiceException {
        config = Provisioning.getInstance().getConfig();
    }

    @Override
    protected String[] load() throws ServiceException {
        return config.getClassifierInfo();
    }

    @Override
    protected void save(String encodedClassifier)
            throws ServiceException {
        config.addClassifierInfo(encodedClassifier);
    }

    @Override
    protected void deRegister(String encodedClassifier) throws ServiceException {
        config.removeClassifierInfo(encodedClassifier);
    }
}