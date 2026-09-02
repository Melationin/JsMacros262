package xyz.wagyourtail.jsmacros.client.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Helpers for the {@link ChatFormatting} enum that provide stable access to
 * formatting attributes ({@code getColor}/{@code getId}/{@code getName}/{@code getChar}/{@code isColor}/
 * {@code isFormat}/{@code getByName}/{@code getById}).
 * The enum constant order is unchanged, so ordinal-based lookups stay valid.
 */
public class FormattingUtil {

    private FormattingUtil() {
    }

    /**
     * @return the color value of the formatting, or {@code null} if it is not a color.
     */
    @Nullable
    public static Integer getColor(ChatFormatting formatting) {
        if (!isColor(formatting)) {
            return null;
        }
        TextColor color = TextColor.fromLegacyFormat(formatting);
        return color == null ? null : color.getValue();
    }

    /**
     * @return the index of the formatting ({@code 0}-{@code 15} for colors, {@code 16}-{@code 21} for modifiers).
     */
    public static int getId(ChatFormatting formatting) {
        return formatting.ordinal();
    }

    /**
     * @return the formatting with the given index, or {@code null} if out of range.
     */
    @Nullable
    public static ChatFormatting getById(int id) {
        ChatFormatting[] values = ChatFormatting.values();
        return id >= 0 && id < values.length ? values[id] : null;
    }

    /**
     * @return the formatting with the given name (case-insensitive), or {@code null} if not found.
     */
    @Nullable
    public static ChatFormatting getByName(String name) {
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.name().equalsIgnoreCase(name)) {
                return formatting;
            }
        }
        return null;
    }

    /**
     * @return the legacy section-sign code char for this formatting.
     */
    public static char getChar(ChatFormatting formatting) {
        int ordinal = formatting.ordinal();
        return (char) (ordinal < 16 ? '0' + ordinal : 'k' + (ordinal - 16));
    }

    /**
     * @return {@code true} if this formatting is a color.
     */
    public static boolean isColor(ChatFormatting formatting) {
        return formatting.ordinal() < 16;
    }

    /**
     * @return {@code true} if this formatting is a modifier ({@code RESET} excluded).
     */
    public static boolean isFormat(ChatFormatting formatting) {
        int ordinal = formatting.ordinal();
        return ordinal >= 16 && ordinal < 21;
    }

    /**
     * @return the lowercase name of this formatting, matching the old {@code getName()}.
     */
    public static String getName(ChatFormatting formatting) {
        return formatting.name().toLowerCase(Locale.ROOT);
    }

}
