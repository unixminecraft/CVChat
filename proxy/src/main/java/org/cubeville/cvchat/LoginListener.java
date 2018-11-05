package org.cubeville.cvchat;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import org.cubeville.cvchat.channels.ChannelManager;
import org.cubeville.cvchat.playerdata.PlayerDataManager;
import org.cubeville.cvchat.ranks.RankManager;
import org.cubeville.cvchat.tickets.TicketManager;

public class LoginListener implements Listener
{
    ChannelManager channelManager;
    TicketManager ticketManager;

    Map<UUID, Long> newPlayerLogins;
    
    public LoginListener(ChannelManager channelManager, TicketManager ticketManager) {
        this.channelManager = channelManager;
        this.ticketManager = ticketManager;
        newPlayerLogins = new HashMap<>();
    }

    @EventHandler
    public void onPreLogin(final PreLoginEvent event) {
        String playerName = event.getConnection().getName();
        int protocolVersion = event.getConnection().getVersion();
        if(protocolVersion != 340) {
            event.setCancelled(true);
            String currentVersion = "Undeterminable";
            if(protocolVersion > 340) currentVersion = "1.13 or newer";
            if(protocolVersion < 340) currentVersion = "1.12.1/1.11 or older";
            event.setCancelReason("§cPlease use §aMinecraft v1.12.2 §cfor Cubeville.\nYou're currently using: §e" + currentVersion);
            return;
        }
        Collection<ProxiedPlayer> players = ProxyServer.getInstance().getPlayers();
        for(ProxiedPlayer p: players) {
            if(playerName.equals(p.getName())) {
                event.setCancelled(true);
                event.setCancelReason("§cYou're already logged in to this server.");
                return;
            }
        }
    }

    private int newPlayerLoginCount(int Time) {
        int ret = 0;
        long currentTime = System.currentTimeMillis();
        for(UUID p: newPlayerLogins.keySet()) {
            long tdiff = currentTime - newPlayerLogins.get(p);
            if(tdiff > Time) ret++;
        }
        return ret;
    }
    
    @EventHandler
    public void onLogin(final LoginEvent event) {
        PlayerDataManager pdm = PlayerDataManager.getInstance();
        PendingConnection connection = event.getConnection();
        UUID uuid = connection.getUniqueId();

        if(!pdm.isPlayerKnown(uuid)) {
            // Count number of new players in last two minutes
            int c2min = newPlayerLoginCount(120000);
            int c10min = newPlayerLoginCount(600000);
            System.out.println("New player " + uuid + " logging in. 2 min cnt = " + c2min + ", 10 min cnt = " + c10min);
            if(c2min >= 4 || c10min >= 8) {
                event.setCancelled(true);
                event.setCancelReason("§cSorry, all login slots are currently occupied.\n§cPlease try again in a few minutes.");
                System.out.println("Login denied for player " + uuid + ", 2 minute login count: " + c2min + ", 10 minute count: " + c10min);
                return;
            }
            System.out.println("Access granted at " + System.currentTimeMillis() + ".");
        }

        if(pdm.isBanned(uuid, true)) {
            event.setCancelled(true);
            boolean perm = pdm.isPermanentlyBanned(connection.getUniqueId());
            String type = perm ? "permanently" : "temporarily";
            String endOfBan = "";
            if(!perm) {
                SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss z");
                endOfBan = " until §e" + sdf.format(new Date(pdm.getEndOfTempban(uuid)));
            }
            event.setCancelReason("§cYou're " + type + " banned from this server" + endOfBan + ".\n§cReason: §e" + pdm.getBanReason(uuid));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPostLogin(final PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        channelManager.playerLogin(player);
        
        PlayerDataManager pdm = PlayerDataManager.getInstance();
        UUID playerId = player.getUniqueId();

        boolean newPlayer = false;
        
        if(!player.hasPermission("cvchat.silent.join")) {
            if(!pdm.isPlayerKnown(playerId)) {
                newPlayer = true;
                sendWelcomeMessage(player.getName());
                pdm.addPlayer(playerId, player.getName(), getStrippedIpAddress(player));
                sendMessage(player.getDisplayName(), "joined");
                newPlayerLogins.put(playerId, System.currentTimeMillis());
            }
            else {
                if(pdm.getPlayerName(playerId).equals(player.getName())) {
                    sendMessage(player.getDisplayName(), "joined");
                }
                else {
                    sendMessage(player.getDisplayName() + " (formerly known as " + pdm.getPlayerName(playerId) + ") ", "joined");
                    pdm.changePlayerName(playerId, player.getName());
                }
            }
        }
        else {
            sendSilentMessage(player.getDisplayName(), "joined");
            if(!pdm.isPlayerKnown(playerId)) {
                pdm.addPlayer(playerId, player.getName(), getStrippedIpAddress(player));
            }
            else if(!pdm.getPlayerName(playerId).equals(player.getName())) {
                pdm.changePlayerName(playerId, player.getName());
            }
        }
        pdm.playerLogin(playerId, getStrippedIpAddress(player), RankManager.getInstance().getPriority(player));

        ticketManager.playerLogin(player);
        
        if(!pdm.finishedTutorial(playerId)) {
            channelManager.getIPC().sendMessage("newskylands", "xwportal|" + playerId + "|portal:TutorialSpawn|newskylands");
        }

        if(player.hasPermission("cvchat.ticket")) {
            int cnt = ticketManager.getNumberOfOpenTickets();
            if(cnt > 0) {
                player.sendMessage("§a" + cnt + " open modreq(s).");
            }
        }

        System.out.println("Player " + player.getName() + " logged in" + (newPlayer ? " for the first time." : "."));
    }

    private String getStrippedIpAddress(ProxiedPlayer player) {
        String ret = player.getAddress().getAddress().toString();
        ret = ret.substring(ret.indexOf("/") + 1);
        return ret;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDisconnect(final PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        channelManager.playerDisconnect(player);
        if(!player.hasPermission("cvchat.silent.leave")) {
            sendMessage(player.getDisplayName(), "left");
        }
        else {
            sendSilentMessage(player.getDisplayName(), "left");
        }
        PlayerDataManager.getInstance().playerLogout(player.getUniqueId());
        System.out.println("Player " + player.getName() + " logged off.");
    }

    @EventHandler
    public void onProxyPing(final ProxyPingEvent event) {
        ServerPing ping = event.getResponse();
        int cnt = 0;
        for(ProxiedPlayer player: ProxyServer.getInstance().getPlayers()) {
            if(!Util.playerIsHidden(player)) {
                cnt++;
            }
        }
        ping.getPlayers().setOnline(cnt);
        ping.getPlayers().setSample(new ServerPing.PlayerInfo[0]);
    }
    
    private void sendMessage(String playerName, String status) {
        for(ProxiedPlayer p: ProxyServer.getInstance().getPlayers()) {
            p.sendMessage("§e" + playerName + "§e " + status + " the game.");
        }
    }

    private void sendSilentMessage(String playerName, String status) {
        for(ProxiedPlayer p: ProxyServer.getInstance().getPlayers()) {
            if(p.hasPermission("cvchat.silent.view")) {
                p.sendMessage("§3" + playerName + "§3 " + status + " the game silently.");
            }
        }
    }
    
    private void sendWelcomeMessage(String playerName) {
        for(ProxiedPlayer p: ProxyServer.getInstance().getPlayers()) {
            p.sendMessage("§eEveryone welcome Cubeville's newest player, " + playerName + "§e!");
        }        
    }
}
