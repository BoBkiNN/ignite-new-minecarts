package xyz.bobkinn.minecarts.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bobkinn.minecarts.MinecartBehavior;
import xyz.bobkinn.minecarts.NewMinecartBehavior;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(AbstractMinecart.class)
public class MixinAbstractMinecart {
    @Unique
    private MinecartBehavior new_minecarts$behavior;
    @Unique
    public boolean new_minecarts$flipped;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at=@At("TAIL"))
    private void onInit(EntityType<?> type, Level world, CallbackInfo ci){
        var self = (AbstractMinecart) (Object) this;
        new_minecarts$behavior = new NewMinecartBehavior(self, this);
    }

    @Inject(method = "tick", at=@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;applyGravity()V"), cancellable = true)
    public void onTick(CallbackInfo ci){
        ci.cancel();
        new_minecarts$behavior.tick();
        var self = (AbstractMinecart) (Object) this;

        ((EntityAccessor) self).invokeUpdateInWaterStateAndDoFluidPushing();
        if (self.isInLava()) {
            self.lavaHurt();
            self.fallDistance *= 0.5F;
        }

        ((EntityAccessor) self).setFirstTick(false);
    }

}
