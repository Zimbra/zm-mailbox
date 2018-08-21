/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
/**
 *
 */
package com.zimbra.cs.account;

import java.util.Arrays;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;

public class SearchDirectoryOptions {
    public static final int ALL_RESULTS = 0;
    public static final String[] ALL_ATTRS = null;
    public static final SearchDirectoryOptions.SortOpt DEFAULT_SORT_OPT = SortOpt.NO_SORT;
    public static final String DEFAULT_SORT_ATTR = null;

    /*
     * pseudo attr name for target name
     * honored only for Alias entries
     */
    public static final String SORT_BY_TARGET_NAME = "targetName";

    /*
     * Option to not set account defaults or secondard defaults.
     * bug 36017, 41533
     *
     * when large number of accounts are returned from Provisioning.searchDirectory,
     * in the extreme case where the accounts span many different domains and the
     * domains are not loaded yet, loading all domains will cause slow response.
     *
     *  Domain is needed for:
     *    - determine the cos if cos is not set on the account
     *    - account secondary default
     */
    public static enum MakeObjectOpt {
        ALL_DEFAULTS,
        NO_DEFAULTS,
        NO_SECONDARY_DEFAULTS
    };

    public static enum SortOpt {
        NO_SORT,
        SORT_ASCENDING,
        SORT_DESCENDING
    };

    public static enum ObjectType {
        accounts(Provisioning.SD_ACCOUNT_FLAG),
        aliases(Provisioning.SD_ALIAS_FLAG),
        distributionlists(Provisioning.SD_DISTRIBUTION_LIST_FLAG),
        dynamicgroups(Provisioning.SD_DYNAMIC_GROUP_FLAG),
        resources(Provisioning.SD_CALENDAR_RESOURCE_FLAG),
        domains(Provisioning.SD_DOMAIN_FLAG),
        coses(Provisioning.SD_COS_FLAG),
        servers(Provisioning.SD_SERVER_FLAG),
        ucservices(Provisioning.SD_UC_SERVICE_FLAG),
        habgroups(Provisioning.SD_HAB_FLAG),;

        private int flag;
        private ObjectType(int flag) {
            this.flag = flag;
        }

        public int getFlag() {
            return flag;
        }

        public static int getAllTypesFlags() {
            int flags = 0;
            for (SearchDirectoryOptions.ObjectType type : ObjectType.values()) {
                flags |= type.getFlag();
            }
            return flags;
        }

        public static int getFlags(Set<SearchDirectoryOptions.ObjectType> types) {
            int flags = 0;
            for (SearchDirectoryOptions.ObjectType type : types) {
                flags |= type.getFlag();
            }
            return flags;
        }

        public static Set<SearchDirectoryOptions.ObjectType> fromCSVString(String str)
        throws ServiceException {
            Set<SearchDirectoryOptions.ObjectType> types = Sets.newHashSet();
            for (String type : Splitter.on(',').trimResults().split(str)) {
                types.add(ObjectType.fromString(type));
            }
            return types;
        }

        public static String toCSVString(Set<SearchDirectoryOptions.ObjectType> types) {
            StringBuilder sb = new StringBuilder();

            boolean first = true;
            for (SearchDirectoryOptions.ObjectType type : types) {
                if (!first) {
                    sb.append(",");
                } else {
                    first = false;
                }
                sb.append(type.name());
            }
            return sb.toString();
        }

        public static SearchDirectoryOptions.ObjectType fromString(String str) throws ServiceException {
            try {
                return ObjectType.valueOf(str);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown type: " + str, e);
            }
        }
    }

    private boolean onMaster = false;
    private final boolean useConnPool = true; // TODO: retire this
    private int maxResults = ALL_RESULTS;

    /*
     * search base
     *
     * if domain is set, search base is under the domain tree
     * Note: this does NOT affect the search filter not the return attrs
     */
    private Domain domain = null;

