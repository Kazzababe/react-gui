package ravioli.gravioli.stategui.gui.partition;

import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.stategui.gui.event.ItemClickEvent;
import ravioli.gravioli.stategui.gui.partition.item.MenuItem;
import ravioli.gravioli.stategui.gui.partition.item.SimpleMenuItem;

import java.util.Map;
import java.util.function.Consumer;

public class MaskedMenuPartition extends MenuPartition<MaskedMenuPartition> {
    private String mask;
    private int maskSize;

    public MaskedMenuPartition(@NotNull final Plugin plugin, @NotNull final Player player,
                               @NotNull final Consumer<MaskedMenuPartition> initConsumer) {
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

    /**
     * Get the size of the mask i.e. how many 1s appear in the mask.
     *
     * @return the size of the mask
     */
    public int getMaskSize() {
        return this.maskSize;
    }

    /**
     * Set the mask used when filling in items.
     * Example: "000010000" is a mask that would fill in the central item in the first row of the partition.
     *
     * @param mask mask consisting of 0's and 1's
     */
    public void setMask(@NotNull final String mask) {
        for (int i = 0; i < mask.length(); i++) {
            char maskChar = mask.charAt(i);

            if (maskChar != '0' && maskChar != '1' && maskChar != ' ') {
                throw new IllegalArgumentException("Invalid mask format! Partition mask must contain only 0s, 1s, and spaces.");
            }
        }
        this.mask = mask;
        this.maskSize = StringUtils.countMatches(mask, "1");
    }

    private @Nullable MenuItem getItemAtSlot(final int slot) {
        for (final Map.Entry<Integer, MenuItem> menuItemEntry : this.getItems().entrySet()) {
            if (menuItemEntry.getKey() == slot) {
                return menuItemEntry.getValue();
            }
        }
        return null;
    }

    private int getNextSlot() {
        if (this.mask == null) {
            return -1;
        }
        for (int i = 0, slot = 0; i < this.mask.length(); i++, slot++) {
            char maskChar = this.mask.charAt(i);

            if (maskChar == '0') {
                continue;
            }
            if (maskChar == ' ') {
                slot--;

                continue;
            }
            final int x = this.x + (slot % this.getWidth()); // 1 + 0
            final int y = this.y + (slot / this.getWidth()); // 1 + 0
            final int itemSlot = y * this.parentPartition.width + x; // 1 * 9 + 1
            final MenuItem partitionItem = this.getItemAtSlot(itemSlot);

            if (partitionItem == null) {
                return slot;
            }
        }
        return -1;
    }

    public void fillItem(@NotNull final ItemStack itemStack) {
        final int slot = this.getNextSlot();

        if (slot == -1) {
            return;
        }
        final int x = this.x + (slot % this.getWidth()); // 1 + 0
        final int y = this.y + (slot / this.getWidth()); // 1 + 0
        final int itemSlot = y * this.parentPartition.width + x; // 1 * 9 + 1

        this.getItems().put(
            itemSlot,
            new SimpleMenuItem(itemStack, null)
        );
    }

    public void fillItem(@NotNull final ItemStack itemStack,
                         @NotNull final Consumer<ItemClickEvent> clickEventConsumer) {
        final int slot = this.getNextSlot();

        if (slot == -1) {
            return;
        }
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
