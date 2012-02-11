/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav;

import org.dom4j.Namespace;
import org.dom4j.QName;

public class DavElements {
	public static final String WEBDAV_NS_STRING = "DAV:";
	public static final String CALDAV_NS_STRING = "urn:ietf:params:xml:ns:caldav";
    public static final String CARDDAV_NS_STRING = "urn:ietf:params:xml:ns:carddav";
	public static final String XML_NS_STRING = "xml:";
	public static final String APPLE_NS_STRING = "http://apple.com/ns/ical/";
	public static final String CS_NS_STRING = "http://calendarserver.org/ns/";
	public static final String YAHOO_NS_STRING = "http://yahoo.com/ns/";
	
	public static final Namespace WEBDAV_NS = Namespace.get("D", WEBDAV_NS_STRING);
	public static final Namespace CALDAV_NS = Namespace.get("C", CALDAV_NS_STRING);
    public static final Namespace CARDDAV_NS = Namespace.get("C", CARDDAV_NS_STRING);
	public static final Namespace XML_NS = Namespace.get("X", XML_NS_STRING);
	public static final Namespace APPLE_NS = Namespace.get("A", APPLE_NS_STRING);
	public static final Namespace CS_NS = Namespace.get("CS", CS_NS_STRING);
	public static final Namespace YAHOO_NS = Namespace.get("Y", YAHOO_NS_STRING);

	// general
	public static final String LANG_EN_US = "en-us";
	public static final String P_LANG = "xml:lang";
	//public static final String P_LANG = "lang";
	public static final QName  E_LANG = QName.get(P_LANG);
	//public static final QName  E_LANG = QName.get(P_LANG, XML_NS);

	public static final String ASCII = "i;ascii-casemap";  // case insensitive
	public static final String OCTET = "i;octet";
	public static final String YES = "yes";
	public static final String NO  = "no";
	
	// properties strings in alphabetical order
	public static final String P_ABSTRACT = "abstract";
	public static final String P_ACE = "ace";
	public static final String P_ACL = "acl";
	public static final String P_ACL_PRINCIPAL_PROP_SET = "acl-principal-prop-set";
	public static final String P_ACL_RESTRICTIONS = "acl-restrictions";
	public static final String P_ACTIVELOCK = "activelock";
    public static final String P_ADDRESS_DATA = "address-data";
    public static final String P_ADDRESSBOOK = "addressbook";
    public static final String P_ADDRESSBOOK_DESCRIPTION = "addressbook-description";
    public static final String P_ADDRESSBOOK_HOME_SET = "addressbook-home-set";
    public static final String P_ADDRESSBOOK_MULTIGET = "addressbook-multiget";
    public static final String P_ADDRESSBOOK_QUERY = "addressbook-query";
	public static final String P_ALL = "all";
	public static final String P_ALLCOMP = "allcomp";
	public static final String P_ALLPROP = "allprop";
	public static final String P_ALTERNATE_URI_SET = "alternate-URI-set";
	public static final String P_APPLY_TO_PRINCIPAL_COLLECTION_SET = "apply-to-principal-collection-set";
	public static final String P_AUTHENTICATED = "authenticated";
	
	public static final String P_BIND = "bind";

	public static final String P_CALENDAR = "calendar";
	public static final String P_CALENDAR_COLOR = "calendar-color";
	public static final String P_CALENDAR_DATA = "calendar-data";
	public static final String P_CALENDAR_DESCRIPTION = "calendar-description";
	public static final String P_CALENDAR_FREE_BUSY_SET = "calendar-free-busy-set";
	public static final String P_CALENDAR_HOME_SET = "calendar-home-set";
	public static final String P_CALENDAR_MULTIGET = "calendar-multiget";
	public static final String P_CALENDAR_PROXY_READ = "calendar-proxy-read";
	public static final String P_CALENDAR_PROXY_READ_FOR = "calendar-proxy-read-for";
	public static final String P_CALENDAR_PROXY_WRITE = "calendar-proxy-write";
	public static final String P_CALENDAR_PROXY_WRITE_FOR = "calendar-proxy-write-for";
	public static final String P_CALENDAR_QUERY = "calendar-query";
	public static final String P_CALENDAR_TIMEZONE = "calendar-timezone";
	public static final String P_CALENDAR_USER_ADDRESS_SET = "calendar-user-address-set";
	public static final String P_CALENDAR_USER_TYPE = "calendar-user-type";
	public static final String P_COLLATION = "collation";
	public static final String P_COLLECTION = "collection";
	public static final String P_COMP = "comp";
	public static final String P_COMP_FILTER = "comp-filter";
	public static final String P_CONTENT_TYPE= "content-type";
	public static final String P_CREATIONDATE = "creationdate";
	public static final String P_CURRENT_USER_PRINCIPAL = "current-user-principal";
	public static final String P_CURRENT_USER_PRIVILEGE_SET = "current-user-privilege-set";
	
