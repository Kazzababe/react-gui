package ravioli.gravioli.guiyo.gui.partition.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ravioli.gravioli.guiyo.gui.event.ItemClickEvent;

public interface MenuItem {
    void onClick(@NotNull ItemClickEvent event);

    @Nullable ItemStack getItemStack();
}
