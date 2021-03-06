package org.bgi.flexlab.gaea.tools.mapreduce.jointcalling;

import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.bgi.flexlab.gaea.data.mapreduce.output.vcf.GaeaVCFOutputFormat;
import org.bgi.flexlab.gaea.data.mapreduce.writable.WindowsBasedWritable;
import org.bgi.flexlab.gaea.framework.tools.mapreduce.WindowsBasedMapper;
import org.seqdoop.hadoop_bam.VariantContextWritable;
import org.seqdoop.hadoop_bam.util.VCFHeaderReader;
import org.seqdoop.hadoop_bam.util.WrapSeekable;

import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFContigHeaderLine;
import htsjdk.variant.vcf.VCFHeader;

public class JointCallingMapper extends
	Mapper<LongWritable, VariantContextWritable, WindowsBasedWritable, VariantContextWritable>{
	
	private int windowSize = 10000;
	private HashMap<String,Integer> chrIndexs = null;
	
	private WindowsBasedWritable outKey = new WindowsBasedWritable();
	
	private VCFHeader header = null;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		Configuration conf = context.getConfiguration();
		
		Path path = new Path(conf.get(GaeaVCFOutputFormat.OUT_PATH_PROP));
		SeekableStream in = WrapSeekable.openPath(path.getFileSystem(conf), path);
		header = VCFHeaderReader.readHeaderFrom(in);
		in.close();
		
		chrIndexs = new HashMap<>();
		for (VCFContigHeaderLine line : header.getContigLines()) {
			chrIndexs.put(line.getID(), line.getContigIndex());
		}
		
		this.windowSize = conf.getInt(WindowsBasedMapper.WINDOWS_SIZE, 10000);
	}
	
	@Override
	protected void map(LongWritable key, VariantContextWritable value, Context context)
			throws IOException, InterruptedException {
		VariantContext variantContext = value.get();
		
		int sWin = variantContext.getStart() / windowSize ;
		int eWin = variantContext.getEnd() / windowSize;
		
		int chrIndex = chrIndexs.get(variantContext.getContig());

		for (int i = sWin; i <= eWin; i++) {
			outKey.set(chrIndex, i, variantContext.getStart());
			context.write(outKey, value);
		}
	}
}