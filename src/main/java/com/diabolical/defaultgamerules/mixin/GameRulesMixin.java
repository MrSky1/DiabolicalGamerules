package com.diabolical.defaultgamerules.mixin;

import com.diabolical.defaultgamerules.GameruleDefaults;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRules.class)
public abstract class GameRulesMixin {
    @Inject(method = "register", at = @At("HEAD"))
    private static void diabolicalDefaultGamerules$captureOwner(
            String name,
            GameRules.Category category,
            GameRules.Type<?> type,
            CallbackInfoReturnable<GameRules.Key<?>> cir
    ) {
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames
                        .map(StackWalker.StackFrame::getDeclaringClass)
                        .filter(caller -> caller != GameRules.class
                                && !caller.getName().startsWith("com.diabolical.defaultgamerules.")
                                && !caller.getName().startsWith("net.minecraft.")
                                && !caller.getName().startsWith("net.neoforged.")
                                && !caller.getName().startsWith("java."))
                        .filter(caller -> GameruleDefaults.recordRuleOwner(name, caller))
                        .findFirst());
    }
}