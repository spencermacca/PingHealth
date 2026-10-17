package co.spencermedia.pingboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Owns every viewer's private ping scoreboard and the refresh task.
 * All methods run on the main thread.
 */
final class PingBoard implements Listener {

    private static final String OBJECTIVE_NAME = "pingboard";

    private final Plugin plugin;
    private final Settings settings;
    private final PingSource pingSource;
    private final Map<UUID, Viewer> viewers = new HashMap<>();
    private BukkitTask task;

    PingBoard(Plugin plugin, Settings settings, PingSource pingSource) {
        this.plugin = plugin;
        this.settings = settings;
        this.pingSource = pingSource;
    }

    /** Shows or hides the sidebar for {@code player} only; returns whether it is now shown. */
    boolean toggle(Player player) {
        Viewer viewer = viewers.remove(player.getUniqueId());
        if (viewer != null) {
            viewer.restore(player);
            stopIfIdle();
            return false;
        }
        show(player);
        return true;
    }

    /** Restores every viewer's original scoreboard. Called on plugin disable. */
    void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        viewers.forEach((playerId, viewer) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                viewer.restore(player);
            }
        });
        viewers.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        // No restore needed: the toggle resets on logout, and the next login starts on the main scoreboard.
        viewers.remove(playerId);
        pingSource.forget(playerId);

        String entry = entry(playerId);
        for (Viewer viewer : viewers.values()) {
            viewer.remove(entry);
        }
        stopIfIdle();
    }

    private void show(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, settings.title());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        // Hide the score column entirely. A visible column makes the client reserve a ": "-wide gap before it.
        objective.numberFormat(NumberFormat.blank());

        Viewer viewer = new Viewer(player.getScoreboard(), board, objective);
        // Fill the board before switching to it so the client receives it complete.
        viewer.render(snapshot());
        player.setScoreboard(board);
        viewers.put(player.getUniqueId(), viewer);

        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, settings.refreshTicks(), settings.refreshTicks());
        }
    }

    private void refresh() {
        // If another plugin has since given a viewer a different scoreboard, step aside rather than fight over it.
        viewers.entrySet().removeIf(e -> {
            Player player = Bukkit.getPlayer(e.getKey());
            return player == null || player.getScoreboard() != e.getValue().board;
        });
        if (stopIfIdle()) {
            return;
        }

        List<Line> lines = snapshot();
        for (Viewer viewer : viewers.values()) {
            viewer.render(lines);
        }
    }

    /** The {@code max-lines} highest pings. The client orders them on screen by {@link #sortKey}. */
    private List<Line> snapshot() {
        record Sample(Player player, int ping) {
        }

        return Bukkit.getOnlinePlayers().stream()
            .map(player -> new Sample(player, pingSource.ping(player)))
            .sorted(Comparator.comparingInt(Sample::ping).reversed())
            .limit(settings.maxLines())
            .map(s -> new Line(entry(s.player().getUniqueId()), s.ping(), PingLine.render(s.player(), s.ping())))
            .toList();
    }

    /** The client lists higher scores first, so negate: lowest ping on top, unknown pings at the bottom. */
    private static int sortKey(int ping) {
        return ping < 0 ? Integer.MIN_VALUE : -ping;
    }

    private boolean stopIfIdle() {
        if (!viewers.isEmpty()) {
            return false;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        return true;
    }

    /** Stable, unique score holder for a player. The visible text comes from the score's custom name. */
    private static String entry(UUID playerId) {
        return playerId.toString();
    }

    private record Line(String entry, int ping, Component text) {
    }

    private static final class Viewer {

        final Scoreboard previous;
        final Scoreboard board;
        final Objective objective;
        /** Entry to the ping currently shown for it; the line text only changes when the ping does. */
        final Map<String, Integer> shown = new HashMap<>();

        Viewer(Scoreboard previous, Scoreboard board, Objective objective) {
            this.previous = previous;
            this.board = board;
            this.objective = objective;
        }

        void render(List<Line> lines) {
            Set<String> stale = new HashSet<>(shown.keySet());
            for (Line line : lines) {
                stale.remove(line.entry());
                Integer before = shown.put(line.entry(), line.ping());
                if (before != null && before == line.ping()) {
                    continue;
                }
                Score score = objective.getScore(line.entry());
                score.setScore(sortKey(line.ping()));
                score.customName(line.text());
            }
            for (String entry : stale) {
                remove(entry);
            }
        }

        void remove(String entry) {
            if (shown.remove(entry) != null) {
                board.resetScores(entry);
            }
        }

        /** Puts back the scoreboard the player had before, unless another plugin has replaced ours since. */
        void restore(Player player) {
            if (player.getScoreboard() == board) {
                player.setScoreboard(previous);
            }
        }
    }
}
