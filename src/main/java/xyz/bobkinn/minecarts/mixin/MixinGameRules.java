package xyz.bobkinn.minecarts.mixin;

import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bobkinn.minecarts.CustomGameRule;

@Mixin(GameRules.class)
public class MixinGameRules {

    @Inject(method = "<clinit>", at=@At("TAIL"))
    private static void onInit(CallbackInfo ci){
        try {
            Class.forName(CustomGameRule.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
