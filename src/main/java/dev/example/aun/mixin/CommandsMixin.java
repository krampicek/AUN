package dev.example.aun.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void aun$registerNetherCalc(
            Commands.CommandSelection selection,
            CommandBuildContext context,
            CallbackInfo ci) {
        this.dispatcher.register(Commands.literal("nethercalc").executes(CommandsMixin::aun$executeNetherCalc));
        this.dispatcher.register(Commands.literal("Nethercalc").executes(CommandsMixin::aun$executeNetherCalc));
    }

    @Unique
    private static int aun$executeNetherCalc(CommandContext<CommandSourceStack> ctx) {
        if (!AunConfigManager.get().netherCalc) {
            return 0;
        }
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        double x = aun$roundHalf(player.getX());
        double y = aun$roundHalf(player.getY());
        double z = aun$roundHalf(player.getZ());

        ResourceKey<Level> dim = player.level().dimension();
        if (dim.equals(Level.OVERWORLD)) {
            double outX = aun$roundHalf(x / 8.0);
            double outZ = aun$roundHalf(z / 8.0);
            player.sendSystemMessage(Component.literal(String.format(
                    Locale.ROOT,
                    "§6[NetherCalc] §fNether Coords: §aX: %.1f, Y: %.1f, Z: %.1f",
                    outX, y, outZ)));
            return 1;
        } else if (dim.equals(Level.NETHER)) {
            double outX = aun$roundHalf(x * 8.0);
            double outZ = aun$roundHalf(z * 8.0);
            player.sendSystemMessage(Component.literal(String.format(
                    Locale.ROOT,
                    "§6[NetherCalc] §fOverworld Coords: §aX: %.1f, Y: %.1f, Z: %.1f",
                    outX, y, outZ)));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal(
                    "§c[NetherCalc] You must be in the Overworld or the Nether."));
            return 0;
        }
    }

    @Unique
    private static double aun$roundHalf(double value) {
        return Math.round(value * 2.0) / 2.0;
    }
}
