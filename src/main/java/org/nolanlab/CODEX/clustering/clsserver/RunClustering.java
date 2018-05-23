package org.nolanlab.CODEX.clustering.clsserver;

import org.apache.commons.io.FileUtils;
import org.nolanlab.CODEX.clustering.clsclient.ClusteringConfigParam;
import org.nolanlab.CODEX.utils.codexhelper.ClusteringHelper;
import org.nolanlab.CODEX.utils.codexhelper.ExperimentHelper;
import org.nolanlab.CODEX.utils.logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunClustering {

    private ClusteringHelper clusteringHelper = new ClusteringHelper();
    private int totalFolder;
    private int progress = 0;
    private ExperimentHelper expHelper = new ExperimentHelper();

    public void runClustering(ClusteringConfigParam clusteringConfigParam) throws Exception {
        File clusteringDir = clusteringConfigParam.getClusteringDir();
        if (!clusteringDir.exists()) {
            throw new IllegalArgumentException("Error: Cannot find the input directoty");
        }
        File clusteringJsonOut = new File(clusteringDir.getAbsolutePath() + File.separator + "clusteringConfig.json");
        clusteringHelper.saveToFile(clusteringConfigParam, clusteringJsonOut);

        File dir = clusteringConfigParam.getClusteringDir().getParentFile();

        List<String> lines = Arrays.asList("clustering_columns=" + clusteringConfigParam.getClustCols(), "limit_events_per_file=" + clusteringConfigParam.getLimitEvents(), "transformation=" + clusteringConfigParam.getTransformation().toUpperCase(),
                "scaling_factor=" + clusteringConfigParam.getScalingFactor(), "noise_threshold=" + clusteringConfigParam.getNoiseThreshold(),
                "euclidian_length_threshold=1", "rescale=" + clusteringConfigParam.getRescale().toUpperCase(), "quantile=" + clusteringConfigParam.getQuantile(),
                "rescale_separately=" + clusteringConfigParam.isRescaleSeparately());
        Path file = Paths.get(dir.getCanonicalPath() + File.separator + "importConfig.txt");
        Files.write(file, lines, Charset.forName("UTF-8"));

        String libConfig = System.getProperty("user.dir") + File.separator + "lib";
        File vortexJar = new File(libConfig + File.separator + "VorteX.jar");

        createFcsFileListTxt(dir, clusteringConfigParam);
        copyFile(vortexJar, dir);

        ProcessBuilder pb1 = new ProcessBuilder("cmd", "/C start /B /belownormal java -Xms5G -Xmx48G -Xmn50m -cp " + dir + File.separator + "VorteX.jar standalone.Xshift");
        pb1.directory(dir);
        pb1.redirectErrorStream(true);
        pb1.redirectOutput();
        pb1.redirectError();
        Process proc = pb1.start();

        System.out.println("Starting process: " + pb1.command().toString());
        expHelper.waitAndPrint(proc);

        System.out.println("X-Shift done");

        File impF = new File(dir.getCanonicalPath() + File.separator + "importConfig.txt");
        File fcsF = new File(dir.getCanonicalPath() + File.separator + "fcsFileList.txt");
        File vorJ = new File(dir.getCanonicalPath() + File.separator + "VorteX.jar");
        removeFile(impF);
        removeFile(fcsF);
        removeFile(vorJ);

        File outDir = new File(dir + File.separator + "out");
        if(outDir.exists() && outDir.isDirectory()) {
            FileUtils.copyDirectoryToDirectory(outDir, clusteringDir);
            removeFile(outDir);
        }
    }

    public static void copyFile(File srcFile, File destDir) throws IOException {
        FileUtils.copyFileToDirectory(srcFile, destDir);
    }

    public static void createFcsFileListTxt(File dir, ClusteringConfigParam clusteringConfigParam) throws IOException{
        File[] fcsFiles = getFcsFiles(dir);
        String compensated = "compensated";
        List<String> fcsLines = new ArrayList<>();
        for(File aFcsFile : fcsFiles) {
            if(aFcsFile.getName().toLowerCase().contains(compensated)) {
                if(aFcsFile.getName().toLowerCase().contains(clusteringConfigParam.getGateName().toLowerCase())) {
                    fcsLines.add(aFcsFile.getAbsolutePath());
                }
                else if(clusteringConfigParam.getGateName().toLowerCase().equals("ungated")) {
                    if(!aFcsFile.getName().toLowerCase().contains("-")) {
                        fcsLines.add(aFcsFile.getAbsolutePath());
                    }
                }
            }
        }
        Path fcsFile = Paths.get(dir.getCanonicalPath() + File.separator + "fcsFileList.txt");
        Files.write(fcsFile, fcsLines, Charset.forName("UTF-8"));
        logger.print("The fcsFileList.txt file was created successfully!");
    }

    private static File[] getFcsFiles(File dir) {
        File[] fcsFiles = null;
        if(dir != null) {
            fcsFiles = dir.listFiles(f -> f.getName().endsWith(".fcs"));
        }
        return fcsFiles;
    }

    public static void removeFile(File dir) {
        if(dir != null) {
            if (dir.delete()) {
                logger.print("Removing the newly created file "+dir.getName());
            }
        }
    }
}
