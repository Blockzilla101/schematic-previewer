package schematichandler;

import arc.*;
import arc.graphics.*;
import arc.graphics.Color;
import arc.graphics.Pixmap.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.graphics.g2d.TextureAtlas.TextureAtlasData.*;
import arc.math.*;
import arc.mock.*;
import arc.struct.*;
import arc.files.*;

import com.jidesoft.swing.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;

public class Schematic {
    public static final String header = schematicBaseStart;
    public BufferedImage image;
    public mindustry.game.Schematic schematic;

    private BufferedImage currentImage;
    private Graphics2D currentGraphics;
    private final float bridgeOpacity = 0.75f;
    private ObjectMap<String, BufferedImage> regions = new ObjectMap<>();

    public int size = 4;
    public int tileSize = 8;
    public long loadTime = 0;

    Schematic(String path, boolean drawBackground, int backgroundOffset, java.awt.Color borderColor, boolean createImage) throws IOException {
        init();
        if (Fi.get(path).exists()) {
            schematic = Schematics.read(Fi.get(path));
        } else if (path.startsWith(header)) {
            try {
                schematic = Schematics.readBase64(path);
            } catch(RuntimeException e) {
                e.printStackTrace();
                throw new IOException("Either the schematic is inaccessible or provided base64 is invalid");
            }
        } else {
            throw new IOException("That schematic is no where to be found");
        }

        if (!createImage) return;

        while(getMemUsed(drawBackground, backgroundOffset) > Runtime.getRuntime().freeMemory() && size > 1) size--;
        // while(getMemUsed(drawBackground, backgroundOffset) > Runtime.getRuntime().freeMemory() && tileSize > 1) tileSize--;

        if (getMemUsed(drawBackground, backgroundOffset) > Runtime.getRuntime().freeMemory()) {
            throw new RuntimeException("Schematic is way to big to render even at a reduced size");
        }

        System.out.printf("Will be rendering at size %d and tilesize %d\n\n", size, tileSize);
        var schematicImage = new BufferedImage(schematic.width * size * tileSize, schematic.height * size * tileSize, BufferedImage.TYPE_INT_ARGB);

        Draw.reset();
        Seq<BuildPlan> requests = schematic.tiles.map(t -> new BuildPlan(t.x, t.y, t.rotation, t.block, t.config));
        currentImage = schematicImage;
        currentGraphics = schematicImage.createGraphics();
        requests.each(req -> {
            req.animScale = 1f;
            req.worldContext = false;
            req.block.drawRequestRegion(req, requests::each);
            Draw.reset();
        });
        requests.each(req -> { // Draw bridge conveyors separately first to avoid some being over power node connections and some below
            if (req.block instanceof ItemBridge){
                Draw.alpha(bridgeOpacity);
                req.block.drawRequestConfigTop(req, requests::each);
                Draw.reset();
            }
        });
        requests.each(req -> {
            if (!(req.block instanceof ItemBridge)){
                req.block.drawRequestConfigTop(req, requests::each);
                Draw.reset();
            }
        });
        image = schematicImage;

        if (drawBackground) {
            int width = schematicImage.getWidth() + (backgroundOffset * 2);
            int height = schematicImage.getHeight() + (backgroundOffset * 2);

            var factory = new ShadowFactory();
            factory.setRenderingHint(ShadowFactory.KEY_BLUR_QUALITY, ShadowFactory.VALUE_BLUR_QUALITY_HIGH);
            factory.setColor(java.awt.Color.black);
            factory.setOpacity(0.75f);
            factory.setSize(24);
            var shadow = factory.createShadow(schematicImage);

            BufferedImage background = ImageIO.read(Core.files.internal("sprites/schematic-background.png").read());
            BufferedImage withBackground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            background = repeatToSize(background, width, height);
            currentGraphics = withBackground.createGraphics();
            currentImage = withBackground;

            currentGraphics.drawImage(background, 0, 0, null);
            currentGraphics.drawImage(shadow, backgroundOffset - 24, backgroundOffset - 24, null);
            currentGraphics.drawImage(schematicImage, backgroundOffset, backgroundOffset, null);

            currentGraphics.setColor(borderColor);
            currentGraphics.setStroke(new BasicStroke(4f));
            currentGraphics.drawRect(2, 2, width - 4, height - 4);

            this.image = withBackground;
        }
        currentGraphics.dispose();
    }

