package ravioli.gravioli.stategui.gui.partition.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.stategui.gui.event.ItemClickEvent;

import java.util.function.Consumer;

public class SimpleMenuItem implements MenuItem {
    private final ItemStack itemStack;
    private final Consumer<ItemClickEvent> clickEventConsumer;

    public SimpleMenuItem(@Nullable final ItemStack itemStack,
                          @Nullable final Consumer<ItemClickEvent> clickEventConsumer) {
        this.itemStack = itemStack;
        this.clickEventConsumer = clickEventConsumer;
    }

    @Override
    public void onClick(@NotNull final ItemClickEvent event) {
        if (this.clickEventConsumer == null) {
            return;
        }
        this.clickEventConsumer.accept(event);
    }

    @Override
    public @Nullable ItemStack getItemStack() {
        return this.itemStack;
    }
}
