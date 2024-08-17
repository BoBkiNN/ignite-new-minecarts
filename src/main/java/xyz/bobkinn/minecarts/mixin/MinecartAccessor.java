package xyz.bobkinn.minecarts.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMinecart.class)
public interface MinecartAccessor {

    @Invoker("comeOffTrack")
    void invokeComeOffTrack();

    @Accessor
    void setOnRails(boolean value);

    @Invoker("exits")
    static Pair<Vec3i, Vec3i> invokeExits(RailShape shape){
        throw new AssertionError();
    }
}
