package org.nolanlab.CODEX.viewer.viewerclient.gui;

import ij.WindowManager;
import ij.gui.ImageLayout;
import ij.gui.ScrollbarWithLabel;
import ij.gui.StackWindow;
import ij.gui.TextRoi;
import org.nolanlab.CODEX.viewer.viewerclient.i5d.MultiCompositeImage;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;


public class MultichannelViewerWindow extends StackWindow  implements KeyListener{

        JFrame frame = new JFrame();
        JDesktopPane dp = new JDesktopPane();

        ArrayList<ij.gui.ScrollbarWithLabel> scrollbars = new ArrayList<>();

        public MultichannelViewerWindow(MultiCompositeImage img){
           super(img);

            dp.putClientProperty("JDesktopPane.dragMode", "faster");
            dp.setBorder(new LineBorder(Color.red,5));

           for(Component c: this.getComponents()){
               System.out.println(c.toString());
               if(c instanceof ij.gui.ScrollbarWithLabel){
                   System.out.println("Adding scrollbar:"+c);
                   scrollbars.add((ij.gui.ScrollbarWithLabel)c);
               }
           }

           frame.setBounds(this.getBounds());
           this.setVisible(false);



           GridBagConstraints gbc = new GridBagConstraints();
           gbc.anchor= GridBagConstraints.NORTHEAST;
           gbc.weighty = 1.0;
           gbc.weightx=1.0;
           gbc.fill=GridBagConstraints.BOTH;
           JInternalFrame if1 = createInternalFrame(ic, img.getTitle(), gbc);
           if1.setResizable(true);

           ImageLayout imageLayout = new ImageLayout(ic);



            if1.setLayout(imageLayout);
            imageLayout.ignoreNonImageWidths(true);
            updateImageLayout(imageLayout,if1);
            if1.setDoubleBuffered(true);


            //imageLayout.layoutContainer(if1.getContentPane());

            if1.addComponentListener(
                   new ComponentAdapter() {
                       @Override
                       public void componentResized(ComponentEvent e) {
                           ic.setBounds(0,0,if1.getWidth(),if1.getHeight());
                           updateImageLayout(imageLayout,if1);
                           updateImageLayout(imageLayout,if1);
                       }
                   }
           );

            ic.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    ic.setBounds(0,0,if1.getWidth(),if1.getHeight());
                    updateImageLayout(imageLayout,if1);
                    updateImageLayout(imageLayout,if1);
                }
            });



            ic.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    if(e.getKeyChar()=='-'||e.getKeyChar()=='+'||e.getKeyChar()=='='||e.getKeyChar()=='_') {
                        //System.out.println(e.getKeyChar());

                        ic.setBounds(0, 0, if1.getWidth(), if1.getHeight());
                        updateImageLayout(imageLayout, if1);
                        updateImageLayout(imageLayout, if1);
                   }
                }
            });

            for (ScrollbarWithLabel sc : scrollbars) {
                if1.add(sc);
            }

           dp.add(if1);
            if1.setVisible(true);
            if1.pack();
            //if1.setBounds(100,100,ic.getPreferredSize().width,ic.getPreferredSize().height);

            JInternalFrame if2 = new JInternalFrame();
            ChannelPicker cp = new ChannelPicker(this);
            if2.getContentPane().add(cp);
            dp.add(if2);
            if2.setVisible(true);
            if2.pack();
            if2.setLocation(if1.getLocation().x+if1.getWidth(),if2.getLocation().y);


            JInternalFrame if3 = new JInternalFrame();
            if3.add(new JLabel("Internal Frame #3"));
            dp.add(if3);
            if3.setVisible(true);
            if3.pack();

            dp.setDoubleBuffered(true);
            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(dp);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);





           /*

           //this.removeAll();

            JPanel main = new JPanel();
            this.setLayout(new BorderLayout());

            main.setLayout(new GridBagLayout());
            ChannelPicker panel = new ChannelPicker(this);


            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 1;
            gc.gridy = 0;
            gc.fill=GridBagConstraints.BOTH;
            gc.weightx= 0.0;

            main.add(panel, gc);

            gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 0;
            gc.fill=GridBagConstraints.BOTH;
            gc.weightx= 1.0;
            gc.weighty= 1.0;

            main.add(ic, gc);

            for (int i = 0; i < scrollbars.size(); i++) {
                gc = new GridBagConstraints();
                gc.gridx = 0;
                gc.gridy = i+1;
                gc.fill=GridBagConstraints.BOTH;
                gc.weightx= 1.0;
                System.out.println("adding scrollbar:"+scrollbars.get(i));
                main.add(scrollbars.get(i), gc);
            }


            dp.setContentPane(main);

            dp.setBounds(100,100,800,600);
            dp.setVisible(true);
            main.setDoubleBuffered(true);
            this.setVisible(false);*/


        }

        private void updateImageLayout(ImageLayout imageLayout, JInternalFrame if1){
            imageLayout.minimumLayoutSize(if1);
            imageLayout.preferredLayoutSize(if1);
            //ic.resetDoubleBuffer();

            //if1.invalidate();
            if1.validate();
            if1.doLayout();
            if1.repaint();
        }

    private JInternalFrame createInternalFrame(Component c, String title){
        JInternalFrame if1 = new JInternalFrame();
        if1.setBorder(new LineBorder(Color.GRAY,3));
        if1.getContentPane().setLayout(new BorderLayout());
        if1.setResizable(true);
        if1.setClosable(false);
        if1.setIconifiable(true);
        if1.setTitle(title);
        if1.getContentPane().add(c);
        return if1;
    }


    private JInternalFrame createInternalFrame(Component c, String title, GridBagConstraints gbc){
        JInternalFrame if1 = new JInternalFrame();
        if1.setBorder(new LineBorder(Color.GRAY,3));
        if1.getContentPane().setLayout(new GridBagLayout());
        if1.setResizable(true);
        if1.setClosable(false);
        if1.setIconifiable(true);
        if1.setTitle(title);
        if1.getContentPane().add(c,gbc);
        return if1;
    }




    public void keyTyped(KeyEvent e) {

    }

    public void keyReleased(KeyEvent e) {

    }

    public void keyPressed(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED && (imp instanceof MultiCompositeImage)) {
            MultiCompositeImage i5d = (MultiCompositeImage)imp;
            int code = e.getKeyCode();
            boolean ctrlPressed = ((e.getModifiers()&KeyEvent.CTRL_MASK)!=0);
            boolean shiftPressed = ((e.getModifiers()&KeyEvent.SHIFT_MASK)!=0);

            // Keystrokes only act on the active Image.
            if (i5d == WindowManager.getCurrentImage()) {
                // Process defined key events. Make them unusable for ImageJ by setting the
                // KeyCode to CHAR_UNDEFINED

                if (code==KeyEvent.VK_NUMPAD1 || (code==KeyEvent.VK_PAGE_DOWN&&shiftPressed )) {
                    // Numpad 1 / 2 and shift-pageDown / shift-pageUp for frame change
                    i5d.setT(i5d.getFrame()-1);
                    e.setKeyCode(KeyEvent.CHAR_UNDEFINED);
                } else if (code==KeyEvent.VK_NUMPAD2 || (code==KeyEvent.VK_PAGE_UP&&shiftPressed )) {
                    i5d.setT(i5d.getFrame()+1);
                    e.setKeyCode(KeyEvent.CHAR_UNDEFINED);

                } else if (code==KeyEvent.VK_NUMPAD7 || (code==KeyEvent.VK_PAGE_DOWN&&ctrlPressed )) {
                    // Numpad 7 / 8 and ctrl-pageDown / ctrl-pageUp (w/o modifier) for channel change
                    i5d.setC(i5d.getChannel()-1);
                    e.setKeyCode(KeyEvent.CHAR_UNDEFINED);
                } else if (code==KeyEvent.VK_NUMPAD8 || (code==KeyEvent.VK_PAGE_UP&&ctrlPressed )) {
                    i5d.setC(i5d.getChannel()+1);
                    e.setKeyCode(KeyEvent.CHAR_UNDEFINED);

                } else if (code==KeyEvent.VK_NUMPAD4 || (code==KeyEvent.VK_PAGE_DOWN )
                        || code==KeyEvent.VK_LESS || code==KeyEvent.VK_COMMA) {
                    // Numpad 4 / 5 and pageDown / pageUp for slice change
                    i5d.setSlice(i5d.getCurrentSlice()-1);
                    e.setKeyCode(KeyEvent.CHAR_UNDEFINED);
                } else if (code==KeyEvent.VK_NUMPAD5 || (code==KeyEvent.VK_PAGE_UP )
                        || code==KeyEvent.VK_GREATER || code==KeyEvent.VK_PERIOD) {
                    i5d.setSlice(i5d.getCurrentSlice()+1);
                    e.setKeyCode(KeyEvent.CHAR_UNDEFINED);

                } else if (code==KeyEvent.VK_LEFT || code==KeyEvent.VK_RIGHT || code==KeyEvent.VK_UP
                        || code==KeyEvent.VK_DOWN || i5d.getRoi() instanceof TextRoi){
                    // arrow keys to move ROIs or text for Text ROI

                }
            }
        }
    }

}
