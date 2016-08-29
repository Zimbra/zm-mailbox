package com.zimbra.cs.ephemeral;

import com.zimbra.common.service.ServiceException;

interface EphemeralBackend {

    /**
     * Opens a connection to the ephemeral backend. Called before each
     * operation.
     *
     * @throws ServiceException
     */
    void open() throws ServiceException;

    /**
     * Closes a connection to the ephemeral backend. Called after each
     * operation.
     *
     * @throws ServiceException
     */
    void close() throws ServiceException;

    /**
     * Get the value for a key. If key does not exist, returns an empty
     * EphemeralResult instance.
     *
     * @param key
     * @param location
     * @return
     * @throws ServiceException
     */
    EphemeralResult get(String key, EphemeralLocation location) throws ServiceException;

    /**
     * Set a value for a key if the key the does not exist, or overwrites
     * otherwise. If the key is multi-valued, all original values are deleted.
     *
     * @param input
     * @param location
     * @throws ServiceException
     */
    void set(EphemeralInput input, EphemeralLocation location) throws ServiceException;

    /**
     * Set a value for a key if the key the does not exist, or add another value
     * otherwise. If this value already exists, the expiration is updated if one
     * is provided.
     *
     * @param input
     * @param location
     * @throws ServiceException
     */
    void update(EphemeralInput input, EphemeralLocation location) throws ServiceException;

    /**
     * Delete the specified key.
     *
     * @param key
     * @param location
     * @throws ServiceException
     */
    void delete(String key, EphemeralLocation location) throws ServiceException;

    /**
     * Delete specified value for a key. If the value does not exist, do
     * nothing. If no values for the key remain, delete the key.
     *
     * @param key
     * @param value
     * @param location
     * @throws ServiceException
     */
    void deleteValue(String key, String value, EphemeralLocation location)
            throws ServiceException;

    /**
     * Delete keys that have passed their expiration. If the backend natively
     * supports key expiry, this may do nothing.
     *
     * @param key
     * @param location
     * @throws ServiceException
     */
    void purgeExpired(String key, EphemeralLocation location) throws ServiceException;

    /**
     * Check whether the specified key exists.
     *
     * @param key
     * @param location
     * @return
     * @throws ServiceException
     */
    boolean hasKey(String key, EphemeralLocation location) throws ServiceException;
}
