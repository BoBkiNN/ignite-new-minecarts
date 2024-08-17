package xyz.bobkinn.minecarts.mixin;

import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bobkinn.minecarts.MinecartInputHandler;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl {

    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlayerInput", at = @At("TAIL"))
    public void onInput(ServerboundPlayerInputPacket packet, CallbackInfo ci){
        Entity entity;
        if (this.player.isPassenger() && (entity = this.player.getVehicle()) instanceof AbstractMinecart) {
            AbstractMinecart abstractMinecart = (AbstractMinecart)entity;
            if ((double)packet.getXxa() != 0.0 || (double)packet.getZza() != 0.0) {
                var vec = new Vec3(packet.getXxa(), 0.0, packet.getZza());
                var mixin = (MinecartInputHandler) abstractMinecart;
                mixin.new_minecarts$setPassengerMoveIntentFromInput(this.player, vec);
            }
        }
    }
}
