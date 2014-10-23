/**
* Hub Miner: a hubness-aware machine learning experimentation library.
* Copyright (C) 2014  Nenad Tomasev. Email: nenad.tomasev at gmail.com
* 
* This program is free software: you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along with
* this program. If not, see <http://www.gnu.org/licenses/>.
*/
package images.mining.clustering;

import data.neighbors.NeighborSetFinder;
import data.representation.DataInstance;
import data.representation.DataSet;
import data.representation.images.sift.SIFTRepresentation;
import data.representation.images.sift.SIFTVector;
import data.representation.images.sift.util.ClusteredSIFTRepresentation;
import data.representation.images.sift.util.ClusteredSIFTVector;
import data.representation.util.DataMineConstants;
import distances.primary.CombinedMetric;
import distances.primary.SIFTMetric;
import distances.primary.SIFTSpatialMetric;
import images.mining.calc.AverageColorGrabber;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import learning.unsupervised.Cluster;
import learning.unsupervised.ClusteringAlg;
import learning.unsupervised.evaluation.quality.OptimalConfigurationFinder;
import learning.unsupervised.methods.KMeans;
import util.AuxSort;

/**
 * This class implements an adapted K-means approach for performing intra-image
 * SIFT feature clustering, as described in the paper "Two pass k-means
 * algorithm for finding SIFT clusters in an image" in 2010 at the Slovenian KDD
 * conference which is a part of the larger Information Society
 * multi-conference. This is one of the K-means implementations that was used in
 * the experiments. This code should be merged with IntraImageKMeansAdapted, but
 * these experiments were a one-off, so it was not worth the effort afterwards
 * to re-factor things completely.
 *
 * @author Nenad Tomasev <nenad.tomasev at gmail.com>
 */
public class IntraImageKMeansAdaptedScale extends ClusteringAlg {

    private float alpha = 1; // Weight of the decriptors.
    private float beta = 1; // Weight of the color information.
    private float gamma = 1; // Weight of feature scale.
    public static final int MIN_ITER = 5;
    public static final int MAX_ITER = 15;
    public static final float XY_PRIORITY = 1;
    public static final float COLOR_PRIORITY = 0.4f;
    public static final float DESC_PRIORITY = 0.5f;
    private static final float ERROR_THRESHOLD = 0.001f;
    private float error;
    private int minClusters = 1;
    private int maxClusters = 1;
    private int repetitions = 1; // How many times to repeat for each K.
    private Cluster[][] configurations = null;
    private int[][] colorNeighborhoods = null;
    private boolean colorNeighborhoodsCalculated = false;
    private boolean randomInit = false;
    // The image where the features are being clustered.
    private BufferedImage image;
    
    @Override
    public HashMap<String, String> getParameterNamesAndDescriptions() {
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("alpha", "Weight of the descriptors.");
        paramMap.put("beta", "Weight of the color information.");
        paramMap.put("gamma", "Weight of the scale features.");
        paramMap.put("minClusters", "Minimal number of clusters to try.");
        paramMap.put("maxClusters", "Maximal number of clusters to try.");
        paramMap.put("repetitions", "How many times to repeat for each K.");
        return paramMap;
    }

    /**
     * Initialization.
     *
     * @param bi BufferedImage that is being clustered into SIFT feature groups.
     * @param rep SIFTRepresentation that is a set of all the SIFT features from
     * the image.
     * @param numClusters Number of clusters to cluster to.
     */
    public IntraImageKMeansAdaptedScale(BufferedImage bi,
            SIFTRepresentation rep, int numClusters) {
        setDataSet(rep);
        setNumClusters(numClusters);
        minClusters = numClusters;
        maxClusters = numClusters;
        repetitions = 1;
        image = bi;
    }

    /**
     * Initialization.
     *
     * @param bi BufferedImage that is being clustered into SIFT feature groups.
     * @param rep SIFTRepresentation that is a set of all the SIFT features from
     * the image.
     * @param numClusters Number of clusters to cluster to.
     * @param alpha Float value that is the weight of the descriptors in the
     * distance.
     * @param beta Float value that is the weight of the color in the distance.
     */
    public IntraImageKMeansAdaptedScale(BufferedImage bi,
            SIFTRepresentation rep, int numClusters, float alpha, float beta) {
        setDataSet(rep);
        setNumClusters(numClusters);
        minClusters = numClusters;
        maxClusters = numClusters;
        repetitions = 1;
        image = bi;
        this.alpha = alpha;
        this.beta = beta;
    }

