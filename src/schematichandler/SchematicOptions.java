package schematichandler;

import com.beust.jcommander.*;
import schematichandler.SchematicHandler.*;

import java.util.*;

public class SchematicOptions{
    @Parameter(description = "Schematic file or base64 to read", arity = 1)
    public ArrayList<String> schemFiles = new ArrayList<>();
    public String schematic;

    @Parameter(names = {"-o", "--out", "--output"}, description = "Where to put the generated preview")
    public String outPath;

    @Parameter(names = {"-d", "--data-path"}, description = "Where to put schematic data (requirements, width, height, etc). If its not stdout then its formatted using json")
    public String dataPath;

    @Parameter(names = {"-bc", "--border-color"}, description = "Color for the border when generating with background", validateValueWith = ColorValidator.class)
    public String borderColor = "#454545";

    @Parameter(names = {"-p", "--preview"}, description = "Whether to create the preview or not", arity = 1)
    public boolean createPreview = true;

    @Parameter(names = {"-art", "--pixel-art"}, description = "Whether to put pixel art of pixel art schematics at top left")
    public boolean pixelArt = false;

    @Parameter(names = {"-bg", "--background"}, description = "Whether to create the preview with the background")
    public boolean background = false;

    @Parameter(names = {"-offset", "--background-offset"}, description = "Number of pixels between the edge and the schematic")
    public int backgroundOffset = 32;

    @Parameter(names = {"--bulk"}, description = "Read schematics to parse from a file")
    public String bulk;

    public static JCommander parser;
}
