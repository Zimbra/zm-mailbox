/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.imap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OperationProgress {

    private AtomicInteger completedCount;

    private Integer totalCount;

    private String tag;

    private AtomicBoolean isRunning;

    public OperationProgress(Integer completedCount, Integer totalCount, String tag, Boolean isRunning) {
        this.completedCount = new AtomicInteger(completedCount);
        this.totalCount = totalCount;
        this.tag = tag;
        this.isRunning = new AtomicBoolean(isRunning);
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount.set(completedCount);
    }

    public void setIsRunning(Boolean isRunning) {
        this.isRunning.set(isRunning);
    }

    public Integer getCompletedCount() {
        return completedCount.get();
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public String getTag() {
        return tag;
    }

    public Boolean getIsRunning() {
        return isRunning.get();
    }

    public Integer getPercentageComplete() {
        return totalCount > 0 ? completedCount.get() * 100 / totalCount : 0;
    }

    public boolean isComplete() {
        return completedCount != null
                && totalCount != null
                && completedCount.get() >= totalCount;
    }
}