	public static final String P_DEFAULT_CALENDAR = "default-calendar";
	public static final String P_DENY = "deny";
	public static final String P_DENY_BEFORE_GRANT = "deny-before-grant";
	public static final String P_DEPTH = "depth";
	public static final String P_DESCRIPTION = "description";
	public static final String P_DISPLAYNAME = "displayname";
	
	public static final String P_EMAIL_ADDRESS_SET = "email-address-set";
	public static final String P_END = "end";
	public static final String P_ERROR = "error";
	public static final String P_EXCLUSIVE = "exclusive";
	public static final String P_EXPAND = "expand";
	public static final String P_EXPAND_PROPERTY = "expand-property";
	
	public static final String P_FILTER = "filter";
	public static final String P_FIRST_NAME = "first-name";
	public static final String P_FREE_BUSY_QUERY = "free-busy-query";
	
	public static final String P_GETCONTENTLANGUAGE = "getcontentlanguage";
	public static final String P_GETCONTENTLENGTH = "getcontentlength";
	public static final String P_GETCONTENTTYPE = "getcontenttype";
	public static final String P_GETCREATIONDATE = "getcreationdate";
	public static final String P_GETCTAG = "getctag";
	public static final String P_GETETAG = "getetag";
	public static final String P_GETLASTMODIFIED = "getlastmodified";
	public static final String P_GRANT = "grant";
	public static final String P_GRANT_ONLY = "grant-only";
	public static final String P_GROUP = "group";
	public static final String P_GROUP_MEMBER_SET = "group-member-set";
	public static final String P_GROUP_MEMBERSHIP = "group-membership";
	
	public static final String P_HREF = "href";
	
	public static final String P_INVERT = "invert";
	public static final String P_INHERITED = "inherited";
	public static final String P_INHERITED_ACL_SET = "inherited-acl-set";
	public static final String P_IS_NOT_DEFINED = "is-not-defined";
	
	public static final String P_LAST_NAME = "last-name";
	public static final String P_LIMIT_FREEBUSY_SET = "limit-freebusy-set";
	public static final String P_LIMIT_RECURRENCE_SET = "limit-recurrence-set";
	public static final String P_LOCKINFO = "lockinfo";
	public static final String P_LOCKDISCOVERY = "lockdiscovery";
	public static final String P_LOCKSCOPE = "lockscope";
	public static final String P_LOCKTOKEN = "locktoken";
	public static final String P_LOCKTYPE = "locktype";

	public static final String P_MATCH = "match";
    public static final String P_MATCH_TYPE = "match-type";
	public static final String P_MAX_ATTENDEES_PER_INSTANCE = "max-attendees-per-instance";
	public static final String P_MAX_DATE_TIME = "max-date-time";
	public static final String P_MAX_INSTANCES = "max-instances";
	public static final String P_MAX_RESOURCE_SIZE = "max-resource-size";
	public static final String P_MIN_DATE_TIME = "min-date-time";
	public static final String P_MOUNTPOINT = "mountpoint";
	public static final String P_MOUNTPOINT_TARGET_PRIVILEGE_SET = "mountpoint-target-privilege-set";
	public static final String P_MOUNTPOINT_TARGET_URL = "mountpoint-target-URL";
    public static final String P_MKADDRESSBOOK = "mkaddressbook";
	public static final String P_MKCALENDAR = "mkcalendar";
	public static final String P_MULTISTATUS = "multistatus";
	
