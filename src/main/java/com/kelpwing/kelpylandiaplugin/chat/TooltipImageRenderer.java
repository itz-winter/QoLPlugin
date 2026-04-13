package com.kelpwing.kelpylandiaplugin.chat;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders a Minecraft-style tooltip as a PNG image using Java2D.
 * <p>
 * The generated image mimics the in-game item tooltip appearance:
 * <ul>
 *   <li>Dark semi-transparent background with purple gradient border</li>
 *   <li>Item name coloured by rarity: white (common), yellow (uncommon),
 *       aqua (rare), light-purple (epic). Custom-renamed items always show aqua italic.</li>
 *   <li>Potion effects in aqua</li>
 *   <li>Enchantments in gray (curses in red)</li>
 *   <li>Attribute modifier lines in dark green (positive) / red (negative),
 *       grouped under a gray slot header</li>
 *   <li>Unbreakable in blue; Durability line in white</li>
 *   <li>Lore in dark purple italic</li>
 *   <li>Material ID in dark gray</li>
 * </ul>
 * <p>
 * This class is thread-safe and designed to be called from async threads
 * (e.g. Discord event handlers).
 * <p>
 * Content logic cherry-picked from InteractiveChat-DiscordSRV-Addon's
 * {@code DiscordItemStackUtils.getToolTip()}, using Java2D rendering
 * instead of the resource-pack bitmap font system.
 */
public class TooltipImageRenderer {

    // ── Colour palette (Minecraft tooltip colours) ───────────────
    private static final Color BG_FILL             = new Color(16, 0, 16, 240);
    private static final Color BORDER_OUTER        = new Color(16, 0, 16, 255);
    private static final Color BORDER_INNER_TOP    = new Color(80, 0, 255, 80);
    private static final Color BORDER_INNER_BOTTOM = new Color(40, 0, 127, 80);
    private static final Color TEXT_WHITE          = new Color(255, 255, 255);
    private static final Color TEXT_YELLOW         = new Color(255, 255, 85);
    private static final Color TEXT_AQUA           = new Color(85, 255, 255);
    private static final Color TEXT_LIGHT_PURPLE   = new Color(255, 85, 255);
    private static final Color TEXT_GRAY           = new Color(170, 170, 170);
    private static final Color TEXT_DARK_GRAY      = new Color(85, 85, 85);
    private static final Color TEXT_RED            = new Color(255, 85, 85);
    private static final Color TEXT_PURPLE         = new Color(170, 0, 170);
    private static final Color TEXT_DARK_GREEN     = new Color(0, 170, 0);
    private static final Color TEXT_BLUE           = new Color(85, 85, 255);

    // ── Layout constants ─────────────────────────────────────────
    private static final int PADDING_X   = 10;
    private static final int PADDING_Y   = 8;
    private static final int LINE_HEIGHT = 18;
    private static final int BORDER_WIDTH = 2;
    private static final String FONT_NAME = "Consolas";
    private static final int FONT_SIZE    = 14;

    /**
     * A single line of tooltip text with its colour.
     */
    private static class TooltipLine {
        final String text;
        final Color color;
        final boolean italic;

        TooltipLine(String text, Color color, boolean italic) {
            this.text   = text;
            this.color  = color;
            this.italic = italic;
        }
    }

