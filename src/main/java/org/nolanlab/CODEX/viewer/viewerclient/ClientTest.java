package org.nolanlab.CODEX.viewer.viewerclient;

import ij.ImageJ;
import org.nolanlab.CODEX.viewer.viewerclient.i5d.MultiCompositeImage;
import org.nolanlab.CODEX.viewer.viewerclient.i5d.RemoteImageStack;

public class ClientTest {

    public static void main(String[] args) throws Exception{

        RemoteImageStack remoteImageStack = new RemoteImageStack("http://localhost:4567", "getStitchedImageList?user=Yury&exp=thymus&reg=1");
        new ImageJ().setVisible(true);

        new MultiCompositeImage("Stitched Image: user=Yury, exp=thymus, reg=1",remoteImageStack, 63,1,1).show();

    }
}
