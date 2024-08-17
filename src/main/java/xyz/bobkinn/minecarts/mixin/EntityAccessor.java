package xyz.bobkinn.minecarts.mixin;

import net.minecraft.world.entity.Entity;
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
}
