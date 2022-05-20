package ravioli.gravioli.stategui.gui.property;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.stategui.gui.partition.MenuPartition;

import java.util.Objects;

public class MenuProperty<T> {
    private final Plugin plugin;
    private final MenuPartition<?> menuPartition;
    private T value;

    public MenuProperty(@NotNull final Plugin plugin, @Nullable final T value, @NotNull final MenuPartition<?> menuPartition) {
        this.plugin = plugin;
        this.value = value;
        this.menuPartition = menuPartition;
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
     * Set the value of the property and re-render the parent partition if the value is different.
     *
     * @param value the new value
     */
    public void set(@Nullable final T value) {
        if (Objects.equals(this.value, value)) {
            return;
        }
        this.value = value;
        this.menuPartition.checkRefresh();
    }
}
