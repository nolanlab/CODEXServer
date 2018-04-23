package org.nolanlab.CODEX.segm.segmserver;

import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.plugin.FolderOpener;
import ij.plugin.HyperStackConverter;
import ij.plugin.ImageCalculator;
import ij.process.ImageProcessor;
import org.nolanlab.CODEX.segm.segmclient.SegConfigParam;
import org.nolanlab.CODEX.uploader.uplclient.Experiment;
import org.nolanlab.CODEX.utils.codexhelper.ExperimentHelper;
import org.nolanlab.CODEX.utils.codexhelper.SegmHelper;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class RunSegm {

    private SegmHelper segmHelper = new SegmHelper();
    private String timestamp;
    private int totalFolder;
    private int progress = 0;

    public void runSeg(SegConfigParam segParam, String ts) throws Exception {
        timestamp = ts;

        File rootDir = segParam.getRootDir();
        if (!rootDir.exists()) {
            throw new IllegalArgumentException("Error: Cannot find the input directoty");
        }

        File tilesDir = new File(rootDir + File.separator + "tiles");
        File[] regFolder = tilesDir.listFiles(r -> r.isDirectory() && r.getName().startsWith("reg"));
        if (regFolder != null && regFolder.length != 0) {
            totalFolder = regFolder.length;
            File expJSON = new File(segParam.getRootDir().getParentFile() + File.separator + "Experiment.json");
            ExperimentHelper experimentHelper = new ExperimentHelper();
            Experiment exp = experimentHelper.loadFromJSON(expJSON);
            for (int reg = 0; reg < regFolder.length; reg++) {
                if(segParam != null) {
                    FolderOpener fo = new FolderOpener();
                    fo.openAsVirtualStack(true);
                    ImagePlus imp = fo.openFolder(regFolder[reg].getPath());
                    ImagePlus hyp = HyperStackConverter.toHyperStack(imp, exp.getChannel_names().length, exp.getNum_z_planes(), exp.getNum_cycles(), "default", "Composite");
                    segmentTiff(hyp, 0, segParam);
                    this.setProgress(calculateProgress(reg+1));
                    if(reg+1 == totalFolder) {
                        progress=0;
                    }
                }
            }
        }
        else {
            throw new IllegalStateException("No image sequence folders found. Run uploader and try again!");
        }
        File segJsonOut = new File(rootDir.getAbsolutePath()+File.separator+"segm"+File.separator+"segm_"+timestamp + File.separator + "segmConfig.json");
        segmHelper.saveToFile(segParam, segJsonOut);
    }

    private void segmentTiff(ImagePlus imp, int tile, SegConfigParam segConfigParam) throws Exception {
        Duplicator dup = new Duplicator();
        int j;
        int i;
        ++tile;

        System.out.print("\nprocessing image seq: " + imp.getTitle() + "\n");

        int[] readoutChannels = new int[imp.getNChannels()];
        for (int x = 0; x < imp.getNChannels(); x++) {
            readoutChannels[x] = x + 1;
        }

        imp.getNFrames();
        ImagePlus nucl = dup.run(imp, segConfigParam.getNuclearStainChannel(), segConfigParam.getNuclearStainChannel(), 1, imp.getNSlices(), segConfigParam.getNuclearStainCycle(), segConfigParam.getNuclearStainCycle());
        ImagePlus mult = nucl;
        if (Math.min(segConfigParam.getMembraneStainCycle(), segConfigParam.getMembraneStainChannel()) > 0) {
            System.out.println("Multiplying by membrane");
            ImagePlus memb = dup.run(imp, segConfigParam.getMembraneStainChannel(), segConfigParam.getMembraneStainChannel(), 1, imp.getNSlices(), segConfigParam.getMembraneStainCycle(), segConfigParam.getMembraneStainCycle());
            ImageStack membS = memb.getImageStack();
            if (segConfigParam.isDont_inverse_memb()) {
                System.out.println("Bypassing membrane inversion");
            } else {
                for (int i2 = 1; i2 <= membS.getSize(); ++i2) {
                    ImageProcessor ip = membS.getProcessor(i2);
                    ip.invert();
                }
            }
            ImageCalculator ic = new ImageCalculator();
            mult = segConfigParam.isDont_inverse_memb() ? ic.run("Add stack float create", nucl, memb) : ic.run("Multiply stack float create", nucl, memb);
            memb = null;
        }
        nucl = null;

        System.gc();
        FFTFilter filter = new FFTFilter();
        filter.setup(10000.0, (double) segConfigParam.getRadius(), 0, 5.0, false, false, false);
        System.out.print("running FFT bandpass filter");
        filter.run(mult);

        //GaussianBlur3D.blur(mult, radius, radius, radius);
        SegmentedObject[] cellsSegmentedObject = MaximaFinder3D.findCellsByIntensityGradient((ImagePlus) mult, segConfigParam.getRadius(), (double) segConfigParam.getMaxCutoff(), (double) segConfigParam.getMinCutoff(), (double) segConfigParam.getRelativeCutoff(), (boolean) segConfigParam.isShowImage(), segConfigParam.isSubtractInnerRing() ? 1.0 : segConfigParam.getInner_ring_size());
        if (segConfigParam.isSubtractInnerRing()) {
            segConfigParam.setUse_membrane(false);
        }
        SegmentedObject[] innerRings = null;

        if (segConfigParam.isSubtractInnerRing()) {
            innerRings = MaximaFinder3D.findCellsByIntensityGradient((ImagePlus) mult, segConfigParam.getRadius(), (double) segConfigParam.getMaxCutoff(), (double) segConfigParam.getMinCutoff(), (double) segConfigParam.getInner_ring_size(), (boolean) segConfigParam.isShowImage(), 1.0);
        }

        if (segConfigParam.isShowImage()) {
            try {
                new ImageJ();
                mult.show();
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        //Filter out small sized regions and remove that row from the txt file
        double sizeCutoff = (segConfigParam.getRadius() * segConfigParam.getRadius() * segConfigParam.getRadius()) * Math.PI * (4.0 / 3.0);
        cellsSegmentedObject = Arrays.stream(cellsSegmentedObject).filter(c -> c.getPoints().length >= sizeCutoff).toArray(SegmentedObject[]::new);
        BufferedImage[] bi2 = null;

        File segOut = new File(segConfigParam.getRootDir()+File.separator+"segm"+File.separator+"segm_"+timestamp);
        if(!segOut.exists() && !segOut.isDirectory()) {
            segOut.mkdirs();
        }

        File masksOut = new File(segOut + File.separator + "masks");
        if(!masksOut.exists() && !masksOut.isDirectory()) {
            masksOut.mkdirs();
        }

        File fcsOut = new File(segOut + File.separator + "FCS");
        if(!fcsOut.exists() && !fcsOut.isDirectory()) {
            fcsOut.mkdirs();
        }

        File compensatedOut = new File(fcsOut + File.separator + "compensated");
        if(!compensatedOut.exists() && !compensatedOut.isDirectory()) {
            compensatedOut.mkdirs();
        }

        File uncompensatedOut = new File(fcsOut + File.separator + "uncompensated");
        if(!uncompensatedOut.exists() && !uncompensatedOut.isDirectory()) {
            uncompensatedOut.mkdirs();
        }


        File outdir = new File(masksOut+File.separator+imp.getTitle());
        if(!outdir.exists()) {
            outdir.mkdirs();
        }

        RegionImageWriter.writeRegionImage(cellsSegmentedObject, mult, imp.getTitle(), outdir);

        File dir = segConfigParam.getRootDir();
        if(dir == null) {
            throw new IllegalStateException("The root directory for segmentation is not right!");
        }

        int numFrames = imp.getNFrames();
        String title = imp.getTitle();
        if (cellsSegmentedObject.length == 0) {
            System.out.println("Didn't find any cells here. exiting");

            BufferedWriter bwUncomp = new BufferedWriter(new FileWriter(new File(uncompensatedOut + File.separator + title) + "_Expression_Uncompensated.txt"));
            BufferedWriter bwComp = new BufferedWriter(new FileWriter(new File(compensatedOut + File.separator + title) + "_Expression_Compensated.txt"));

            for (BufferedWriter bw : new BufferedWriter[]{bwUncomp, bwComp}) {
                bw.write("cell_id\ttile_nr\tX\tY\tZ\tsize");
                for (int i3 = 1; i3 <= numFrames; ++i3) {
                    for (int j2 = 0; j2 < readoutChannels.length; ++j2) {
                        bw.write("\tCyc_" + i3 + "_ch_" + readoutChannels[j2]);
                    }
                }
                for (int k = 0; k < segConfigParam.getConcentricCircles(); k++) {
                    for (int i3 = 1; i3 <= numFrames; ++i3) {
                        for (int j2 = 0; j2 < readoutChannels.length; ++j2) {
                            bw.write("\tCircle_" + k + "_Cyc_" + i3 + "_ch_" + readoutChannels[j2]);
                        }
                    }
                }
                bw.write("\n");
            }

            BufferedWriter bwGN = new BufferedWriter(new FileWriter(new File(fcsOut + File.separator + title) + "_GabrielGraph.txt"));

            bwGN.write("");
            bwGN.flush();
            bwGN.close();
            //continue;
        }

        int w = mult.getWidth();
        int h = mult.getHeight();
        int d = mult.getStackSize();
        mult = null;
        System.gc();
        System.out.println("Computing region intensities " + (segConfigParam.isUse_membrane() ? "by membrane" : "by whole cell"));
        double[][] regionIntensities = new double[cellsSegmentedObject.length][(imp.getNFrames() * readoutChannels.length)];
        ImagePlus mem = null;
        if (segConfigParam.isUse_membrane()) {
            mem = dup.run(imp, segConfigParam.getMembraneStainChannel(), segConfigParam.getMembraneStainChannel(), 1, imp.getNSlices(), segConfigParam.getMembraneStainCycle(), segConfigParam.getMembraneStainCycle());
        }
        for (int cycle = 1; cycle <= imp.getNFrames(); ++cycle) {
            for (int ch = 0; ch < readoutChannels.length; ++ch) {
                System.out.println("Cycle:" + cycle + ", channel" + readoutChannels[ch]);
                ImagePlus readout = dup.run(imp, readoutChannels[ch], readoutChannels[ch], 1, imp.getNSlices(), cycle, cycle);

                double[] intens = null;
                if (!segConfigParam.isCount_puncta()) {
                    intens = segConfigParam.isUse_membrane() ? Segmentation.computeMembraneIntensityOfRegions((ImageStack) readout.getImageStack(), (ImageStack) mem.getImageStack(), (SegmentedObject[]) cellsSegmentedObject) : Segmentation.computeMeanIntensityOfRegions((ImageStack) readout.getImageStack(), (SegmentedObject[]) cellsSegmentedObject);
                    if (segConfigParam.isSubtractInnerRing()) {
                        double[] innerRingIntens = Segmentation.computeMeanIntensityOfRegions((ImageStack) readout.getImageStack(), (SegmentedObject[]) innerRings);
                        for (int i4 = 0; i4 < intens.length; ++i4) {
                            intens[i4] -= innerRingIntens[i4];
                        }
                    }
                } else {
                    System.out.println("Counting puncta:");
                    intens = Segmentation.computePunctaCountOfRegions(readout, (SegmentedObject[]) cellsSegmentedObject, 100);
                }

                for (int i4 = 0; i4 < intens.length; ++i4) {
                    regionIntensities[i4][(cycle - 1) * readoutChannels.length + ch] = intens[i4];
                }
            }
        }

        ProfileAverager[][] pa = new ProfileAverager[cellsSegmentedObject.length][segConfigParam.getConcentricCircles()];

        System.out.println("Featurizing circles:" + segConfigParam.getConcentricCircles());
        for (int ci = 0; ci < segConfigParam.getConcentricCircles(); ci++) {
            System.out.println("Circle#" + (ci + 1) + ":" + Math.pow((segConfigParam.getRadius() * 2), 1 + (ci / 3.0)));
        }
        for (int r = 0; r < cellsSegmentedObject.length; ++r) {
            Point3D cent = cellsSegmentedObject[r].getCenter();
            for (int k = 0; k < cellsSegmentedObject.length; ++k) {
                double dist = Segmentation.dist(cent, cellsSegmentedObject[k].getCenter());
                for (int ci = 0; ci < segConfigParam.getConcentricCircles(); ci++) {
                    if (pa[r][ci] == null) {
                        pa[r][ci] = new ProfileAverager();
                    }
                    if (dist < Math.pow((segConfigParam.getRadius() * 2), 1 + (ci / 2.0))) {
                        pa[r][ci].addProfile(regionIntensities[k]);
                    }
                }
            }
        }

        double[][] featurizedVec = new double[cellsSegmentedObject.length][0];

        for (int r = 0; r < cellsSegmentedObject.length; ++r) {
            for (int ci = 0; ci < segConfigParam.getConcentricCircles(); ci++) {
                double[] avg = pa[r][ci].count > 0 ? pa[r][ci].getAverage() : new double[(imp.getNFrames() * readoutChannels.length)];
                featurizedVec[r] = MatrixOp.concat(featurizedVec[r], avg);
            }
        }

        imp = null;
        System.gc();
        ArrayList<Cell> cellsForTile = new ArrayList<>();

        for (i = 0; i < regionIntensities.length; ++i) {
            Cell c = new Cell(i + 1, cellsSegmentedObject[i], tile, regionIntensities[i], featurizedVec[i]);
            cellsForTile.add(c);
        }
        regionIntensities = new double[cellsForTile.size()][];
        for (i = 0; i < regionIntensities.length; ++i) {
            regionIntensities[i] = ((Cell) cellsForTile.get(i)).getExpressionVector();
        }
        Cell[] cellArr = cellsForTile.toArray(new Cell[cellsForTile.size()]);
        System.out.print("Building adj graph");
        double[][] adjN = Neighborhood.buildAdjacencyMatrix(cellArr, w, h, d, true);
        cellsForTile.clear();
        System.out.println("Compensating:");
        double[][] compRegionIntensities = Segmentation.compensatePositionalSpilloverOfExpressionMtx(cellsSegmentedObject, adjN, regionIntensities);
        for (int dt = 0; dt < compRegionIntensities.length; ++dt) {
            int id = dt + 1;
            Cell c = new Cell(id, cellsSegmentedObject[dt], tile, compRegionIntensities[dt], featurizedVec[dt]);
            cellsForTile.add(c);
        }
        Cell[] compCellArray = cellsForTile.toArray(new Cell[cellsForTile.size()]);
        adjN = null;
        System.gc();

        BufferedWriter bwUncomp = new BufferedWriter(new FileWriter(new File(uncompensatedOut + File.separator + title) + "_Expression_Uncompensated.txt"));;
        BufferedWriter bwComp = new BufferedWriter(new FileWriter(new File(compensatedOut +File.separator + title) + "_Expression_Compensated.txt"));;

        for (BufferedWriter bw22 : new BufferedWriter[]{bwUncomp, bwComp}) {
            bw22.write("cell_id\ttile_nr\tX\tY\tZ\tsize");
            for (int i6 = 1; i6 <= numFrames; ++i6) {
                for (j = 0; j < readoutChannels.length; ++j) {
                    bw22.write("\tCyc_" + i6 + "_ch_" + readoutChannels[j]);
                }
            }
            for (int k = 0; k < segConfigParam.getConcentricCircles(); k++) {
                for (int i3 = 1; i3 <= numFrames; ++i3) {

                    for (int j2 = 0; j2 < readoutChannels.length; ++j2) {
                        bw22.write("\tCircle_" + k + "_Cyc_" + i3 + "_ch_" + readoutChannels[j2]);
                    }
                }
            }
            bw22.write("\n");
        }
        for (BufferedWriter bw : new BufferedWriter[]{bwUncomp, bwComp}) {
            Cell[] cells = bw == bwUncomp ? cellArr : compCellArray;
            j = cells.length;
            for (int k = 0; k < j; ++k) {
                Cell c = cells[k];
                bw.write("" + c.getId() + "\t");
                bw.write("" + c.getTile() + "\t");
                bw.write("" + c.getSegmentedObject().getCenter().x + "\t");
                bw.write("" + c.getSegmentedObject().getCenter().y + "\t");
                bw.write("" + c.getSegmentedObject().getCenter().z + "\t");
                bw.write("" + c.getSegmentedObject().getPoints().length + "\t");
                double[] ri = MatrixOp.concat(c.getExpressionVector(), c.getNeighFeaturizationVec());
                for (int ch = 0; ch < ri.length; ++ch) {
                    bw.write(String.valueOf(ri[ch]));
                    bw.write("\t");
                }

                bw.newLine();
            }
        }
        for (BufferedWriter bw22 : new BufferedWriter[]{bwUncomp, bwComp}) {
            bw22.flush();
            bw22.close();
        }
        if (segConfigParam.isDelaunay_graph()) {
            System.out.println("Computing Delaunay graph:");
            Collection<Cell>[] gn = Neighborhood.findDelaunayNeighbors(cellArr, (int) w, (int) h, (int) d);

            BufferedWriter bwGN = new BufferedWriter(new FileWriter(fcsOut + File.separator + "_DelaunayGraph.txt"));

            for (int i7 = 0; i7 < gn.length; ++i7) {
                for (Cell cell : gn[i7]) {
                    bwGN.write("" + cellArr[i7].getId() + "\t" + cell.getId() + "\n");
                    bwGN.write("" + cell.getId() + "\t" + cellArr[i7].getId() + "\n");
                }
            }
            bwGN.flush();
            bwGN.close();

            gn = null;
            System.gc();
        }
    }

    private int calculateProgress(int reg) {
        return reg*100/totalFolder;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getTotalFolder() {
        return totalFolder;
    }

    public void setTotalFolder(int totalFolder) {
        this.totalFolder = totalFolder;
    }
}
