package ravioli.gravioli.stategui.gui.event.partition;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.stategui.Menu;

import java.util.Objects;

public class BottomInventoryClickEvent implements MenuPartitionEvent {
    private final InventoryClickEvent clickEvent;

    public BottomInventoryClickEvent(@NotNull final InventoryClickEvent clickEvent) {
        this.clickEvent = clickEvent;
    }

    /**
     * Get the type of click.
     *
     * @return the click type
     */
    public @NotNull ClickType getClickType() {
        return this.clickEvent.getClick();
    }

    /**
     * Get the clicked inventory.
     *
     * @return The clicked inventory
     */
    public @NotNull Inventory getClickedInventory() {
        return this.clickEvent.getClickedInventory();
    }

    /**
     * Return the item that was clicked.
     *
     * @return the clicked item or air if there wasn't one
     */
    public @NotNull ItemStack getClickedItem() {
        return Objects.requireNonNullElse(this.clickEvent.getCurrentItem(), Menu.AIR.clone());
    }

    /**
     * Set the item that was clicked.
     *
     * @param itemStack the new item
     */
    public void setClickedItem(@Nullable final ItemStack itemStack) {
        this.clickEvent.setCurrentItem(itemStack);
    }

    /**
     * Return the item on the player's cursor when this event was triggered.
     *
     * @return the cursor item or air if there was no item
     */
    public @NotNull ItemStack getItemOnCursor() {
        return Objects.requireNonNullElse(this.clickEvent.getCursor(), Menu.AIR.clone());
    }

    /**
     * Set the item on the player's cursor.
     *
     * @param itemStack the new item
     */
    public void setItemOnCursor(@NotNull final ItemStack itemStack) {
        this.clickEvent.getWhoClicked().setItemOnCursor(itemStack);
    }

    /**
     * Get whether the click event is cancelled or not.
     *
     * @return whether the click event is cancelled
     */
    public boolean isCancelled() {
        return this.clickEvent.isCancelled();
    }

    /**
     * Set whether the click event should be cancelled or not.
     *
     * @param cancelled cancel event or not
     */
    public void setCancelled(final boolean cancelled) {
        this.clickEvent.setCancelled(cancelled);
    }
}
