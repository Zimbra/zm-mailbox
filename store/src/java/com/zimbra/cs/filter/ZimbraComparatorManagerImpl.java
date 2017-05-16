/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
import static org.apache.jsieve.comparators.ComparatorNames.ASCII_CASEMAP_COMPARATOR;
import static org.apache.jsieve.comparators.ComparatorNames.OCTET_COMPARATOR;
import static com.zimbra.cs.filter.jsieve.ComparatorName.ASCII_NUMERIC_COMPARATOR;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.jsieve.ComparatorManager;
import org.apache.jsieve.comparators.Comparator;
import org.apache.jsieve.exception.LookupException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

public class ZimbraComparatorManagerImpl implements ComparatorManager {

    /**
     * Constructs a set containing the names of those comparisons for which <code>require</code>
     * is not necessary before usage, according to RFC5228.
     * See <a href='http://tools.ietf.org/html/rfc5228#section-2.7.3'>RFC5228, 2.7.3 Comparators</a>.
     */
    public static CopyOnWriteArraySet<String> standardDefinedComparators() {
        final CopyOnWriteArraySet<String> results = new CopyOnWriteArraySet<String>();
        results.add(OCTET_COMPARATOR);
        results.add(ASCII_CASEMAP_COMPARATOR);
        results.add(ASCII_NUMERIC_COMPARATOR);
        return results;
    }

    private final ConcurrentMap<String, String> classNameMap;
    /**
     * The names of those comparisons for which <code>require</code> is not necessary before usage.
     * See <a href='http://tools.ietf.org/html/rfc5228#section-2.7.3'>RFC5228, 2.7.3 Comparators</a>.
     */
    private final CopyOnWriteArraySet<String> implicitlyDeclared;

    /**
     * Constructs a manager with the standard comparators implicitly defined.
     * @param classNameMap not null
     */
    public ZimbraComparatorManagerImpl(final ConcurrentMap<String, String> classNameMap) {
        this(classNameMap, standardDefinedComparators());
    }

    /**
     * Constructor for ComparatorManager.
     * @param classNameMap indexes names of implementation classes against logical names, not null
     * @param implicitlyDeclared names of those comparisons for which <code>require</code> is not necessary before usage
     */
    public ZimbraComparatorManagerImpl(final ConcurrentMap<String, String> classNameMap, final CopyOnWriteArraySet<String> implicitlyDeclared) {
        super();
        this.classNameMap = classNameMap;
        this.implicitlyDeclared = implicitlyDeclared;

        String className = null;
        try {
            className = LC.zimbra_class_jsieve_comparators_ascii_casemap.value();
            if (className != null && !className.equals("")) {
                this.classNameMap.put(ASCII_CASEMAP_COMPARATOR, className);
            }
            ZimbraLog.filter.info("[%s] = [%s]",
                ASCII_CASEMAP_COMPARATOR, getClassName(ASCII_CASEMAP_COMPARATOR));

            className = LC.zimbra_class_jsieve_comparators_ascii_numeric.value();
            if (className != null && !className.equals("")) {
                this.classNameMap.put(ASCII_NUMERIC_COMPARATOR, className);
            }
            ZimbraLog.filter.info("[%s] = [%s]",
                ASCII_NUMERIC_COMPARATOR, getClassName(ASCII_NUMERIC_COMPARATOR));

            className = LC.zimbra_class_jsieve_comparators_octet.value();
            if (className != null && !className.equals("")) {
                this.classNameMap.put(OCTET_COMPARATOR, className);
            }
            ZimbraLog.filter.info("[%s] = [%s]",
                ASCII_NUMERIC_COMPARATOR, getClassName(ASCII_NUMERIC_COMPARATOR));
        } catch (Exception e) {
            ZimbraLog.webclient.warn("exception classname: [%s] not found", className);
        }
    }

    /**
     * Is an explicit declaration in a <code>require</code> statement
     * unnecessary for this comparator?
     * @param comparatorName not null
     * @return true when this comparator need not be declared by <core>require</code>,
     * false when any usage of this comparator must be declared in a <code>require</code> statement
     */
    @Override
    public boolean isImplicitlyDeclared(final String comparatorName) {
        return implicitlyDeclared.contains(comparatorName);
    }

    /**
     * <p>
     * Method lookup answers the class to which a Comparator name is mapped.
     * </p>
     *
     * @param name -
     *            The name of the Comparator
     * @return Class - The class of the Comparator
     * @throws LookupException
     */
    public Class lookup(String name) throws LookupException {
        Class comparatorClass = null;
        try {
            comparatorClass = getClass().getClassLoader().loadClass(
                    getClassName(name));
        } catch (ClassNotFoundException e) {
            throw new LookupException("Comparator named '" + name
                    + "' not found.");
        }
        if (!Comparator.class.isAssignableFrom(comparatorClass))
            throw new LookupException("Class " + comparatorClass.getName()
                    + " must implement " + Comparator.class.getName());
        return comparatorClass;
    }

    /**
     * <p>
     * Method newInstance answers an instance of the class to which a Comparator
     * name is mapped.
     * </p>
     *
     * @param name -
     *            The name of the Comparator
     * @return Class - The class of the Comparator
     * @throws LookupException
     */
    @Override
    public ZimbraComparator getComparator(String name) throws LookupException {
        try {
            return (ZimbraComparator) lookup(name).newInstance();
        } catch (InstantiationException e) {
            throw new LookupException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new LookupException(e.getMessage());
        }
    }

    /**
     * <p>
     * Method getClassName answers the name of the class to which a Comparator
     * name is mapped.
     * </p>
     *
     * @param name -
     *            The name of the Comparator
     * @return String - The name of the class
     * @throws LookupException
     */
    private String getClassName(String name) throws LookupException {
        String className = classNameMap.get(name.toLowerCase());
        if (null == className)
            throw new LookupException("Comparator named '" + name
                    + "' not mapped.");
        return className;
    }

    /**
     * @see ComparatorManager#isSupported(String)
     */
    @Override
    public boolean isSupported(String name) {
        try {
            getComparator(name);
            return true;
        } catch (LookupException e) {
            return false;
        }
    }

    /**
     * @see org.apache.jsieve.ComparatorManager#getExtensions()
     */
    @Override
    public List<String> getExtensions() {
        List<String> extensions = new ArrayList<String>(classNameMap.size());
        for (String key : classNameMap.keySet())
        {
            if (!isImplicitlyDeclared(key))
            {
                extensions.add(key);
            }
        }
        return extensions;
    }
}
