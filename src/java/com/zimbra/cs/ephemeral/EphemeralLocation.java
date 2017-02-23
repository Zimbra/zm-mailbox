package com.zimbra.cs.ephemeral;


/**
 * This abstract class represents a hierarchical specification of the
 * location of ephemeral attributes.
 *
 * The only method, getLocation(), returns an array of Strings representing
 * a hierarchy under which the key/value pair is stored. It is up to
 * the EphemeralStore implementation to decide how to use this,
 * be it accessing the appropriate database, constructing a key hierarchy,
 * or something else.
 *
 * @author iraykin
 *
 */
public abstract class EphemeralLocation {
    public abstract String[] getLocation();
}
