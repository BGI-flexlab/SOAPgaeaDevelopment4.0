package org.bgi.flexlab.gaea.data.structure.dbsnp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.bgi.flexlab.gaea.data.structure.memoryshare.WholeGenomeShare;
import org.bgi.flexlab.gaea.util.ChromosomeUtils;

public class DbsnpShare extends WholeGenomeShare{
	private static final String CACHE_NAME = "dbsnpList";
	
	private Map<String,ChromosomeDbsnpShare> dbsnpInfo = new ConcurrentHashMap<String,ChromosomeDbsnpShare>();
	
	public static boolean distributeCache(String chrList, Job job){
		try {
			return distributeCache(chrList,job,CACHE_NAME);
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e.toString());
		}
	}
	
	public void loadChromosomeList() {
		loadChromosomeList(CACHE_NAME);
	}
	
	public void loadChromosomeList(String chrList) {
		try {
			loadChromosomeList(new Path(chrList));
		} catch (IllegalArgumentException | IOException e) {
			throw new RuntimeException(e.toString());
		}
	}
	
	@Override
	public boolean addChromosome(String chrName) {
		ChromosomeDbsnpShare newChr = new ChromosomeDbsnpShare();
		dbsnpInfo.put(chrName, newChr);
		if(dbsnpInfo.get(chrName) != null) {
			return true;
		} else {
			return false;
		}
	}
	
	public ChromosomeDbsnpShare getChromosomeDbsnp(String chrName){
		chrName = ChromosomeUtils.formatChrName(chrName);
		if(!dbsnpInfo.containsKey(chrName))
			return null;
		return dbsnpInfo.get(chrName);
	}
	
	public Map<String,ChromosomeDbsnpShare> getDbsnpMap(){
		return this.dbsnpInfo;
	}
	
	public void setChromosome(String path,String chrName,int length){
		if (dbsnpInfo.containsKey(chrName)) {
			// map chr and get length
			dbsnpInfo.get(chrName).loadChromosome(path);
			dbsnpInfo.get(chrName).setLength(length);
			dbsnpInfo.get(chrName).setChromosomeName(chrName);
		}
	}
}