package org.nolanlab.CODEX.gating.gatingserver;

import com.google.common.collect.Streams;
import dataIO.DatasetStub;
import flowcyt_fcs.ExportFCS;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class GateFilter {

    private final DatasetStub dss;
    private final File src;

    private final double [] scaleMin;
    private final double [] scaleMax;

    private final String [] columnNames;
    private  final int numPlotBins;

    private final int len;
    private final int [][] binnedDs; //transposed

    public int[][] getBinnedDs() {
        return binnedDs;
    }

    public GateFilter(File fcsFile, int numPlotBins){
        dss = DatasetStub.createFromFCS(fcsFile);

        src = fcsFile;
        this.numPlotBins = numPlotBins;
        this.len = (int) dss.getRowCount();

        columnNames = Streams.zip(Arrays.stream(dss.getLongColumnNames()), Arrays.stream(dss.getShortColumnNames()), ((a, b) -> a + " ("+b +")")).toArray(String[]::new);

        scaleMax = Arrays.copyOf(dss.getRow(0), columnNames.length);
        scaleMin = Arrays.copyOf(dss.getRow(0), columnNames.length);

        binnedDs = new int[columnNames.length][(int)dss.getRowCount()];

        for(int i = 0 ; i < dss.getRowCount(); i++){
            double [] row = dss.getRow(i);
            for (int j = 0; j < columnNames.length; j++) {
                scaleMin[j] = Math.min(row[j], scaleMin[j]);
                scaleMax[j] = Math.max(row[j], scaleMax[j]);
            }
        }

        for(int i = 0 ; i < dss.getRowCount(); i++){
            double [] row = dss.getRow(i);
            for (int j = 0; j < columnNames.length; j++) {
                binnedDs[j][i] = (int)(((numPlotBins-1)*(row[j]-scaleMin[j]))/(scaleMax[j]-scaleMin[j]));
            }
        }

    }

    public void setGate(int X, int Y, Polygon polygon, String gateName){
        ArrayList<float[]> evt = new ArrayList<>();

        for (int i = 0; i < len; i++) {
            int xbin = binnedDs[X][i];
            int ybin = binnedDs[Y][i];
            if(polygon.contains(new Point(xbin,ybin))){
                double[] row = dss.getRow(i);
                float[] rowF = new float[row.length];
                for (int j = 0; j < row.length; j++) {
                    rowF[j] = (float)row[j];
                }
                evt.add(rowF);
            }
        }

        new ExportFCS().writeFCSAsFloat(src.getAbsolutePath().replace(".fcs","")+"_gate-" + gateName + ".fcs", evt.toArray(new float[evt.size()][]), dss.getShortColumnNames(), dss.getLongColumnNames());
    }

    public BufferedImage getPlot(int X, int Y, ColorMapper mapper){
        int size = numPlotBins;
        int[][] binnedImg = new int [size][size];
        float maxBin = 0;
        for (int i = 0; i < len; i++) {
            int xbin = binnedDs[X][i];
            int ybin = binnedDs[Y][i];
            binnedImg[xbin][ybin]++;
            maxBin = Math.max(binnedImg[xbin][ybin], maxBin);
        }

        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = (Graphics2D) bi.getGraphics();

        g2.setPaint(Config.BIAXIAL_PLOT_BACKGROUND);

        g2.fillRect(0,0,size, size);

        maxBin = Math.min(maxBin, Config.MAX_DENSITY_BIN_CLIP);


        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if(binnedImg[i][j] > 0) bi.setRGB(i,(size-1)-j, mapper.getColor((binnedImg[i][j]-1)/maxBin));
            }
        }
        return bi;
    }
}
