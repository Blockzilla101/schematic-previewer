package schematichandler;

import arc.*;
import arc.graphics.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.mock.*;
import arc.struct.*;
import arc.files.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jidesoft.swing.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.power.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class Schematic{
    public static final boolean drawBackground = true;
    public static final java.awt.Color borderColor = awtColor(Color.valueOf("#454545"));
    public static final int backgroundOffset = 32;
    public static final boolean makePixelArt = true;

    public static final String header = schematicBaseStart;
    public BufferedImage image;
    public mindustry.game.Schematic schematic;
    public long batteryStorage = 0;

    static private BufferedImage currentImage;
    static private Graphics2D currentGraphics;
    static private final float bridgeOpacity = 0.75f;
    static private final ObjectMap<String, BufferedImage> regions = new ObjectMap<>();
    static private final StringMap imageFiles = new StringMap();

    public int pixelSize = 4;
    public int pixelArtBorderPixels = 4; // top & bottom, left & right
    public int renderSize = maxRenderSize;

    final private Seq<String> pixelArtBlocks = Seq.with("sorter", "inverted-sorter", "item-source");
    private boolean hasPixelArt;

    static private boolean inited = false;
    /** render size but static */
    static private int tempSize = 4;
    static public long timeToLoad;
    static public final int maxRenderSize = 4;

    Schematic(String path, boolean createImage) throws IOException{
        // attempt to read the schematic
        if(Fi.get(path).exists()){
            schematic = Schematics.read(Fi.get(path));
        }else if(path.startsWith(header)){
            try{
                schematic = Schematics.readBase64(path);
            }catch(RuntimeException e){
                e.printStackTrace();
                throw new IOException("Either the schematic is inaccessible or provided base64 is invalid");
            }
        }else{
            throw new IOException("That schematic is no where to be found");
        }

        // bad schematic
        if(schematic.tiles.size == 0) throw new IOException("Schematic has no blocks");

        // get all the blocks in the schematic
        Seq<BuildPlan> requests = schematic.tiles.map(t -> new BuildPlan(t.x, t.y, t.rotation, t.block, t.config));

        // check if it has pixel art
        hasPixelArt = makePixelArt;
        requests.each(req -> {
            if(makePixelArt && hasPixelArt && !pixelArtBlocks.contains(req.block.name)) hasPixelArt = false;
            if (req.block instanceof Battery) {
                batteryStorage += req.block.consPower.capacity;
            }
        });

        // no image creation
        if(!createImage) return;

        // check if it can be rendered otherwise keep reducing resolution
        tempSize = maxRenderSize;
        while(getMemUsed() > Runtime.getRuntime().freeMemory() && tempSize > 1) tempSize--;
        renderSize = tempSize;

        if(getMemUsed() > Runtime.getRuntime().freeMemory()){
            throw new IOException("Schematic is way to big to render even at a reduced size");
        }

//        System.out.printf("Will be rendering at %s:1 the size\n", String.format("%.2f", size / 4f).replace(".00", ""));
        // empty image
        var schematicImage = new BufferedImage(schematic.width * tempSize * tilesize, schematic.height * tempSize * tilesize, BufferedImage.TYPE_INT_ARGB);

        // draw all the blocks
        Draw.reset();
        currentImage = schematicImage;
        currentGraphics = schematicImage.createGraphics();
        requests.each(req -> {
            req.animScale = 1f;
            req.worldContext = false;
            req.block.drawPlanRegion(req, requests);
            Draw.reset();
        });

        // draw bridge conveyors separately first to avoid some being over power node connections and some below
        requests.each(req -> {
            if(req.block instanceof ItemBridge || req.block instanceof DirectionBridge){
                Draw.alpha(bridgeOpacity);
                req.block.drawPlanConfigTop(req, requests);
                Draw.reset();
            }
        });

        // draw rest of the config
        requests.each(req -> {
            if(!(req.block instanceof ItemBridge || req.block instanceof DirectionBridge)){
                req.block.drawPlanConfigTop(req, requests);
                Draw.reset();
            }
        });

        image = schematicImage;

        // render background
        if(drawBackground){
            int width = schematicImage.getWidth() + (backgroundOffset * 2);
            int height = schematicImage.getHeight() + (backgroundOffset * 2);

            int schematicOffsetX = backgroundOffset;
            int schematicOffsetY = backgroundOffset;

            BufferedImage art = null;

            // render pixel arg (if it has any)
            if(makePixelArt && hasPixelArt){
                art = getPixelArt(requests, pixelSize, pixelArtBorderPixels);

                // calculate position of the pixel art

                if(art.getHeight() <= art.getWidth()){
                    width -= backgroundOffset * 2;
                    height -= 20;

                    width += art.getWidth() + 5;
                    height += art.getHeight();

                    schematicOffsetX = art.getWidth() / 3;
                    schematicOffsetY = art.getHeight() + 20;
                }

//                if (art.getHeight() == art.getWidth()) {
//                    width -= backgroundOffset;
//                    height -= backgroundOffset;
//
//                    width += art.getWidth() + 5;
//                    height += art.getHeight() + 5;
//
//                    schematicOffsetX = 20;
//                    schematicOffsetY = art.getHeight() + 20;
//                }

                if(art.getHeight() > art.getWidth()){
                    width -= backgroundOffset;
                    height -= 10;

                    width += art.getWidth() + 20;

                    schematicOffsetX = 25;
                    schematicOffsetY = 25;
                }
            }

            // shadow for the schematic blocks
            var factory = new ShadowFactory();
            factory.setRenderingHint(ShadowFactory.KEY_BLUR_QUALITY, ShadowFactory.VALUE_BLUR_QUALITY_HIGH);
            factory.setColor(java.awt.Color.black);
            factory.setOpacity(0.75f);
            factory.setSize(24);
            var shadow = factory.createShadow(schematicImage);

            BufferedImage background = ImageIO.read(Core.files.internal("schematic-background.png").read());
            BufferedImage withBackground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // make background fit schematic size (used as base image)
            background = repeatToSize(background, width, height);
            currentGraphics = withBackground.createGraphics();
            currentImage = withBackground;

            // draw background, schematic and shadow on base image
            currentGraphics.drawImage(background, 0, 0, null);
            currentGraphics.drawImage(shadow, schematicOffsetX - 24, schematicOffsetY - 24, null);
            currentGraphics.drawImage(schematicImage, schematicOffsetX, schematicOffsetY, null);

            // draw borders on base image
            currentGraphics.setColor(borderColor);
            currentGraphics.setStroke(new BasicStroke(4f));
            currentGraphics.drawRect(2, 2, width - 4, height - 4);

            // draw pixel art on base image
            if(makePixelArt && hasPixelArt){
                currentGraphics.drawImage(art, currentImage.getWidth() - art.getWidth() - 4, 4, null);

                currentGraphics.setColor(borderColor);
                currentGraphics.setStroke(new BasicStroke(2f));
                currentGraphics.drawRect(currentImage.getWidth() - art.getWidth() - 4 - 1, 3, art.getWidth() + 2, art.getHeight() + 2);
            }

            // replace schematic image with one with the background
            this.image = withBackground;
        }else{
            // draw pixel art on the schematic image (no background here)
            if(makePixelArt && hasPixelArt){
                var art = getPixelArt(requests, pixelSize, pixelArtBorderPixels, true);
                var full = new BufferedImage(currentImage.getWidth() + art.getWidth(), currentImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

                currentGraphics = full.createGraphics();

                currentGraphics.drawImage(schematicImage, 0, 0, null);
                currentGraphics.drawImage(art, schematicImage.getWidth(), 0, null);

                currentImage = full;
                image = full;
            }
        }
        currentGraphics.dispose();
    }

    /**
     * @return memory required for rendering schematic preview
     */
    private long getMemUsed(){
        if(drawBackground){
            var schemMem = (schematic.width * tilesize * tempSize) * (schematic.height * tilesize * tempSize) * 4;
            var backgroundMem = 0;
            var shadowMem = schemMem * 2;

            if(makePixelArt && hasPixelArt){
                var artWidth = (schematic.width + pixelArtBorderPixels) * pixelSize;
                var artHeight = (schematic.height + pixelArtBorderPixels) * pixelSize;
                backgroundMem = (((schematic.width + backgroundOffset) * tilesize * tempSize) + artWidth) * (((schematic.height + backgroundOffset) * tilesize * tempSize) + artHeight) * 4;

            }else{
                backgroundMem = ((schematic.width + backgroundOffset) * tilesize * tempSize) * ((schematic.height + backgroundOffset) * tilesize * tempSize) * 4;
            }

            var total = schemMem + backgroundMem + shadowMem;

            if(makePixelArt && hasPixelArt){
                total += (((schematic.width + pixelArtBorderPixels) * pixelSize) * ((schematic.height + pixelArtBorderPixels) * pixelSize)) * 3;
            }

            return total;
        }else if(makePixelArt && hasPixelArt){
            var schemWidth = schematic.width * tilesize * tempSize;
            var schemHeight = schematic.height * tilesize * tempSize;

            var artWidth = (schematic.width + pixelArtBorderPixels) * pixelSize;
            var artHeight = (schematic.height + pixelArtBorderPixels) * pixelSize;

            var schemMem = schemWidth * schemHeight * 4;
            var artMem = artWidth * artHeight * 4;

            var renderedMem = ((schemWidth + artWidth) * (schemHeight)) * 4;

            return schemMem + artMem + renderedMem;
        }else{
            return ((long)schematic.width * tilesize * tempSize) * ((long)schematic.height * tilesize * tempSize) * 4;
        }
    }

    public JsonObject toJson() {
        var obj = new JsonObject();

        var req = new JsonObject();
        schematic.requirements().forEach(item -> req.addProperty(item.item.name, item.amount));

        var labels = new JsonArray();
        schematic.labels.forEach(labels::add);

        obj.addProperty("name", schematic.name());
        obj.addProperty("description", schematic.description());
        obj.addProperty("blockCount", schematic.tiles.size);
        obj.addProperty("powerProduced", schematic.powerProduction() * 60f);
        obj.addProperty("powerConsumed", schematic.powerConsumption() * 60f);
        obj.addProperty("powerStored", batteryStorage);
        obj.addProperty("width", schematic.width);
        obj.addProperty("height", schematic.height);
        obj.addProperty("quality", renderSize);

        obj.add("requirements", req);
        obj.add("labels", labels);

        return obj;
    }

    /**
     * Initialize all the mindustry blocks, sprites etc
     */
    static public void init(){
        if (inited) return;
        long start = System.currentTimeMillis();

        Version.enabled = false;
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();

        Core.files = new MockFiles();

        for(ContentType type : ContentType.all){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.init();
                }catch(Throwable ignored){
                }
            }
        }

        Vars.state = new GameState();

        TextureAtlasData data = new TextureAtlasData(Core.files.internal("sprites.aatls"), Core.files.internal("assets"), false);
        Core.atlas = new TextureAtlas();

        try {
            var src = Schematic.class.getProtectionDomain().getCodeSource();
            if (src != null) {
                var zip = new ZipInputStream(src.getLocation().openStream());
                ZipEntry ze;

                while ((ze = zip.getNextEntry()) != null) {
                    String entryName = ze.getName();
                    if(entryName.startsWith("sprites") && entryName.endsWith(".png") ) {
                        imageFiles.put(entryName.substring(entryName.lastIndexOf('/') + 1, entryName.length() - 4), entryName);
                    }
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        data.getPages().each(page -> {
            page.texture = Texture.createEmpty(null);
            page.texture.width = page.width;
            page.texture.height = page.height;
        });

        data.getRegions().each(reg -> Core.atlas.addRegion(reg.name, new AtlasRegion(reg.page.texture, reg.left, reg.top, reg.width, reg.height){{
            name = reg.name;
            texture = reg.page.texture;
        }}));

        Lines.useLegacyLine = true;
        Core.atlas.setErrorRegion("error");
        Draw.scl = 1f / 4f;
        Core.batch = new SpriteBatch(0){
            @Override
            protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
                x += 4;
                y += 4;

                x *= tempSize;
                y *= tempSize;
                width *= tempSize;
                height *= tempSize;

                y = currentImage.getHeight() - (y + height / 2f) - (height / 2f);

                AffineTransform at = new AffineTransform();
                at.translate(x, y);
                at.rotate(-rotation * Mathf.degRad, originX * tempSize, originY * tempSize);

                currentGraphics.setTransform(at);
                BufferedImage image = getImage(((AtlasRegion)region).name);
                if(!color.equals(Color.white)){
                    image = tint(image, color);
                }

                currentGraphics.drawImage(image, 0, 0, (int)width, (int)height, null);
            }

            @Override
            protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
                //do nothing
            }
        };

        for(ContentType type : ContentType.values()){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.load();
                    content.loadIcon();
                }catch(Throwable ignored){
                }
            }
        }

        world = new World(){
            public Tile tile(int x, int y){
                return new Tile(x, y);
            }
        };

        timeToLoad = System.currentTimeMillis() - start;
        inited = true;
    }

    static private BufferedImage tint(BufferedImage image, Color color){
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Color tmp = new Color();
        for(int x = 0; x < copy.getWidth(); x++){
            for(int y = 0; y < copy.getHeight(); y++){
                int argb = image.getRGB(x, y);
                tmp.argb8888(argb);
                tmp.mul(color);
                copy.setRGB(x, y, tmp.argb8888());
            }
        }
        return copy;
    }

    /**
     * repeats a small image over and over until it reaches the desired dimensions (mainly used for drawing the background, could be optimized)
     * @param image image to repeat
     * @param newWidth width of the repeated image
     * @param newHeight height of the repeated image
     * @return repeated image
     */
    private BufferedImage repeatToSize(BufferedImage image, int newWidth, int newHeight){
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();

        for(int x = 0; x <= newWidth; x += image.getWidth()){
            for(int y = 0; y <= newHeight; y += image.getHeight()){
                g.drawImage(image, x, y, null);
            }
        }

        g.dispose();
        return resized;
    }

    private BufferedImage getPixelArt(Seq<BuildPlan> plans, int pixelSize, int bgSize) {
        return getPixelArt(plans, pixelSize, bgSize, false);
    }

    /**
     * @param plans schematic blocks
     * @param pixelSize size of each pixel
     * @param bgSize padding around the pixel art
     * @param noBg render with a background (it is a solid color)
     * @return render pixel art
     */
    private BufferedImage getPixelArt(Seq<BuildPlan> plans, int pixelSize, int bgSize, boolean noBg) {
        BufferedImage pixelArt = new BufferedImage((schematic.width + bgSize) * pixelSize, (schematic.height + bgSize) * pixelSize, noBg ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        var g = pixelArt.createGraphics();

        if (!noBg) {
            g.setColor(awtColor(Pal.darkerMetal));
            g.fillRect(0, 0, pixelArt.getWidth(), pixelArt.getHeight());
        }

        plans.forEach(plan -> {
            g.setColor(awtColor(((Item)plan.config).color));
            g.fillRect((plan.x + (bgSize / 2)) * pixelSize, pixelArt.getHeight() - pixelSize - ((plan.y + (bgSize / 2)) * pixelSize), pixelSize, pixelSize);
        });

        g.dispose();
        return pixelArt;
    }

    /**
     * @param col arc color
     * @return converted to java.awt.color
     */
    private static java.awt.Color awtColor(Color col) {
        return new java.awt.Color(col.r, col.g, col.b, col.a);
    }

    /**
     * @param name name of the block
     * @return image for that block
     */
    static private BufferedImage getImage(String name){
        return regions.get(name, () -> {
            try{
                return ImageIO.read(Schematic.class.getClassLoader().getResource(imageFiles.get(name, "error")));
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        });
    }
}