    /**
     * Initialization.
     *
     * @param bi BufferedImage that is being clustered into SIFT feature groups.
     * @param rep SIFTRepresentation that is a set of all the SIFT features from
     * the image.
     * @param numClusters Number of clusters to cluster to.
     * @param alpha Float value that is the weight of the descriptors in the
     * distance.
     * @param beta Float value that is the weight of the color in the distance.
     * @param gamma Float value that is the weight of the scale in the distance.
     */
    public IntraImageKMeansAdaptedScale(BufferedImage bi,
            SIFTRepresentation rep, int numClusters, float alpha,
            float beta, float gamma) {
        setDataSet(rep);
        setNumClusters(numClusters);
        minClusters = numClusters;
        maxClusters = numClusters;
        repetitions = 1;
        image = bi;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }

    /**
     * Initialization.
     *
     * @param bi BufferedImage that is being clustered into SIFT feature groups.
     * @param rep SIFTRepresentation that is a set of all the SIFT features from
     * the image.
     * @param minClusters Integer that is the minimum number of clusters to try.
     * @param maxClusters Integer that is the maximum number of clusters to try.
     * @param repetitions Integer that is the number of repetitions.
     * @param randomInit Boolean indicating whether to use random
     * initialization.
     * @param alpha Float value that is the weight of the descriptors in the
     * distance.
     * @param beta Float value that is the weight of the color in the distance.
     */
    public IntraImageKMeansAdaptedScale(BufferedImage bi,
            SIFTRepresentation rep, int minClusters, int maxClusters,
            int repetitions, boolean randomInit, float alpha, float beta) {
        setDataSet(rep);
        setNumClusters(minClusters);
        this.minClusters = minClusters;
        this.maxClusters = maxClusters;
        this.repetitions = repetitions;
        this.randomInit = randomInit;
        image = bi;
        this.alpha = alpha;
        this.beta = beta;
    }

    /**
     * Initialization.
     *
     * @param bi BufferedImage that is being clustered into SIFT feature groups.
     * @param rep SIFTRepresentation that is a set of all the SIFT features from
     * the image.
     * @param minClusters Integer that is the minimum number of clusters to try.
     * @param maxClusters Integer that is the maximum number of clusters to try.
     * @param repetitions Integer that is the number of repetitions.
     * @param randomInit Boolean indicating whether to use random
     * initialization.
     * @param alpha Float value that is the weight of the descriptors in the
     * distance.
     * @param beta Float value that is the weight of the color in the distance.
     * @param gamma Float value that is the weight of the scale in the distance.
     */
    public IntraImageKMeansAdaptedScale(BufferedImage bi,
            SIFTRepresentation rep, int minClusters, int maxClusters,
            int repetitions, boolean randomInit, float alpha, float beta,
            float gamma) {
        setDataSet(rep);
        setNumClusters(minClusters);
        this.minClusters = minClusters;
        this.maxClusters = maxClusters;
        this.repetitions = repetitions;
        this.randomInit = randomInit;
        image = bi;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }

    /**
     *
     * @param bi BufferedImage that is being clustered into SIFT feature groups.
     * @param rep SIFTRepresentation that is a set of all the SIFT features from
     * the image.
     * @param minClusters Integer that is the minimum number of clusters to try.
     * @param maxClusters Integer that is the maximum number of clusters to try.
     * @param repetitions Integer that is the number of repetitions.
     * @param randomInit Boolean indicating whether to use random
     * initialization.
     */
    public IntraImageKMeansAdaptedScale(BufferedImage bi,
            SIFTRepresentation rep, int minClusters, int maxClusters,
            int repetitions, boolean randomInit) {
        setDataSet(rep);
        setNumClusters(minClusters);
        this.minClusters = minClusters;
        this.maxClusters = maxClusters;
        this.repetitions = repetitions;
        this.randomInit = randomInit;
        image = bi;
    }

    /**
     * Reinitialize before another clustering run.
     */
    public void reinit() {
        error = Float.MAX_VALUE;
        setClusterAssociations(null);
    }

