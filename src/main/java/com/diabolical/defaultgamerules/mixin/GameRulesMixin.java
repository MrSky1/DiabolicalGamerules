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
        Class<?> directCaller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> {
                    var stack = frames.toList();
                    for (int i = 0; i < stack.size(); i++) {
                        StackWalker.StackFrame frame = stack.get(i);
                        if (frame.getDeclaringClass() == GameRules.class
                                && frame.getMethodName().equals("register")) {
                            return i + 1 < stack.size()
                                    ? stack.get(i + 1).getDeclaringClass()
                                    : null;
                        }
                    }
                    return null;
                });

        // Vanilla's own static initializer is the direct caller for vanilla
        // rules. Do not walk farther up the stack: another mod may merely
        // have triggered GameRules class initialization.
        if (directCaller == null
                || directCaller == GameRules.class
                || directCaller.getName().startsWith("com.diabolical.defaultgamerules.")
                || directCaller.getName().startsWith("net.minecraft.")
                || directCaller.getName().startsWith("net.neoforged.")
                || directCaller.getName().startsWith("java.")) {
            return;
        }

        GameruleDefaults.recordRuleOwner(name, directCaller);
    }
}
