package ravioli.gravioli.guiyo.gui.partition;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.guiyo.gui.event.ItemClickEvent;
import ravioli.gravioli.guiyo.gui.partition.item.SimpleMenuItem;

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

    public void setSlot(final int slot, @Nullable final ItemStack itemStack) {
        final int x = this.x + (slot % this.getWidth()); // 1 + 0
        final int y = this.y + (slot / this.getWidth()); // 1 + 0
        final int itemSlot = y * this.parentPartition.width + x; // 1 * 9 + 1

        this.items.add(
            new MenuPartitionItem(
                new SimpleMenuItem(itemStack, null),
                null,
                itemSlot
            )
        );
    }

    public void setSlot(final int slot, @Nullable final ItemStack itemStack,
                        @NotNull final Consumer<ItemClickEvent> clickEventConsumer) {
        final int x = this.x + (slot % this.getWidth()); // 1 + 0
        final int y = this.y + (slot / this.getWidth()); // 1 + 0
        final int itemSlot = y * this.parentPartition.width + x; // 1 * 9 + 1

        this.items.add(
            new MenuPartitionItem(
                new SimpleMenuItem(itemStack, clickEventConsumer),
                clickEventConsumer,
                itemSlot
            )
        );
        this.rootPartition.setClickEventSlot(itemSlot, clickEventConsumer);
    }
}
