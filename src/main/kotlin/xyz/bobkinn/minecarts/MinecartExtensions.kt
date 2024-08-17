package xyz.bobkinn.minecarts;

import net.minecraft.world.entity.vehicle.AbstractMinecart
import xyz.bobkinn.minecarts.mixin.MinecartAccessor

fun AbstractMinecart.moveAlongTrack() {
    val mixin = this as MinecartAccessor
    mixin.invokeMoveAlongTrack(null, null)
}
