package ravioli.gravioli.stategui.gui.property;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class MenuPropertyEffect {
    private CleanupRunnable effect;

    private final MenuProperty[] dependencies;
    private final Object[] previousValues;
    private Runnable cleanup;

    public MenuPropertyEffect(@NotNull final CleanupRunnable effect, @NotNull final MenuProperty... dependencies) {
        this.effect = effect;
        this.dependencies = dependencies;
        this.previousValues = new Object[dependencies.length];

        for (int i = 0; i < dependencies.length; i++) {
            this.previousValues[i] = dependencies[i].get();
        }
    }

    public void updateEffect(@NotNull final CleanupRunnable effect) {
        this.effect = effect;
    }

    public void runEffect(@NotNull final Plugin plugin, final boolean forceRun) {
        if (!forceRun) {
            if (!this.shouldRun(true)) {
                return;
            }
        }
        for (int i = 0; i < this.dependencies.length; i++) {
            this.previousValues[i] = this.dependencies[i].get();
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (this.cleanup != null) {
                this.cleanup.run();
            }
            this.cleanup = this.effect.run();
        });
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

    public void cleanup() {
        if (this.cleanup != null) {
            this.cleanup.run();
        }
    }

    @FunctionalInterface
    public interface CleanupRunnable {
        @Nullable Runnable run();
    }
}
