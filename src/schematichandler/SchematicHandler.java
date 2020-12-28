package schematichandler;

import arc.util.*;

import javax.imageio.*;
import java.awt.*;
import java.io.*;

public class SchematicHandler {
    public static void main(String[] args ){
        if (args.length == 0) {
            System.out.println("Usage: previewer.jar <Path> [Background=false] [Background Offset=32] [Border Color=Gray]");
            return;
        }

        if (args.length >= 3 && !Strings.canParseInt(args[2])) {
            System.out.println(args[2] + " is not a number");
            return;
        }

        var path = args[0];
        var background = args.length >= 2 && args[1].equals("true");
        var offset = args.length >= 3 ? Strings.parseInt(args[2]) : 32;
        var color = args.length >= 4 ? fromHex(args[3]) : Color.GRAY;

        try {
            System.out.println("Reading schematic " + path);
            var schem = new Schematic(path, background, offset, color);
            ImageIO.write(schem.image, "png", new File(path.replaceAll("\\.msch$", ".png")));

        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Color fromHex(String colorStr) {
        return new Color(
        Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
        Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
        Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }
}
