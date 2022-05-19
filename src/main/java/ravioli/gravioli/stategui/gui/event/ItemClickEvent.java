package ravioli.gravioli.stategui.gui.event;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class ItemClickEvent {
    private final InventoryClickEvent bukkitEvent;

    private final ClickType clickType;

    public ItemClickEvent(@NotNull final InventoryClickEvent bukkitEvent) {
        this.bukkitEvent = bukkitEvent;

        this.clickType = bukkitEvent.getClick();
    }

    public @NotNull InventoryClickEvent getBukkitEvent() {
        return this.bukkitEvent;
    }

    public @NotNull ClickType getClickType() {
        return this.clickType;
    }

    public boolean isCancelled() {
        return this.bukkitEvent.isCancelled();
    }

    public void setCancelled(final boolean cancelled) {
        this.bukkitEvent.setCancelled(cancelled);
    }
}
