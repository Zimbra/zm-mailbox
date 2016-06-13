/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.filter;

import org.apache.jsieve.CommandManager;
import org.apache.jsieve.ComparatorManager;
import org.apache.jsieve.ConfigurationManager;
import org.apache.jsieve.SieveConfigurationException;

/**
 * For Bug 77287
 * Zimbra's own configuration manager which make it possible
 * to use Zimbra's own CommandManager.
 */
public class ZimbraConfigurationManager extends ConfigurationManager {

	/**
	 * Constructor for Zimbra's own ConfigurationManager.
	 * 
	 * @throws SieveConfigurationException
	 */
	public ZimbraConfigurationManager() throws SieveConfigurationException {
		super();
	}

    /**
     * return an instance of Zimbra's own CommandManager, which loads classes
     * with class name registered in commandMap when getting sieve script processed.
     */
	@Override
    public CommandManager getCommandManager() {
        // getCommandMap gives you ConcurrentMap with action name as key and
        // class name of that action as the corresponding value
        return new ZimbraCommandManagerImpl(getCommandMap());
    }

    /**
     * Return an instance of Zimbra specific ComparatorManager.
     */
    @Override
    public ComparatorManager getComparatorManager() {
        return new ZimbraComparatorManagerImpl(getComparatorMap());
    }
}
