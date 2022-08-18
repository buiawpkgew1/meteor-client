/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.function.Predicate;

public class Surround extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("渲染");

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("用什么区块来做环绕.")
        .defaultValue(Blocks.OBSIDIAN)
        .filter(this::blockFilter)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("延迟")
        .description("块放置之间的延迟，以刻度为单位.")
        .min(0)
        .defaultValue(0)
        .build()
    );

    private final Setting<Center> center = sgGeneral.add(new EnumSetting.Builder<Center>()
        .name("中心")
        .description("将你传送到街区的中心.")
        .defaultValue(Center.Incomplete)
        .build()
    );

    private final Setting<Boolean> doubleHeight = sgGeneral.add(new BoolSetting.Builder()
        .name("双高")
        .description("将黑曜石放在原来的环绕块之上，以防止人们对你进行脸部定位.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("只有在地面上")
        .description("只有当你站在积木上时才会工作.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleOnYChange = sgGeneral.add(new BoolSetting.Builder()
        .name("变化时的切换")
        .description("当你的Y水平发生变化时，自动关闭（阶梯，跳跃等）.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleOnComplete = sgGeneral.add(new BoolSetting.Builder()
        .name("完成时切换")
        .description("当所有区块放置完毕后切换为关闭状态.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("轮换")
        .description("自动朝向被放置的黑曜石.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> protect = sgGeneral.add(new BoolSetting.Builder()
        .name("保护")
        .description("试图打破环绕位置的晶体，以防止环绕断裂.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("渲染")
        .description("渲染一个放置黑曜石的区块覆盖层.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderBelow = sgRender.add(new BoolSetting.Builder()
        .name("以下是")
        .description("渲染你下面的区块.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("形状-模式")
        .description("形状是如何呈现的.")
        .defaultValue(ShapeMode.Sides)
        .build()
    );

    private final Setting<SettingColor> safeColor = sgRender.add(new ColorSetting.Builder()
        .name("安全颜色")
        .description("安全区块的颜色.")
        .defaultValue(new SettingColor(13, 255, 0, 50))
        .build()
    );

    private final Setting<SettingColor> normalColor = sgRender.add(new ColorSetting.Builder()
        .name("正常颜色")
        .description("正常环绕块的颜色.")
        .defaultValue(new SettingColor(0, 255, 238, 50))
        .build()
    );

    private final Setting<SettingColor> unSafeColor = sgRender.add(new ColorSetting.Builder()
        .name("不安全颜色")
        .description("不安全区块的颜色.")
        .defaultValue(new SettingColor(204, 0, 0, 50))
        .build()
    );

    private final BlockPos.Mutable placePos = new BlockPos.Mutable();
    private final BlockPos.Mutable renderPos = new BlockPos.Mutable();
    private final BlockPos.Mutable testPos = new BlockPos.Mutable();
    private int ticks;

    public Surround() {
        super(Categories.Combat, "自我包围", "用方块包围你以防止大量水晶爆炸.");
    }

    // Render

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;

        // Below
        if (renderBelow.get()) draw(event, null, -1, 0);

        // Regular surround positions
        for (CardinalDirection direction : CardinalDirection.values()) {
            draw(event, direction, 0, doubleHeight.get() ? Dir.UP : 0);
        }

        // Double height
        if (doubleHeight.get()) {
            for (CardinalDirection direction : CardinalDirection.values()) {
                draw(event, direction, 1, Dir.DOWN);
            }
        }
    }

    private void draw(Render3DEvent event, CardinalDirection direction, int y, int exclude) {
        renderPos.set(offsetPosFromPlayer(direction, y));
        Color color = getBlockColor(renderPos);
        event.renderer.box(renderPos, color, color, shapeMode.get(), exclude);
    }

    // Function

    @Override
    public void onActivate() {
        // Center on activate
        if (center.get() == Center.OnActivate) PlayerUtils.centerPlayer();

        // Reset delay
        ticks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Toggle if Y level changed
        if (toggleOnYChange.get() && mc.player.prevY < mc.player.getY()) {
            toggle();
            return;
        }

        // Wait till player is on ground
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;

        // Wait until the player has a block available to place
        if (!getInvBlock().found()) return;

        // Centering player
        if (center.get() == Center.Always) PlayerUtils.centerPlayer();

        // Tick the placement timer
        if (ticks > 0) {
            ticks--;
            return;
        }
        else {
            ticks = delay.get();
        }

        // Check surround blocks in order and place the first missing one if present
        int safe = 0;

        // Looping through feet blocks
        for (CardinalDirection direction : CardinalDirection.values()) {
            if (place(direction, 0)) break;
            safe++;
        }

        // Looping through head blocks
        if (doubleHeight.get() && safe == 4) {
            for (CardinalDirection direction : CardinalDirection.values()) {
                if (place(direction, 1)) break;
                safe++;
            }
        }

        boolean complete = safe == (doubleHeight.get() ? 8 : 4);

        // Disable if all the surround blocks are placed
        if (complete && toggleOnComplete.get()) {
            toggle();
            return;
        }

        // Keep the player centered until all the blocks are placed to avoid collision
        if (!complete && center.get() == Center.Incomplete) PlayerUtils.centerPlayer();
    }

    private boolean place(CardinalDirection direction, int y) {
        placePos.set(offsetPosFromPlayer(direction, y));

        // Attempt to place
        boolean placed = BlockUtils.place(
            placePos,
            getInvBlock(),
            rotate.get(),
            100,
            true
        );

        // Check if the block is being mined
        boolean beingMined = false;
        for (BlockBreakingInfo value : ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values()) {
            if (value.getPos().equals(placePos)) {
                beingMined = true;
                break;
            }
        }

        boolean isThreat = mc.world.getBlockState(placePos).getMaterial().isReplaceable() || beingMined;

        // If the block is air or is being mined, destroy nearby crystals to be safe
        if (protect.get() && !placed && isThreat) {
            Box box = new Box(
                placePos.getX() - 1, placePos.getY() - 1, placePos.getZ() - 1,
                placePos.getX() + 1, placePos.getY() + 1, placePos.getZ() + 1
            );

            Predicate<Entity> entityPredicate = entity -> entity instanceof EndCrystalEntity && DamageUtils.crystalDamage(mc.player, entity.getPos()) < PlayerUtils.getTotalHealth();

            for (Entity crystal : mc.world.getOtherEntities(null, box, entityPredicate)) {
                if (rotate.get()) {
                    Rotations.rotate(Rotations.getPitch(crystal), Rotations.getYaw(crystal), () -> {
                        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                    });
                }
                else {
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                }

                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }

        return placed;
    }

    private BlockPos.Mutable offsetPosFromPlayer(CardinalDirection direction, int y) {
        return offsetPos(mc.player.getBlockPos(), direction, y);
    }

    private BlockPos.Mutable offsetPos(BlockPos origin, CardinalDirection direction, int y) {
        if (direction == null) {
            return testPos.set(
                origin.getX(),
                origin.getY() + y,
                origin.getZ()
            );
        }

        return testPos.set(
            origin.getX() + direction.toDirection().getOffsetX(),
            origin.getY() + y,
            origin.getZ() + direction.toDirection().getOffsetZ()
        );
    }

    private BlockType getBlockType(BlockPos pos) {
        BlockState blockState = mc.world.getBlockState(pos);

        // Unbreakable eg. bedrock
        if (blockState.getBlock().getHardness() < 0) return BlockType.Safe;
        // Blast resistant eg. obsidian
        else if (blockState.getBlock().getBlastResistance() >= 600) return BlockType.Normal;
        // Anything else
        else return BlockType.Unsafe;
    }

    private Color getBlockColor(BlockPos pos) {
        return switch (getBlockType(pos)) {
            case Safe -> safeColor.get();
            case Normal -> normalColor.get();
            case Unsafe -> unSafeColor.get();
        };
    }

    private FindItemResult getInvBlock() {
        return InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
    }

    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.RESPAWN_ANCHOR;
    }

    public enum Center {
        Never,
        OnActivate,
        Incomplete,
        Always
    }

    public enum BlockType {
        Safe,
        Normal,
        Unsafe
    }
}
