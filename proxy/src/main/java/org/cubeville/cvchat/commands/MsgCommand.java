package org.cubeville.cvchat.commands;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import org.cubeville.cvchat.Util;
import org.cubeville.cvchat.playerdata.PlayerDataManager;
import org.cubeville.cvchat.sanctions.SanctionManager;

public class MsgCommand extends CommandBase
{
    private static Map<UUID, UUID> lastMessageReceived;
    private static Map<UUID, UUID> lastMessageSent;
    private static Set<UUID> disableRefusal;
    
    public MsgCommand() {

        super("msg");
        setUsage("§c/msg <target> <message...>");
        lastMessageReceived = new HashMap<>();
        lastMessageSent = new HashMap<>();
        disableRefusal = new HashSet<>();
    }

    public static boolean disabledRefusal(UUID player) {
        return disableRefusal.contains(player);
    }
    
    public void executeC(CommandSender commandSender, String[] args) {
        if(!(commandSender instanceof ProxiedPlayer)) return;
        ProxiedPlayer sender = (ProxiedPlayer) commandSender;

        if(sender.hasPermission("cvchat.refusepm")) {
            if(args.length == 0) {
                sender.sendMessage("§aYou " + (disabledRefusal(sender.getUniqueId()) ? "can" : "can't") + " receive private messages when you're vanished.");
                return;
            }
            else if(args.length == 1) {
                if(args[0].equals("on")) {
                    disableRefusal.add(sender.getUniqueId());
                    sender.sendMessage("§aYou will receive private messages now.");
                    return;
                }
                else if(args[0].equals("off")) {
                    disableRefusal.remove(sender.getUniqueId());
                    sender.sendMessage("§aYou will not receive private messages now.");
                    return;
                }
            }
        }
        
        if(!verifyNotLessArguments(sender, args, 2)) return;

        boolean fakeNotFound = false;
            
        ProxiedPlayer recipient = getPlayer(args[0]);
        
        if(recipient == null ||
           (Util.playerIsHidden(recipient) == true && recipient.hasPermission("cvchat.refusepm") == true && sender.hasPermission("cvchat.showvanished") == false && disabledRefusal(recipient.getUniqueId()) == false)) {
            sender.sendMessage("§cPlayer not found!");
            if(recipient == null) return;
            fakeNotFound = true;
        }
        
        if(SanctionManager.getInstance().isPlayerMuted(sender)) {
            if(!recipient.hasPermission("cvchat.mute.staff")) {
                sender.sendMessage("§cYou are muted. You can only send messages to staff members.");
                return;
            }
        }
        
        long firstLogin = PlayerDataManager.getInstance().getFirstLogin(sender.getUniqueId());
        if(firstLogin == 0 || System.currentTimeMillis() - firstLogin < 600000) {
            if(!recipient.hasPermission("cvchat.mute.staff")) {
                sender.sendMessage("§cNo permission.");
                return;
            }
        }
        
        sendMessage(sender, recipient, args, 1, fakeNotFound);
    }

    protected static void sendMessage(ProxiedPlayer sender, ProxiedPlayer recipient, String[] args, int argsOffset, boolean fakeNotFound) {
        String message = Util.removeSectionSigns(Util.joinStrings(args, argsOffset));

        if(!fakeNotFound) sender.sendMessage("§3(To " + recipient.getDisplayName() + "§3): §r" + message);
        String mark = "";
        if(fakeNotFound) mark = "§c*";
        recipient.sendMessage("§3(From " + sender.getDisplayName() + mark + "§3): §r" + message);

        if(!fakeNotFound) {
            lastMessageSent.put(sender.getUniqueId(), recipient.getUniqueId());
            lastMessageReceived.put(sender.getUniqueId(), recipient.getUniqueId());
        }
        lastMessageReceived.put(recipient.getUniqueId(), sender.getUniqueId());
    }

    protected static UUID getLastMessageReceived(UUID player) {
        return lastMessageReceived.get(player);
    }

    protected static UUID getLastMessageSent(UUID player) {
        return lastMessageSent.get(player);
    }
}
