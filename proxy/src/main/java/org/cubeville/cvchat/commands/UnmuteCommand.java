package org.cubeville.cvchat.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import org.cubeville.cvchat.Util;
import org.cubeville.cvchat.sanctions.SanctionManager;

public class UnmuteCommand extends CommandBase
{
    public UnmuteCommand() {
        super("unmute");
    }

    public void executeC(CommandSender commandSender, String[] args) {
        if(!(commandSender instanceof ProxiedPlayer)) return;
        ProxiedPlayer sender = (ProxiedPlayer) commandSender;

        if(args.length != 1) {
            sender.sendMessage("§cToo " + (args.length < 1 ? "few" : "many") + " arguments.");
            sender.sendMessage("§c/unmute <player>");
            return;
        }

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[0]);
        if(player == null) {
            sender.sendMessage("§cNo player found!");
            return;
        }

        if(!verify(sender, !player.getUniqueId().equals(sender.getUniqueId()), "§cYou can't unmute yourself.")) return;

        if(!verifyOutranks(sender, player)) {
            sender.sendMessage("§cNo permission.");
            return;
        }
        
        if(!getSanctionManager().isPlayerMuted(player)) {
            sender.sendMessage("§cPlayer is not muted.");
            return;
        }

        getSanctionManager().unmutePlayer(player);

        player.sendMessage("§aYou have been unmuted.");

        for(ProxiedPlayer n: Util.getPlayersWithPermission("cvchat.mute.notify")) {
            n.sendMessage("§a" + player.getDisplayName() + "§a has been unmuted by " + sender.getDisplayName() + "§a.");
        }

        sender.sendMessage("§cPlease consider making a /note if you think this mute would justify one.");
    }

    private SanctionManager getSanctionManager() {
        return SanctionManager.getInstance();
    }

}
