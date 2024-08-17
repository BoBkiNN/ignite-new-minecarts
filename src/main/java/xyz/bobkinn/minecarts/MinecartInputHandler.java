package xyz.bobkinn.minecarts;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Unique;

public interface MinecartInputHandler {
    @Unique
    void new_minecarts$setPassengerMoveIntentFromInput(LivingEntity livingEntity, Vec3 vec3);
}
