package schematichandler;

import arc.util.*;

import javax.imageio.*;
import java.awt.*;
import java.io.*;
import java.util.concurrent.*;

public class SchematicHandler {
    public static void main(String[] args ){
        if (args.length == 0) {
            System.out.println("Usage: previewer.jar <Path> [Background=false] [Background Offset=32] [Border Color=Gray] [Create Image=true]");
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
        var createImage = args.length < 5 || args[4].equals("true");

        try {
            if (path.startsWith(Schematic.header)) {
                System.out.println("Reading base64 schematic");
            } else {
                System.out.println("Reading schematic " + path);
            }
            if (!createImage) System.out.println("Not creating image");
            long start = System.currentTimeMillis();

            var schem = new Schematic(path, background, offset, color, createImage);
            if (createImage) {
                if (path.startsWith(Schematic.header)) {
                    String name = Time.millis() + ".png";
                    ImageIO.write(schem.image, "png", new File(name));
                    System.out.println("Wrote png to " + name);
                } else {
                    ImageIO.write(schem.image, "png", new File(path.replaceAll("\\.msch$", ".png")));
                }
            }

            long end = System.currentTimeMillis();

            System.out.println(">name=" + schem.schematic.name());
            System.out.println(">description=" + schem.schematic.description());
            var temp = new StringBuilder(">requirements={ ");
            schem.schematic.requirements().forEach(item -> temp.append("\"").append(item.item.name).append("\"").append(" : ").append(item.amount).append(", "));
            System.out.println(temp.substring(0, temp.length() - 2) + " }");
            System.out.println(">numBlocks=" + schem.schematic.tiles.size);
            System.out.println(">width=" + schem.schematic.width);
            System.out.println(">height=" + schem.schematic.height);
            System.out.println();

            long elapsed = end - start;
            System.out.printf("Took %d.%ds%n",
                TimeUnit.MILLISECONDS.toSeconds(elapsed),
                elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed))
            );

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
