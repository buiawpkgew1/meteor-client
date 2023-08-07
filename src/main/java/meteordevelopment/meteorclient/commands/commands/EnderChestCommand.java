/*
 * This file is part of the Meteor Client distribution (https://github.com/buiawpkgew1/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class EnderChestCommand extends Command {
    public EnderChestCommand() {
        super("ender-chest", "允许你预览你末影箱中的物品.", "ec", "echest");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            Utils.openContainer(Items.ENDER_CHEST.getDefaultStack(), new ItemStack[27], true);
            return SINGLE_SUCCESS;
        });
    }
}
