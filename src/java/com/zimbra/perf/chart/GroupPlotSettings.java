/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.perf.chart;

public class GroupPlotSettings extends PlotSettings {

    private final String mGroupBy;
    public GroupPlotSettings(String groupBy, String infile,
            String dataCol, boolean showRaw,
            boolean showMovingAvg, int movingAvgPoints,
            double multiplier, double divisor,
            boolean nonNegative, boolean percentTime,
            String dataFunction, String aggFunction,
            boolean optional) {
        super(groupBy, infile, dataCol, showRaw, showMovingAvg,
                movingAvgPoints, multiplier, divisor, nonNegative,
                percentTime, dataFunction, aggFunction, optional);
        mGroupBy = groupBy;
    }

    public String getGroupBy() {
        return mGroupBy;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<groupplot\n");
        sb.append("  ").append("legend=\"").append(getLegend()).append("\"\n");
        sb.append("  ").append("infile=\"").append(getInfile()).append("\"\n");
        sb.append("  ").append("data=\"").append(getDataColumn()).append("\"\n");
        sb.append("  ").append("showRaw=\"").append(getShowRaw()).append("\"\n");
        sb.append("  ").append("showMovingAvg=\"").append(getShowMovingAvg()).append("\"\n");
        sb.append("  ").append("movingAvgPoints=\"").append(getMovingAvgPoints()).append("\"\n");
        sb.append("  ").append("multiplier=\"").append(getMultiplier()).append("\"\n");
        sb.append("  ").append("divisor=\"").append(getDivisor()).append("\"\n");
        sb.append("  ").append("nonNegative=\"").append(getNonNegative()).append("\"\n");
        sb.append("  ").append("percentTime=\"").append(getPercentTime()).append("\"\n");
        sb.append("  ").append("dataFunction=\"").append(getDataFunction()).append("\"\n");
        sb.append("  ").append("aggregateFunction=\"").append(getAggregateFunction()).append("\"\n");
        sb.append("  ").append("optional=\"").append(getOptional()).append("\"\n");
        sb.append("  ").append("groupBy=\"").append(mGroupBy).append("\"\n");
        sb.append("/>\n");
        return sb.toString();
    }
}
