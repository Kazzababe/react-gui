package ravioli.gravioli.guiyo.gui.event.partition;

import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

public class MenuCloseEvent implements MenuPartitionEvent {
    private final InventoryCloseEvent.Reason closeReason;

    private boolean cancelled;

    public MenuCloseEvent(@NotNull final InventoryCloseEvent.Reason closeReason) {
        this.closeReason = closeReason;
    }

    public @NotNull InventoryCloseEvent.Reason getCloseReason() {
        return this.closeReason;
    }

    /**
     * Get whether the close event is cancelled or not.
     *
     * @return whether the close event is cancelled
     */
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Set whether the close event should be cancelled or not.
     *
     * @param cancelled cancel event or not
     */
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
}
