package org.cubeville.cvchat.channels;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.CommandSender;

import org.cubeville.cvchat.Util;

public class LocalChannel extends Channel
{
    Set<UUID> localMuted;

    public LocalChannel(String name, String viewPermission, String sendPermission, String colorPermission, String leavePermission, String format, boolean isDefault, boolean autojoin, boolean listable, boolean filtered, Collection<String> commands) {
        super(name, viewPermission, sendPermission, colorPermission, leavePermission, format, isDefault, autojoin, listable, filtered, commands);
        localMuted = new HashSet<>();
    }

    protected void doSendMessage(CommandSender sender, String formattedMessage) {
        if(!(sender instanceof ProxiedPlayer)) return; // Console can't chat locally
        ProxiedPlayer player = (ProxiedPlayer) sender;

        // 1) Send to all players who monitor local chat and are on different server
        String serverName = player.getServer().getInfo().getName();
        Collection<ProxiedPlayer> allPlayers = ProxyServer.getInstance().getPlayers();
        String greyMessage = "§7" + Util.removeColorCodes(formattedMessage);
        String lm = "";
        for(ProxiedPlayer p: allPlayers) {
            if(p.hasPermission("cvchat.monitor.local")) {
                if(!p.getServer().getInfo().getName().equals(serverName)) {
                    if(!localMuted.contains(p.getUniqueId())) p.sendMessage(greyMessage + " §e*");
                }
                else {
                    if(localMuted.contains(p.getUniqueId())) {
                        if(lm.length() > 0) lm += ",";
                        lm += p.getUniqueId().toString();
                    }
                }
            }
        }

        String idlist = player.getUniqueId().toString();
        if(lm.length() > 0) {
            idlist += ";" + lm;
        }
        // 2) Send message to player's server for further handling
        String msg = "locchat|" + idlist + "|" + formattedMessage;
        ChannelManager.getInstance().getIPC().sendMessage(serverName, msg);
    }

    public void setLocalMonitorMute(UUID playerId, boolean mute) {
        if(mute) {
            localMuted.add(playerId);
        }
        else {
            localMuted.remove(playerId);
        }
    }
}
