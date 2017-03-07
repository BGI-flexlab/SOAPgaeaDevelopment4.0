package org.bgi.flexlab.gaea.tools.bamqualtiycontrol.report;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.util.LineReader;
import org.bgi.flexlab.gaea.data.structure.positioninformation.depth.PositionDepth;
import org.bgi.flexlab.gaea.data.structure.reference.ReferenceShare;
import org.bgi.flexlab.gaea.util.SamRecordDatum;

public class ReportBuilder {
	
	ResultReport report;
	
	public void setReportChoice(ResultReport report) {
		this.report = report;
	}
	
	public void initReports(String sampleName, String chrName) throws IOException {
		if(report instanceof WholeGenomeResultReport)
			((WholeGenomeResultReport) report).initReports(chrName);
		else if(report instanceof RegionResultReport)
			((RegionResultReport) report).initReports(sampleName);
	}
	
	public void initReports(String sampleName) throws IOException {
		if(report instanceof WholeGenomeResultReport)
			((WholeGenomeResultReport) report).initReports();
		else if(report instanceof RegionResultReport)
			((RegionResultReport) report).initReports(sampleName);
	}
	
	public boolean unmappedReport(long winNum, String chrName, Iterable<Text> values) {
		return report.unmappedReport(winNum, chrName, values);
	}
	
	public void finalizeUnmappedReport(String chrName) {
		report.finalizeUnmappedReport(chrName);
	}
	
	public boolean mappedReport(SamRecordDatum datum, String chrName, Context context) {
		return report.mappedReport(datum, chrName, context);
	}
	
	public void constructDepthReport(PositionDepth pd, int i, String chrName, long pos) {
		report.constructDepthReport(pd, i, chrName, pos);
	}
	
	public void singleRegionReports(String chrName, long winStart, int winSize , PositionDepth pd) {
		report.singleRegionReports(chrName, winStart, winSize, pd);
	}
	
	public void insertReport(SamRecordDatum datum) {
		report.insertReport(datum);
	}
	
	public int getSampleLaneSzie(String sample) {
		return report instanceof RegionResultReport ? ((RegionResultReport) report).getSampleLaneSize(sample) : 0;
	}
	
	public void parseReport(LineReader lineReader, Text line, ReferenceShare genome) throws IOException {
		report.parseReport(lineReader, line, genome);
	}
	
	public ResultReport build() {
		return report;
	}
}