package xyz.bobkinn.minecarts.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BiConsumer;

@Mixin(GameRules.IntegerValue.class)
public interface GameRuleIntegerValueAccessor {
    @Invoker("create")
    static GameRules.Type<GameRules.IntegerValue> doCreate(int initialValue, int min, int max, BiConsumer<ServerLevel, GameRules.IntegerValue> changeCallback) {
        throw new AssertionError();
    }

}
