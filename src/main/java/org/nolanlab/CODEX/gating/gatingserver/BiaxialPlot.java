package org.nolanlab.CODEX.gating.gatingserver;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BiaxialPlot {

    ColorMapper mapper;
    TableModel tm;

    int size = Config.BIAXIAL_PLOT_SIZE;

    public BufferedImage getImage(int axisX, int axisY){
        int len = tm.getRowCount();
        float [] x = new float[len];
        float [] y = new float[len];

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < x.length; i++) {
            x[i] = (Float) tm.getValueAt(i, axisX);
            minX = Math.min(x[i],minX);
            maxX = Math.max(x[i],maxX);

            y[i] = (Float) tm.getValueAt(i, axisY);
            minY = Math.min(y[i],minY);
            maxY = Math.max(y[i],maxY);
        }

        DefaultTableModel dtm = (DefaultTableModel)tm;

        float spanX = maxX-minX;
        float spanY = maxY-minY;

        //Binning

        int[][] binned = new int [size][size];

        float maxBin = 0;
        for (int i = 0; i < len; i++) {
            int xbin = (int)Math.floor(((x[i]-minX)/spanX)*(size-1));
            int ybin = (int)Math.floor(((y[i]-minY)/spanY)*(size-1));
            if(xbin>=Config.X_MIN && ybin>=Config.Y_MIN){
                binned[xbin][ybin]++;
                maxBin = Math.max(binned[xbin][ybin], maxBin);
            }
        }

        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = (Graphics2D) bi.getGraphics();

        g2.setPaint(Config.BIAXIAL_PLOT_BACKGROUND);

        g2.fillRect(0,0,size, size);

        maxBin = Math.min(maxBin, Config.MAX_DENSITY_BIN_CLIP);

        int[] raster = new int[size*size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if(binned[i][j] > 0) bi.setRGB(i,(size-1)-j, mapper.getColor((float)((binned[i][j]-1)/maxBin)));
            }
        }
        return bi;

    }

    public BiaxialPlot(TableModel tm){
        this.mapper = new ColorMapper();
        this.tm = tm;
    }

    public ColorMapper getMapper() {
        return mapper;
    }

    public void setColorMapper(ColorMapper mapper) {
        this.mapper = mapper;
    }
}

