package org.kohsuke.bookart;

import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
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

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

    private void run(String[] args) throws Exception {
        new CmdLineParser(this).parseArgument(args);

        BufferedImage canvas = ImageIO.read(image);

        StringBuilder instructions = new StringBuilder();

        for (int i=0; i<slices; i++) {
            final int x = (canvas.getWidth()*i)/slices;

            // scan a line and find edges
            List<Integer> edges = new ArrayList<Integer>();
            for (int y=1; y<canvas.getHeight(); y++) {
                if (bw(pick(canvas, x, y-1))
                 != bw(pick(canvas, x, y)))
                    edges.add(y);
            }

            // TODO: remove noise

            instructions.append("<div>Page ").append(i*2+start).append(": ");
            for (int j=0; j<edges.size(); j++) {
                if (j>0)    instructions.append(", ");
                instructions.append(round(imageHeight*edges.get(j)/canvas.getHeight()+pageHeight/2));
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
     * Round at the mm unit
     */
    private float round(float f) {
        return (float) (Math.round(f*10)/10.0);
    }

    private Color pick(BufferedImage canvas, int x, int y) {
        return new Color(canvas.getRGB(x,y));
    }

    private boolean bw(Color c) {
        return c.getBlue() + c.getRed() + c.getGreen() > 128*3;
    }
}