    /*
     * search filter
     *
     * Either one of filter or filterId+filterStr is set.
     *
     * If filter is set, the filter will used as is, no objectClass will
     * be added to the search filter
     *
     * If filterId+filterStr is set, objectClass filters computed from types
     * will be prepended to the filterStr.
     *
     */
    private ZLdapFilter filter;
    private FilterId filterId;
    private String filterStr;

    /*
     * List of wanted object types. If null, it means no type, which is
     * an error.
     *
     * types is for:
     * - computing enough set of attributes to retrieve in order
     *   construct an object.
     * and
     * - computing objectClass filters to be prepended to filterStr
     *
     */
    private Set<ObjectType> types;

    private String[] returnAttrs = ALL_ATTRS;
    private SearchDirectoryOptions.MakeObjectOpt makeObjOpt = MakeObjectOpt.ALL_DEFAULTS;
    private SearchDirectoryOptions.SortOpt sortOpt = DEFAULT_SORT_OPT;
    private String sortAttr = DEFAULT_SORT_ATTR;
    private boolean convertIDNToAscii;
    private boolean isUseControl = true;
    private boolean isManageDSAit = false;
    
    private String habRootGroupDn;

    public SearchDirectoryOptions() {
    }

    public SearchDirectoryOptions(Domain domain) {
        setDomain(domain);
    }

    public SearchDirectoryOptions(String[] returnAttrs) {
        setReturnAttrs(returnAttrs);
    }

