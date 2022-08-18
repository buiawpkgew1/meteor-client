/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoCity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("目标范围")
        .description("玩家成为目标的半径.")
        .defaultValue(4)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("自动切换")
        .description("启用 AutoCity 时自动切换到镐.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
        .name("支持")
        .description("如果城市街区下方没有街区,它将在采矿前放置一个.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("旋转")
        .description("自动将您转向城市街区.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("自切换")
        .description("激活后自动关闭.")
        .defaultValue(true)
        .build()
    );

    private PlayerEntity target;
    private BlockPos blockPosTarget;
    private boolean sentMessage;

    public AutoCity() {
        super(Categories.Combat, "自动城市", "通过挖掘附近最近的黑曜石来自动占领目标.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            PlayerEntity search = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
            if (search != target) sentMessage = false;
            target = search;
        }

        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            target = null;
            blockPosTarget = null;
            if (selfToggle.get()) toggle();
            return;
        }

        blockPosTarget = EntityUtils.getCityBlock(target);

        if (blockPosTarget == null) {
            if (selfToggle.get()) {
                error("未找到目标块...禁用.");
                toggle();
            }
            target = null;
            return;
        }

        if (PlayerUtils.distanceTo(blockPosTarget) > mc.interactionManager.getReachDistance() && selfToggle.get()) {
            error("目标块遥不可及...禁用.");
            toggle();
            return;
        }

        if (!sentMessage) {
            info("正在尝试城市 %s.", target.getEntityName());
            sentMessage = true;
        }

        FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);

        if (!pickaxe.isHotbar()) {
            if (selfToggle.get()) {
                error("没有找到镐...禁用.");
                toggle();
            }
            return;
        }

        if (support.get()) BlockUtils.place(blockPosTarget.down(1), InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, true);

        if (autoSwitch.get()) InvUtils.swap(pickaxe.slot(), false);

        if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPosTarget), Rotations.getPitch(blockPosTarget), () -> mine(blockPosTarget));
        else mine(blockPosTarget);

        if (selfToggle.get()) toggle();
    }

    private void mine(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
