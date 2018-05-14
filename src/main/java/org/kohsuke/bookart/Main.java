package org.kohsuke.bookart;

import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    @Argument(required=true,index=0)
    public File image;

    @Argument(required=true,index=1)
    public int slices;

    @Option(name="-s",usage="If the page doesn't begin by 0")
    public int start;

    @Option(name="-p",usage="Height of a page")
    public float pageHeight;

    @Option(name="-h",usage="Height of the image")
    public float imageHeight;
    private BufferedImage canvas;

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

    private void run(String[] args) throws Exception {
        new CmdLineParser(this).parseArgument(args);

        canvas = ImageIO.read(image);

        StringBuilder instructions = new StringBuilder();

        for (int i=0; i<slices; i++) {
            final int x = (canvas.getWidth()*i)/slices;

            // scan a line and find edges
            List<Float> edges = findEdges(x);

            noiseReduction(edges);

            instructions.append("<div>Page ").append(i*2+start).append(": ");
            for (int j=0; j<edges.size(); j++) {
                if (j>0)    instructions.append(", ");
                if (j%2==0) instructions.append("<div>(");
                instructions.append(edges.get(j));
                if (j%2==1) instructions.append(")</div>");
            }
            instructions.append("</div>");


            String html = IOUtils.toString(Main.class.getResourceAsStream("/main.html"))
                .replace("${body}",instructions);

            File txt = new File(image.getPath() + ".html");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(txt),"UTF-8")) {
                w.write(html);
            }
        }
    }

    /**
     * Very short 'up' region is not stable, so remove them.
     */
    private void noiseReduction(List<Float> edges) {
        for (int i=0; i<edges.size()-1; ) {
            if (edges.get(i+1)-edges.get(i)<=0.1) {
                edges.remove(i);
                edges.remove(i);
            } else {
                i++;
            }
        }
    }

    /**
     * Scans a vertical line and find all the edges.
     */
    private List<Float> findEdges(int x) {
        final List<Float> edges = new ArrayList<>();
        final int h = canvas.getHeight();
        final float topMargin = (pageHeight - imageHeight) / 2;

        for (int y = 1; y< h; y++) {
            if (bw(pick(x, y-1))
             != bw(pick(x, y))) {
                edges.add(round(imageHeight*y/h+ topMargin));
            }
        }
        return edges;
    }

    /**
     * Round at the mm unit
     */
    private float round(float f) {
        return (float) (Math.round(f*10)/10.0);
    }

    private Color pick(int x, int y) {
        return new Color(canvas.getRGB(x,y));
    }

    private boolean bw(Color c) {
        return c.getBlue() + c.getRed() + c.getGreen() > 128*3;
    }
}
