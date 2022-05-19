package ravioli.gravioli.stategui.gui.property;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MenuPropertyEffect {
    private Runnable effect;

    private final MenuProperty[] dependencies;
    private final Object[] previousValues;

    public MenuPropertyEffect(@NotNull final Runnable effect, @NotNull final MenuProperty... dependencies) {
        this.effect = effect;
        this.dependencies = dependencies;
        this.previousValues = new Object[dependencies.length];

        for (int i = 0; i < dependencies.length; i++) {
            this.previousValues[i] = dependencies[i].get();
        }
    }

    public void updateEffect(@NotNull final Runnable effect) {
        this.effect = effect;
    }

    public void runEffect(@NotNull final Plugin plugin) {
        for (int i = 0; i < this.dependencies.length; i++) {
            this.previousValues[i] = this.dependencies[i].get();
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this.effect);
    }

    public boolean shouldRun(boolean duringRender) {
        if (this.dependencies.length == 0 && duringRender) {
            return true;
        }
        for (int i = 0; i < this.dependencies.length; i++) {
            final Object previousValue = this.previousValues[i];

            if (!Objects.equals(previousValue, this.dependencies[i].get())) {
                return true;
            }
        }
        return false;
    }
}
