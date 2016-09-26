package org.bgi.flexlab.gaea.tools.annotator.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.bgi.flexlab.gaea.tools.annotator.codons.CodonTable;
import org.bgi.flexlab.gaea.tools.annotator.codons.CodonTables;
import org.bgi.flexlab.gaea.tools.annotator.effect.SnpEffectPredictor;
import org.bgi.flexlab.gaea.tools.annotator.interval.Chromosome;
import org.bgi.flexlab.gaea.tools.annotator.interval.Genome;
import org.bgi.flexlab.gaea.tools.annotator.util.CountByType;
import org.bgi.flexlab.gaea.tools.annotator.util.Gpr;
import org.bgi.flexlab.gaea.tools.annotator.util.Timer;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class Config implements Serializable {
	
	private static final long serialVersionUID = 2793968455002586646L;
	
	private static Config configInstance = null; 
	
	public static final String KEY_CODON_PREFIX = "codon.";
	public static final String KEY_CODONTABLE_SUFIX = ".codonTable";
	private static final String DB_CONFIG_JSON = "annotatorDatabaseInfo.json";
	public static final String ANNO_FIELDS_SUFIX = ".fields";
	public static int MAX_WARNING_COUNT = 20;
	
	
	private String  ref = null;
	private String  geneInfo = null;
	private String  cytoBandFile = null;
	private String configFilePath = null;
	
	boolean debug = false; // Debug mode?
	boolean verbose = false; // Verbose
	boolean treatAllAsProteinCoding;
	boolean onlyRegulation; // Only use regulation features
	boolean errorOnMissingChromo; // Error if chromosome is missing
	boolean errorChromoHit; // Error if chromosome is not hit in a query
	boolean hgvs = true; // Use HGVS notation?
	boolean hgvsShift = true; // Shift variants according to HGVS notation (towards the most 3prime possible coordinate)
	boolean hgvsOneLetterAa = false; // Use HGVS 1 letter amino acid in HGVS notation?
	boolean hgvsTrId = false; // Use HGVS transcript ID in HGVS notation?
	String genomeVersion;
	Properties properties;
	Genome genome;
	SnpEffectPredictor snpEffectPredictor;
	private Configuration conf;
	
	HashMap<String, String[]> dbFieldsHashMap = null;
	HashMap<String, TableInfo> dbInfo = null;
	
	CountByType warningsCounter = new CountByType();

	public Config(Configuration conf) {
		this.conf = conf;
		init();
		configInstance = this;
	}
	
	private void init(){
		treatAllAsProteinCoding = false;
		onlyRegulation = false;
		errorOnMissingChromo = true;
		errorChromoHit = true;
		
		configFilePath = conf.get("configFile");
		loadProperties(configFilePath); // Read config file and get a genome
		createCodonTables(genomeVersion, properties); 
		setFieldsByDB();
		loadJson();
	}

	/**
	 * Load properties from configuration file
	 * @return true if success
	 */
	boolean loadProperties(String configFileName) {
		try {
			Path confFilePath = new Path(configFileName);
			FileSystem fs = confFilePath.getFileSystem(conf);
			if(!fs.exists(confFilePath)) {
				throw new RuntimeException(confFilePath.toString() + " don't exist.");
			}
			if(!fs.isFile(confFilePath)) {
				throw new RuntimeException(confFilePath.toString() + " is not a file. GaeaSingleVcfHeader parser only support one vcf file.");
			}
			FSDataInputStream inputStream =  fs.open(confFilePath);
			
			properties.load(inputStream);

			if (!properties.isEmpty()) {
				return true;
			}
		} catch (Exception e) {
			properties = null;
			throw new RuntimeException(e);
		}

		return false;
	}
	
	boolean loadJson() {

		URL url = Config.class.getClassLoader().getResource(DB_CONFIG_JSON);
		String fileName = url.getPath();
		
		Gson gson = new Gson();
		try {
			Reader reader =  new InputStreamReader(Config.class.getResourceAsStream(fileName), "UTF-8");
			dbInfo =  gson.fromJson(reader, new TypeToken<HashMap<String, TableInfo>>() {  
            }.getType());
		} catch ( JsonSyntaxException | IOException e) {
			dbInfo = null;
			Timer.showStdErr("Read a wrong json config file:" + fileName);
			e.printStackTrace();
		}
		
		if (dbInfo != null) {
			return true;
		}
		return false;
	}
	
	/**
	 * Extract and create codon tables
	 */
	void createCodonTables(String genomeId, Properties properties) {
		//---
		// Read codon tables
		//---
		for (Object key : properties.keySet()) {
			if (key.toString().startsWith(KEY_CODON_PREFIX)) {
				String name = key.toString().substring(KEY_CODON_PREFIX.length());
				String table = properties.getProperty(key.toString());
				CodonTable codonTable = new CodonTable(name, table);
				CodonTables.getInstance().add(codonTable);
			}
		}

		//---
		// Assign codon tables for different genome+chromosome
		//---
		for (Object key : properties.keySet()) {
			String keyStr = key.toString();
			if (keyStr.endsWith(KEY_CODONTABLE_SUFIX) && keyStr.startsWith(genomeId + ".")) {
				// Everything between gneomeName and ".codonTable" is assumed to be chromosome name
				int chrNameEnd = keyStr.length() - KEY_CODONTABLE_SUFIX.length();
				int chrNameStart = genomeId.length() + 1;
				int chrNameLen = chrNameEnd - chrNameStart;
				String chromo = null;
				if (chrNameLen > 0) chromo = keyStr.substring(chrNameStart, chrNameEnd);

				// Find codon table
				String codonTableName = properties.getProperty(key.toString());
				CodonTable codonTable = CodonTables.getInstance().getTable(codonTableName);
				if (codonTable == null) throw new RuntimeException("Error parsing property '" + key + "'. No such codon table '" + codonTableName + "'");

				if (chromo != null) {
					// Find chromosome
					Chromosome chr = genome.getOrCreateChromosome(chromo);
					CodonTables.getInstance().set(genome, chr, codonTable);
				} else {
					// Set genome-wide chromosome table
					CodonTables.getInstance().set(genome, codonTable);
				}
			}
		}
	}
	
	
	public HashMap<String, String[]> getDbFieldsHashMap() {
		return dbFieldsHashMap;
	}
	
	public HashMap<String, TableInfo> getDbInfo() {
		return dbInfo;
	}
	
	private HashMap<String, String[]> setFieldsByDB() {
		
		// Sorted keys
		ArrayList<String> keys = new ArrayList<String>();
		for (Object k : properties.keySet())
			keys.add(k.toString());
		Collections.sort(keys);

		dbFieldsHashMap = new HashMap<String, String[]>();
		for (String key : keys) {
			if (key.equalsIgnoreCase("ref")) {
				setRef(properties.getProperty(key));
			}else if (key.equalsIgnoreCase("geneInfo")) {
				setGeneInfo(properties.getProperty(key));
			}else if (key.endsWith(ANNO_FIELDS_SUFIX)) {
				String dbName = key.substring(0, key.length() - ANNO_FIELDS_SUFIX.length());

				// Add full name
				String[] fields = properties.getProperty(key).split(",");
				if (fields != null && fields.length != 0) {
					dbFieldsHashMap.put(dbName, fields);
				}
			}
		}
		return dbFieldsHashMap;
	}
	
	public String[] getFieldsByDB(String dbName){
		return dbFieldsHashMap.get(dbName);
	}

	public static Config getConfigInstance() {
		return configInstance;
	}
	
	public static Config get(){
		return configInstance;
	}

	public Genome getGenome() {
		return genome;
	}

	public String getGenomeVersion() {
		return genomeVersion;
	}
	
	public void setHgvsOneLetterAA(boolean hgvsOneLetterAa) {
		this.hgvsOneLetterAa = hgvsOneLetterAa;
	}

	public void setHgvsShift(boolean hgvsShift) {
		this.hgvsShift = hgvsShift;
	}

	public void setHgvsTrId(boolean hgvsTrId) {
		this.hgvsTrId = hgvsTrId;
	}

	public void setOnlyRegulation(boolean onlyRegulation) {
		this.onlyRegulation = onlyRegulation;
	}

	public void setString(String propertyName, String value) {
		properties.setProperty(propertyName, value);
	}

	public void setTreatAllAsProteinCoding(boolean treatAllAsProteinCoding) {
		this.treatAllAsProteinCoding = treatAllAsProteinCoding;
	}

	public void setUseHgvs(boolean useHgvs) {
		hgvs = useHgvs;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
//	/**
//	 * Get the relative path to a config file
//	 */
//	String getRelativeConfigPath() {
//		URL url = Config.class.getProtectionDomain().getCodeSource().getLocation();
//		try {
//			File path = new File(url.toURI());
//			return path.getParent();
//		} catch (Exception e) {
//			throw new RuntimeException("Cannot get path '" + url + "'", e);
//		}
//	}

	public boolean isDebug() {
		return debug;
	}

	public boolean isErrorChromoHit() {
		return errorChromoHit;
	}

	public boolean isErrorOnMissingChromo() {
		return errorOnMissingChromo;
	}

	public boolean isHgvs() {
		return hgvs;
	}

	public boolean isHgvs1LetterAA() {
		return hgvsOneLetterAa;
	}

	public boolean isHgvsShift() {
		return hgvsShift;
	}

	public boolean isHgvsTrId() {
		return hgvsTrId;
	}

	public boolean isOnlyRegulation() {
		return onlyRegulation;
	}

	public boolean isTreatAllAsProteinCoding() {
		return treatAllAsProteinCoding;
	}

	public boolean isVerbose() {
		return verbose;
	}
	
	/**
	 * Show a warning message and exit
	 */
	public void warning(String warningType, String details) {
		long count = warningsCounter.inc(warningType);

		if (debug || count < MAX_WARNING_COUNT) {
			if (debug) Gpr.debug(warningType + details, 1);
			else System.err.println(warningType + details);
		} else if (count <= MAX_WARNING_COUNT) {
			String msg = "Too many '" + warningType + "' warnings, no further warnings will be shown:\n" + warningType + details;
			if (debug) Gpr.debug(msg, 1);
			else System.err.println(msg);
		}
	}

	public String getGeneInfo() {
		return geneInfo;
	}

	public void setGeneInfo(String geneInfo) {
		this.geneInfo = geneInfo;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getCytoBandFile() {
		return cytoBandFile;
	}

	public void setCytoBandFile(String cytoBandFile) {
		this.cytoBandFile = cytoBandFile;
	}
	

	public SnpEffectPredictor getSnpEffectPredictor() {
		return snpEffectPredictor;
	}

	public void setSnpEffectPredictor(SnpEffectPredictor snpEffectPredictor) {
		this.snpEffectPredictor = snpEffectPredictor;
	}

//	public static void main(String[] args) throws Exception {
//		Config config = new Config();
//		Config.testProp();
//	}
}