    public SearchDirectoryOptions(Domain domain, String[] returnAttrs) {
        setDomain(domain);
        setReturnAttrs(returnAttrs);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SearchDirectoryOptions)) {
            return false;
        }
        if (o == this) {
            return true;
        }

        SearchDirectoryOptions other = (SearchDirectoryOptions) o;

        if (onMaster != other.getOnMaster()) {
            return false;
        }

        if (useConnPool != other.getUseConnPool()) {
            return false;
        }

        if (maxResults != other.getMaxResults()) {
            return false;
        }

        if (domain != null) {
            Domain otherDomain = other.getDomain();
            if (otherDomain == null) {
                return false;
            }
            if (!domain.getId().equals(otherDomain.getId())) {
                return false;
            }
        } else {
            if (other.getDomain() != null) {
                return false;
            }
        }

        if (filter != null) {
            ZLdapFilter otherFilter = other.getFilter();
            if (otherFilter == null) {
                return false;
            } else {
                if (!filter.toFilterString().equals(otherFilter.toFilterString())) {
                    return false;
                }
            }
        } else {
            if (other.getFilter() != null) {
                return false;
            }
        }

        if (filterId != other.getFilterId()) {
            return false;
        }

        if (filterStr != null) {
            String otherFilterStr = other.getFilterString();
            if (otherFilterStr == null) {
                return false;
            } else {
                if (!filterStr.equals(otherFilterStr)) {
                    return false;
                }
            }
        } else {
            if (other.getFilterString() != null) {
                return false;
            }
        }

        if (getTypesAsFlags() != other.getTypesAsFlags()) {
            return false;
        }

        if (returnAttrs != null) {
            String[] otherReturnAttrs = other.getReturnAttrs();
            if (otherReturnAttrs == null) {
                return false;
            } else {
                Set<String> attrsSet = Sets.newHashSet(Arrays.asList(returnAttrs));
                Set<String> otherAttrsSet = Sets.newHashSet(Arrays.asList(otherReturnAttrs));
                if (!attrsSet.equals(otherAttrsSet)) {
                    return false;
                }
            }
        } else {
            if (other.getReturnAttrs() != null) {
                return false;
            }
        }

        if (makeObjOpt != other.getMakeObjectOpt()) {
            return false;
        }

        if (sortOpt != other.getSortOpt()) {
            return false;
        }

        if (sortAttr != null) {
            String otherSortAttr = other.getSortAttr();
            if (otherSortAttr == null) {
                return false;
            } else {
                if (!sortAttr.equals(otherSortAttr)) {
                    return false;
                }
            }
        } else {
            if (other.getSortAttr() != null) {
                return false;
            }
        }

        if (convertIDNToAscii != other.getConvertIDNToAscii()) {
            return false;
        }

        return true;
    }

    public void setOnMaster(boolean onMaster) {
        this.onMaster = onMaster;
    }

    public boolean getOnMaster() {
        return onMaster;
    }

    public boolean getUseConnPool() {
        return useConnPool;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setFilter(ZLdapFilter filter) {
        this.filter = filter;
    }

    public ZLdapFilter getFilter() {
        return filter;
    }

    public void setFilterString(FilterId filterId, String filterStr) {
        this.filterId = filterId;
        this.filterStr = filterStr;
    }

    public FilterId getFilterId() {
        return filterId;
    }

    public String getFilterString() {
        return filterStr;
    }

    public void setReturnAttrs(String[] returnAttrs) {
        this.returnAttrs = returnAttrs;
    }

    // from soap
    public void setTypes(String typesStr) throws ServiceException {
        types = SearchDirectoryOptions.ObjectType.fromCSVString(typesStr);
    }

    public void setTypes(SearchDirectoryOptions.ObjectType... objTypes) throws ServiceException {
        setTypesInternal(objTypes);
    }

    protected void setTypesInternal(SearchDirectoryOptions.ObjectType... objTypes) {
        types = Sets.newHashSet();
        for (SearchDirectoryOptions.ObjectType type : objTypes) {
            types.add(type);
        }
    }

    public void addType(SearchDirectoryOptions.ObjectType objType) {
        if (types == null) {
            types = Sets.newHashSet();
        }
        types.add(objType);
    }

    public Set<SearchDirectoryOptions.ObjectType> getTypes() {
        return types;
    }

    public int getTypesAsFlags() {
        return getTypesAsFlags(types);
    }

    public static int getTypesAsFlags(Set<ObjectType> types) {
        if (types == null) {
            return 0;
        } else {
            return SearchDirectoryOptions.ObjectType.getFlags(types);
        }
    }

    public String[] getReturnAttrs() {
        return returnAttrs;
    }

    public void setMakeObjectOpt(SearchDirectoryOptions.MakeObjectOpt makeObjOpt) {
        this.makeObjOpt = makeObjOpt;
    }

    public SearchDirectoryOptions.MakeObjectOpt getMakeObjectOpt() {
        return makeObjOpt;
    }

    public void setSortOpt(SearchDirectoryOptions.SortOpt sortOpt) {
        if (sortOpt == null) {
            sortOpt = DEFAULT_SORT_OPT;
        }
        this.sortOpt = sortOpt;
    }

    public SearchDirectoryOptions.SortOpt getSortOpt() {
        return sortOpt;
    }

    public void setSortAttr(String sortAttr) {
        this.sortAttr = sortAttr;
    }

    public String getSortAttr() {
        return sortAttr;
    }

    /*
     * Applicable only when filterStr is used.
     * Ignored if filter is used.
     *
     * filterStr must be already RFC 2254 escaped.
     * RFC 2254 escaping will bot be done during the unicode -> conversion.
     */
    public void setConvertIDNToAscii(boolean convertIDNToAscii) {
        this.convertIDNToAscii = convertIDNToAscii;
    }

    public boolean getConvertIDNToAscii() {
        return convertIDNToAscii;
    }

    public boolean isUseControl() {
        return isUseControl;
    }

    public void setUseControl(boolean isUseControl) {
        this.isUseControl = isUseControl;
    }

    public boolean isManageDSAit() {
        return isManageDSAit;
    }

    public void setManageDSAit(boolean isManageDSAit) {
        this.isManageDSAit = isManageDSAit;
    }

    
    public String getHabRootGroupDn() {
        return habRootGroupDn;
    }

    public void setHabRootGroupDn(String baseDn) {
        this.habRootGroupDn = baseDn;
    }

}