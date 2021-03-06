package me.confuser.banmanager.commands;

import me.confuser.banmanager.BanManager;
import me.confuser.banmanager.data.PlayerData;
import me.confuser.banmanager.data.PlayerMuteData;
import me.confuser.bukkitutil.Message;
import me.confuser.bukkitutil.commands.BukkitCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public class UnmuteCommand extends BukkitCommand<BanManager> {

  public UnmuteCommand() {
    super("unmute");
  }

  @Override
  public boolean onCommand(final CommandSender sender, Command command, String commandName, String[] args) {
    if (args.length < 1) {
      return false;
    }

    // Check if UUID vs name
    final String playerName = args[0];
    final boolean isUUID = playerName.length() > 16;
    boolean isMuted = false;

    if (isUUID) {
      isMuted = plugin.getPlayerMuteStorage().isMuted(UUID.fromString(playerName));
    } else {
      isMuted = plugin.getPlayerMuteStorage().isMuted(playerName);
    }

    if (!isMuted) {
      Message message = Message.get("unmute.error.noExists");
      message.set("player", playerName);

      sender.sendMessage(message.toString());
      return true;
    }

    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

      @Override
      public void run() {
        PlayerMuteData mute;

        if (isUUID) {
          mute = plugin.getPlayerMuteStorage().getMute(UUID.fromString(playerName));
        } else {
          mute = plugin.getPlayerMuteStorage().getMute(playerName);
        }

        if (mute == null) {
          sender.sendMessage(Message.get("sender.error.notFound").set("player", playerName).toString
                  ());
          return;
        }

        PlayerData actor;

        if (sender instanceof Player) {
          actor = plugin.getPlayerStorage().getOnline((Player) sender);
        } else {
          actor = plugin.getPlayerStorage().getConsole();
        }

        boolean unmuted = false;

        try {
          unmuted = plugin.getPlayerMuteStorage().unmute(mute, actor);
        } catch (SQLException e) {
          sender.sendMessage(Message.get("sender.error.exception").toString());
          e.printStackTrace();
          return;
        }

        if (!unmuted) {
          return;
        }

        Message message = Message.get("unmute.notify");
        message
                .set("player", mute.getPlayer().getName())
                .set("actor", actor.getName());

        if (!sender.hasPermission("bm.notify.unmute")) {
          message.sendTo(sender);
        }

        plugin.getServer().broadcast(message.toString(), "bm.notify.unmute");
      }

    });

    return true;
  }
}
