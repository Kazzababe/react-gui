package ravioli.gravioli.guiyo.gui.partition;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.guiyo.Menu;
import ravioli.gravioli.guiyo.gui.event.ItemClickEvent;
import ravioli.gravioli.guiyo.gui.event.partition.MenuPartitionEvent;
import ravioli.gravioli.guiyo.gui.partition.item.MenuItem;
import ravioli.gravioli.guiyo.gui.property.MenuProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class MenuPartition<T extends MenuPartition<T>> {
    private final Player player;
    private final List<MenuPropertyRenderCheck> properties = new ArrayList<>();
    private final List<MenuPartition<?>> partitions = new ArrayList<>();

    protected final Plugin plugin;
    protected final List<MenuPartitionItem> items = new ArrayList<>();
    protected final Consumer<T> initConsumer;

    private List<MenuPartitionItem> previousItems = new ArrayList<>();
    protected RootPartition rootPartition;
    protected MenuPartition<?> parentPartition;
    protected int width;
    protected int height;
    protected int x;
    protected int y;

    private int propertyIndex;
    private int renderCount;

    public MenuPartition(@NotNull final Plugin plugin, @NotNull final Player player,
                         @NotNull final Consumer<T> initConsumer) {
        this.plugin = plugin;
        this.player = player;
        this.initConsumer = initConsumer;
    }

    /**
     * Get the owning player of this partition.
     *
     * @return the partition's player
     */
    public @NotNull Player getPlayer() {
        return this.player;
    }

    /**
     * Get the width of this partition.
     *
     * @return the partition's width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Get the height of this partition.
     *
     * @return the partition's height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Get the size of this partition i.e. how many slots the partition contains.
     *
     * @return the partition's size
     */
    public int getSize() {
        return this.width * this.height;
    }

    /**
     * Set the width of this partition 1 - 9.
     * Note: This method is not supported in the root partition as the root partition must always have a
     *       partition size of 9.
     *
     * @param width the partition's new width
     */
    public void setWidth(final int width) {
        Preconditions.checkArgument(width >= 1 && width <= 9, "TODO");

        this.width = width;
    }

    /**
     * Set the width of this partition 1 - 6.
     *
     * @param height the partition's new height
     */
    public void setHeight(final int height) {
        Preconditions.checkArgument(height >= 1 && height <= 6, "TODO");

        this.height = height;
    }

    /**
     * Set the width and height of this partition.
     *
     * @param width the partition's new width
     * @param height the partition's new height
     */
    public void setDimensions(final int width, final int height) {
        this.setWidth(width);
        this.setHeight(height);
    }

    /**
     * Set the x and y position of this partition.
     * Note: This method is not supported in the root partition as the root partition must always have a
     *       position of (0, 0).
     * Note: Partition coordinates are 0-based.
     *
     * @param x the partition's new x position
     * @param y the partition's new y position
     */
    public void setPosition(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Register a listener for this partition.
     *
     * @param eventClass the class of the event
     * @param eventConsumer consumer for the event
     */
    public <K extends MenuPartitionEvent> void addEventListener(@NotNull final Class<K> eventClass,
                                                                @NotNull final Consumer<K> eventConsumer) {
        this.rootPartition.partitionEvents.put(eventClass, eventConsumer);
    }

    /**
     * Create a menu property that will persist through partition renders.
     * Setting the value of the property will cause a re-render of this partition and any children partitions
     * provided the value is different from the previous value.
     *
     * @param property the original value of the property
     *
     * @return a menu property
     */
    public @NotNull <K> MenuProperty<K> useProperty(@Nullable final K property) {
        if (this.renderCount == 1) {
            // First render, add the property to the properties list
            final AtomicReference<Function<MenuPartition<?>, Boolean>> rerenderCheck = new AtomicReference<>();

            this.properties.add(new MenuPropertyRenderCheck(
                new MenuProperty<>(this.plugin, property, this, rerenderCheck),
                rerenderCheck
            ));
        }
        return this.properties.get(this.propertyIndex++).menuProperty;
    }

    /**
     * Create a menu partition that will persist through partition renders.
     *
     * @param partitionConsumer the partition builder
     */
    public void usePartition(@NotNull final Consumer<SimpleMenuPartition> partitionConsumer) {
        if (this.renderCount == 1) {
            final SimpleMenuPartition menuPartition = new SimpleMenuPartition(this.plugin, this.player, partitionConsumer);

            menuPartition.rootPartition = this.rootPartition;
            menuPartition.parentPartition = this;

            this.partitions.add(menuPartition);
        }
    }

    /**
     * Create a masked menu partition that will persist through partition renders.
     *
     * @param partitionConsumer the partition builder
     */
    public void useMaskedPartition(@NotNull final Consumer<MaskedMenuPartition> partitionConsumer) {
        if (this.renderCount == 1) {
            final MaskedMenuPartition menuPartition = new MaskedMenuPartition(this.plugin, this.player, partitionConsumer);

            menuPartition.rootPartition = this.rootPartition;
            menuPartition.parentPartition = this;

            this.partitions.add(menuPartition);
        }
    }

    /**
     * Set the title of the menu inventory for the next render.
     * This will recreate the inventory with the new title unless it's the same as the title was during the previous render.
     *
     * @param title the new title
     */
    public abstract void setTitle(@NotNull Component title);

    /**
     * Get the containing inventory for this partition.
     *
     * @return the inventory
     */
    public abstract @NotNull Inventory getInventory();

    public synchronized void render(@Nullable final MenuProperty menuProperty) {
        this.propertyIndex = 0;
        this.renderCount++;
        this.items.clear();

        this.initConsumer.accept((T) this);

        final List<Integer> populatedSlots = new ArrayList<>();

        // TODO: Determine whether to check position constraints during render or during set
        for (final MenuPartitionItem menuPartitionItem : this.items) {
            final int slot = menuPartitionItem.slot;
            final ItemStack currentItemStack = Objects.requireNonNullElse(this.getInventory().getItem(slot), Menu.AIR);
            final ItemStack newItemStack = menuPartitionItem.menuItem.getItemStack();

            if (newItemStack == null) {
                continue;
            }
            if (newItemStack.equals(currentItemStack)) {
                populatedSlots.add(slot);

                continue;
            }
            this.getInventory().setItem(slot, newItemStack);

            populatedSlots.add(slot);
        }
        for (final MenuPartitionItem menuPartitionItem : this.previousItems) {
            final int slot = menuPartitionItem.slot;

            if (populatedSlots.contains(slot)) {
                continue;
            }
            this.getInventory().clear(slot);
        }
        final Function<MenuPartition<?>, Boolean> rerenderCheck = menuProperty != null ?
            this.properties.stream()
                .filter((property) -> property.menuProperty.equals(menuProperty))
                .findFirst()
                .map((property) -> property.rerenderCheck.get())
                .orElse(null) :
            null;

        for (final MenuPartition<?> menuPartition : this.partitions) {
            if (rerenderCheck != null && !rerenderCheck.apply(menuPartition)) {
                continue;
            }
            menuPartition.render();
        }
        this.previousItems = new ArrayList<>(this.items);
    }

    /**
     * Called to render or re-render the items in this partition.
     */
    public synchronized void render() {
        this.render(null);
    }

    record MenuPartitionItem(@NotNull MenuItem menuItem, @Nullable Consumer<ItemClickEvent> clickEventConsumer,
                             int slot) {

    }

    private record MenuPropertyRenderCheck(@NotNull MenuProperty menuProperty,
                                           @NotNull AtomicReference<Function<MenuPartition<?>, Boolean>> rerenderCheck) {

    }
}
