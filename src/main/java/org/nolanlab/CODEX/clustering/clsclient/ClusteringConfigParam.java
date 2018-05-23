package org.nolanlab.CODEX.clustering.clsclient;

import java.io.File;

public class ClusteringConfigParam {

    private File clusteringDir;
    private String clusteringName;
    private String gateName;
    private String clustCols;
    private int limitEvents;
    private String transformation;
    private int scalingFactor;
    private double noiseThreshold;
    private String rescale;
    private double quantile;
    private boolean rescaleSeparately;

    public File getClusteringDir() {
        return clusteringDir;
    }

    public void setClusteringDir(File clusteringDir) {
        this.clusteringDir = clusteringDir;
    }

    public String getClusteringName() {
        return clusteringName;
    }

    public void setClusteringName(String clusteringName) {
        this.clusteringName = clusteringName;
    }

    public String getGateName() {
        return gateName;
    }

    public void setGateName(String gateName) {
        this.gateName = gateName;
    }

    public String getClustCols() {
        return clustCols;
    }

    public void setClustCols(String clustCols) {
        this.clustCols = clustCols;
    }

    public int getLimitEvents() {
        return limitEvents;
    }

    public void setLimitEvents(int limitEvents) {
        this.limitEvents = limitEvents;
    }

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    public int getScalingFactor() {
        return scalingFactor;
    }

    public void setScalingFactor(int scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    public double getNoiseThreshold() {
        return noiseThreshold;
    }

    public void setNoiseThreshold(double noiseThreshold) {
        this.noiseThreshold = noiseThreshold;
    }

    public String getRescale() {
        return rescale;
    }

    public void setRescale(String rescale) {
        this.rescale = rescale;
    }

    public double getQuantile() {
        return quantile;
    }

    public void setQuantile(double quantile) {
        this.quantile = quantile;
    }

    public boolean isRescaleSeparately() {
        return rescaleSeparately;
    }

    public void setRescaleSeparately(boolean rescaleSeparately) {
        this.rescaleSeparately = rescaleSeparately;
    }
}
