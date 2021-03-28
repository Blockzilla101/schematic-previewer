package schematichandler;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import com.beust.jcommander.*;
import com.google.gson.*;

import javax.imageio.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class SchematicHandler {
    public static void main(String[] args) {
        SchematicOptions parsedArgs = null;
        try {
            parsedArgs = parseArgs(args);
            if (parsedArgs.bulk != null) parseBulk(Fi.get(parsedArgs.bulk).readString());
        } catch(ParameterException e) {
            System.out.println(e.getMessage());
            SchematicOptions.parser.usage();
            System.exit(1);
        }

        System.out.print("Loading Sprites...");
        Schematic.init();
        System.out.println("Done");

        try {
            long start = System.currentTimeMillis();
            if (parsedArgs.bulk == null) {
                var schem = new Schematic(parsedArgs.schematic, parsedArgs.background, parsedArgs.backgroundOffset, fromHex(parsedArgs.borderColor), parsedArgs.createPreview, parsedArgs.pixelArt);
                if (parsedArgs.createPreview) ImageIO.write(schem.image, "png", new File(parsedArgs.outPath));
                if (parsedArgs.dataPath == null) {
                    System.out.println(schem.toString(">"));
                } else {
                    var map = schem.toMap();
                    if (parsedArgs.createPreview) map.put("previewPath", parsedArgs.outPath);
                    Fi.get(parsedArgs.dataPath).writeString(new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(map) + "\n");
                }
            } else {
                var bulk = parseBulk(Fi.get(parsedArgs.bulk).readString());
                System.out.println("Parsing " + bulk.size + " schematics");
                var gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
                var done = new Seq<HashMap<String, Object>>();

                for(var opts: bulk) {
                    var schem = new Schematic(opts.schematic, opts.background, opts.backgroundOffset, fromHex(opts.borderColor), opts.createPreview, opts.pixelArt);
                    var previewPath = "schem-preview-" + Time.millis() + ".png";

                    if (opts.createPreview) ImageIO.write(schem.image, "png", new File(previewPath));

                    var map = schem.toMap();
                    if (opts.createPreview) map.put("previewPath", previewPath);
                    done.add(map);
                }

                Fi.get(parsedArgs.dataPath).writeString(gson.toJson(done.toArray()) + "\n");
            }
            long end = System.currentTimeMillis();

            long genTime = end - start;
            long total = genTime + Schematic.timeToLoad;
            System.out.printf("Total Time %d.%ds | Preview %d.%ds | Load %d.%ds",
                TimeUnit.MILLISECONDS.toSeconds(total), total - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(total)),
                TimeUnit.MILLISECONDS.toSeconds(genTime), genTime - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(genTime)),
                TimeUnit.MILLISECONDS.toSeconds(Schematic.timeToLoad), Schematic.timeToLoad - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(Schematic.timeToLoad))
            );
            System.out.println();

        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Seq<SchematicOptions> parseBulk(String bulk) {
        var parsed = new Seq<SchematicOptions>();
        var lines = bulk.split("\r?\n");
        var hadError = false;

        for(int i = 0; i < lines.length; i++) {
            try {
                if (lines[i].length() > 0) parsed.add(parseArgs(lines[i].split(" "), true));
            } catch(ParameterException e) {
                System.err.printf("Bulk Link[%d]: %s\n", i, e.getMessage());
                hadError = true;
            }
        }

        if (hadError) {
            SchematicOptions.parser.usage();
            System.exit(1);
        }

        return parsed;
    }

    public static SchematicOptions parseArgs(String[] args) throws ParameterException {
        return parseArgs(args, false);
    }

    public static SchematicOptions parseArgs(String[] args, boolean inBulk) throws ParameterException {
        var parsed = new SchematicOptions();
        SchematicOptions.parser = JCommander.newBuilder().addObject(parsed).build();
        SchematicOptions.parser.parse(args);
        if (parsed.bulk == null) {
            if (parsed.schemFiles.size() == 0) {
                throw new ParameterException("Missing schematic file");
            }
            parsed.schematic = parsed.schemFiles.get(0);
            if (parsed.outPath == null && parsed.createPreview && !inBulk) {
                throw new ParameterException("The following option is required: [-o, --out, --output]");
            }
            if (parsed.createPreview && parsed.pixelArt && !parsed.background) {
                throw new ParameterException("Pixel art option needs background option to be set to true");
            }
            if (parsed.backgroundOffset < 0) {
                throw new ParameterException("Background offset cant be smaller then zero");
            }
            if (!Fi.get(parsed.schematic).exists() && !parsed.schematic.startsWith(Schematic.header)) {
                throw new ParameterException("Schematic is not a valid path and not base64");
            }
            if (inBulk && parsed.dataPath != null) {
                throw new ParameterException("Data path cannot be used in bulk file");
            }
            if (inBulk && parsed.outPath != null) {
                throw new ParameterException("Output path cannot be used in bulk file");
            }
        } else {
            if (inBulk) {
                throw new ParameterException("Bulk option cannot be used in bulk file");
            }
            if (!Fi.get(parsed.bulk).exists()) {
                throw new ParameterException("Path to bulk file is invalid");
            }
            if (parsed.dataPath == null) {
                throw new ParameterException("Data path is required in bulk mode");
            }
        }
        return parsed;
    }

    public static String removeNewlines(String str) {
        return str.replace(";", "\\;").replace("\r\n", "\n").replace("\r", "\n").replace("\n", ";");
    }

    public static Color fromHex(String colorStr) {
        return new Color(
        Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
        Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
        Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }

    public static class ColorValidator implements IValueValidator<String> {
        @Override
        public void validate(String name, String value) throws ParameterException {
            try {
                fromHex(value);
            } catch(Exception ignored) {
                throw new ParameterException("Parameter " + name + " is not a valid hex color");
            }
        }
    }
}
