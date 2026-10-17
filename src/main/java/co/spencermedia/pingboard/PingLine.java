package co.spencermedia.pingboard;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.entity.Player;

/**
 * Builds a sidebar line laid out like the tab list, with the ping after the bars:
 * {@code [player head][name] [ping bars] [ping]ms}.
 */
final class PingLine {

    private static final Key GUI_ATLAS = Key.key("minecraft:gui");
    // U+200C is 0px wide in the vanilla font, and bold adds 1px: the same 1px head gap as the tab list.
    private static final Component PIXEL_GAP = Component.text("\u200c").decorate(TextDecoration.BOLD);

    // Indexed by bar count; 0 is "unknown".
    private static final Component[] BAR_ICONS = {
        sprite("icon/ping_unknown"),
        sprite("icon/ping_1"),
        sprite("icon/ping_2"),
        sprite("icon/ping_3"),
        sprite("icon/ping_4"),
        sprite("icon/ping_5"),
    };
    // Matches the bar shadow colours of the VanillaTweaks coloured ping icons.
    private static final TextColor[] PING_COLORS = {
        NamedTextColor.DARK_GRAY,
        TextColor.color(0x4D0003),
        TextColor.color(0x872100),
        TextColor.color(0x875500),
        TextColor.color(0x838700),
        TextColor.color(0x00870F),
    };

    private PingLine() {
    }

    static Component render(Player player, int ping) {
        int bars = bars(ping);
        String pingText = (ping < 0 ? "?" : Integer.toString(ping)) + "ms";
        return Component.textOfChildren(
            Component.object(ObjectContents.playerHead(player.getUniqueId())),
            PIXEL_GAP,
            Component.text(player.getName(), NamedTextColor.WHITE),
            Component.space(),
            BAR_ICONS[bars],
            PIXEL_GAP,
            PIXEL_GAP,
            Component.text(pingText, PING_COLORS[bars])
        );
    }

    /** Bar count using the vanilla tab list thresholds; 0 when the ping is unknown. */
    static int bars(int ping) {
        if (ping < 0) return 0;
        if (ping < 150) return 5;
        if (ping < 300) return 4;
        if (ping < 600) return 3;
        if (ping < 1000) return 2;
        return 1;
    }

    private static Component sprite(String sprite) {
        return Component.object(ObjectContents.sprite(GUI_ATLAS, Key.key(sprite)));
    }
}
