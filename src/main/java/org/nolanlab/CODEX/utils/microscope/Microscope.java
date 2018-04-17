package org.nolanlab.CODEX.utils.microscope;


import org.nolanlab.CODEX.uploader.uplclient.ExperimentView;

import java.io.File;

/**
 *
 * @author Vishal
 */
public interface Microscope {

    void guessZSlices(File dir, ExperimentView experimentView);
    void guessChannelNamesAndWavelength(File dir, ExperimentView experimentView);
    void guessCycleRange(File dir, ExperimentView experimentView);
    boolean isTilesAProductOfRegionXAndY(File dir, ExperimentView experimentView);
    int getMaxCycNumberFromFolder(File dir);

}