    @Override
    public void cluster() throws Exception {
        flagAsActive();
        int diffKs = maxClusters - minClusters + 1;
        if (repetitions < 1) {
            throw new Exception("Must be at least one repetition,"
                    + "variable repetitions = " + repetitions);
        }
        if (minClusters > maxClusters) {
            throw new Exception("minClusters must be less than maxClusters,"
                    + "minClusters = " + minClusters + ", maxClusters = "
                    + maxClusters);
        }
        if ((minClusters == maxClusters) && (repetitions == 1)) {
            // In this case, we are not testing across a range.
            clusterOnce();
            return;
        }
        configurations = new Cluster[diffKs * repetitions][];
        for (int cIndex = minClusters; cIndex <= maxClusters; cIndex++) {
            setNumClusters(cIndex);
            for (int rIndex = 0; rIndex < repetitions; rIndex++) {
                reinit();
                System.out.println("Clustering for k: " + cIndex);
                clusterOnce();
                configurations[(cIndex - minClusters) * repetitions + rIndex] =
                        getClusters();
            }
        }
        flagAsInactive();
    }

    /**
     * Get the best configuration after all the clustering runs.
     *
     * @return Cluster[] representing the best achieved clustering
     * representation according to the Dunn index.
     * @throws Exception
     */
    public Cluster[] getBestConfiguration() throws Exception {
        if (configurations == null) {
            return getClusters();
        }
        CombinedMetric cmet = new CombinedMetric(null, new SIFTSpatialMetric(),
                CombinedMetric.DEFAULT);
        OptimalConfigurationFinder selector = new OptimalConfigurationFinder(
                configurations, getDataSet(), cmet,
                OptimalConfigurationFinder.DUNN_INDEX);
        return selector.findBestConfiguration();
    }

    /**
     * Gets all the generated clustering configurations.
     *
     * @return Cluster[][] containing all the produced clusterings during the
     * run.
     */
    public Cluster[][] getClusterConfigurations() {
        if (configurations != null) {
            return configurations;
        } else {
            configurations = new Cluster[1][];
            configurations[0] = getClusters();
            return configurations;
        }
    }

