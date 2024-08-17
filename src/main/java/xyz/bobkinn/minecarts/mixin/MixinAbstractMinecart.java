package xyz.bobkinn.minecarts.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bobkinn.minecarts.MinecartInputHandler;
import xyz.bobkinn.minecarts.NewMinecartBehavior;

@Mixin(AbstractMinecart.class)
public class MixinAbstractMinecart implements MinecartInputHandler {
    @Unique
    public NewMinecartBehavior new_minecarts$behavior;

    @Override
    @Unique
    public void new_minecarts$setPassengerMoveIntentFromInput(LivingEntity livingEntity, Vec3 vec3) {
        var vec = EntityAccessor.invokeGetInputVector(vec3, 1.0f, livingEntity.getYRot());
        new_minecarts$behavior.setPassengerMoveIntent2(vec);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at=@At("TAIL"))
    private void onInit(EntityType<?> type, Level world, CallbackInfo ci){
        var self = (AbstractMinecart) (Object) this;
        new_minecarts$behavior = new NewMinecartBehavior(self);
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
