/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.DefaultRedoLogProvider;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.file.FileBlobStore;

@Configuration
@EnableAspectJAutoProxy
public class ZimbraConfig {

    @Bean(name="mailboxManager")
	public MailboxManager mailboxManagerBean() throws ServiceException {
		MailboxManager instance = null;
        String className = LC.zimbra_class_mboxmanager.value();
        if (className != null && !className.equals("")) {
            try {
                try {
                    instance = (MailboxManager) Class.forName(className).newInstance();
                } catch (ClassNotFoundException cnfe) {
                    // ignore and look in extensions
                    instance = (MailboxManager) ExtensionUtil.findClass(className).newInstance();
                }
            } catch (Exception e) {
                ZimbraLog.account.error("could not instantiate MailboxManager interface of class '" + className + "'; defaulting to MailboxManager", e);
            }
        }
        if (instance == null) {
            instance = new MailboxManager();
        }
		return instance;
	}

	@Bean(name="redologProvider")
    public RedoLogProvider redoLogProviderBean() throws Exception {
        RedoLogProvider instance = null;
        Class klass = null;
        Server config = Provisioning.getInstance().getLocalServer();
        String className = config.getAttr(Provisioning.A_zimbraRedoLogProvider);
        if (className != null) {
            klass = Class.forName(className);
        } else {
            klass = DefaultRedoLogProvider.class;
            ZimbraLog.misc.debug("Redolog provider name not specified.  Using default " +
                                 klass.getName());
        }
        instance = (RedoLogProvider) klass.newInstance();
        return instance;
    }

	@Bean(name="storeManager")
    public StoreManager storeManagerBean() throws Exception {
		StoreManager instance = null;
        String className = LC.zimbra_class_store.value();
        if (className != null && !className.equals("")) {
            try {
                instance = (StoreManager) Class.forName(className).newInstance();
            } catch (ClassNotFoundException e) {
                instance = (StoreManager) ExtensionUtil.findClass(className).newInstance();
            }
        }
        if (instance == null) {
            instance = new FileBlobStore();
        }
        return instance;
    }

	@Bean(name="zimbraApplication")
	public ZimbraApplication zimbraApplicationBean() throws Exception {
	    ZimbraApplication instance = null;
        String className = LC.zimbra_class_application.value();
        if (className != null && !className.equals("")) {
            try {
                instance = (ZimbraApplication)Class.forName(className).newInstance();
            } catch (Exception e) {
                ZimbraLog.misc.error(
                    "could not instantiate ZimbraApplication class '"
                        + className + "'; defaulting to ZimbraApplication", e);
            }
        }
        if (instance == null) {
            instance = new ZimbraApplication();
        }
        return instance;
	}
}
