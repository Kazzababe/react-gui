package ravioli.gravioli.stategui.gui.partition;

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

        super.setWidth(9);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public NamespacedKey getKey() {
        return this.id;
    }

    @Override
    public void setTitle(@NotNull final Component title) {
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

    private void refreshInventory() {
        //final CountDownLatch latch = new CountDownLatch(1);

        //Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (this.inventory == null) {
                this.inventory = Bukkit.createInventory(this, this.getSize(), this.title);
                this.previousTitle = this.title;
            } else if (!Objects.equals(this.previousTitle, this.title)) {
                final Inventory newInventory = Bukkit.createInventory(this, this.getSize(), this.title);

                for (int i = 0; i < this.inventory.getSize(); i++) {
                    final ItemStack itemStack = this.inventory.getItem(i);

                    if (itemStack == null || itemStack.getType().isAir()) {
                        continue;
                    }
                    newInventory.setItem(i, itemStack);
                }
                final List<HumanEntity> viewers = new ArrayList<>(this.inventory.getViewers());

                this.inventory = newInventory;
                this.previousTitle = this.title;

                for (final HumanEntity viewer : viewers) {
                    viewer.openInventory(this.inventory);
                }
            }
            //latch.countDown();
        //});
//        try {
//            latch.await();
//        } catch (final InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
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
        if (!(event.getPlayer() instanceof final Player player)) {
            return;
        }
        if (!this.partitionEvents.containsKey(MenuCloseEvent.class)) {
            HandlerList.unregisterAll(this);

            return;
        }
        final Consumer<MenuCloseEvent> closeEvent =
            (Consumer<MenuCloseEvent>) this.partitionEvents.get(MenuCloseEvent.class);
        final MenuCloseEvent menuCloseEvent = new MenuCloseEvent(event.getReason());

        closeEvent.accept(menuCloseEvent);

        if (menuCloseEvent.isCancelled()) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> player.openInventory(inventory), 1);

            return;
        }
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    private void onDrag(final InventoryDragEvent event) {
        final Set<Integer> rawSlots = event.getRawSlots();

        if (rawSlots.isEmpty()) {
            return;
        }
        if (rawSlots.size() == 1) {
            final int slot = rawSlots.iterator().next();

            this.onInventoryClick(new InventoryClickEvent(
                event.getView(),
                event.getView().getSlotType(slot),
                slot,
                event.getType() == DragType.SINGLE ?
                    ClickType.RIGHT :
                    ClickType.LEFT,
                InventoryAction.UNKNOWN
            ));
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
