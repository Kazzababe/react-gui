package ravioli.gravioli.stategui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ravioli.gravioli.stategui.gui.partition.RootPartition;

import java.util.function.Consumer;

public class Menu {
    public static ItemStack AIR = new ItemStack(Material.AIR);

    public static void openMenu(@NotNull final NamespacedKey key, @NotNull final Plugin plugin, @NotNull final Player player,
                                @NotNull final Consumer<RootPartition> partitionConsumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final RootPartition rootPartition = new RootPartition(key, plugin, player, partitionConsumer);

            rootPartition.render();

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(rootPartition.getInventory()));
        });
    }

    public static void openMenu(@NotNull final String key, @NotNull final Plugin plugin, @NotNull final Player player,
                                @NotNull final Consumer<RootPartition> partitionConsumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            final RootPartition rootPartition = new RootPartition(namespacedKey, plugin, player, partitionConsumer);

            rootPartition.render();

            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(rootPartition.getInventory()));
        });
    }

    public static boolean isMenuOpen(@NotNull final Player player, @NotNull final NamespacedKey key) {
        final Inventory inventory = player.getOpenInventory().getTopInventory();

        if (inventory == null) {
            return false;
        }
        return inventory.getHolder() instanceof final RootPartition rootPartition &&
            rootPartition.getKey().equals(key);
    }
}