	public static final String P_NAME = "name";
	public static final String P_NAMESPACE = "namespace";
	public static final String P_NEGATE_CONDITION = "negate-condition";
	public static final String P_NEED_PRIVILEGES = "need-privileges";
	public static final String P_NO_INVERT = "no-invert";
	public static final String P_NOVALUE = "novalue";
	
	public static final String P_OWNER = "owner";
	
	public static final String P_PARAM_FILTER = "param-filter";
	public static final String P_PRINCIPAL = "principal";
	public static final String P_PRINCIPAL_COLLECTION_SET = "principal-collection-set";
	public static final String P_PRINCIPAL_MATCH = "principal-match";
	public static final String P_PRINCIPAL_PROPERTY = "principal-property";
	public static final String P_PRINCIPAL_PROPERTY_SEARCH = "principal-property-search";
	public static final String P_PRINCIPAL_SEARCH_PROPERTY = "principal-search-property";
	public static final String P_PRINCIPAL_SEARCH_PROPERTY_SET = "principal-search-property-set";
	public static final String P_PRINCIPAL_URL = "principal-URL";
	public static final String P_PRIVILEGE = "privilege";
	public static final String P_PROP = "prop";
	public static final String P_PROP_FILTER = "prop-filter";
	public static final String P_PROPFIND = "propfind";
	public static final String P_PROPERTY = "property";
	public static final String P_PROPERTY_SEARCH = "property-search";
	public static final String P_PROPERTYUPDATE = "propertyupdate";
	public static final String P_PROPNAME = "propname";
	public static final String P_PROPSTAT = "propstat";
	public static final String P_PROTECTED = "protected";
	
	public static final String P_READ = "read";
	public static final String P_READ_ACL = "read-acl";
	public static final String P_READ_CURRENT_USER_PRIVILEGE_SET = "read-current-user-privilege-set";
	public static final String P_READ_FREE_BUSY = "read-free-busy";
	public static final String P_RECIPIENT = "recipient";
	public static final String P_RECORD_TYPE = "record-type";
	public static final String P_REMOVE = "remove";
	public static final String P_REPORT = "report";
	public static final String P_REQUEST_STATUS = "request-status";
	public static final String P_REQUIRED_PRINCIPAL = "required-principal";
	public static final String P_RESOURCE = "resource";
	public static final String P_RESOURCETYPE = "resourcetype";
	public static final String P_RESPONSE = "response";
	public static final String P_RESPONSEDESCRIPTION = "responsedescription";
	
	public static final String P_SCHEDULE_INBOX = "schedule-inbox";
	public static final String P_SCHEDULE_INBOX_URL = "schedule-inbox-URL";
	public static final String P_SCHEDULE_OUTBOX = "schedule-outbox";
	public static final String P_SCHEDULE_OUTBOX_URL = "schedule-outbox-URL";
	public static final String P_SCHEDULE_RESPONSE = "schedule-response";
	public static final String P_SELF = "self";
	public static final String P_SET = "set";
	public static final String P_SHARED = "shared";
	public static final String P_SOURCE = "source";
    public static final String P_SUPPORTED_ADDRESS_DATA = "supported-address-data";
    public static final String P_SUPPORTED_CALENDAR_COMPONENT = "supported-calendar-component";
	public static final String P_SUPPORTED_CALENDAR_COMPONENT_SET = "supported-calendar-component-set";
	public static final String P_SUPPORTED_CALENDAR_DATA = "supported-calendar-data";
	public static final String P_SUPPORTED_COLLATION = "supported-collation";
	public static final String P_SUPPORTED_COLLATION_SET = "supported-collation-set";
	public static final String P_SUPPORTED_PRIVILEGE = "supported-privilege";
	public static final String P_SUPPORTED_PRIVILEGE_SET = "supported-privilege-set";
	public static final String P_SUPPORTED_REPORT = "supported-report";
	public static final String P_SUPPORTED_REPORT_SET = "supported-report-set";
	public static final String P_SUPPORTEDLOCK = "supportedlock";
	public static final String P_START = "start";
	public static final String P_STATUS = "status";
	
	public static final String P_TEXT_MATCH = "text-match";
	public static final String P_TIME_RANGE = "time-range";
	public static final String P_TIMEOUT = "timeout";
	public static final String P_TIMEZONE = "timezone";

