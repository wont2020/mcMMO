package com.gmail.nossr50.util;

import org.bukkit.entity.Player;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.events.chat.McMMOAdminChatEvent;
import com.gmail.nossr50.events.chat.McMMOPartyChatEvent;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.party.Party;

public class ChatManager {
    private mcMMO plugin;
    private String playerName;
    private String message;

    public ChatManager (mcMMO plugin, String playerName, String message) {
        this.plugin = plugin;
        this.playerName = playerName;
        this.message = message;
    }

    public void handleAdminChat() {
        McMMOAdminChatEvent chatEvent = new McMMOAdminChatEvent(playerName, message);
        plugin.getServer().getPluginManager().callEvent(chatEvent);

        if (chatEvent.isCancelled()) {
            return;
        }

        String adminMessage = chatEvent.getMessage();

        plugin.getLogger().info("[A]<" + playerName + "> " + adminMessage);

        for (Player otherPlayer : plugin.getServer().getOnlinePlayers()) {
            if (Permissions.adminChat(otherPlayer) || otherPlayer.isOp()) {
                otherPlayer.sendMessage(LocaleLoader.getString("Commands.AdminChat.Prefix", new Object[] {playerName}) + adminMessage);
            }
        }
    }

    public void handlePartyChat(Party party) {
        String partyName = party.getName();

        McMMOPartyChatEvent chatEvent = new McMMOPartyChatEvent(playerName, partyName, message);
        plugin.getServer().getPluginManager().callEvent(chatEvent);

        if (chatEvent.isCancelled()) {
            return;
        }

        String partyMessage = chatEvent.getMessage();

        plugin.getLogger().info("[P](" + partyName + ")" + "<" + playerName + "> " + partyMessage);

        for (Player member : party.getOnlineMembers()) {
            member.sendMessage(LocaleLoader.getString("Commands.Party.Chat.Prefix", new Object[] {playerName}) + partyMessage);
        }
    }
}
