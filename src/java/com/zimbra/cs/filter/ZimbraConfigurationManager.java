package com.zimbra.cs.filter;

import org.apache.jsieve.CommandManager;
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

}
