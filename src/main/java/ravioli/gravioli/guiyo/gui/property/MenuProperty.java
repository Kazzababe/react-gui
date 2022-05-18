package ravioli.gravioli.guiyo.gui.property;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.guiyo.gui.partition.MenuPartition;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class MenuProperty<T> {
    private final Plugin plugin;
    private final MenuPartition<?> parent;
    private final Set<MenuPartition<?>> callingPartitions = new HashSet<>();

    private T value;

    public MenuProperty(@NotNull final Plugin plugin, @Nullable final T value, @NotNull final MenuPartition<?> parent,
                        @NotNull final AtomicReference<Function<MenuPartition<?>, Boolean>> rerenderCheck) {
        this.plugin = plugin;
        this.value = value;
        this.parent = parent;

        rerenderCheck.set(this.callingPartitions::contains);
    }

    /**
     * Get the value of this property.
     *
     * @return the property value
     */
    public @Nullable T get() {
        return this.value;
    }

    /**
     * Get the value of this property but when setting this property, the partition will not re-render
     * the calling partition when a menu property of a parent partition changes unless this property is the
     * one being changed.
     *
     * @return the property value
     */
    public @Nullable T get(@NotNull final MenuPartition<?> callingPartition) {
        this.callingPartitions.add(callingPartition);

        return this.value;
    }

    /**
     * Set the value of the property and re-render the parent partition if the value is different.
     *
     * @param value the new value
     */
    public void set(@Nullable final T value) {
        if (Objects.equals(this.value, value)) {
            return;
        }
        this.value = value;

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.parent.render(this));
    }
}
