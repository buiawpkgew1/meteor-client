/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class SelfTrap extends Module {
    public enum TopMode {
        AntiFacePlace,
        Full,
        Top,
        None
    }

    public enum BottomMode {
        Single,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("渲染");

    // General

    private final Setting<TopMode> topPlacement = sgGeneral.add(new EnumSetting.Builder<TopMode>()
            .name("顶部模式")
            .description("在你的上半身放置哪些位置.")
            .defaultValue(TopMode.Top)
            .build()
    );

    private final Setting<BottomMode> bottomPlacement = sgGeneral.add(new EnumSetting.Builder<BottomMode>()
            .name("底层模式")
            .description("在你的下半身放置哪些位置.")
            .defaultValue(BottomMode.None)
            .build()
    );

    private final Setting<Integer> delaySetting = sgGeneral.add(new IntSetting.Builder()
            .name("地点-延迟")
            .description("块状放置之间有多少个刻度.")
            .defaultValue(1)
            .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
            .name("中心")
            .description("以你所站的区块为中心,然后再放置.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("关掉")
            .description("放置后关闭.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("轮换")
            .description("放置时向服务器发送旋转数据包。.")
            .defaultValue(true)
            .build()
    );

    // 渲染

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("渲染")
            .description("渲染一个放置黑曜石的区块覆盖层.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("形状-模式")
            .description("形状是如何呈现的.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("侧面颜色")
            .description("被渲染的块的边的颜色.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("线条颜色")
            .description("被渲染的块的线条的颜色.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
    );

    private final List<BlockPos> placePositions = new ArrayList<>();
    private boolean placed;
    private int delay;

    public SelfTrap(){
        super(Categories.Combat, "自我陷阱", "在头顶放置黑曜石.");
    }

    @Override
    public void onActivate() {
        if (!placePositions.isEmpty()) placePositions.clear();
        delay = 0;
        placed = false;

        if (center.get()) PlayerUtils.centerPlayer();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);

        if (turnOff.get() && ((placed && placePositions.isEmpty()) || !obsidian.found())) {
            toggle();
            return;
        }

        if (!obsidian.found()) {
            placePositions.clear();
            return;
        }

        findPlacePos();

        if (delay >= delaySetting.get() && placePositions.size() > 0) {
            BlockPos blockPos = placePositions.get(placePositions.size() - 1);

            if (BlockUtils.place(blockPos, obsidian, rotate.get(), 50)) {
                placePositions.remove(blockPos);
                placed = true;
            }

            delay = 0;
        }
        else delay++;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || placePositions.isEmpty()) return;
        for (BlockPos pos : placePositions) event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private void findPlacePos() {
        placePositions.clear();
        BlockPos pos = mc.player.getBlockPos();

        switch (topPlacement.get()) {
            case Full:
                add(pos.add(0, 2, 0));
                add(pos.add(1, 1, 0));
                add(pos.add(-1, 1, 0));
                add(pos.add(0, 1, 1));
                add(pos.add(0, 1, -1));
                break;
            case Top:
                add(pos.add(0, 2, 0));
                break;
            case AntiFacePlace:
                add(pos.add(1, 1, 0));
                add(pos.add(-1, 1, 0));
                add(pos.add(0, 1, 1));
                add(pos.add(0, 1, -1));

        }

        if (bottomPlacement.get() == BottomMode.Single) add(pos.add(0, -1, 0));
    }


    private void add(BlockPos blockPos) {
        if (!placePositions.contains(blockPos) && mc.world.getBlockState(blockPos).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent())) placePositions.add(blockPos);
    }
}
