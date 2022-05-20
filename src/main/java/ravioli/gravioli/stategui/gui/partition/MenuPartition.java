package ravioli.gravioli.stategui.gui.partition;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import ravioli.gravioli.stategui.Menu;
import ravioli.gravioli.stategui.gui.event.ItemClickEvent;
import ravioli.gravioli.stategui.gui.event.partition.MenuPartitionEvent;
import ravioli.gravioli.stategui.gui.partition.item.MenuItem;
import ravioli.gravioli.stategui.gui.partition.state.RenderPhase;
import ravioli.gravioli.stategui.gui.property.MenuProperty;
import ravioli.gravioli.stategui.gui.property.MenuPropertyEffect;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class MenuPartition<T extends MenuPartition<T>> {
    private final Player player;
    private final List<MenuPartition<?>> partitions = new ArrayList<>();
    private final List<MenuPropertyEffect> effects = new ArrayList<>();
    private final Lock renderLock = new ReentrantLock();

    protected final Plugin plugin;
    protected final List<MenuPropertyRenderCheck> properties = new ArrayList<>();
    protected final Consumer<T> initConsumer;
    protected final Map<RenderPhase, Map<Integer, MenuItem>> items = new ConcurrentHashMap<>();

    private Map<Integer, MenuItem> previousItems = new HashMap<>();
    protected RootPartition rootPartition;
    protected MenuPartition<?> parentPartition;
    protected int width;
    protected int height;
    protected int x;
    protected int y;
    protected int renderCount;

    private int propertyIndex;
    private int effectIndex;
    private RenderPhase renderPhase;
    private String previousHash;
    private boolean initialRender;
    private boolean renderCheckQueued;

    public MenuPartition(@NotNull final Plugin plugin, @NotNull final Player player,
                         @NotNull final Consumer<T> initConsumer) {
        this.plugin = plugin;
        this.player = player;
        this.initConsumer = (partition) -> {
            this.renderLock.lock();

            try {
                this.renderCount++;
                this.propertyIndex = 0;
                this.effectIndex = 0;
                this.getItems().clear();

                final Map<Integer, Consumer<ItemClickEvent>> clickEventMap = this.rootPartition.itemClickActions.get(this);

                if (clickEventMap != null) {
                    clickEventMap.clear();
                }
                initConsumer.accept(partition);

                if (this.renderPhase == RenderPhase.REGULAR) {
                    this.previousHash = this.hashContents();
                }
            } finally {
                this.renderLock.unlock();
            }
        };
        this.renderPhase = RenderPhase.REGULAR;
        this.initialRender = true;
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
                new MenuProperty<>(this.plugin, property, this),
                rerenderCheck
            ));
        }
        return this.properties.get(this.propertyIndex++).menuProperty;
    }

    /**
     * Run a function after the partition is finished rendering.
     * With optional dependencies (properties), this effect will only run when one of those dependencies is modified.
     *
     * @param effect the function
     * @param dependencies any dependent properties
     */
    public void useEffect(@NotNull final Runnable effect, @NotNull final MenuProperty... dependencies) {
        final MenuPropertyEffect.CleanupRunnable cleanupRunnable = () -> {
            effect.run();

            return null;
        };

        if (this.renderCount == 1) {
            this.effects.add(new MenuPropertyEffect(cleanupRunnable, dependencies));
        } else {
            this.effects.get(this.effectIndex++).updateEffect(cleanupRunnable);
        }
    }

    /**
     * Run a function after the partition is finished rendering.
     * With optional dependencies (properties), this effect will only run when one of those dependencies is modified.
     * Before a partition is re-rendered, the cleanup function of the effect will be called.
     *
     * @param effect the function
     * @param dependencies any dependent properties
     */
    public void useEffect(@NotNull final MenuPropertyEffect.CleanupRunnable effect, @NotNull final MenuProperty... dependencies) {
        if (this.renderCount == 1) {
            this.effects.add(new MenuPropertyEffect(effect, dependencies));
        } else {
            this.effects.get(this.effectIndex++).updateEffect(effect);
        }
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

    protected @NotNull Map<Integer, MenuItem> getItems() {
        if (!this.items.containsKey(this.renderPhase)) {
            this.items.put(this.renderPhase, new ConcurrentHashMap<>());
        }
        return this.items.get(this.renderPhase);
    }

    protected @NotNull Map<Integer, MenuItem> getItems(@NotNull final RenderPhase renderPhase) {
        return this.items.get(renderPhase);
    }

    private @NotNull String serializeItemStack(@NotNull final ItemStack itemStack) throws IllegalStateException {
        try (
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        ) {
            dataOutput.writeObject(itemStack);

            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to serialize item stacks.", e);
        }
    }

    public void checkRefresh() {
        if (this.renderCheckQueued) {
            return;
        }
        this.renderCheckQueued = true;

        Bukkit.getScheduler().runTaskLater(
            this.plugin,
            () -> {
                final String hash = this.hashContents();

                this.renderCheckQueued = false;

                if (Objects.equals(hash, this.previousHash)) {
                    this.checkRefreshChildren();

                    return;
                }
                if (this.previousHash == null) {
                    this.previousHash = hash;
                    this.checkRefreshChildren();

                    return;
                }
                this.render();
                this.checkRefreshChildren();
            },
            0
        );
    }

    private void checkRefreshChildren() {
        for (final MenuPartition<?> childPartition : this.partitions) {
            childPartition.checkRefresh();
        }
    }

    public @NotNull String hashContents() {
        final JsonArray jsonHash = new JsonArray();

        this.renderPhase = RenderPhase.CHECK;
        this.initConsumer.accept((T) this);
        this.renderPhase = RenderPhase.REGULAR;

        if (this instanceof final RootPartition rootPartition && rootPartition.title != null) {
            jsonHash.add(rootPartition.title.toString());
        }
        jsonHash.add(this.x);
        jsonHash.add(this.y);
        jsonHash.add(this.width);
        jsonHash.add(this.height);

        for (final MenuPropertyEffect effect : this.effects) {
            if (effect.shouldRun(false)) {
                jsonHash.add(UUID.randomUUID().toString());
            }
        }
        for (final Map.Entry<Integer, MenuItem> menuItemEntry : this.getItems(RenderPhase.CHECK).entrySet()) {
            jsonHash.add(menuItemEntry.getKey());

            if (menuItemEntry.getValue().getItemStack() != null) {
                jsonHash.add(this.serializeItemStack(menuItemEntry.getValue().getItemStack()));
            }
        }
        return jsonHash.toString();
    }

    /**
     * Called to render or re-render the items in this partition.
     * It is not recommended to call this yourself, but can be done if an instant re-render is required.
     *
     * Note: Must be called in a thread that is not bukkit's primary thread.
     */
    public void render() {
        this.initConsumer.accept((T) this);

        final List<Integer> populatedSlots = new ArrayList<>();

        // TODO: Determine whether to check position constraints during render or during set
        for (final Map.Entry<Integer, MenuItem> menuItemEntry : this.getItems().entrySet()) {
            final int slot = menuItemEntry.getKey();
            final MenuItem menuItem = menuItemEntry.getValue();
            final ItemStack currentItemStack = Objects.requireNonNullElse(this.getInventory().getItem(slot), Menu.AIR);
            final ItemStack newItemStack = menuItem.getItemStack();

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
        for (final Map.Entry<Integer, MenuItem> menuItemEntry : this.previousItems.entrySet()) {
            final int slot = menuItemEntry.getKey();

            if (populatedSlots.contains(slot)) {
                continue;
            }
            this.getInventory().clear(slot);
        }
        for (final MenuPropertyEffect effect : this.effects) {
            effect.runEffect(this.plugin, this.initialRender);
        }
        if (!this.initialRender) {
            return;
        }
        this.initialRender = false;

        for (final MenuPartition<?> menuPartition : this.partitions) {
            menuPartition.render();
        }
        this.previousItems = new HashMap<>(this.getItems());
    }

    private record MenuPropertyRenderCheck(@NotNull MenuProperty menuProperty,
                                           @NotNull AtomicReference<Function<MenuPartition<?>, Boolean>> rerenderCheck) {

    }
}
