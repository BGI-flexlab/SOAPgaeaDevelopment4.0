/*******************************************************************************
 * Copyright (c) 2017, BGI-Shenzhen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * This file incorporates work covered by the following copyright and 
 * Permission notices:
 *
 * Copyright (c) 2009-2012 The Broad Institute
 *  
 *     Permission is hereby granted, free of charge, to any person
 *     obtaining a copy of this software and associated documentation
 *     files (the "Software"), to deal in the Software without
 *     restriction, including without limitation the rights to use,
 *     copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the
 *     Software is furnished to do so, subject to the following
 *     conditions:
 *  
 *     The above copyright notice and this permission notice shall be
 *     included in all copies or substantial portions of the Software.
 *  
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *     EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *     OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *     NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *     HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *     WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *     FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *     OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package org.bgi.flexlab.gaea.tools.vcfqualitycontrol.variantrecalibratioin.traindata;

import org.bgi.flexlab.gaea.data.structure.location.GenomeLocation;
import org.bgi.flexlab.gaea.tools.vcfqualitycontrol.variantrecalibratioin.model.MultipleVariateGaussian;

import java.io.Serializable;
import java.util.Comparator;

public class VariantDatum {
	public double[] annotations;
    public boolean[] isNull;
    public boolean isKnown;
    public double lod;
    public boolean atTruthSite;
    public boolean atTrainingSite;
    public boolean atAntiTrainingSite;
    public boolean isTransition;
    public boolean isSNP;
    public boolean failingSTDThreshold;
    public double originalQual;
    public double prior;
    public int consensusCount;
    public GenomeLocation loc;
    public int worstAnnotation;
    public MultipleVariateGaussian assignment; // used in K-means implementation 

    public static class VariantDatumLODComparator implements Comparator<VariantDatum>, Serializable {
        @Override
        public int compare(final VariantDatum datum1, final VariantDatum datum2) {
            return Double.compare(datum1.lod, datum2.lod);
        }
    }
}
