package ravioli.gravioli.stategui.gui.partition;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.stategui.gui.event.ItemClickEvent;
import ravioli.gravioli.stategui.gui.event.partition.BottomInventoryClickEvent;
import ravioli.gravioli.stategui.gui.event.partition.MenuCloseEvent;
import ravioli.gravioli.stategui.gui.event.partition.MenuPartitionEvent;
import ravioli.gravioli.stategui.gui.partition.item.SimpleMenuItem;
import ravioli.gravioli.stategui.gui.partition.state.RerenderType;
import ravioli.gravioli.stategui.gui.property.MenuPropertyEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class RootPartition extends MenuPartition<RootPartition> implements InventoryHolder, Listener {
    private final NamespacedKey id;

    protected final Map<MenuPartition<?>, Map<Integer, Consumer<ItemClickEvent>>> itemClickActions = new HashMap<>();
    protected final Map<Class<? extends MenuPartitionEvent>, Consumer<? extends MenuPartitionEvent>> partitionEvents = new HashMap<>();

    protected Component title;
    protected RerenderType rerenderType;

    private Component previousTitle;
    private Inventory inventory;

    public RootPartition(@NotNull final NamespacedKey id, @NotNull final Plugin plugin, @NotNull final Player player,
                         @NotNull final Consumer<RootPartition> initConsumer) {
        super(plugin, player, (rootPartition) -> {
            initConsumer.accept(rootPartition);

            rootPartition.refreshInventory();
        });

        this.id = id;
        this.rootPartition = this;
        this.parentPartition = this;
        this.rerenderType = RerenderType.ONLY_ON_RENDER_CHANGE;

        super.setWidth(9);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Get the key that is used to identify this menu/partition.
     * This key can be used to fetch which menu the player has open with {@code Menu.isMenuOpen(player, key)}
     *
     * @return the menu key
     */
    public @NotNull NamespacedKey getKey() {
        return this.id;
    }

    @Override
    public void setTitle(@Nullable final Component title) {
        this.title = title;
    }

    @Override
    public void setWidth(final int width) {
        throw new UnsupportedOperationException("Cannot set the width of the root partition.");
    }

    @Override
    public void setPosition(final int x, final int y) {
        throw new UnsupportedOperationException("Cannot set the position of the root partition.");
    }

    private @NotNull Inventory createInventory() {
        if (this.title != null) {
            return Bukkit.createInventory(this, this.getSize(), this.title);
        }
        return Bukkit.createInventory(this, this.getSize());
    }

    private void refreshInventory() {
        if (this.inventory == null) {
            this.inventory = this.createInventory();
            this.previousTitle = this.title;
        } else if (!Objects.equals(this.previousTitle, this.title)) {
            final Inventory newInventory = this.createInventory();

            for (int i = 0; i < this.inventory.getSize(); i++) {
                final ItemStack itemStack = this.inventory.getItem(i);

                if (itemStack == null || itemStack.getType().isAir()) {
                    continue;
                }
                newInventory.setItem(i, itemStack);
            }
            final List<HumanEntity> viewers = new ArrayList<>(this.inventory.getViewers());

            this.inventory.clear();
            this.inventory = newInventory;
            this.previousTitle = this.title;

            Bukkit.getScheduler().getMainThreadExecutor(this.plugin).execute(() -> {
                for (final HumanEntity viewer : viewers) {
                    viewer.openInventory(this.inventory);
                }
            });
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    /**
     * Set how the root partition/menu handles rerendering.
     * It is recommended to keep this value consistent per menu and not set it in a variable manner.
     *
     * @param rerenderType rerender type
     */
    public void setRerenderType(@NotNull final RerenderType rerenderType) {
        this.rerenderType = rerenderType;
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
        this.setClickEventSlot(this, slot, clickEventConsumer);
    }

    public void setSlot(final int slot, @Nullable final ItemStack itemStack,
                        @NotNull final Consumer<ItemClickEvent> clickEventConsumer) {
        this.getItems().put(
            slot,
            new SimpleMenuItem(itemStack, clickEventConsumer)
        );
        this.setClickEventSlot(this, slot, clickEventConsumer);
    }

    public void setSlot(final int slot, @Nullable final ItemStack itemStack) {
        this.getItems().put(
            slot,
            new SimpleMenuItem(itemStack, null)
        );
    }

    void setClickEventSlot(@NotNull final MenuPartition<?> owningPartition, final int slot,
                           @NotNull final Consumer<ItemClickEvent> clickEventConsumer) {
        if (!this.itemClickActions.containsKey(owningPartition)) {
            this.itemClickActions.put(owningPartition, new HashMap<>());
        }
        this.itemClickActions.get(owningPartition).put(slot, clickEventConsumer);
    }

    @EventHandler
    private void onInventoryClick(final InventoryClickEvent event) {
        final Inventory inventory = event.getClickedInventory();

        if (inventory == null || !inventory.equals(this.inventory)) {
            if (Objects.equals(inventory, this.getPlayer().getInventory()) && event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);

        this.itemClickActions.values()
            .stream()
            .map((entry) -> entry.get(event.getSlot()))
            .filter(Objects::nonNull)
            .forEach((consumer) -> {
                consumer.accept(new ItemClickEvent(event));
            });
    }

    @EventHandler
    private void onInventoryClose(final InventoryCloseEvent event) {
        final Inventory inventory = event.getInventory();

        if (!inventory.equals(this.inventory)) {
            return;
        }
        this.cleanupEffects();
        HandlerList.unregisterAll(this);

        if (!(event.getPlayer() instanceof final Player player)) {
            return;
        }
        if (!this.partitionEvents.containsKey(MenuCloseEvent.class)) {
            return;
        }
        final Consumer<MenuCloseEvent> closeEvent = (Consumer<MenuCloseEvent>) this.partitionEvents.get(MenuCloseEvent.class);
        final MenuCloseEvent menuCloseEvent = new MenuCloseEvent(event.getReason());

        closeEvent.accept(menuCloseEvent);

        if (menuCloseEvent.isCancelled()) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> player.openInventory(inventory), 1);
        }
    }

    @EventHandler
    private void onDrag(final InventoryDragEvent event) {
        final Set<Integer> rawSlots = event.getRawSlots();

        if (rawSlots.isEmpty()) {
            return;
        }
        if (rawSlots.size() == 1) {
            final int slot = rawSlots.iterator().next();

            if (slot >= this.inventory.getSize()) {
                return;
            }
            final InventoryClickEvent fakeEvent = new InventoryClickEvent(
                event.getView(),
                event.getView().getSlotType(slot),
                slot,
                event.getType() == DragType.SINGLE ?
                    ClickType.RIGHT :
                    ClickType.LEFT,
                InventoryAction.UNKNOWN
            ) {
                @Override
                public @NotNull ItemStack getCursor() {
                    return event.getOldCursor();
                }
            };

            this.onInventoryClick(fakeEvent);

            if (fakeEvent.isCancelled()) {
                event.setCancelled(true);
            }
            return;
        }
        for (final int slot : event.getRawSlots()) {
            if (slot < this.inventory.getSize()) {
                event.setCancelled(true);

                break;
            }
        }
    }

    @EventHandler
    private void onBottomClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Inventory topInventory = event.getView().getTopInventory();

        if (topInventory == null || !topInventory.equals(this.inventory)) {
            return;
        }
        final Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null || !clickedInventory.equals(event.getView().getBottomInventory())) {
            return;
        }
        if (!this.partitionEvents.containsKey(BottomInventoryClickEvent.class)) {
            return;
        }
        final Consumer<BottomInventoryClickEvent> clickEvent =
            (Consumer<BottomInventoryClickEvent>) this.partitionEvents.get(BottomInventoryClickEvent.class);

        clickEvent.accept(new BottomInventoryClickEvent(event));
    }
}
