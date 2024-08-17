package xyz.bobkinn.minecarts;

import net.minecraft.world.level.GameRules;
import xyz.bobkinn.minecarts.mixin.GameRuleIntegerValueAccessor;
import xyz.bobkinn.minecarts.mixin.GameRulesAccessor;

public class CustomGameRule {

    public static final GameRules.Key<GameRules.IntegerValue> RULE_MINECART_MAX_SPEED =
            GameRulesAccessor.doRegister("minecartMaxSpeed",
                    GameRules.Category.MISC,
                    GameRuleIntegerValueAccessor.doCreate(8, 1, 1000, (minecraftServer, integerValue) -> {}));
}