	public static final String P_UNAUTHENTICATED = "unauthenticated";
	public static final String P_UNBIND = "unbind";
	public static final String P_UNLOCK = "unlock";
	
	public static final String P_VALID_ADDRESS_DATA = "valid-address-data";
	public static final String P_VALID_CALENDAR_DATA = "valid-calendar-data";
	public static final String P_VALID_CALENDAR_OBJECT_RESOURCE = "valid-calendar-object-resource";
	public static final String P_VERSION= "version";
	
	public static final String P_WRITE = "write";
	public static final String P_WRITE_ACL = "write-acl";
	public static final String P_WRITE_CONTENT = "write-content";
	public static final String P_WRITE_PROPERTIES = "write-properties";
	
	public static final String ORGANIZER        = "ORGANIZER";
	public static final String ORGANIZER_HREF   = "X-ORGANIZER-HREF";
	public static final String ORGANIZER_MAILTO = "X-ORGANIZER-MAILTO";
	

	// QNames
	public static final QName E_ABSTRACT = QName.get(P_ABSTRACT, WEBDAV_NS);
	public static final QName E_ACE = QName.get(P_ACE, WEBDAV_NS);
	public static final QName E_ACL = QName.get(P_ACL, WEBDAV_NS);
	public static final QName E_ACL_PRINCIPAL_PROP_SET = QName.get(P_ACL_PRINCIPAL_PROP_SET, WEBDAV_NS);
	public static final QName E_ACL_RESTRICTIONS = QName.get(P_ACL_RESTRICTIONS, WEBDAV_NS);
	public static final QName E_ACTIVELOCK = QName.get(P_ACTIVELOCK, WEBDAV_NS);
	public static final QName E_ALL    = QName.get(P_ALL, WEBDAV_NS);
	public static final QName E_ALLCOMP = QName.get(P_ALLCOMP, CALDAV_NS);
	public static final QName E_ALLPROP = QName.get(P_ALLPROP, WEBDAV_NS);
	public static final QName E_ALLPROP_CALDAV = QName.get(P_ALLPROP, CALDAV_NS);
	public static final QName E_ALTERNATE_URI_SET = QName.get(P_ALTERNATE_URI_SET, WEBDAV_NS);
	public static final QName E_APPLY_TO_PRINCIPAL_COLLECTION_SET = QName.get(P_APPLY_TO_PRINCIPAL_COLLECTION_SET, WEBDAV_NS);
	public static final QName E_AUTHENTICATED = QName.get(P_AUTHENTICATED, WEBDAV_NS);

	public static final QName E_BIND    = QName.get(P_BIND, WEBDAV_NS);

	public static final QName E_CALENDAR = QName.get(P_CALENDAR, CALDAV_NS);
	public static final QName E_CALENDAR_COLOR = QName.get(P_CALENDAR_COLOR, APPLE_NS);
	public static final QName E_CALENDAR_DATA = QName.get(P_CALENDAR_DATA, CALDAV_NS);
	public static final QName E_CALENDAR_DESCRIPTION = QName.get(P_CALENDAR_DESCRIPTION, CALDAV_NS);
	public static final QName E_CALENDAR_FREE_BUSY_SET = QName.get(P_CALENDAR_FREE_BUSY_SET, CALDAV_NS);
	public static final QName E_CALENDAR_HOME_SET = QName.get(P_CALENDAR_HOME_SET, CALDAV_NS);
	public static final QName E_CALENDAR_MULTIGET = QName.get(P_CALENDAR_MULTIGET, CALDAV_NS);
	public static final QName E_CALENDAR_PROXY_READ = QName.get(P_CALENDAR_PROXY_READ, CS_NS);
	public static final QName E_CALENDAR_PROXY_READ_FOR = QName.get(P_CALENDAR_PROXY_READ_FOR, CS_NS);
	public static final QName E_CALENDAR_PROXY_WRITE = QName.get(P_CALENDAR_PROXY_WRITE, CS_NS);
	public static final QName E_CALENDAR_PROXY_WRITE_FOR = QName.get(P_CALENDAR_PROXY_WRITE_FOR, CS_NS);
	public static final QName E_CALENDAR_QUERY = QName.get(P_CALENDAR_QUERY, CALDAV_NS);
	public static final QName E_CALENDAR_TIMEZONE = QName.get(P_CALENDAR_TIMEZONE, CALDAV_NS);
	public static final QName E_CALENDAR_USER_ADDRESS_SET = QName.get(P_CALENDAR_USER_ADDRESS_SET, CALDAV_NS);
	public static final QName E_CALENDAR_USER_TYPE = QName.get(P_CALENDAR_USER_TYPE, CALDAV_NS);
	public static final QName E_COLLATION = QName.get(P_COLLATION, WEBDAV_NS);
	public static final QName E_COLLECTION = QName.get(P_COLLECTION, WEBDAV_NS);
	public static final QName E_COMP = QName.get(P_COMP, CALDAV_NS);
	public static final QName E_COMP_FILTER = QName.get(P_COMP_FILTER, CALDAV_NS);
	public static final QName E_CREATIONDATE = QName.get(P_CREATIONDATE, WEBDAV_NS);
	public static final QName E_CURRENT_USER_PRINCIPAL = QName.get(P_CURRENT_USER_PRINCIPAL, WEBDAV_NS);
	public static final QName E_CURRENT_USER_PRIVILEGE_SET = QName.get(P_CURRENT_USER_PRIVILEGE_SET, WEBDAV_NS);
	
