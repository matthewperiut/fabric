package net.fabricmc.fabric.mixin.itemgroup.client;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;

import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemGroups.class)
public class ItemGroupsMixin {
	@Shadow
	@Final
	private static Identifier ITEM_SEARCH_TAB_TEXTURE_ID;

	// Modify the search tab to appear as a regular tab so that we can render the search bar above a regular tab texture
	@ModifyArg(
			method = "registerAndGetDefault(Lnet/minecraft/registry/Registry;)Lnet/minecraft/item/ItemGroup;",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/item/ItemGroup$Builder;texture(Lnet/minecraft/util/Identifier;)Lnet/minecraft/item/ItemGroup$Builder;"
			),
			index = 0
	)
	private static Identifier modifySearchTexture(Identifier original) {
		if (original.equals(ITEM_SEARCH_TAB_TEXTURE_ID)) {
			return ItemGroup.getTabTextureId("items");
		}
		return original;
	}
}
