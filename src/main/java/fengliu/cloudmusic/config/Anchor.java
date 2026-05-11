package fengliu.cloudmusic.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.minecraft.text.Text;

public enum Anchor implements IConfigOptionListEntry {
    TOP_LEFT    ("cloudmusic.gui.anchor.top_left"),
    TOP_CENTER  ("cloudmusic.gui.anchor.top_center"),
    TOP_RIGHT   ("cloudmusic.gui.anchor.top_right"),
    CENTER_LEFT ("cloudmusic.gui.anchor.center_left"),
    CENTER      ("cloudmusic.gui.anchor.center"),
    CENTER_RIGHT("cloudmusic.gui.anchor.center_right"),
    BOTTOM_LEFT ("cloudmusic.gui.anchor.bottom_left"),
    BOTTOM_CENTER("cloudmusic.gui.anchor.bottom_center"),
    BOTTOM_RIGHT("cloudmusic.gui.anchor.bottom_right");

    private final String translateKey;

    Anchor(String translateKey) {
        this.translateKey = translateKey;
    }

    public static float positionX(Anchor a, float screenWidth, float elementWidth, float offsetX) {
        return switch (a) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> offsetX;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> (screenWidth - elementWidth) / 2f + offsetX;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> screenWidth - elementWidth - offsetX;
        };
    }

    public static float positionY(Anchor a, float screenHeight, float elementHeight, float offsetY) {
        return switch (a) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> offsetY;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> (screenHeight - elementHeight) / 2f + offsetY;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - elementHeight - offsetY;
        };
    }

    public static float anchorX(Anchor a, float elementWidth, float offsetX) {
        return switch (a) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> offsetX;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> offsetX + elementWidth / 2;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> offsetX + elementWidth;
        };
    }

    public static float anchorY(Anchor a, float elementHeight, float offsetY) {
        return switch (a) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> offsetY;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> offsetY + elementHeight / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> offsetY + elementHeight;
        };
    }

    @Override
    public String getStringValue() {
        return this.name();
    }

    @Override
    public String getDisplayName() {
        return Text.translatable(this.translateKey).getString();
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward) {
        int id = this.ordinal();
        if (forward) {
            if (++id >= values().length) id = 0;
        } else {
            if (--id < 0) id = values().length - 1;
        }
        return values()[id];
    }

    @Override
    public IConfigOptionListEntry fromString(String value) {
        for (Anchor a : values()) {
            if (a.getDisplayName().equals(value)) return a;
        }
        return TOP_LEFT;
    }
}