    private long getMemUsed(boolean drawBackground, int backgroundOffset) {
        if (drawBackground) {
            var schemMem = (schematic.width * tileSize * size) * (schematic.height * tileSize * size) * 4;
            var backgroundMem = ((schematic.width + backgroundOffset) * tileSize * size) * ((schematic.height + backgroundOffset) * tileSize * size) * 4;
            var shadowMem = schemMem * 2;
            return schemMem + backgroundMem + shadowMem;
        } else {
            return (schematic.width * tileSize * size) * (schematic.height * tileSize * size) * 4;
        }
    }

    private void init() {
        long start = System.currentTimeMillis();
        Version.enabled = false;
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();

        Core.files = new MockFiles();

        for(ContentType type : ContentType.values()){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.init();
                }catch(Throwable ignored){
                }
            }
        }

        Vars.state = new GameState();

        TextureAtlasData data = new TextureAtlasData(Core.files.internal("sprites/sprites.atlas"), Core.files.internal("sprites"), false);
        Core.atlas = new TextureAtlas();

        ObjectMap<Page, BufferedImage> images = new ObjectMap<>();

        data.getPages().each(page -> {
            try{
                BufferedImage image = ImageIO.read(page.textureFile.read());
                images.put(page, image);
                page.texture = Texture.createEmpty(new ImageData(image));
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        });

        data.getRegions().each(reg -> {
            try{
                BufferedImage image = new BufferedImage(reg.width, reg.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();

                graphics.drawImage(images.get(reg.page), 0, 0, reg.width, reg.height, reg.left, reg.top, reg.left + reg.width, reg.top + reg.height, null);
                ImageRegion region = new ImageRegion(reg.name, reg.page.texture, reg.left, reg.top, image);

                Core.atlas.addRegion(region.name, region);
                regions.put(region.name, image);
            }catch(Exception e){
                e.printStackTrace();
            }
        });

        Lines.useLegacyLine = true;
        Core.atlas.setErrorRegion("error");
        Draw.scl = 1f / 4f;
        Core.batch = new SpriteBatch(0){
            @Override
            protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
                x += 4;
                y += 4;

                x *= size;
                y *= size;
                width *= size;
                height *= size;

                y = currentImage.getHeight() - (y + height/2f) - height/2f;

                AffineTransform at = new AffineTransform();
                at.translate(x, y);
                at.rotate(-rotation * Mathf.degRad, originX * size, originY * size);

                currentGraphics.setTransform(at);
                BufferedImage image = regions.get(((AtlasRegion)region).name);

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
                }catch(Throwable ignored){
                }
            }
        }

        world = new World(){
            public Tile tile(int x, int y){
                return new Tile(x, y);
            }
        };

        long end = System.currentTimeMillis();
        loadTime = end - start;
        System.out.printf("Time to load %d.%ds%n",
            TimeUnit.MILLISECONDS.toSeconds(loadTime),
            loadTime - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(loadTime))
        );
    }

    private BufferedImage tint(BufferedImage image, Color color){
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

    static class ImageData implements TextureData{
        final BufferedImage image;

        public ImageData(BufferedImage image){
            this.image = image;
        }

        @Override
        public TextureDataType getType(){
            return TextureDataType.Custom;
        }

        @Override
        public boolean isPrepared(){
            return false;
        }

        @Override
        public void prepare(){

        }

        @Override
        public Pixmap consumePixmap(){
            return null;
        }

        @Override
        public boolean disposePixmap(){
            return false;
        }

        @Override
        public void consumeCustomData(int target){

        }

        @Override
        public int getWidth(){
            return image.getWidth();
        }

        @Override
        public int getHeight(){
            return image.getHeight();
        }

        @Override
        public Format getFormat(){
            return Format.rgba8888;
        }

        @Override
        public boolean useMipMaps(){
            return false;
        }

        @Override
        public boolean isManaged(){
            return false;
        }
    }

    static class ImageRegion extends AtlasRegion{
        final BufferedImage image;
        final int x, y;

        public ImageRegion(String name, Texture texture, int x, int y, BufferedImage image){
            super(texture, x, y, image.getWidth(), image.getHeight());
            this.name = name;
            this.image = image;
            this.x = x;
            this.y = y;
        }
    }
}