    /**
     * Render a tooltip image for the given item data.
     *
     * @param data the item display data snapshot
     * @return PNG image bytes as an InputStream, or null if the item is air/nothing
     */
    public static InputStream renderTooltip(ItemDisplayData data) {
        if (data == null || data.getType() != ItemDisplayData.DisplayType.ITEM) {
            return null;
        }
        if ("AIR".equals(data.getMaterialName()) || data.getAmount() == 0) {
            return null;
        }

        List<TooltipLine> lines = buildTooltipLines(data);
        if (lines.isEmpty()) return null;

        try {
            BufferedImage image = renderLines(lines);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Render a tooltip for an inventory/ender chest display.
     */
    public static InputStream renderInventoryTooltip(ItemDisplayData data) {
        if (data == null) return null;
        if (data.getType() != ItemDisplayData.DisplayType.INVENTORY
                && data.getType() != ItemDisplayData.DisplayType.ENDER_CHEST) {
            return null;
        }

        List<TooltipLine> lines = buildInventoryLines(data);
        if (lines.isEmpty()) return null;

        try {
            BufferedImage image = renderLines(lines);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Line building
    // ════════════════════════════════════════════════════════════════

    private static List<TooltipLine> buildTooltipLines(ItemDisplayData data) {
        List<TooltipLine> lines = new ArrayList<>();

        // 1. Item name — colour by rarity; custom-renamed → italic aqua (matches vanilla)
        String name = data.getItemName();
        if (data.getAmount() > 1) {
            name += " x" + data.getAmount();
        }
        if (data.hasCustomName()) {
            // Custom name always renders italic aqua regardless of rarity
            lines.add(new TooltipLine(name, TEXT_AQUA, true));
        } else {
            Color rarityColor = rarityToColor(data.getRarity());
            lines.add(new TooltipLine(name, rarityColor, false));
        }

        // 2. Potion effects (aqua — matching IC's rendering for positive effects)
        for (String effect : data.getPotionEffects()) {
            lines.add(new TooltipLine(effect, TEXT_AQUA, false));
        }

        // 3. Enchantments
        if (!data.isHideEnchants()) {
            for (Map.Entry<String, Integer> ench : data.getEnchantments().entrySet()) {
                String enchName = ench.getKey();
                int level = ench.getValue();
                boolean isCurse = enchName.toLowerCase().contains("curse");
                Color color = isCurse ? TEXT_RED : TEXT_GRAY;
                String levelStr = "";
                // IC: only show level numeral if maxLevel > 1 or level > 1
                if (level > 1) {
                    levelStr = " " + toRoman(level);
                }
                lines.add(new TooltipLine(enchName + levelStr, color, false));
            }
        }

        // 4. Lore
        for (String loreLine : data.getLore()) {
            lines.add(new TooltipLine(loreLine, TEXT_PURPLE, true));
        }

        // 5. Attribute modifier lines (slot header in gray, values in dark green/red)
        if (!data.isHideAttributes()) {
            for (String attrLine : data.getAttributeLines()) {
                if (attrLine.startsWith("When in ")) {
                    // Blank separator before each slot section (matches IC)
                    lines.add(new TooltipLine("", TEXT_GRAY, false));
                    lines.add(new TooltipLine(attrLine, TEXT_GRAY, false));
                } else {
                    // Determine colour from sign
                    boolean negative = attrLine.trim().startsWith("-")
                            || attrLine.trim().startsWith(" -");
                    lines.add(new TooltipLine(attrLine, negative ? TEXT_RED : TEXT_DARK_GREEN, false));
                }
            }
        }

        // 6. Unbreakable (blue — matches IC/vanilla)
        if (data.isUnbreakable() && !data.isHideDurability()) {
            lines.add(new TooltipLine("Unbreakable", TEXT_BLUE, false));
        }

        // 7. Durability line — IC shows "Durability: X / MAX" only when damaged
        if (!data.isUnbreakable() && !data.isHideDurability()
                && data.getMaxDurability() > 0
                && data.getDurability() < data.getMaxDurability()) {
            String durLine = "Durability: " + data.getDurability() + " / " + data.getMaxDurability();
            lines.add(new TooltipLine(durLine, TEXT_WHITE, false));
        }

        // 8. Material ID (dark gray — matches IC's "showAdvanceDetails" minecraft:id line)
        String matId = "minecraft:" + data.getMaterialName().toLowerCase();
        lines.add(new TooltipLine(matId, TEXT_DARK_GRAY, false));

        return lines;
    }

    private static List<TooltipLine> buildInventoryLines(ItemDisplayData data) {
        List<TooltipLine> lines = new ArrayList<>();

        boolean isEC = data.getType() == ItemDisplayData.DisplayType.ENDER_CHEST;
        String title = isEC ? "Ender Chest" : "Inventory";
        lines.add(new TooltipLine(title, isEC ? TEXT_LIGHT_PURPLE : TEXT_WHITE, false));

        lines.add(new TooltipLine(
                "Slots: " + data.getUsedSlots() + "/" + data.getTotalSlots(),
                TEXT_GRAY, false));

        for (String item : data.getTopItems()) {
            lines.add(new TooltipLine("  " + item, TEXT_GRAY, false));
        }

        if (data.getTopItems().size() < data.getUsedSlots()) {
            int remaining = data.getUsedSlots() - data.getTopItems().size();
            if (remaining > 0) {
                lines.add(new TooltipLine("  ..." + remaining + " more", TEXT_DARK_GRAY, true));
            }
        }

        return lines;
    }

    // ════════════════════════════════════════════════════════════════
    //  Image rendering
    // ════════════════════════════════════════════════════════════════

    private static BufferedImage renderLines(List<TooltipLine> lines) {
        // First pass: measure text widths to determine image size
        BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = measure.createGraphics();
        Font baseFont   = new Font(FONT_NAME, Font.PLAIN, FONT_SIZE);
        Font italicFont = new Font(FONT_NAME, Font.ITALIC, FONT_SIZE);
        mg.setFont(baseFont);

        int maxWidth = 0;
        for (TooltipLine line : lines) {
            if (line.text.isEmpty()) continue;
            mg.setFont(line.italic ? italicFont : baseFont);
            int w = mg.getFontMetrics().stringWidth(line.text);
            if (w > maxWidth) maxWidth = w;
        }
        mg.dispose();

        int imgWidth  = maxWidth + PADDING_X * 2 + BORDER_WIDTH * 2 + 4;
        int imgHeight = lines.size() * LINE_HEIGHT + PADDING_Y * 2 + BORDER_WIDTH * 2;

        // Minimum size
        imgWidth  = Math.max(imgWidth, 100);
        imgHeight = Math.max(imgHeight, 30);

        // Second pass: render
        BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // ── Background ───────────────────────────────────────────
        g.setColor(BORDER_OUTER);
        g.fillRect(0, 0, imgWidth, imgHeight);

        g.setColor(BG_FILL);
        g.fillRect(BORDER_WIDTH, BORDER_WIDTH,
                imgWidth - BORDER_WIDTH * 2, imgHeight - BORDER_WIDTH * 2);

        // Inner gradient border (the characteristic MC purple fade)
        g.setPaint(new GradientPaint(
                0, BORDER_WIDTH, BORDER_INNER_TOP,
                0, imgHeight - BORDER_WIDTH, BORDER_INNER_BOTTOM));
        g.fillRect(BORDER_WIDTH, BORDER_WIDTH + 1,
                1, imgHeight - BORDER_WIDTH * 2 - 2);
        g.fillRect(imgWidth - BORDER_WIDTH - 1, BORDER_WIDTH + 1,
                1, imgHeight - BORDER_WIDTH * 2 - 2);
        g.setColor(BORDER_INNER_TOP);
        g.fillRect(BORDER_WIDTH + 1, BORDER_WIDTH,
                imgWidth - BORDER_WIDTH * 2 - 2, 1);
        g.setColor(BORDER_INNER_BOTTOM);
        g.fillRect(BORDER_WIDTH + 1, imgHeight - BORDER_WIDTH - 1,
                imgWidth - BORDER_WIDTH * 2 - 2, 1);

        // ── Text ─────────────────────────────────────────────────
        int x = PADDING_X + BORDER_WIDTH;
        int y = PADDING_Y + BORDER_WIDTH + FONT_SIZE; // baseline

        for (TooltipLine line : lines) {
            if (!line.text.isEmpty()) {
                g.setFont(line.italic ? italicFont : baseFont);

                // Drop shadow (Minecraft-style: 1px offset at ¼ brightness)
                Color shadow = new Color(
                        line.color.getRed() / 4,
                        line.color.getGreen() / 4,
                        line.color.getBlue() / 4,
                        line.color.getAlpha());
                g.setColor(shadow);
                g.drawString(line.text, x + 1, y + 1);

                g.setColor(line.color);
                g.drawString(line.text, x, y);
            }
            y += LINE_HEIGHT;
        }

        g.dispose();
        return image;
    }

    // ════════════════════════════════════════════════════════════════
    //  Utility
    // ════════════════════════════════════════════════════════════════

    /** Maps ItemDisplayData.Rarity to a Java2D Color matching Minecraft's name colours. */
    private static Color rarityToColor(ItemDisplayData.Rarity rarity) {
        switch (rarity) {
            case UNCOMMON: return TEXT_YELLOW;
            case RARE:     return TEXT_AQUA;
            case EPIC:     return TEXT_LIGHT_PURPLE;
            default:       return TEXT_WHITE;
        }
    }

    private static String toRoman(int value) {
        if (value <= 0 || value > 10) return String.valueOf(value);
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return numerals[value];
    }
}
