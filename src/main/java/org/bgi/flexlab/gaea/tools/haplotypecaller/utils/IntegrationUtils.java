package org.bgi.flexlab.gaea.tools.haplotypecaller.utils;

import org.bgi.flexlab.gaea.tools.haplotypecaller.math.GaussIntegrator;
import org.bgi.flexlab.gaea.tools.haplotypecaller.math.GaussIntegratorFactory;
import org.bgi.flexlab.gaea.tools.jointcalling.util.GvcfMathUtils;
import org.bgi.flexlab.gaea.util.IndexRange;

import java.util.function.DoubleFunction;
import java.util.function.ToDoubleBiFunction;

public class IntegrationUtils {
    private static final GaussIntegratorFactory integratorFactory = new GaussIntegratorFactory();

    public static double integrate(final DoubleFunction<Double> getIntegrand,
                                   final double lowerBound,
                                   final double upperBound,
                                   final int numPoints) {
        final GaussIntegrator integrator = integratorFactory.legendre(numPoints, lowerBound, upperBound);

        final double[] gaussIntegrationWeights = new IndexRange(0, numPoints).mapToDouble(integrator::getWeight);
        final double[] gaussIntegrationAbscissas = new IndexRange(0, numPoints).mapToDouble(integrator::getPoint);
        final double[] integrands = GvcfMathUtils.applyToArrayInPlace(gaussIntegrationAbscissas,getIntegrand::apply);

        return GaeaProtectedMathUtils.dotProduct(gaussIntegrationWeights, integrands);
    }

    public static double integrate2d(final ToDoubleBiFunction<Double, Double> getIntegrand,
                              final double xLowerBound, final double xUpperBound, final int xNumPoints,
                              final double yLowerBound, final double yUpperBound, final int yNumPoints){
        final GaussIntegrator xIntegrator = integratorFactory.legendre(xNumPoints, xLowerBound, xUpperBound);
        final GaussIntegrator yIntegrator = integratorFactory.legendre(yNumPoints, yLowerBound, yUpperBound);

        final double[] xIntegrationWeights = new IndexRange(0, xNumPoints).mapToDouble(xIntegrator::getWeight);
        final double[] xAbscissas = new IndexRange(0, xNumPoints).mapToDouble(xIntegrator::getPoint);

        final double[] yIntegrationWeights = new IndexRange(0, yNumPoints).mapToDouble(yIntegrator::getWeight);
        final double[] yAbscissas = new IndexRange(0, yNumPoints).mapToDouble(yIntegrator::getPoint);

        double integral = 0;
        for (int i = 0; i < xNumPoints; i++) {
            final double x = xAbscissas[i];
            for (int j = 0; j < yNumPoints; j++) {
                final double y = yAbscissas[j];
                final double integrand = getIntegrand.applyAsDouble(x, y);
                integral += xIntegrationWeights[i] * yIntegrationWeights[j] * integrand;
            }
        }

        return integral;
    }
}
