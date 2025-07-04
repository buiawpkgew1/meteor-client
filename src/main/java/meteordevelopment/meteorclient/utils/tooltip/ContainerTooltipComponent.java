/*
 * This file is part of the Meteor Client distribution (https://github.com/buiawpkgew1/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ContainerTooltipComponent implements TooltipComponent, MeteorTooltipData {
    private static final Identifier TEXTURE_CONTAINER_BACKGROUND = MeteorClient.identifier("textures/container.png");

    private final ItemStack[] items;
    private final Color color;

    public ContainerTooltipComponent(ItemStack[] items, Color color) {
        this.items = items;
        this.color = color;
    }

    @Override
    public TooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return 67;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 176;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        // Background
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE_CONTAINER_BACKGROUND, x, y, 0, 0, 176, 67, 176, 67, color.getPacked());

        // Contents
        int row = 0;
        int i = 0;

        for (ItemStack itemStack : items) {
            RenderUtils.drawItem(context, itemStack, x + 8 + i * 18, y + 7 + row * 18, 1, true, null, false);

            i++;
            if (i >= 9) {
                i = 0;
                row++;
            }
        }
    }
}
