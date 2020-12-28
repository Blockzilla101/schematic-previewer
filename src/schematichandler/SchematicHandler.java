package schematichandler;

import javax.imageio.*;
import java.io.*;

public class SchematicHandler {
    public static void main(String[] args ){
        if (args.length == 0) {
            System.out.println("Usage: previewer.jar <schematic path> [background]");
            return;
        }
        var path = args[0];

        try {
            System.out.println("Reading schematic " + path);
            var schem = new Schematic(path);
            ImageIO.write(schem.image, "png", new File(path.replaceAll("\\.msch$", ".png")));

        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
