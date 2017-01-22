package org.bgi.flexlab.gaea.tools.variantrecalibratioin.tranche;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bgi.flexlab.gaea.data.exception.MalformedFile;
import org.bgi.flexlab.gaea.data.exception.UserException;
import org.bgi.flexlab.gaea.tools.mapreduce.variantrecalibratioin.VariantRecalibrationOptions;
import org.bgi.flexlab.gaea.tools.mapreduce.variantrecalibratioin.VariantRecalibrationOptions.Mode;


public class Tranche {
	private static final int CURRENT_VERSION = 5;

    public double ts, minVQSLod, knownTiTv, novelTiTv;
    public int knownTi,knownTv,numKnown,novelTi,novelTv,numNovel;
    public String name;
    public VariantRecalibrationOptions.Mode model;

    int accessibleTruthSites = 0;
    int callsAtTruthSites = 0;

    public Tranche(double ts, double minVQSLod, int knownTi, int knownTv, double knownTiTv, int novelTi, int novelTv, double novelTiTv, int accessibleTruthSites, int callsAtTruthSites, VariantRecalibrationOptions.Mode model) {	
    	this(ts, minVQSLod, knownTi + knownTv, knownTiTv, novelTi + novelTv, novelTiTv, accessibleTruthSites, callsAtTruthSites, model);
    	this.knownTi = knownTi;
    	this.knownTv = knownTv;
    	this.novelTi = novelTi;
    	this.novelTv = novelTv;
    }
    
    public Tranche(double ts, double minVQSLod, int numKnown, double knownTiTv, int numNovel, double novelTiTv, int accessibleTruthSites, int callsAtTruthSites, VariantRecalibrationOptions.Mode model) {
        this(ts, minVQSLod, numKnown, knownTiTv, numNovel, novelTiTv, accessibleTruthSites, callsAtTruthSites, model, "anonymous");
    }

    public Tranche(double ts, double minVQSLod, int numKnown, double knownTiTv, int numNovel, double novelTiTv, int accessibleTruthSites, int callsAtTruthSites, VariantRecalibrationOptions.Mode model, String name ) {
        this.ts = ts;
        this.minVQSLod = minVQSLod;
        this.novelTiTv = novelTiTv;
        this.numNovel = numNovel;
        this.knownTiTv = knownTiTv;
        this.numKnown = numKnown;
        this.model = model;
        this.name = name;

        this.accessibleTruthSites = accessibleTruthSites;
        this.callsAtTruthSites = callsAtTruthSites;

        if ( ts < 0.0 || ts > 100.0)
            throw new UserException("Target FDR is unreasonable " + ts);

        if ( numKnown < 0 || numNovel < 0)
            throw new RuntimeException("Invalid tranche - no. variants is < 0 : known " + numKnown + " novel " + numNovel);

        if ( name == null )
            throw new RuntimeException("BUG -- name cannot be null");
    }

    private double getTruthSensitivity() {
        return accessibleTruthSites > 0 ? callsAtTruthSites / (1.0*accessibleTruthSites) : 0.0;
    }

    public static class TrancheTruthSensitivityComparator implements Comparator<Tranche>, Serializable {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public int compare(final Tranche tranche1, final Tranche tranche2) {
            return Double.compare(tranche1.ts, tranche2.ts);
        }
    }

    @Override
    public String toString() {
        return String.format("Tranche ts=%.2f minVQSLod=%.4f known=(%d @ %.4f) novel=(%d @ %.4f) truthSites(%d accessible, %d called), name=%s]",
                ts, minVQSLod, numKnown, knownTiTv, numNovel, novelTiTv, accessibleTruthSites, callsAtTruthSites, name);
    }

    /**
     * Returns an appropriately formatted string representing the raw tranches file on disk.
     *
     * @param tranches
     * @return
     */
    public static String tranchesString( final List<Tranche> tranches ) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final PrintStream stream = new PrintStream(bytes);

        Collections.sort( tranches, new TrancheTruthSensitivityComparator() );

        stream.println("# Variant quality score tranches file");
        stream.println("# Version number " + CURRENT_VERSION);
        stream.println("targetTruthSensitivity,numKnown,numNovel,knownTiTv,novelTiTv,minVQSLod,filterName,model,accessibleTruthSites,callsAtTruthSites,truthSensitivity");

        Tranche prev = null;
        for ( Tranche t : tranches ) {
            stream.printf("%.2f,%d,%d,%.4f,%.4f,%.4f,VQSRTranche%s%.2fto%.2f,%s,%d,%d,%.4f%n",
                    t.ts, t.numKnown, t.numNovel, t.knownTiTv, t.novelTiTv, t.minVQSLod, t.model.toString(),
                    (prev == null ? 0.0 : prev.ts), t.ts, t.model.toString(), t.accessibleTruthSites, t.callsAtTruthSites, t.getTruthSensitivity());
            prev = t;
        }

        return bytes.toString();
    }

    private static double getDouble(Map<String,String> bindings, String key, boolean required) {
        if ( bindings.containsKey(key) ) {
            String val = bindings.get(key);
            return Double.valueOf(val);
        }
        else if ( required ) {
            throw new MalformedFile("Malformed tranches file.  Missing required key " + key);
        }
        else
            return -1;
    }

    private static int getInteger(Map<String,String> bindings, String key, boolean required) {
        if ( bindings.containsKey(key) )
            return Integer.valueOf(bindings.get(key));
        else if ( required ) {
            throw new MalformedFile("Malformed tranches file.  Missing required key " + key);
        }
        else
            return -1;
    }

    /**
     * Returns a list of tranches, sorted from most to least specific, read in from file f
     *
     * @param f
     * @return
     */
    public static List<Tranche> readTranches(File f) {
        String[] header = null;
        List<Tranche> tranches = new ArrayList<Tranche>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;
            while((line = reader.readLine()) != null){
                if ( line.startsWith("#") )
                    continue;

                final String[] vals = line.split(",");
                if( header == null ) {
                    header = vals;
                    if ( header.length == 5 || header.length == 8 || header.length == 10 )
                        // old style tranches file, throw an error
                        throw new MalformedFile(f, "Unfortunately your tranches file is from a previous version of this tool and cannot be used with the latest code.  Please rerun VariantRecalibrator");
                    if ( header.length != 11 )
                        throw new MalformedFile(f, "Expected 11 elements in header line " + line);
                } else {
                    if ( header.length != vals.length )
                        throw new MalformedFile(f, "Line had too few/many fields.  Header = " + header.length + " vals " + vals.length + ". The line was: " + line);

                    Map<String,String> bindings = new HashMap<String, String>();
                    for ( int i = 0; i < vals.length; i++ ) bindings.put(header[i], vals[i]);
                    tranches.add(new Tranche(getDouble(bindings,"targetTruthSensitivity", true),
                            getDouble(bindings,"minVQSLod", true),
                            getInteger(bindings,"numKnown", false),
                            getDouble(bindings,"knownTiTv", false),
                            getInteger(bindings,"numNovel", true),
                            getDouble(bindings,"novelTiTv", true),
                            getInteger(bindings,"accessibleTruthSites", false),
                            getInteger(bindings,"callsAtTruthSites", false),
                            Mode.valueOf(bindings.get("model").toUpperCase()),
                            bindings.get("filterName")));
                }
            }

            Collections.sort( tranches, new TrancheTruthSensitivityComparator() );
            return tranches;
        } catch( IOException e ) {
            throw new UserException.CouldNotReadInputFile(f, e);
        }
    }
}