	public static final QName E_DEFAULT_CALENDAR = QName.get(P_DEFAULT_CALENDAR, YAHOO_NS);
	public static final QName E_DENY = QName.get(P_DENY, WEBDAV_NS);
	public static final QName E_DENY_BEFORE_GRANT = QName.get(P_DENY_BEFORE_GRANT, WEBDAV_NS);
	public static final QName E_DEPTH = QName.get(P_DEPTH, WEBDAV_NS);
	public static final QName E_DESCRIPTION = QName.get(P_DESCRIPTION, WEBDAV_NS);
	public static final QName E_DISPLAYNAME = QName.get(P_DISPLAYNAME, WEBDAV_NS);
	
	public static final QName E_EMAIL_ADDRESS_SET = QName.get(P_EMAIL_ADDRESS_SET, CS_NS);
	public static final QName E_ERROR = QName.get(P_ERROR, WEBDAV_NS);
	public static final QName E_EXCLUSIVE = QName.get(P_EXCLUSIVE, WEBDAV_NS);
	public static final QName E_EXPAND = QName.get(P_EXPAND, CALDAV_NS);
	public static final QName E_EXPAND_PROPERTY = QName.get(P_EXPAND_PROPERTY, WEBDAV_NS);
	
	public static final QName E_FILTER = QName.get(P_FILTER, CALDAV_NS);
	public static final QName E_FIRST_NAME = QName.get(P_FIRST_NAME, CS_NS);
	public static final QName E_FREE_BUSY_QUERY = QName.get(P_FREE_BUSY_QUERY, CALDAV_NS);
	
	public static final QName E_GETCONTENTLANGUAGE = QName.get(P_GETCONTENTLANGUAGE, WEBDAV_NS);
	public static final QName E_GETCONTENTLENGTH = QName.get(P_GETCONTENTLENGTH, WEBDAV_NS);
	public static final QName E_GETCONTENTTYPE = QName.get(P_GETCONTENTTYPE, WEBDAV_NS);
	public static final QName E_GETCREATIONDATE = QName.get(P_GETCREATIONDATE, WEBDAV_NS);
	public static final QName E_GETCTAG = QName.get(P_GETCTAG, CS_NS);
	public static final QName E_GETETAG = QName.get(P_GETETAG, WEBDAV_NS);
	public static final QName E_GETLASTMODIFIED = QName.get(P_GETLASTMODIFIED, WEBDAV_NS);
	public static final QName E_GRANT = QName.get(P_GRANT, WEBDAV_NS);
	public static final QName E_GRANT_ONLY = QName.get(P_GRANT_ONLY, WEBDAV_NS);
	public static final QName E_GROUP = QName.get(P_GROUP, WEBDAV_NS);
	public static final QName E_GROUP_MEMBER_SET = QName.get(P_GROUP_MEMBER_SET, WEBDAV_NS);
	public static final QName E_GROUP_MEMBERSHIP = QName.get(P_GROUP_MEMBERSHIP, WEBDAV_NS);
	
