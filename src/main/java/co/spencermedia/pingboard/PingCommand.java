package co.spencermedia.pingboard;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /ping}: toggles the ping sidebar for the player who runs it. */
final class PingCommand implements BasicCommand {

    static final String PERMISSION = "pingboard.admin";

    private final PingBoard pingBoard;

    PingCommand(PingBoard pingBoard) {
        this.pingBoard = pingBoard;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /ping.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return;
        }

        boolean shown = pingBoard.toggle(player);
        player.sendMessage(shown
            ? Component.text("Ping sidebar enabled.", NamedTextColor.GREEN)
            : Component.text("Ping sidebar disabled.", NamedTextColor.RED));
    }
}
