package de.codingair.codingapi.player.gui;

import de.codingair.codingapi.API;
import de.codingair.codingapi.server.specification.Version;
import de.codingair.codingapi.utils.Removable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public abstract class PlayerItem extends ItemStack implements Removable {
    private final UUID uniqueId = UUID.randomUUID();
    private final JavaPlugin plugin;
    private final Player player;
    private boolean freezed = true;
    private long lastClick = 0;

    public PlayerItem(JavaPlugin plugin, Player player, ItemStack item) {
        super(item);

        this.plugin = plugin;
        this.player = player;

        API.addRemovable(this);
        GUIListener.register(plugin);
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public void destroy() {
        remove();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public void remove() {
        // fix (1.20.5):
        // If the item could not be found,
        // the slot number was moved out of the actual available content slots
        // which caused players being kicked from the server due to a network issue.

        Integer slot = null;
        for (int i = 0; i < player.getInventory().getContents().length; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (equals(stack)) {
                slot = i;
                break;
            }
        }

        if (slot != null) player.getInventory().setItem(slot, null);
        API.removeRemovable(this);
    }

    public void trigger(PlayerInteractEvent e) {
        if (System.currentTimeMillis() - lastClick <= 50) return;
        lastClick = System.currentTimeMillis();
        onInteract(e);
    }

    public abstract void onInteract(PlayerInteractEvent e);

    public abstract void onHover(PlayerItemHeldEvent e);

    public abstract void onUnhover(PlayerItemHeldEvent e);

    public boolean isFreezed() {
        return freezed;
    }

    public PlayerItem setFreezed(boolean freezed) {
        this.freezed = freezed;
        return this;
    }

    public static boolean isUsing(Player p) {
        return API.getRemovable(p, PlayerItem.class) != null;
    }

    public void setDisplayName(String name) {
        ItemMeta meta = getItemMeta();
        meta.setDisplayName(name);
        setItemMeta(meta);
    }

    public String getDisplayName() {
        ItemMeta meta = getItemMeta();
        return meta.hasDisplayName() ? meta.getDisplayName() : null;
    }
}
