package br.club.nuven.skywarslab;

import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

final class EffectScope implements AutoCloseable {
    private final Set<Entity> entities = new LinkedHashSet<>();
    private final Set<BukkitTask> tasks = new LinkedHashSet<>();
    private final Set<Runnable> cleanup = new LinkedHashSet<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    <T extends Entity> T track(T entity) {
        if (closed.get()) entity.remove();
        else entities.add(entity);
        return entity;
    }

    BukkitTask track(BukkitTask task) {
        if (closed.get()) task.cancel();
        else tasks.add(task);
        return task;
    }

    void onClose(Runnable action) {
        if (closed.get()) action.run();
        else cleanup.add(action);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        tasks.forEach(task -> {
            if (!task.isCancelled()) task.cancel();
        });
        entities.forEach(entity -> {
            if (entity.isValid()) entity.remove();
        });
        cleanup.forEach(action -> {
            try {
                action.run();
            } catch (RuntimeException ignored) {
                // A limpeza precisa continuar mesmo se um recurso já tiver sumido.
            }
        });
        tasks.clear();
        entities.clear();
        cleanup.clear();
    }
}