	public static final QName E_HREF = QName.get(P_HREF, WEBDAV_NS);
	
	public static final QName E_INHERITED_ACL_SET = QName.get(P_INHERITED_ACL_SET, WEBDAV_NS);
	public static final QName E_IS_NOT_DEFINED = QName.get(P_IS_NOT_DEFINED, CALDAV_NS);
	
	public static final QName E_LAST_NAME = QName.get(P_LAST_NAME, CS_NS);
	public static final QName E_LOCKDISCOVERY = QName.get(P_LOCKDISCOVERY, WEBDAV_NS);
	public static final QName E_LOCKSCOPE = QName.get(P_LOCKSCOPE, WEBDAV_NS);
	public static final QName E_LOCKTOKEN = QName.get(P_LOCKTOKEN, WEBDAV_NS);
	public static final QName E_LOCKTYPE = QName.get(P_LOCKTYPE, WEBDAV_NS);
	
	public static final QName E_MATCH = QName.get(P_MATCH, WEBDAV_NS);
	public static final QName E_MOUNTPOINT = QName.get(P_MOUNTPOINT, YAHOO_NS);
	public static final QName E_MOUNTPOINT_TARGET_PRIVILEGE_SET = QName.get(P_MOUNTPOINT_TARGET_PRIVILEGE_SET, YAHOO_NS);
	public static final QName E_MOUNTPOINT_TARGET_URL = QName.get(P_MOUNTPOINT_TARGET_URL, YAHOO_NS);
	public static final QName E_MULTISTATUS = QName.get(P_MULTISTATUS, WEBDAV_NS);
	
	public static final QName E_NEED_PRIVILEGES = QName.get(P_NEED_PRIVILEGES, WEBDAV_NS);
	public static final QName E_NO_INVERT = QName.get(P_NO_INVERT, WEBDAV_NS);
	
	public static final QName E_OWNER = QName.get(P_OWNER, WEBDAV_NS);
	
	public static final QName E_PARAM_FILTER = QName.get(P_PARAM_FILTER, CALDAV_NS);
	public static final QName E_PRINCIPAL = QName.get(P_PRINCIPAL, WEBDAV_NS);
	public static final QName E_PRINCIPAL_COLLECTION_SET = QName.get(P_PRINCIPAL_COLLECTION_SET, WEBDAV_NS);
	public static final QName E_PRINCIPAL_MATCH = QName.get(P_PRINCIPAL_MATCH, WEBDAV_NS);
	public static final QName E_PRINCIPAL_PROPERTY = QName.get(P_PRINCIPAL_PROPERTY, WEBDAV_NS);
	public static final QName E_PRINCIPAL_PROPERTY_SEARCH = QName.get(P_PRINCIPAL_PROPERTY_SEARCH, WEBDAV_NS);
	public static final QName E_PRINCIPAL_SEARCH_PROPERTY = QName.get(P_PRINCIPAL_SEARCH_PROPERTY, WEBDAV_NS);
	public static final QName E_PRINCIPAL_SEARCH_PROPERTY_SET = QName.get(P_PRINCIPAL_SEARCH_PROPERTY_SET, WEBDAV_NS);
	public static final QName E_PRINCIPAL_URL = QName.get(P_PRINCIPAL_URL, WEBDAV_NS);
	public static final QName E_PRIVILEGE = QName.get(P_PRIVILEGE, WEBDAV_NS);
	public static final QName E_PROP = QName.get(P_PROP, WEBDAV_NS);
	public static final QName E_PROPERTY = QName.get(P_PROPERTY, WEBDAV_NS);
	public static final QName E_PROPERTYUPDATE = QName.get(P_PROPERTYUPDATE, WEBDAV_NS);
	public static final QName E_PROPFIND = QName.get(P_PROPFIND, WEBDAV_NS);
	public static final QName E_PROPNAME = QName.get(P_PROPNAME, WEBDAV_NS);
	public static final QName E_PROP_CALDAV = QName.get(P_PROP, CALDAV_NS);
	public static final QName E_PROP_FILTER = QName.get(P_PROP_FILTER, CALDAV_NS);
	public static final QName E_PROPERTY_SEARCH = QName.get(P_PROPERTY_SEARCH, WEBDAV_NS);
	public static final QName E_PROPSTAT = QName.get(P_PROPSTAT, WEBDAV_NS);
	
