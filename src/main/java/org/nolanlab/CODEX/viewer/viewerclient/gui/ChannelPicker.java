package org.nolanlab.CODEX.viewer.viewerclient.gui;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import ij.process.LUT;
import org.apache.commons.io.FilenameUtils;
import org.nolanlab.CODEX.viewer.viewerclient.i5d.MultiCompositeImage;

import javax.swing.*;


import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * @author Vishal
 *
 */
public class ChannelPicker extends JPanel {

	protected Panel subPanel;
	private Checkbox redCb;
	private Checkbox greenCb;
	private Checkbox blueCb;
	private RangeSlider slider;
	private Label channelName;
	private Label channelParametersLabel;
	private Label opacityLabel;
	private JSlider opacitySlider;
	private MultichannelViewerWindow win;
	private MultiCompositeImage img;
	private int currentChannel, nChannels;


	public ChannelPicker(MultichannelViewerWindow win) {
		super();

		this.win = win;
		this.img = (MultiCompositeImage) win.getImagePlus();

		currentChannel = img.getC();
		nChannels = img.getNChannels();

		//Initialize all channels to black color when loading the i5d image on viewer. 
		for (int i = 0; i < img.getNChannels(); i++) {
			img.setActiveChannel(i,false,false);
		}

		//Handling of Key events
		addKeyListener(win);
		addKeyListener(IJ.getInstance());


		Checkbox arr[][] = new Checkbox[img.getNChannels()][3];
		
		//Layout preferences
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		//Initialize all components & overlay if present
		initComponents(arr);
		initOverlay();
        img.updateAndDraw();
    }



	
	/**
	 * If overlay is present, add to the image5d object
	 * Create a new slider to adjust opacity of overlay object
	 */
	private void initOverlay() {

		if(true)return;
		Overlay overlay = new Overlay();

		if(img != null) {
			File dir = null; //new File(i5d.getFileLocation());
			File[] regFiles = dir.getParentFile().listFiles(t -> t.getName().contains("regions_"+ FilenameUtils.removeExtension(dir.getName())));
			if(regFiles != null && regFiles.length != 0) {				
				opacityLabel = new Label();
				opacityLabel.setText("Adjust overlay opacity");
				opacityLabel.setPreferredSize(new Dimension(25, 10));
				this.add(opacityLabel);
				opacitySlider = new JSlider();
				opacitySlider.setToolTipText("Overlay opacity");
				opacitySlider.setMinimum(0);
				opacitySlider.setMaximum(10);
				opacitySlider.setValue(10);
				opacitySlider.setPreferredSize(new Dimension(100, 25));
				opacitySlider.setOpaque(false);
				for(int z = 0; z < regFiles.length; z++) {
					ImagePlus im2 = IJ.openImage(regFiles[z].getPath());
					ImageRoi imgRoi = new ImageRoi(0, 0, im2.getProcessor());
					imgRoi.setNonScalable(true);
					imgRoi.setZeroTransparent(true);
					imgRoi.setOpacity(1);
					imgRoi.setPosition(z+1);
					overlay.add(imgRoi);
				}
				img.setOverlay(overlay);
				
				opacitySlider.addChangeListener(e -> {
					JSlider sl = (JSlider) e.getSource();
					es.submit(new Runnable() {
						@Override
						public void run() {
							Overlay ov = img.getOverlay();
							for(int z=0; z<img.getNSlices(); z++) {
								ImageRoi imgRoi = (ImageRoi)ov.get(z+1);
								imgRoi.setOpacity((double)(sl.getValue())/100);
								ov.add(imgRoi);
							}
							img.setOverlay(ov);
							//i5d.updateAndRepaintWindow();
						}
					});
				});
				this.add(opacitySlider);
		  	}
		}
	}
	

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(350, img.getNChannels() * 30);
	}

	ExecutorService es = Executors.newFixedThreadPool(1);

	private void initComponents(Checkbox arr[][]) {
		channelParametersLabel = new Label();
		channelParametersLabel.setText("Channel Parameters");
		channelParametersLabel.setPreferredSize(new Dimension(25, 10));
		this.add(channelParametersLabel);

		for (int i = 0; i < img.getNChannels(); i++) {
			Panel smallP = new Panel();
			
			//Checkboxes for RGB
			redCb = new Checkbox();
			greenCb = new Checkbox();
			blueCb = new Checkbox();

			redCb.setLabel("R");
			greenCb.setLabel("G");
			blueCb.setLabel("B");

			//Number of checkboxes based on input image channels
			arr[i][0] = redCb;
			arr[i][1] = greenCb;
			arr[i][2] = blueCb;

			final int j = i;
			
			//Item listeners to capture select and deselect of RGB checkboxes
			redCb.addItemListener(e -> {
                boolean selected = e.getStateChange() == ItemEvent.SELECTED;
                img.setActiveChannel(j,selected,true);
                if (selected) {
					adjustRowColors(arr, j, 0);
					adjustColumnColors(arr, j, 0);
					img.setChannelLut(LUT.createLutFromColor(Color.RED), j+1);
				}
				img.updateAndRepaintWindow();
			});

			greenCb.addItemListener(e -> {
			    boolean selected = e.getStateChange() == ItemEvent.SELECTED;
                img.setActiveChannel(j,selected,true);
				if (selected) {
					adjustRowColors(arr, j, 1);
					adjustColumnColors(arr, j, 1);
                    img.setChannelLut(LUT.createLutFromColor(Color.GREEN), j+1);
				}
				img.updateAndRepaintWindow();
			});

			blueCb.addItemListener(e -> {
                boolean selected = e.getStateChange() == ItemEvent.SELECTED;
                img.setActiveChannel(j,selected,true);
                if (selected) {
                    adjustRowColors(arr, j, 2);
					adjustColumnColors(arr, j, 2);
                    img.setChannelLut(LUT.createLutFromColor(Color.BLUE), j+1);
				}
				img.updateAndRepaintWindow();
			});
			
			//Add a range slider after checkbox
			slider = new RangeSlider();
			slider.setMinimum(0);
			slider.setMaximum((int) Math.pow(2, img.getBitDepth()) - 1);
			//img.setPosition(j + 1, img.getSlice(), img.getFrame());
			img.resetDisplayRange();
			slider.setValue((int) img.getDisplayRangeMin());
			slider.setUpperValue((int) img.getDisplayRangeMax());
			slider.setPreferredSize(new Dimension(250,20));
			slider.setOpaque(false);

			slider.addChangeListener(e -> {
//				if (slider.getValueIsAdjusting())
//					return;
				RangeSlider rs = (RangeSlider) e.getSource();
				es.submit(new Runnable() {
					@Override
					public void run() {
						img.setC(j + 1);
                        img.setDisplayRange(rs.getValue(), rs.getUpperValue());
                        img.updateAndRepaintWindow();
					}
				});
			});

			smallP.setLayout(new BoxLayout(smallP, BoxLayout.X_AXIS));
			smallP.add(redCb);
			smallP.add(greenCb);
			smallP.add(blueCb);
			smallP.add(slider);
			smallP.setPreferredSize(new Dimension(300, 5));
			this.add(smallP);
		}

        this.setPreferredSize(new Dimension(300,arr.length*20));
        this.setMinimumSize(new Dimension(300,arr.length*20));
	}

	/**
	 * Update the values of checkbox selection based on row
	 * @param matrix
	 * @param r
	 * @param c
	 */
	public void adjustRowColors(Checkbox[][] matrix, int r, int c) {
		for (int k = 0; k < img.getNChannels(); k++) {
			if (matrix[k][c].getState() && r != k) {
				matrix[k][c].setState(false);
				img.setActiveChannel(k,false, false);
			}
		}
		img.updateAndDraw();
	}

	/**
	 * Update the values of checkbox selection based on col 
	 * @param matrix
	 * @param r
	 * @param c
	 */
	public void adjustColumnColors(Checkbox[][] matrix, int r, int c) {
		for (int k = 0; k < 3; k++) {
			if (matrix[r][k].getState() && c != k) {
				matrix[r][k].setState(false);
			}
		}
	}


	/**
	 * Override non used method from ChannelControl and do nothing
	 */
	public void setColor(Color c) {

	}
	
	public Label getChannelParametersLabel() {
		return channelParametersLabel;
	}
	
	public Label getOpacityLabel() {
		return opacityLabel;
	}
	
	public JSlider getOpacitySlider() {
		return opacitySlider;
	}
	
	
}
