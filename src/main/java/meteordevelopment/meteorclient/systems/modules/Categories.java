/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.Items;

public class Categories {
    public static final Category Combat = new Category(I18n.translate("Combat"), Items.GOLDEN_SWORD.getDefaultStack());
    public static final Category Player = new Category(I18n.translate("Player"), Items.ARMOR_STAND.getDefaultStack());
    public static final Category Movement = new Category(I18n.translate("Movement"), Items.DIAMOND_BOOTS.getDefaultStack());
    public static final Category Render = new Category(I18n.translate("Render"), Items.GLASS.getDefaultStack());
    public static final Category World = new Category(I18n.translate("World"), Items.GRASS_BLOCK.getDefaultStack());
    public static final Category Misc = new Category(I18n.translate("Misc"), Items.LAVA_BUCKET.getDefaultStack());

    public static boolean REGISTERING;

    public static void init() {
        REGISTERING = true;

        // Meteor
        Modules.registerCategory(Combat);
        Modules.registerCategory(Player);
        Modules.registerCategory(Movement);
        Modules.registerCategory(Render);
        Modules.registerCategory(World);
        Modules.registerCategory(Misc);

        // Addons
        AddonManager.ADDONS.forEach(MeteorAddon::onRegisterCategories);

        REGISTERING = false;
    }
}
