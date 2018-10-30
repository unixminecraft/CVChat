package org.cubeville.cvchat.commands;

// TODO: Lots of common code with /who command

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import org.cubeville.cvchat.Util;
import org.cubeville.cvchat.sanctions.SanctionManager;
import org.cubeville.cvchat.ranks.RankManager;

public class ModlistCommand extends CommandBase
{
    public ModlistCommand() {
        super("modlist", null);
    }

    public void executeC(CommandSender sender, String[] args) {
        if(args.length > 1) {
            sender.sendMessage("§cToo many arguments.");
            sender.sendMessage("/modlist [filter]");
            return;
        }

        String list = "";
        int cnt = 0;
        for(ProxiedPlayer player: ProxyServer.getInstance().getPlayers()) {
            if(RankManager.getInstance().getPriority(player) >= 20) {
                if(args.length == 0 || player.getDisplayName().toUpperCase().indexOf(args[0].toUpperCase()) >= 0) {
                    boolean vis = !Util.playerIsHidden(player);
                    boolean hvis = true;
                    if(sender instanceof ProxiedPlayer) {
                        ProxiedPlayer senderPlayer = (ProxiedPlayer) sender;
                        boolean outranks = getPDM().outranksOrEqual(senderPlayer.getUniqueId(), player.getUniqueId());
                        hvis = isPlayerEqual(senderPlayer, player) || outranks;
                    }
                    if(vis || hvis) {
                        if(list.length() > 0) list += "§r, ";
                        list += "§" + RankManager.getInstance().getColor(player);
                        if(!vis && hvis) list += "§o";
                        list += player.getDisplayName();
                        cnt++;
                    }
                }
            }
        }

        sender.sendMessage("§6Moderators §a(" + cnt + ")§r: " + list);
        
    }

}
