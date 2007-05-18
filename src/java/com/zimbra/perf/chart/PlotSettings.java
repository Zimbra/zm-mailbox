package com.zimbra.perf.chart;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * The Original Code is: Zimbra Network
 * 
 * ***** END LICENSE BLOCK *****
 */


public class PlotSettings {
	public static final String FUNCTION_AVG = "avg";

	public static final String FUNCTION_LAST = "last";

	public static final String FUNCTION_MAX = "max";

	public static final String FUNCTION_MAX_PERCENTAGE = "maxPercentage";

	public static final String DEFAULT_PLOT_TSTAMP_COLUMN = "timestamp";

	public static final boolean DEFAULT_PLOT_SHOW_RAW = true;

	public static final boolean DEFAULT_PLOT_SHOW_SUM = false;

	public static final boolean DEFAULT_PLOT_SHOW_MOVING_AVG = false;

	public static final int DEFAULT_PLOT_MOVING_AVG_POINTS = -1;

	public static final double DEFAULT_PLOT_MULTIPLIER = 1.0;

	public static final double DEFAULT_PLOT_DIVISOR = 1.0;

	public static final String DEFAULT_PLOT_AGGREGATE_FUNCTION = FUNCTION_AVG;

	public static final boolean DEFAULT_PLOT_STAT = true;

	private String mLegend;

	private String mInfile;

	private String mTstampColumn;

	private String mDataColumn;

	private boolean mShowRaw;

	private boolean mShowSum;

	private boolean mShowMovingAvg;

	private int mMovingAvgPoints;

	private double mMultiplier;

	private double mDivisor;

	private boolean mStat;

	private String mAggregateFunction;

	public PlotSettings(String legend, String infile, String tstampCol,
			String dataCol, boolean showRaw, boolean showSum,
			boolean showMovingAvg, int movingAvgPoints, double multiplier,
			double divisor, String function) {
		mLegend = legend;
		mInfile = infile;
		mTstampColumn = tstampCol;
		mDataColumn = dataCol;
		mShowRaw = showRaw;
		mShowSum = showSum;
		mShowMovingAvg = showMovingAvg;
		mMovingAvgPoints = movingAvgPoints;
		mMultiplier = multiplier;
		mDivisor = divisor;
		mAggregateFunction = function;

		if (mLegend == null || mLegend.length() < 1)
			mLegend = mDataColumn;
		if (mShowSum)
			mLegend += " (sum)";

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

	public String getLegend() {
		return mLegend;
	}

	public String getInfile() {
		return mInfile;
	}

	public String getTstampColumn() {
		return mTstampColumn;
	}

	public String getDataColumn() {
		return mDataColumn;
	}

	public boolean getShowRaw() {
		return mShowRaw;
	}

	public boolean getShowSum() {
		return mShowSum;
	}

	public boolean getShowMovingAvg() {
		return mShowMovingAvg;
	}

	public int getMovingAvgPoints() {
		return mMovingAvgPoints;
	}

	public double getMultiplier() {
		return mMultiplier;
	}

	public double getDivisor() {
		return mDivisor;
	}

	public String getAggregateFunction() {
		return mAggregateFunction;
	}

	public boolean getStat() {
		return mStat;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<plot>\n");
		sb.append("  ").append("legend=\"").append(mLegend).append("\"\n");
		sb.append("  ").append("infile=\"").append(mInfile).append("\"\n");
		sb.append("  ").append("tstamp=\"").append(mTstampColumn)
				.append("\"\n");
		sb.append("  ").append("data=\"").append(mDataColumn).append("\"\n");
		sb.append("  ").append("showRaw=\"").append(mShowRaw).append("\"\n");
		sb.append("  ").append("showSum=\"").append(mShowSum).append("\"\n");
		sb.append("  ").append("showMovingAvg=\"").append(mShowMovingAvg)
				.append("\"\n");
		sb.append("  ").append("movingAvgPoints=\"").append(mMovingAvgPoints)
				.append("\"\n");
		sb.append("  ").append("multiplier=\"").append(mMultiplier).append(
				"\"\n");
		sb.append("  ").append("divisor=\"").append(mDivisor).append("\"\n");
		sb.append("  ").append("aggregateFunction=\"").append(
				mAggregateFunction).append("\"\n");
		sb.append("</plot>\n");
		return sb.toString();
	}
}
