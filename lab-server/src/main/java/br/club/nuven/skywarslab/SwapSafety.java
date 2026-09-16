package br.club.nuven.skywarslab;

import org.bukkit.Location;
import org.bukkit.WorldBorder;

final class SwapSafety {
    enum Result { SAFE, DIFFERENT_WORLD, CHUNK_UNLOADED, OUTSIDE_BORDER, SUFFOCATING, INVALID_HEIGHT }

    private SwapSafety() {}

    static Result validate(Location from, Location destination) {
        if (from.getWorld() == null || destination.getWorld() == null || from.getWorld() != destination.getWorld()) {
            return Result.DIFFERENT_WORLD;
        }
        if (destination.getY() < destination.getWorld().getMinHeight()
                || destination.getY() + 1.8 >= destination.getWorld().getMaxHeight()) {
            return Result.INVALID_HEIGHT;
        }
        if (!destination.getWorld().isChunkLoaded(destination.getBlockX() >> 4, destination.getBlockZ() >> 4)) {
            return Result.CHUNK_UNLOADED;
        }
        WorldBorder border = destination.getWorld().getWorldBorder();
        if (!border.isInside(destination)) return Result.OUTSIDE_BORDER;
        if (!destination.getBlock().isPassable() || !destination.clone().add(0, 1, 0).getBlock().isPassable()) {
            return Result.SUFFOCATING;
        }
        return Result.SAFE;
    }
}