    /**
     * Perform a single clustering run for a fixed K value.
     *
     * @throws Exception
     */
    public void clusterOnce() throws Exception {
        SIFTRepresentation dset = (SIFTRepresentation) (getDataSet());
        int numClusters = getNumClusters();
        performBasicChecks();
        boolean trivial = checkIfTrivial();
        if (trivial) {
            return;
        }
        int[] clusterAssociations = new int[dset.data.size()];
        if (numClusters == 1) {
            setClusterAssociations(clusterAssociations);
        } else if (numClusters == clusterAssociations.length) {
            for (int i = 0; i < clusterAssociations.length; i++) {
                clusterAssociations[i] = i;
            }
        } else {
            setClusterAssociations(clusterAssociations);
            // Calculate the color neighborhoods.
            DataInstance instance;
            AverageColorGrabber avg = new AverageColorGrabber(image);
            // Since there may be multiple runs, this array is reused to avoid
            // calculating it all over again.
            if (!colorNeighborhoodsCalculated) {
                colorNeighborhoods = new int[dset.size()][3];
                for (int i = 0; i < dset.size(); i++) {
                    instance = dset.data.get(i);
                    colorNeighborhoods[i] = avg.getAverageColorInArray(
                            instance.fAttr[1], instance.fAttr[0]);
                }
                colorNeighborhoodsCalculated = true;
            }
            // Centroid initialization.
            ClusteredSIFTVector[] centroids =
                    new ClusteredSIFTVector[numClusters];
            ClusteredSIFTRepresentation clusterCentroidDSet =
                    new ClusteredSIFTRepresentation(128);
            for (int i = 0; i < clusterAssociations.length; i++) {
                clusterAssociations[i] = -1;
            }
            if (randomInit) {
                // Random initialization.
                Random initializer = new Random();
                int centroidIndex;
                for (int cIndex = 0; cIndex < numClusters; cIndex++) {
                    centroidIndex = initializer.nextInt(
                            clusterAssociations.length);
                    while (clusterAssociations[centroidIndex] != -1) {
                        centroidIndex = initializer.nextInt(
                                clusterAssociations.length);
                    }
                    clusterAssociations[centroidIndex] = cIndex;
                    centroids[cIndex] =
                            new ClusteredSIFTVector(clusterCentroidDSet);
                    clusterCentroidDSet.addDataInstance(centroids[cIndex]);
                    centroids[cIndex].fAttr =
                            (dset.data.get(centroidIndex)).fAttr;
                    centroids[cIndex].iAttr[0] = cIndex;
                }
            } else {
                // Smart initalization. It is very important for the initial
                // centroids to be well positioned. First we do spatial KMeans
                // to fix centroids in dense areas, then get these final 
                // centroids there for the initial seed of the secondary K-means
                // pass through the feature data.
                CombinedMetric cmet = new CombinedMetric(null,
                        new SIFTSpatialMetric(), CombinedMetric.DEFAULT);
                KMeans clusterer = new KMeans(dset, cmet, numClusters);
                clusterer.cluster();
                Cluster[] seedClusters = clusterer.getClusters();
                for (int cIndex = 0; cIndex < numClusters; cIndex++) {
                    if (!seedClusters[cIndex].isEmpty()) {
                        centroids[cIndex] = new ClusteredSIFTVector(
                                seedClusters[cIndex].getCentroid());
                        clusterCentroidDSet.addDataInstance(centroids[cIndex]);
                        centroids[cIndex].setContext(clusterCentroidDSet);
                        centroids[cIndex].iAttr[0] = cIndex;
                    } else {
                        Random initializer = new Random();
                        int centroidIndex = -1;
                        boolean different = false;
                        while (!different) {
                            different = true;
                            centroidIndex = initializer.nextInt(
                                    clusterAssociations.length);
                            while (clusterAssociations[centroidIndex] != -1) {
                                centroidIndex = initializer.nextInt(
                                        clusterAssociations.length);
                            }
                            int j = -1;
                            while (++j < cIndex) {
                                if ((dset.data.get(centroidIndex)).
                                        equalsByFloatValue(centroids[j])) {
                                    different = false;
                                    break;
                                }
                            }
                        }
                        centroids[cIndex] =
                                new ClusteredSIFTVector(clusterCentroidDSet);
                        centroids[cIndex].fAttr =
                                (dset.data.get(centroidIndex)).fAttr;
                        clusterCentroidDSet.addDataInstance(centroids[cIndex]);
                        centroids[cIndex].iAttr[0] = cIndex;
                    }
                }
            }
            // Preparations for the main clustering loop.
            Cluster[] clusters;
            float errorPrevious;
            float errorCurrent = Float.MAX_VALUE;
            setIterationIndex(0);
            boolean noReassignments;
            boolean errorDifferenceSignificant = true;
            SIFTMetric smet = new SIFTMetric();
            SIFTSpatialMetric ssm = new SIFTSpatialMetric();
            for (int i = 0; i < clusterAssociations.length; i++) {
                float minCDist = Float.MAX_VALUE;
                int closestIndex = -1;
                float currDist;
                for (int cIndex = 0; cIndex < centroids.length; cIndex++) {
                    currDist = ssm.dist(dset.data.get(i), centroids[cIndex]);
                    if (currDist < minCDist) {
                        minCDist = currDist;
                        closestIndex = cIndex;
                    }
                }
                clusterAssociations[i] = closestIndex;
            }
            do {
                nextIteration();
                // Dynamically calculate the iteration error.
                error = 0;
                // Assigning to clusters.
                noReassignments = true;
                // Find the average colors for centroids.
                float[][] centroidColorNeighborhoods =
                        new float[numClusters][3];
                float[] clSize = new float[centroids.length];
                for (int i = 0; i < clusterAssociations.length; i++) {
                    clSize[clusterAssociations[i]]++;
                    for (int j = 0; j < 3; j++) {
                        centroidColorNeighborhoods[
                                clusterAssociations[i]][j] +=
                                colorNeighborhoods[i][j];
                    }
                }
                // Now normalize.
                for (int cIndex = 0; cIndex < centroids.length; cIndex++) {
                    if (clSize[cIndex] > 0) {
                        for (int j = 0; j < 3; j++) {
                            centroidColorNeighborhoods[cIndex][j] /=
                                    clSize[cIndex];
                        }
                    }
                }
                for (int i = 0; i < clusterAssociations.length; i++) {
                    int closestCentroidIndex = -1;
                    instance = dset.getInstance(i);
                    // Here is the altered part.
                    float[] colorDists = new float[numClusters];
                    float[] xyDists = new float[numClusters];
                    float[] descDists = new float[numClusters];
                    float[] scaleDists = new float[numClusters];
                    for (int cIndex = 0; cIndex < numClusters; cIndex++) {
                        colorDists[cIndex] += Math.abs(colorNeighborhoods[i][0]
                                - centroidColorNeighborhoods[cIndex][0]);
                        colorDists[cIndex] += Math.abs(colorNeighborhoods[i][1]
                                - centroidColorNeighborhoods[cIndex][1]);
                        colorDists[cIndex] += Math.abs(colorNeighborhoods[i][2]
                                - centroidColorNeighborhoods[cIndex][2]);
                        xyDists[cIndex] += Math.pow(instance.fAttr[1]
                                - centroids[cIndex].getX(), 2);
                        xyDists[cIndex] += Math.pow(instance.fAttr[0]
                                - centroids[cIndex].getY(), 2);
                        // No need to take the root for, as it doesn't affect
                        // the ordering.
                        descDists[cIndex] = smet.dist(instance.fAttr,
                                centroids[cIndex].fAttr);
                        scaleDists[cIndex] = centroids[i].getScale()
                                - ((SIFTVector) instance).getScale();
                    }
                    boolean descending = false;
                    // At index 0 is the closest centroid according to the
                    // respective criterion.
                    int[] colorPriorities = AuxSort.sortIndexedValue(colorDists,
                            descending);
                    int[] xyPriorities = AuxSort.sortIndexedValue(xyDists,
                            descending);
                    int[] descPriorities = AuxSort.sortIndexedValue(descDists,
                            descending);
                    int[] scalePriorities = AuxSort.sortIndexedValue(scaleDists,
                            descending);
                    // Now calculate sums of ranks in order by the centroid
                    // natural ordering in order to do the altered cluster
                    // assignments.
                    float[] centroidRepulsiveness = new float[numClusters];
                    for (int cIndex = 0; cIndex < numClusters; cIndex++) {
                        centroidRepulsiveness[colorPriorities[cIndex]] +=
                                beta * (cIndex + 1);
                        centroidRepulsiveness[xyPriorities[cIndex]] +=
                                (cIndex + 1);
                        centroidRepulsiveness[descPriorities[cIndex]] +=
                                alpha * (cIndex + 1);
                        centroidRepulsiveness[scalePriorities[cIndex]] +=
                                gamma * (cIndex + 1);
                    }
                    // Minimize the weighted sum of the sorted index values
                    // increased by one, so that zeroes don't ignore weights.
                    float currMin = Float.MAX_VALUE;
                    for (int cIndex = 0; cIndex < numClusters; cIndex++) {
                        if (centroidRepulsiveness[cIndex] < currMin) {
                            closestCentroidIndex = cIndex;
                            currMin = centroidRepulsiveness[cIndex];
                        }
                    }
                    if (closestCentroidIndex != clusterAssociations[i]) {
                        noReassignments = false;
                    }
                    clusterAssociations[i] = closestCentroidIndex;
                    error += centroidRepulsiveness[closestCentroidIndex];
                }
                clusters = getClusters();
                clusterCentroidDSet.data = new ArrayList<>(numClusters);
                for (int cIndex = 0; cIndex < numClusters; cIndex++) {
                    centroids[cIndex] = new ClusteredSIFTVector(
                            clusters[cIndex].getCentroid());
                    centroids[cIndex].iAttr[0] = cIndex;
                    centroids[cIndex].setContext(clusterCentroidDSet);
                    clusterCentroidDSet.addDataInstance(centroids[cIndex]);
                }
                errorPrevious = errorCurrent;
                errorCurrent = error;
                if (getIterationIndex() >= MIN_ITER) {
                    if (DataMineConstants.isAcceptableDouble(
                            errorPrevious) && DataMineConstants.
                            isAcceptableDouble(errorCurrent)
                            && (Math.abs(errorCurrent / errorPrevious) - 1f)
                            < ERROR_THRESHOLD) {
                        errorDifferenceSignificant = false;
                    } else {
                        errorDifferenceSignificant = true;
                    }
                }
                if (getIterationIndex() >= MAX_ITER) {
                    noReassignments = true;
                }
            } while (errorDifferenceSignificant && !noReassignments);
        }
    }

    @Override
    public int[] assignPointsToModelClusters(DataSet dset,
            NeighborSetFinder nsfTest) {
        // A dummy method to satisfy the interface, since this is not used in
        // the context of the experiments where it is being run.
        return new int[dset.size()];
    }
}