	public static final QName E_READ = QName.get(P_READ, WEBDAV_NS);
	public static final QName E_READ_ACL = QName.get(P_READ_ACL, WEBDAV_NS);
	public static final QName E_READ_CURRENT_USER_PRIVILEGE_SET = QName.get(P_READ_CURRENT_USER_PRIVILEGE_SET, WEBDAV_NS);
	public static final QName E_READ_FREE_BUSY = QName.get(P_READ_FREE_BUSY, WEBDAV_NS);
	public static final QName E_RECIPIENT = QName.get(P_RECIPIENT, CALDAV_NS);
	public static final QName E_RECORD_TYPE = QName.get(P_RECORD_TYPE, CS_NS);
	public static final QName E_REPORT = QName.get(P_REPORT, WEBDAV_NS);
	public static final QName E_REQUEST_STATUS = QName.get(P_REQUEST_STATUS, CALDAV_NS);
	public static final QName E_REQUIRED_PRINCIPAL = QName.get(P_REQUIRED_PRINCIPAL, WEBDAV_NS);
	public static final QName E_RESOURCE = QName.get(P_RESOURCE, WEBDAV_NS);
	public static final QName E_RESOURCETYPE = QName.get(P_RESOURCETYPE, WEBDAV_NS);
	public static final QName E_RESPONSE = QName.get(P_RESPONSE, WEBDAV_NS);
	public static final QName E_RESPONSEDESCRIPTION = QName.get(P_RESPONSEDESCRIPTION, WEBDAV_NS);
	public static final QName E_CALDAV_RESPONSE = QName.get(P_RESPONSE, CALDAV_NS);
	
	public static final QName E_SCHEDULE_INBOX = QName.get(P_SCHEDULE_INBOX, CALDAV_NS);
	public static final QName E_SCHEDULE_INBOX_URL = QName.get(P_SCHEDULE_INBOX_URL, CALDAV_NS);
	public static final QName E_SCHEDULE_OUTBOX = QName.get(P_SCHEDULE_OUTBOX, CALDAV_NS);
	public static final QName E_SCHEDULE_OUTBOX_URL = QName.get(P_SCHEDULE_OUTBOX_URL, CALDAV_NS);
	public static final QName E_SCHEDULE_RESPONSE = QName.get(P_SCHEDULE_RESPONSE, CALDAV_NS);
	public static final QName E_SET = QName.get(P_SET, WEBDAV_NS);
	public static final QName E_SHARED = QName.get(P_SHARED, WEBDAV_NS);
	public static final QName E_SOURCE = QName.get(P_SOURCE, WEBDAV_NS);
	public static final QName E_STATUS = QName.get(P_STATUS, WEBDAV_NS);
	public static final QName E_SUPPORTED_CALENDAR_COMPONENT = QName.get(P_SUPPORTED_CALENDAR_COMPONENT, CALDAV_NS);
	public static final QName E_SUPPORTED_CALENDAR_COMPONENT_SET = QName.get(P_SUPPORTED_CALENDAR_COMPONENT_SET, CALDAV_NS);
	public static final QName E_SUPPORTED_CALENDAR_DATA = QName.get(P_SUPPORTED_CALENDAR_DATA, CALDAV_NS);
	public static final QName E_SUPPORTED_COLLATION = QName.get(P_SUPPORTED_COLLATION, CALDAV_NS);
	public static final QName E_SUPPORTED_COLLATION_SET = QName.get(P_SUPPORTED_COLLATION_SET, CALDAV_NS);
	public static final QName E_SUPPORTED_PRIVILEGE = QName.get(P_SUPPORTED_PRIVILEGE, WEBDAV_NS);
	public static final QName E_SUPPORTED_PRIVILEGE_SET = QName.get(P_SUPPORTED_PRIVILEGE_SET, WEBDAV_NS);
	public static final QName E_SUPPORTED_REPORT = QName.get(P_SUPPORTED_REPORT, WEBDAV_NS);
	public static final QName E_SUPPORTED_REPORT_SET = QName.get(P_SUPPORTED_REPORT_SET, WEBDAV_NS);
	public static final QName E_SUPPORTEDLOCK = QName.get(P_SUPPORTEDLOCK, WEBDAV_NS);
	
