package br.club.nuven.legacyfeel;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class LegacyFeelSounds {
    public static final SoundEvent CLASSIC_ROD_THROW = SoundEvent.createVariableRangeEvent(
        Identifier.fromNamespaceAndPath("legacyfeel", "classic_rod_throw"));

    private LegacyFeelSounds() {}
}
