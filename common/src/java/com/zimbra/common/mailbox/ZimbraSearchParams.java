/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.common.mailbox;

import java.util.Set;
import java.util.TimeZone;

public interface ZimbraSearchParams {
    public boolean getIncludeTagDeleted();
    public void setIncludeTagDeleted(boolean value);
    public boolean getPrefetch();
    public void setPrefetch(boolean value);
    public String getQueryString();
    public void setQueryString(String value);
    public Set<MailItemType> getMailItemTypes();
    public ZimbraSearchParams setMailItemTypes(Set<MailItemType> values);
    public ZimbraSortBy getZimbraSortBy();
    public ZimbraSearchParams setZimbraSortBy(ZimbraSortBy value);
    public int getLimit();
    public void setLimit(int value);
    public ZimbraFetchMode getZimbraFetchMode();
    public ZimbraSearchParams setZimbraFetchMode(ZimbraFetchMode value);
    public TimeZone getTimeZone();
    public void setTimeZone(TimeZone value);
}
