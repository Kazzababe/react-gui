package ravioli.gravioli.stategui.gui.partition;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.stategui.gui.event.ItemClickEvent;
import ravioli.gravioli.stategui.gui.partition.item.SimpleMenuItem;

import java.util.function.Consumer;

public class SimpleMenuPartition extends MenuPartition<SimpleMenuPartition> {
    public SimpleMenuPartition(@NotNull final Plugin plugin, @NotNull final Player player,
                               @NotNull final Consumer<SimpleMenuPartition> initConsumer) {
        super(plugin, player, initConsumer);
    }

    @Override
    public void setTitle(@NotNull final Component title) {
        this.rootPartition.setTitle(title);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.rootPartition.getInventory();
    }

    public void setSlot(final int x, final int y, @Nullable final ItemStack itemStack) {
        Preconditions.checkArgument(x < this.getWidth(), "Cannot put an item outside of a partition's width.");
        Preconditions.checkArgument(y < this.getHeight(), "Cannot put an item outside of a partition's height.");

        this.getItems().put(
            y * this.getWidth() + x,
            new SimpleMenuItem(itemStack, null)
        );
    }

    public void setSlot(final int x, final int y, @Nullable final ItemStack itemStack,
                        @NotNull final Consumer<ItemClickEvent> clickEventConsumer) {
        Preconditions.checkArgument(x < this.getWidth(), "Cannot put an item outside of a partition's width.");
        Preconditions.checkArgument(y < this.getHeight(), "Cannot put an item outside of a partition's height.");

        final int slot = y * this.getWidth() + x;

        this.getItems().put(
            slot,
            new SimpleMenuItem(itemStack, null)
        );
        this.rootPartition.setClickEventSlot(this, slot, clickEventConsumer);
    }

    public void setSlot(final int slot, @Nullable final ItemStack itemStack) {
        final int x = this.x + (slot % this.getWidth()); // 1 + 0
        final int y = this.y + (slot / this.getWidth()); // 1 + 0
        final int itemSlot = y * this.parentPartition.width + x; // 1 * 9 + 1

        this.getItems().put(
            itemSlot,
            new SimpleMenuItem(itemStack, null)
        );
    }

    public void setSlot(final int slot, @Nullable final ItemStack itemStack,
                        @NotNull final Consumer<ItemClickEvent> clickEventConsumer) {
        final int x = this.x + (slot % this.getWidth()); // 1 + 0
        final int y = this.y + (slot / this.getWidth()); // 1 + 0
        final int itemSlot = y * this.parentPartition.width + x; // 1 * 9 + 1

        this.getItems().put(
            itemSlot,
            new SimpleMenuItem(itemStack, clickEventConsumer)
        );
        this.rootPartition.setClickEventSlot(this, itemSlot, clickEventConsumer);
    }
}
