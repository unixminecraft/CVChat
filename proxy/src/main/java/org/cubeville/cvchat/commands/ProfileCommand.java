package org.cubeville.cvchat.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;

import org.cubeville.cvchat.CVChat;
import org.cubeville.cvchat.playerdata.ProfileEntry;
import org.cubeville.cvchat.playerdata.ProfilesDao;

public class ProfileCommand extends CommandBase
{
    CVChat plugin;
    SimpleDateFormat dateFormat;
    
    public ProfileCommand(CVChat plugin) {
        super("profile", "cvchat.profile");
        setUsage("§c/profile <player>");
        this.plugin = plugin;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    }

    public void executeC(CommandSender sender, String[] args) {                                
        if(!verifyNotLessArguments(sender, args, 1)) return;
        if(!verifyNotMoreArguments(sender, args, 1)) return;

        UUID playerId = getPDM().getPlayerId(args[0]);
        if(playerId == null) {
            List<String> searchNames = getPDM().getMatchingPlayerNames(args[0]);
            if(searchNames.size() == 0 || args[0].length() < 4) {
                sender.sendMessage("§cPlayer not found.");
            }
            else {
                String n = "";
                for(String name: searchNames) {
                    if(n.length() > 0) n += ", ";
                    if(n.length() >= 150) {
                        n += "... ";
                        break;
                    }
                    n += name;
                }
                if(n.length() > 0) {
                    sender.sendMessage("§cPlayer not found. Did you mean §a" + n + "?");
                }
                else {
                    sender.sendMessage("§cPlayer not found.");
                }
            }
            return;
        }
        if(!sender.hasPermission("cvchat.profile.unlimited")) {
            if(!verifyOutranks(sender, playerId)) return;
        }

        String playerName = getPDM().getPlayerName(playerId);
        boolean isOnline = ProxyServer.getInstance().getPlayer(playerId) != null;

        sender.sendMessage("§4* §r" + playerName);
        if(sender.hasPermission("cvchat.profile.extended")) {
            sender.sendMessage("§4- §r" + (isOnline ? "Peer" : "Last") + " address: §9" + getPDM().getIPAddress(playerId));
        }

        String lastOnline;
        if(isOnline) {
            lastOnline = "§aOnline now";
        }
        else {
            lastOnline = "§9";
            long lastOnlineTime = getPDM().getLastOnline(playerId);
            long diff = (System.currentTimeMillis() - lastOnlineTime) / 1000;
            if(diff < 240) {
                lastOnline ="just now";
            }
            else if(diff < 7200) {
                lastOnline += String.valueOf(diff / 60) + " minutes ago";
            }
            else if(diff < 172800) {
                lastOnline += String.valueOf(diff / 3600) + " hours ago";
            }
            else if(diff < 1209600) {
                lastOnline += String.valueOf(diff / 86400) + " days ago";
            }
            else if(diff < 5356800) {
                lastOnline += String.valueOf(diff / 604800) + " weeks ago";
            }
            else {
                lastOnline += String.valueOf(diff / 2592000) + " months ago";
            }
            lastOnline += "§r (" + dateFormat.format(new Date(lastOnlineTime)) + ")";
        }
        final String lastOnlineMessage = "§4- §rLast on: " + lastOnline;
        // TODO: current location
        ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
                public void run() {
                    List<ProfileEntry> entries = ProfilesDao.getInstance().getProfileEntries(playerId);
                    for(ProfileEntry entry: entries) {
                        String txt = "§c" + dateFormat.format(new Date(entry.getCommentTime())) + "§r " + entry.getComment() + " [" + getPDM().getPlayerName(entry.getAuthor()) + "]";
                        sender.sendMessage(txt);
                    }
                    sender.sendMessage(lastOnlineMessage);
                }
            });
    }
}
