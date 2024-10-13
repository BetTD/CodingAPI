package de.codingair.codingapi.player.chat;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import de.codingair.codingapi.API;
import de.codingair.codingapi.player.data.PacketReader;
import de.codingair.codingapi.server.reflections.IReflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ChatListener implements Listener {
    static final Set<UUID> DEAD_BUTTONS = new HashSet<>();
    private static final IReflection.FieldAccessor<String> text;
    static boolean showError = true;
    private static Class<?> chatPacket;
    private static String messagePrefix = "";

    static {
        try {
            chatPacket = IReflection.getSaveClass(IReflection.ServerPacket.PACKETS, "ServerboundChatCommandPacket");
            messagePrefix = "/";
        } catch (ClassNotFoundException e) {
            chatPacket = IReflection.getClass(IReflection.ServerPacket.PACKETS, "PacketPlayInChat");
        }

        text = IReflection.getField(chatPacket, String.class, 0);
    }

    public ChatListener() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            inject(onlinePlayer);
        }
    }

    @NotNull
    static UUID getRandom() {
        while (true) {
            UUID id = UUID.randomUUID();
            if (DEAD_BUTTONS.contains(id)) continue;
            return id;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        inject(e.getPlayer());
    }

    private void inject(Player player) {
        new PacketReader(player, "chat-button-listener", API.getInstance().getMainPlugin()) {
            @Override
            public boolean readPacket(Object packet) {
                if (packet.getClass().equals(chatPacket)) {
                    try {
                        String msg = text.get(packet);
                        if (msg != null) msg = messagePrefix + msg;

                        if (!ChatButton.isChatButton(msg)) return false;
                        String type = null;
                        UUID uniqueId;

                        if (msg.contains("#")) {
                            String[] a = msg.split("#");
                            uniqueId = UUID.fromString(ChatButton.removePrefix(a[0]));
                            type = a[1];
                        } else uniqueId = UUID.fromString(ChatButton.removePrefix(msg));

                        if (DEAD_BUTTONS.contains(uniqueId)) return true;

                        List<SimpleMessage> messageList = API.getRemovables(null, SimpleMessage.class);
                        boolean used = handleSimpleMessages(type, uniqueId, player, messageList);

                        messageList = API.getRemovables(player, SimpleMessage.class);
                        used |= handleSimpleMessages(type, uniqueId, player, messageList);

                        return used;
                    } catch (Throwable t) {
                        if (!showError) return false;
                        t.printStackTrace();
                        showError = false;
                        return false;
                    }
                }

                return false;
            }

            @Override
            public boolean writePacket(Object packet) {
                return false;
            }
        }.inject();
    }

    private boolean handleSimpleMessages(String type, UUID uniqueId, Player player, List<SimpleMessage> messageList) {
        boolean clicked = false;

        if (!messageList.isEmpty()) {
            for (SimpleMessage message : messageList) {

                ChatButton button = message.getButton(uniqueId);
                if (button != null) {
                    clicked = true;

                    UniversalScheduler.getScheduler(API.getInstance().getMainPlugin()).runTask(() -> {
                        if (button.canClick()) {
                            if (button.getSound() != null) button.getSound().play(player);
                            button.onClick(player);
                        }
                    });

                    break;
                }
            }

            messageList.clear();
        }

        if (clicked) return true;
        else return callForeignClick(player, uniqueId, type);
    }

    private boolean callForeignClick(Player player, UUID uniqueId, String type) {
        return ChatButtonManager.onAsyncInteract(l -> l.onAsyncClick(player, uniqueId, type));
    }
}
