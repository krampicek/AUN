package dev.example.aun.mixin;

import dev.example.aun.Aun;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Unique private static final Map<String, BlockPos> AUN_PORTAL_LINKS = new ConcurrentHashMap<>();

    @Unique private int aun$trackedHotbarSlot = -1;
    @Unique private ItemStack aun$trackedMainStack = ItemStack.EMPTY;
    @Unique private int aun$trackedMainTotalCount = 0;
    @Unique private ItemStack aun$trackedOffhandStack = ItemStack.EMPTY;
    @Unique private int aun$trackedOffhandTotalCount = 0;
    @Unique private boolean aun$droppedThisTick = false;

    @Unique private ResourceKey<Level> aun$lastDimension = null;
    @Unique private BlockPos aun$lastPortalBlockPos = null;

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$hideOtherPlayerNametags(CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().hideNametags.hidePlayerNametags) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("RETURN"),
        require = 0
    )
    private void aun$persistentDrops(ItemStack stack, boolean randomly, boolean thrower,
                                     CallbackInfoReturnable<ItemEntity> cir) {
        this.aun$droppedThisTick = true;
        ItemEntity item = cir.getReturnValue();
        if (item != null && AunConfigManager.get().playerDropsNeverDespawn) {
            item.setUnlimitedLifetime();
            item.addTag(Aun.DROP_TAG);
            item.addTag(Aun.sessionTag());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void aun$onPlayerTick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        AunConfig cfg = AunConfigManager.get();
        cfg.syncMirrors();

        if (cfg.autoClimbLadders
                && self.onClimbable()
                && !self.isSpectator()
                && !self.getAbilities().flying) {
            float pitch = self.getXRot();
            Vec3 v = self.getDeltaMovement();
            if (pitch < -20.0F) {
                self.setDeltaMovement(v.x, 0.2D, v.z);
            } else if (pitch > 20.0F) {
                self.setDeltaMovement(v.x, -0.15D, v.z);
            } else if (v.y < 0.0D && !self.isShiftKeyDown()) {
                self.setDeltaMovement(v.x, 0.0D, v.z);
            }
        }

        if (!self.level().isClientSide()) {
            if (self instanceof ServerPlayer serverPlayer) {
                this.aun$handleNetherPortalLinking(serverPlayer);
            }

            if (!self.isCreative() && !self.isSpectator()) {
                this.aun$handleStackRefill(self);
            }
        }
        this.aun$droppedThisTick = false;
    }

    @Unique
    private void aun$handleNetherPortalLinking(ServerPlayer player) {
        ResourceKey<Level> currentDim = player.level().dimension();
        ServerLevel serverLevel = (ServerLevel) player.level();
        BlockPos currentPos = player.blockPosition();

        if (this.aun$lastDimension != null && !this.aun$lastDimension.equals(currentDim)) {
            boolean isOverworldNetherSwap =
                    (this.aun$lastDimension.equals(Level.OVERWORLD) && currentDim.equals(Level.NETHER))
                    || (this.aun$lastDimension.equals(Level.NETHER) && currentDim.equals(Level.OVERWORLD));
            if (isOverworldNetherSwap && this.aun$lastPortalBlockPos != null && AunConfigManager.get().netherPortalFix) {
                String fromKey = aun$portalKey(player, this.aun$lastDimension, this.aun$lastPortalBlockPos);
                BlockPos rememberedDest = AUN_PORTAL_LINKS.get(fromKey);
                if (rememberedDest != null) {
                    BlockPos validPortal = aun$findNearestPortalBlock(serverLevel, rememberedDest, 6);
                    if (validPortal != null) {
                        player.teleportTo(
                                validPortal.getX() + 0.5D,
                                validPortal.getY(),
                                validPortal.getZ() + 0.5D);
                        currentPos = validPortal;
                    }
                }
                BlockPos arrivalPortal = aun$findNearestPortalBlock(serverLevel, currentPos, 6);
                if (arrivalPortal != null) {
                    AUN_PORTAL_LINKS.put(fromKey, arrivalPortal.immutable());
                    AUN_PORTAL_LINKS.put(aun$portalKey(player, currentDim, arrivalPortal), this.aun$lastPortalBlockPos.immutable());
                }
            }
        }

        BlockPos nearbyPortal = aun$findNearestPortalBlock(serverLevel, currentPos, 2);
        if (nearbyPortal != null) {
            this.aun$lastPortalBlockPos = nearbyPortal;
        }
        this.aun$lastDimension = currentDim;
    }

    @Unique
    private static String aun$portalKey(ServerPlayer player, ResourceKey<Level> dim, BlockPos pos) {
        return player.getUUID() + ":" + dim.toString() + ":" + (pos.getX() >> 3) + ":" + (pos.getZ() >> 3);
    }

    @Unique
    private static BlockPos aun$findNearestPortalBlock(ServerLevel level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos check : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (level.getBlockState(check).is(Blocks.NETHER_PORTAL)) {
                double d = check.distSqr(center);
                if (d < bestDist) {
                    bestDist = d;
                    best = check.immutable();
                }
            }
        }
        return best;
    }

    @Unique
    private void aun$handleStackRefill(Player player) {
        Inventory inv = player.getInventory();
        boolean cursorHoldingItem = !player.containerMenu.getCarried().isEmpty();

        if (AunConfigManager.get().stackRefill && !this.aun$droppedThisTick && !cursorHoldingItem) {
            if (this.aun$trackedHotbarSlot >= 0
                    && this.aun$trackedHotbarSlot < 9
                    && !this.aun$trackedMainStack.isEmpty()
                    && player.getMainHandItem().isEmpty()
                    && inv.getItem(this.aun$trackedHotbarSlot).isEmpty()) {
                int currentTotal = aun$countTotalMatching(inv, this.aun$trackedMainStack);
                // Only refill if an item was actually consumed/broken, NEVER if moved to another inventory slot!
                if (currentTotal < this.aun$trackedMainTotalCount) {
                    int match = aun$findMatchingSlot(inv, this.aun$trackedMainStack, this.aun$trackedHotbarSlot);
                    if (match != -1) {
                        ItemStack replacement = inv.getItem(match).copy();
                        inv.setItem(match, ItemStack.EMPTY);
                        inv.setItem(this.aun$trackedHotbarSlot, replacement);
                    }
                }
            }

            if (!this.aun$trackedOffhandStack.isEmpty() && player.getOffhandItem().isEmpty()) {
                int currentTotal = aun$countTotalMatching(inv, this.aun$trackedOffhandStack);
                if (currentTotal < this.aun$trackedOffhandTotalCount) {
                    int match = aun$findMatchingSlot(inv, this.aun$trackedOffhandStack, -1);
                    if (match != -1) {
                        ItemStack replacement = inv.getItem(match).copy();
                        inv.setItem(match, ItemStack.EMPTY);
                        player.setItemInHand(InteractionHand.OFF_HAND, replacement);
                    }
                }
            }
        }

        ItemStack currentMain = player.getMainHandItem();
        this.aun$trackedHotbarSlot = -1;
        this.aun$trackedMainStack = ItemStack.EMPTY;
        this.aun$trackedMainTotalCount = 0;
        if (!currentMain.isEmpty()) {
            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i) == currentMain) {
                    this.aun$trackedHotbarSlot = i;
                    this.aun$trackedMainStack = currentMain.copy();
                    this.aun$trackedMainTotalCount = aun$countTotalMatching(inv, currentMain);
                    break;
                }
            }
        }
        ItemStack currentOff = player.getOffhandItem();
        if (!currentOff.isEmpty()) {
            this.aun$trackedOffhandStack = currentOff.copy();
            this.aun$trackedOffhandTotalCount = aun$countTotalMatching(inv, currentOff) + currentOff.getCount();
        } else {
            this.aun$trackedOffhandStack = ItemStack.EMPTY;
            this.aun$trackedOffhandTotalCount = 0;
        }
    }

    @Unique
    private static int aun$countTotalMatching(Inventory inv, ItemStack target) {
        int sum = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, target)) {
                sum += s.getCount();
            }
        }
        return sum;
    }

    @Unique
    private static int aun$findMatchingSlot(Inventory inv, ItemStack target, int excludeSlot) {
        for (int i = 9; i < 36; i++) {
            ItemStack candidate = inv.getItem(i);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, target)) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i == excludeSlot) {
                continue;
            }
            ItemStack candidate = inv.getItem(i);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, target)) {
                return i;
            }
        }
        return -1;
    }
}
