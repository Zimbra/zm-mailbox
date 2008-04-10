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

public class PlotSettings {

    public static final String DATA_FUNCTION_IDENTITY = "identity";
    public static final String DATA_FUNCTION_DIFF = "diff";
    public static final String DATA_FUNCTION_SUM = "sum";

    public static final String AGG_FUNCTION_AVG = "avg";
    public static final String AGG_FUNCTION_LAST = "last";
    public static final String AGG_FUNCTION_MAX = "max";
    public static final String AGG_FUNCTION_MAX_PERCENTAGE = "maxPercentage";

    public static final String TSTAMP_COLUMN = "timestamp";

    public static final boolean DEFAULT_PLOT_SHOW_RAW = true;
    public static final boolean DEFAULT_PLOT_SHOW_MOVING_AVG = false;
    public static final int     DEFAULT_PLOT_MOVING_AVG_POINTS = -1;
    public static final double  DEFAULT_PLOT_MULTIPLIER = 1.0;
    public static final double  DEFAULT_PLOT_DIVISOR = 1.0;
    public static final boolean DEFAULT_PLOT_NON_NEGATIVE = false;
    public static final boolean DEFAULT_PLOT_PERCENT_TIME = false;
    public static final String  DEFAULT_PLOT_DATA_FUNCTION = DATA_FUNCTION_IDENTITY;
    public static final String  DEFAULT_PLOT_AGGREGATE_FUNCTION = AGG_FUNCTION_AVG;
    public static final boolean DEFAULT_PLOT_OPTIONAL = false;

    private String mLegend;
    private String mInfile;
    private String mDataColumn;
    private boolean mShowRaw;
    private boolean mShowMovingAvg;
    private int mMovingAvgPoints;
    private double mMultiplier;
    private double mDivisor;
    private boolean mNonNegative;
    private boolean mPercentTime;
    private String mDataFunction;
    private String mAggregateFunction;
    private boolean mOptional;
    private String mRatioTop;
    private String mRatioBottom;

    public PlotSettings(String legend, String infile,
                        String dataCol, boolean showRaw,
                        boolean showMovingAvg, int movingAvgPoints,
                        double multiplier, double divisor,
                        boolean nonNegative, boolean percentTime,
                        String dataFunction, String aggFunction,
                        boolean optional, String ratioTop, String ratioBottom) {
        mLegend = legend;
        mInfile = infile;
        mDataColumn = dataCol;
        mShowRaw = showRaw;
        mShowMovingAvg = showMovingAvg;
        mMovingAvgPoints = movingAvgPoints;
        mMultiplier = multiplier;
        mDivisor = divisor;
        mNonNegative = nonNegative;
        mPercentTime = percentTime;
        if (mPercentTime)
            mNonNegative = true;
        mDataFunction = dataFunction;
        mAggregateFunction = aggFunction;
        mOptional = optional;
        mRatioTop = ratioTop;
        mRatioBottom = ratioBottom;

        if (mLegend == null || mLegend.length() < 1)
            mLegend = mDataColumn;
        if (mLegend == null) // must be a ratio
            mLegend = "(" + mRatioTop + ")" + " / " + "(" + mRatioBottom + ")";

        if (mInfile == null || mInfile.length() < 1)
            throw new IllegalArgumentException(
                    "Missing infile attribute on plot " + mLegend);

        if (!mShowRaw && !mShowMovingAvg)
            throw new IllegalArgumentException(
                    "At least one of showRaw, showSum and showMovingAvg "
                            + "must be true in plot " + mLegend);

        if (mDivisor == 0.0)
            throw new IllegalArgumentException(
                    "Divisor of 0 is not allowed in plot " + mLegend);
    }

    public String getLegend() { return mLegend; }
    public String getInfile() { return mInfile; }
    public String getDataColumn() { return mDataColumn; }
    public void setDataColumn(String c) { mDataColumn = c; }
    public boolean getShowRaw() { return mShowRaw; }
    public boolean getShowMovingAvg() { return mShowMovingAvg; }
    public int getMovingAvgPoints() { return mMovingAvgPoints; }
    public double getMultiplier() { return mMultiplier; }
    public double getDivisor() { return mDivisor; }
    public boolean getNonNegative() { return mNonNegative; }
    public boolean getPercentTime() { return mPercentTime; }
    public String getDataFunction() { return mDataFunction; }
    public String getAggregateFunction() { return mAggregateFunction; }
    public boolean getOptional() { return mOptional; }
    public String getRatioTop() { return mRatioTop; }
    public String getRatioBottom() { return mRatioBottom; }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<plot\n");
        sb.append("  ").append("legend=\"").append(mLegend).append("\"\n");
        sb.append("  ").append("infile=\"").append(mInfile).append("\"\n");
        sb.append("  ").append("data=\"").append(mDataColumn).append("\"\n");
        sb.append("  ").append("showRaw=\"").append(mShowRaw).append("\"\n");
        sb.append("  ").append("showMovingAvg=\"").append(mShowMovingAvg).append("\"\n");
        sb.append("  ").append("movingAvgPoints=\"").append(mMovingAvgPoints).append("\"\n");
        sb.append("  ").append("multiplier=\"").append(mMultiplier).append("\"\n");
        sb.append("  ").append("divisor=\"").append(mDivisor).append("\"\n");
        sb.append("  ").append("nonNegative=\"").append(mNonNegative).append("\"\n");
        sb.append("  ").append("percentTime=\"").append(mPercentTime).append("\"\n");
        sb.append("  ").append("dataFunction=\"").append(mDataFunction).append("\"\n");
        sb.append("  ").append("aggregateFunction=\"").append(mAggregateFunction).append("\"\n");
        sb.append("  ").append("optional=\"").append(mOptional).append("\"\n");
        sb.append("/>\n");
        return sb.toString();
    }
}
