package org.cubeville.cvchat.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
        
import org.cubeville.cvchat.playerdata.PlayerDataManager;

import java.util.*;

public class SCommand extends CommandBase {

    public SCommand() {
        super("s", "cvchat.commandsearch");
        setUsage("§c/s <text>");
    }

    public void executeC(CommandSender commandSender, String[] args) {
        if(!(commandSender instanceof ProxiedPlayer)) return;
        ProxiedPlayer sender = (ProxiedPlayer) commandSender;

        String searchstr = "";
        if(args.length >= 1) searchstr = args[0];

        UUID playerId = sender.getUniqueId();

        List<Map<Long, String>> playerCommands = PlayerDataManager.getInstance().getPlayerCommands(playerId);
        if(playerCommands != null) {
            List<String> entries = new ArrayList<>();
            for(Map<Long, String> messages: playerCommands) {
                for(Map.Entry<Long, String> message: messages.entrySet()) {
                    String command = message.getValue();
                    if(command.indexOf(searchstr) >= 0 && command.startsWith("/s ") == false && command.equals("/s") == false)
                        entries.add(command);
                }
            }
            if(entries.size() > 0) {
                Collections.sort(entries);
                String lastentry = "";
                for(String entry: entries) {
                    if(!entry.equals(lastentry)) {
                        TextComponent chat = new TextComponent("§b[§aChat§b] ");
                        chat.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, entry));
                        TextComponent copy = new TextComponent("§b[§aClip§b] ");
                        copy.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry));
                        chat.addExtra(copy);
                        chat.addExtra(new TextComponent("§6" + entry));
                        sender.sendMessage(chat);
                    }
                    lastentry = entry;
                }
            }
            else {
                sender.sendMessage("§cNo matching commands found.");
            }
        }
        else {
            sender.sendMessage("§cNo matching commands found.");            
        }

    }
}

