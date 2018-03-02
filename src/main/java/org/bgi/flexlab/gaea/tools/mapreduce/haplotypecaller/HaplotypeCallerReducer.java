package org.bgi.flexlab.gaea.tools.mapreduce.haplotypecaller;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.bgi.flexlab.gaea.data.mapreduce.input.bed.RegionHdfsParser;
import org.bgi.flexlab.gaea.data.mapreduce.input.header.SamHdfsFileHeader;
import org.bgi.flexlab.gaea.data.mapreduce.util.VariantContextHadoopWriter;
import org.bgi.flexlab.gaea.data.mapreduce.writable.WindowsBasedWritable;
import org.bgi.flexlab.gaea.data.structure.dbsnp.DbsnpShare;
import org.bgi.flexlab.gaea.data.structure.reference.ChromosomeInformationShare;
import org.bgi.flexlab.gaea.data.structure.reference.ReferenceShare;
import org.bgi.flexlab.gaea.data.structure.reference.index.VcfIndex;
import org.bgi.flexlab.gaea.data.structure.vcf.VCFLocalLoader;
import org.bgi.flexlab.gaea.data.variant.filter.VariantRegionFilter;
import org.bgi.flexlab.gaea.tools.haplotypecaller.HaplotypeCaller;
import org.bgi.flexlab.gaea.tools.haplotypecaller.utils.RefMetaDataTracker;
import org.bgi.flexlab.gaea.util.Window;
import org.seqdoop.hadoop_bam.SAMRecordWritable;
import org.seqdoop.hadoop_bam.VariantContextWritable;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.variant.variantcontext.VariantContext;

public class HaplotypeCallerReducer extends Reducer<WindowsBasedWritable, SAMRecordWritable, NullWritable, VariantContextWritable>{

	/**
     * region
     */
    private RegionHdfsParser region = null;
    
    private HaplotypeCallerOptions options = new HaplotypeCallerOptions();
    
    /**
     * sam file header
     */
    private SAMFileHeader header;

    /**
     * shared reference
     */
    private ReferenceShare genomeShare;
    
    /**
     * shared dbsnp
     */
    private DbsnpShare dbsnpShare = null;
    
    /**
     * vcf loader
     */
	private VCFLocalLoader DBloader = null;
	
	private DbsnpShare alleleShare = null;
	
	private VCFLocalLoader alleleLoader = null;
	
	/**
	 * vcf filter
	 */
	private VariantRegionFilter filter = null;
	
	private HaplotypeCaller haplotypecaller = null;
	
	/**
	 * variant context writer
	 */
	private VariantContextHadoopWriter writer = null;
    
	@Override
    protected void setup(Context context) throws IOException {
		Configuration conf = context.getConfiguration();
		options.getOptionsFromHadoopConf(conf);
		
		if(options.getRegion() != null){
			region = new RegionHdfsParser();
            region.parseBedFileFromHDFS(options.getRegion(), false);
		}
		
		header = SamHdfsFileHeader.getHeader(conf);
        genomeShare = new ReferenceShare();
        genomeShare.loadChromosomeList(options.getReference());

        filter = new VariantRegionFilter();
        if(options.getDBSnp() != null) {
        	dbsnpShare = new DbsnpShare(options.getDBSnp(), options.getReference());
        	dbsnpShare.loadChromosomeList(options.getDBSnp() + VcfIndex.INDEX_SUFFIX);
        	DBloader = new VCFLocalLoader(options.getDBSnp());
        }
        
        if(options.getAlleleFile() != null) {
        	alleleShare = new DbsnpShare(options.getAlleleFile(), options.getReference());
        	alleleShare.loadChromosomeList(options.getAlleleFile() + VcfIndex.INDEX_SUFFIX);
        	alleleLoader = new VCFLocalLoader(options.getAlleleFile());
        }
        
        haplotypecaller = new HaplotypeCaller(region,options,header);
        
        writer = new VariantContextHadoopWriter(context,haplotypecaller.getVCFHeader());
	}
	
	private ArrayList<VariantContext> getRegionVatiantContext(String chr,int number,int winSize,int end,DbsnpShare dbsnpShare,VCFLocalLoader loader){
		ArrayList<VariantContext> dbsnps = null;
		if(dbsnpShare != null) {
			long startPosition = dbsnpShare.getStartPosition(chr, number, winSize);
			if(startPosition >= 0)
				dbsnps = filter.loadFilter(loader, chr, startPosition, end);
		}
		
		return dbsnps;
	}
	
	private RefMetaDataTracker createTracker(String chr,int number,int winSize,int end) {
		RefMetaDataTracker tracker = null;
		
		if(dbsnpShare != null) {
			if(tracker == null)
				tracker = new RefMetaDataTracker();
			ArrayList<VariantContext> dbsnps = getRegionVatiantContext(chr,number,winSize,end,dbsnpShare,DBloader);
			tracker.add(RefMetaDataTracker.DB_VALUE, dbsnps);
		}
		if(alleleShare != null) {
			if(tracker == null)
				tracker = new RefMetaDataTracker();
			ArrayList<VariantContext> dbsnps = getRegionVatiantContext(chr,number,winSize,end,alleleShare,alleleLoader);
			tracker.add(RefMetaDataTracker.ALLELE_VALUE, dbsnps);
		}
		
		return tracker;
	}
	
	@Override
    public void reduce(WindowsBasedWritable key, Iterable<SAMRecordWritable> values, Context context) throws IOException, InterruptedException {
		Window win = new Window(header, key.getChromosomeIndex(), key.getWindowsNumber(), options.getWindowSize());
		
		int start = key.getWindowsNumber() * options.getWindowSize() ;
		start = start == 0 ? 1 : start;
		int end = start + options.getWindowSize() - 1;
		
		String chr = header.getSequence(key.getChromosomeIndex()).getSequenceName();
		ChromosomeInformationShare chrInfo = genomeShare.getChromosomeInfo(chr);
		
		RefMetaDataTracker tracker = createTracker(chr,key.getWindowsNumber(),options.getWindowSize(),end);
		haplotypecaller.dataSourceReset(win, values, chrInfo, tracker);
		haplotypecaller.traverse(writer);
	}
	
	@Override
    protected void cleanup(Context context) {
		haplotypecaller.clear();
    }
}
