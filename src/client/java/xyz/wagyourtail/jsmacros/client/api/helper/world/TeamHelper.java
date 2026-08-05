package xyz.wagyourtail.jsmacros.client.api.helper.world;

import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.PlayerTeam;
import xyz.wagyourtail.jsmacros.client.util.FormattingUtil;
import xyz.wagyourtail.doclet.DocletReplaceReturn;
import xyz.wagyourtail.jsmacros.client.api.helper.FormattingHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.TextHelper;
import xyz.wagyourtail.jsmacros.api.BaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Wagyourtail
 * @since 1.3.0
 */
@SuppressWarnings("unused")
public class TeamHelper extends BaseHelper<PlayerTeam> {
    public TeamHelper(PlayerTeam t) {
        super(t);
    }

    /**
     * @return
     * @since 1.3.0
     */
    public String getName() {
        return base.getName();
    }

    /**
     * @return
     * @since 1.3.0
     */
    public TextHelper getDisplayName() {
        return TextHelper.wrap(base.getDisplayName());
    }

    /**
     * @return
     * @since 1.3.0
     */
    public List<String> getPlayerList() {
        return new ArrayList<>(base.getPlayers());
    }

    /**
     * @return the formatting of this team's color.
     * @since 1.8.4
     */
    public FormattingHelper getColorFormat() {
        return new FormattingHelper(getColorFormatting());
    }

    /**
     * @return
     * @since 1.3.0
     * @deprecated use {@link #getColorIndex()} instead.
     */
    @Deprecated
    public int getColor() {
        return getColorIndex();
    }

    /**
     * @return the color index of this team.
     * @since 1.8.4
     */
    public int getColorIndex() {
        return FormattingUtil.getId(getColorFormatting());
    }

    /**
     * @return the color value for this team or {@code -1} if it has no color.
     * @since 1.8.4
     */
    public int getColorValue() {
        Integer color = FormattingUtil.getColor(getColorFormatting());
        return color == null ? -1 : color;
    }

    /**
     * @return the name of this team's color.
     * @since 1.8.4
     */
    @DocletReplaceReturn("FormattingColorName")
    public String getColorName() {
        return FormattingUtil.getName(getColorFormatting());
    }

    /**
     * @return the team's color, or {@link ChatFormatting#RESET} if it has no color.
     */
    private ChatFormatting getColorFormatting() {
        return base.getColor().map(c -> ChatFormatting.values()[c.ordinal()]).orElse(ChatFormatting.RESET);
    }

    /**
     * @return the scoreboard including this team.
     * @since 1.8.4
     */
    public ScoreboardsHelper getScoreboard() {
        return new ScoreboardsHelper(base.getScoreboard());
    }

    /**
     * @return
     * @since 1.3.0
     */
    public TextHelper getPrefix() {
        return TextHelper.wrap(base.getPlayerPrefix());
    }

    /**
     * @return
     * @since 1.3.0
     */
    public TextHelper getSuffix() {
        return TextHelper.wrap(base.getPlayerSuffix());
    }

    /**
     * @return
     * @since 1.3.0
     */
    @DocletReplaceReturn("TeamCollisionRule")
    public String getCollisionRule() {
        return base.getCollisionRule().name;
    }

    /**
     * @return
     * @since 1.3.0
     */
    public boolean isFriendlyFire() {
        return base.isAllowFriendlyFire();
    }

    /**
     * @return
     * @since 1.3.0
     */
    public boolean showFriendlyInvisibles() {
        return base.canSeeFriendlyInvisibles();
    }

    /**
     * @return
     * @since 1.3.0
     */
    @DocletReplaceReturn("TeamVisibilityRule")
    public String nametagVisibility() {
        return base.getNameTagVisibility().name;
    }

    /**
     * @return
     * @since 1.3.0
     */
    @DocletReplaceReturn("TeamVisibilityRule")
    public String deathMessageVisibility() {
        return base.getDeathMessageVisibility().name;
    }

    @Override
    public String toString() {
        return String.format("TeamHelper:{\"name\": \"%s\"}", getDisplayName().toString());
    }

}
