package xyz.bobkinn.minecarts.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("UnusedReturnValue")
@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor
    void setFirstTick(boolean value);

    @Invoker("updateInWaterStateAndDoFluidPushing")
    boolean invokeUpdateInWaterStateAndDoFluidPushing();

    @Invoker("applyGravity")
    void invokeApplyGravity();

    @Invoker("getInputVector")
    static Vec3 invokeGetInputVector(Vec3 movementInput, float speed, float yaw) {
        throw new AssertionError();
    }
}
