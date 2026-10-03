package larrytllama.pvcmappermod;

import larrytllama.pvcmappermod.utils.*;

//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;*///?}

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class MapRenderUtils {

    public static void drawTooltipComponent(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        context, List<? extends Component> content, int x, int y) {
        int lines = content.size();
        Font mcfont = Minecraft.getInstance().font;
        int maxSize = 0;
        for (int i = 0; i < lines; i++) {
            int w = mcfont.width(content.get(i));
            if (w > maxSize) {
                maxSize = w;
            }
        }
        GraphicsHelper.drawTooltipBackground(context, x, y, maxSize, mcfont.lineHeight * lines);
        for (int i = 0; i < lines; i++) {
            GraphicsHelper.drawString(context, mcfont, content.get(i), x, y + (i * mcfont.lineHeight), 0xFFFFFFFF);
        }
    }

    public static void drawTooltipString(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        context, List<String> content, int x, int y) {
        int lines = content.size();
        Font mcfont = Minecraft.getInstance().font;
        int maxSize = 0;
        for (int i = 0; i<lines; i++) {
            int w = mcfont.width(content.get(i));
            if(w > maxSize) {
                maxSize = w;
            }
        }
        GraphicsHelper.drawTooltipBackground(context, x, y, maxSize, mcfont.lineHeight * lines);
        for (int i = 0; i < lines; i++) {
            GraphicsHelper.drawString(context, mcfont, content.get(i), x, y + (i * mcfont.lineHeight), 0xFFFFFFFF);
        }
    }


    public static double getScale() {
        return 1 / Math.pow(2, 8);
    }

    public static double metersToPixels(double num) {
        return Math.round(num / getScale());
    }

    /**
     * Converts a raw world coordinate to the appropriate map tile coordinate.
     * Safely handles negative coordinate integer division mapping.
     */
    public static int worldToTileCoordinate(double worldCoord, int tileSize) {
        return (int) Math.floorDiv((long) worldCoord, tileSize);
    }

    /**
     * Converts a raw world coordinate to the local pixel offset inside its map tile.
     * Safely handles negative coordinate modulo wrapping.
     */
    public static int worldToLocalTileCoordinate(double worldCoord, int tileSize) {
        return (int) Math.floorMod((long) worldCoord, tileSize);
    }

    // Improved drawLine
    public static void drawLine(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int currentY = y0;
        int lineMinX = x0;
        int lineMaxX = x0;

        while (true) {
            int e2 = err * 2;

            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx)  { err += dx; y0 += sy; }

            // If Y changed, draw the accumulated scanline before moving on
            if (y0 != currentY) {
                drawScanline(g, lineMinX, lineMaxX, currentY, color);
                currentY = y0;
                lineMinX = x0;
                lineMaxX = x0;
            } else {
                // Still on same Y, accumulate X
                lineMinX = Math.min(lineMinX, x0);
                lineMaxX = Math.max(lineMaxX, x0);
            }

            if (x0 == x1 && y0 == y1) {
                // Draw final scanline
                drawScanline(g, lineMinX, lineMaxX, currentY, color);
                break;
            }
        }
    }

    private static void drawScanline(
        //? if <26.1 {
        GuiGraphics
        //?} else {
        /*GuiGraphicsExtractor*///?}
        g, int minX, int maxX, int y, int color) {
        
        if (y <= 0 || y >= g.guiHeight()) return;
        
        minX = Math.max(minX, 0);
        maxX = Math.min(maxX, g.guiWidth() - 1);
        
        if (minX <= maxX) {
            g.fill(minX, y, maxX + 1, y + 1, color);
        }
    }

    public static boolean doesIntersect(double x1, double y1, double x2, double y2, double boxX, double boxY, double boxWidth, double boxHeight) {
        Rectangle2D rect = new Rectangle2D.Double(boxX, boxY, boxWidth, boxHeight);
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
                                        
        // intersectsLine checks both endpoint containment and edge crossings automatically
        return rect.intersectsLine(line);
    }

    public static int networkTypeToColour(String type, String source) {
        // Source parameter included for debugging context as requested
        if (SettingsProvider.getInstance().debugMode) {
            // Very noisy debug if uncommented, leaving standard just in case
            // LogUtils.debug("[%s] Requesting colour for network type: %s", source, type);
        }
        
        // Brought these over from Full Screen Map and unified them!
        // This will prevent the biting of butts.
        switch (type) {
            case "ice":
            case "boat":
                return 0xFF13F2F2;
            case "rail":
                return 0xFF000000;
            case "pathMark":
            case "pathUnmark":
                return 0xFFFFFFFF;
            default:
                return 0x00000000;
        }
    }

    /** Source - https://stackoverflow.com/a/9462757
    * Posted by Ivan T, modified by community. See post 'Timeline' for change history
    * Retrieved 2026-06-16, License - CC BY-SA 3.0
    * (Hell yeah! I'm still using stack overflow)*/
    public static double bearing(double lat1, double lon1, double lat2, double lon2){
        double longitude1 = lon1;
        double longitude2 = lon2;
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff= Math.toRadians(longitude2-longitude1);
        double y= Math.sin(longDiff)*Math.cos(latitude2);
        double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);    
        return (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }
}

