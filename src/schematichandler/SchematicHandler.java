package schematichandler;

import arc.files.*;
import com.google.gson.*;

import javax.imageio.*;
import java.io.*;

public class SchematicHandler {
    public static Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    public static void main(String[] args) {
        Schematic.init();

        try {
            var schematicOptions = gson.fromJson(Fi.get(args[0]).reader(), JsonArray.class);
            var previewed = new JsonArray();

            schematicOptions.forEach(schematicOption -> {
                var path = schematicOption.getAsJsonObject().get("schematicPath").getAsString();
                var previewPath = schematicOption.getAsJsonObject().get("previewPath") == null ? null : schematicOption.getAsJsonObject().get("previewPath").getAsString();

                try {
                    var rendered = new Schematic(path, previewPath != null);
                    var previewData = rendered.toJson();
                    previewData.addProperty("schematicPath", path);

                    if (previewPath != null) {
                        ImageIO.write(rendered.image, "png", Fi.get(previewPath).write());
                        previewData.addProperty("previewPath", previewPath);
                    }

                    previewed.add(previewData);

                } catch (IOException e) {
                    var error = new JsonObject();
                    error.addProperty("schematicPath", path);
                    error.addProperty("error", e.getMessage());

                    if (e.getMessage().equals("Either the schematic is inaccessible or provided base64 is invalid") || e.getMessage().equals("That schematic is no where to be found") || e.getMessage().equals("Schematic has no blocks")) {
                        error.addProperty("code", SchematicErrorCodes.InvalidSchematic.ordinal());
                    } else if (e.getMessage().equals("Schematic is way to big to render even at a reduced size")) {
                        error.addProperty("code", SchematicErrorCodes.TooBig.ordinal());
                    } else {
                        error.addProperty("code", SchematicErrorCodes.Other.ordinal());
                    }

                    previewed.add(error);
                }
            });

            System.out.println(gson.toJson(previewed));

        } catch (JsonSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public enum SchematicErrorCodes {
        InvalidSchematic,
        TooBig,
        Other
    }
}