	public static final QName E_TEXT_MATCH = QName.get(P_TEXT_MATCH, CALDAV_NS);
	public static final QName E_TIME_RANGE = QName.get(P_TIME_RANGE, CALDAV_NS);
	public static final QName E_TIMEOUT = QName.get(P_TIMEOUT, WEBDAV_NS);
	public static final QName E_TIMEZONE = QName.get(P_TIMEZONE, CALDAV_NS);
	
	public static final QName E_UNAUTHENTICATED = QName.get(P_UNAUTHENTICATED, WEBDAV_NS);
	public static final QName E_UNBIND = QName.get(P_UNBIND, WEBDAV_NS);
	public static final QName E_UNLOCK = QName.get(P_UNLOCK, WEBDAV_NS);

	public static final QName E_WRITE = QName.get(P_WRITE, WEBDAV_NS);
	public static final QName E_WRITE_ACL = QName.get(P_WRITE_ACL, WEBDAV_NS);
	public static final QName E_WRITE_CONTENT  = QName.get(P_WRITE_CONTENT, WEBDAV_NS);
	public static final QName E_WRITE_PROPERTIES  = QName.get(P_WRITE_PROPERTIES, WEBDAV_NS);

	// errors
	public static final QName E_CANNOT_MODIFY_PROTECTED_PROPERTY = QName.get("cannot-modify-protected-property", WEBDAV_NS);
	public static final QName E_PROPFIND_FINITE_DEPTH = QName.get("propfind-finite-depth", WEBDAV_NS);
	public static final QName E_VALID_CALENDAR_DATA = QName.get(P_VALID_CALENDAR_DATA, CALDAV_NS);
	public static final QName E_VALID_CALENDAR_OBJECT_RESOURCE = QName.get(P_VALID_CALENDAR_OBJECT_RESOURCE, CALDAV_NS);
	
	public static class CardDav {
        public static final QName E_ADDRESS_DATA = QName.get(P_ADDRESS_DATA, CARDDAV_NS);
        public static final QName E_ADDRESSBOOK = QName.get(P_ADDRESSBOOK, CARDDAV_NS);
	    public static final QName E_ADDRESSBOOK_DESCRIPTION = QName.get(P_ADDRESSBOOK_DESCRIPTION, CARDDAV_NS);
	    public static final QName E_ADDRESSBOOK_HOME_SET = QName.get(P_ADDRESSBOOK_HOME_SET, CARDDAV_NS);
	    public static final QName E_ADDRESSBOOK_MULTIGET = QName.get(P_ADDRESSBOOK_MULTIGET, CARDDAV_NS);
	    public static final QName E_ADDRESSBOOK_QUERY = QName.get(P_ADDRESSBOOK_QUERY, CARDDAV_NS);
	    public static final QName E_ALLPROP = QName.get(P_ALLPROP, CARDDAV_NS);
	    
	    public static final QName E_FILTER = QName.get(P_FILTER, CARDDAV_NS);
	    
	    public static final QName E_IS_NOT_DEFINED = QName.get(P_IS_NOT_DEFINED, CARDDAV_NS);
	    
	    public static final QName E_MAX_RESOURCE_SIZE_ADDRESSBOOK = QName.get(P_MAX_RESOURCE_SIZE, CARDDAV_NS);
	    
	    public static final QName E_PARAM_FILTER = QName.get(P_PARAM_FILTER, CARDDAV_NS);
	    public static final QName E_PROP = QName.get(P_PROP, CARDDAV_NS);
	    public static final QName E_PROP_FILTER = QName.get(P_PROP_FILTER, CARDDAV_NS);
	    
	    public static final QName E_SUPPORTED_ADDRESS_DATA = QName.get(P_SUPPORTED_ADDRESS_DATA, CARDDAV_NS);
	    public static final QName E_VALID_ADDRESS_DATA = QName.get(P_VALID_ADDRESS_DATA, CARDDAV_NS);
	    public static final QName E_TEXT_MATCH = QName.get(P_TEXT_MATCH, CARDDAV_NS);
	}
}
