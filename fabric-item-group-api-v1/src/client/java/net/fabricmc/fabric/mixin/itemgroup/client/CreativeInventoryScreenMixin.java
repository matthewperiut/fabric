/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.itemgroup.client;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroupImpl;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends HandledScreen<CreativeScreenHandler> implements FabricCreativeInventoryScreen {
	public CreativeInventoryScreenMixin(CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
		super(screenHandler, playerInventory, text);
	}

	@Shadow
	protected abstract void setSelectedTab(ItemGroup itemGroup_1);

	@Shadow
	private static ItemGroup selectedTab;

	// "static" matches selectedTab
	@Unique
	private static int currentPage = 0;

	@Unique
	private void updateSelection() {
		if (!isGroupVisible(selectedTab)) {
			ItemGroups.getGroups()
					.stream()
					.filter(this::isGroupVisible)
					.min((a, b) -> Boolean.compare(a.isSpecial(), b.isSpecial()))
					.ifPresent(this::setSelectedTab);
		}
	}

	@Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setEditableColor(I)V", shift = At.Shift.AFTER))
	private void init(CallbackInfo info) {
		currentPage = getPage(selectedTab);

		int xpos = x + 166;
		int ypos = y + 4;

		CreativeInventoryScreen self = (CreativeInventoryScreen) (Object) this;
		addDrawableChild(new FabricCreativeGuiComponents.ItemGroupButtonWidget(xpos + 11, ypos, FabricCreativeGuiComponents.Type.NEXT, self));
		addDrawableChild(new FabricCreativeGuiComponents.ItemGroupButtonWidget(xpos, ypos, FabricCreativeGuiComponents.Type.PREVIOUS, self));
	}

	@Inject(method = "setSelectedTab", at = @At("HEAD"), cancellable = true)
	private void setSelectedTab(ItemGroup itemGroup, CallbackInfo info) {
		if (!isGroupVisible(itemGroup)) {
			info.cancel();
		}
	}

	@Inject(method = "renderTabTooltipIfHovered", at = @At("HEAD"), cancellable = true)
	private void renderTabTooltipIfHovered(DrawContext drawContext, ItemGroup itemGroup, int mx, int my, CallbackInfoReturnable<Boolean> info) {
		if (!isGroupVisible(itemGroup)) {
			info.setReturnValue(false);
		}
	}

	@Inject(method = "isClickInTab", at = @At("HEAD"), cancellable = true)
	private void isClickInTab(ItemGroup itemGroup, double mx, double my, CallbackInfoReturnable<Boolean> info) {
		if (!isGroupVisible(itemGroup)) {
			info.setReturnValue(false);
		}
	}

	@Inject(method = "renderTabIcon", at = @At("HEAD"), cancellable = true)
	private void renderTabIcon(DrawContext drawContext, ItemGroup itemGroup, CallbackInfo info) {
		if (!isGroupVisible(itemGroup)) {
			info.cancel();
		}
	}

	@Unique
	private boolean isGroupVisible(ItemGroup itemGroup) {
		return itemGroup.shouldDisplay() && currentPage == getPage(itemGroup);
	}

	@Override
	public int getPage(ItemGroup itemGroup) {
		if (FabricCreativeGuiComponents.COMMON_GROUPS.contains(itemGroup)) {
			return currentPage;
		}

		final FabricItemGroupImpl fabricItemGroup = (FabricItemGroupImpl) itemGroup;
		return fabricItemGroup.fabric_getPage();
	}

	@Unique
	private boolean hasGroupForPage(int page) {
		return ItemGroups.getGroupsToDisplay()
				.stream()
				.anyMatch(itemGroup -> getPage(itemGroup) == page);
	}

	@Override
	public boolean switchToPage(int page) {
		if (!hasGroupForPage(page)) {
			return false;
		}

		if (currentPage == page) {
			return false;
		}

		currentPage = page;
		updateSelection();
		return true;
	}

	@Override
	public int getCurrentPage() {
		return currentPage;
	}

	@Override
	public int getPageCount() {
		return FabricCreativeGuiComponents.getPageCount();
	}

	@Override
	public List<ItemGroup> getItemGroupsOnPage(int page) {
		return ItemGroups.getGroupsToDisplay()
				.stream()
				.filter(itemGroup -> getPage(itemGroup) == page)
				// Thanks to isXander for the sorting
				.sorted(Comparator.comparing(ItemGroup::getRow).thenComparingInt(ItemGroup::getColumn))
				.sorted((a, b) -> {
					if (a.isSpecial() && !b.isSpecial()) return 1;
					if (!a.isSpecial() && b.isSpecial()) return -1;
					return 0;
				})
				.toList();
	}

	@Override
	public boolean hasAdditionalPages() {
		return ItemGroups.getGroupsToDisplay().size() > (Objects.requireNonNull(ItemGroups.displayContext).hasPermissions() ? 14 : 13);
	}

	@Override
	public ItemGroup getSelectedItemGroup() {
		return selectedTab;
	}

	@Override
	public boolean setSelectedItemGroup(ItemGroup itemGroup) {
		Objects.requireNonNull(itemGroup, "itemGroup");

		if (selectedTab == itemGroup) {
			return false;
		}

		if (currentPage != getPage(itemGroup)) {
			if (!switchToPage(getPage(itemGroup))) {
				return false;
			}
		}

		setSelectedTab(itemGroup);
		return true;
	}

	@Unique
	private boolean displaySearchBarModification = false;

	@Inject(method = "setSelectedTab", at = @At("TAIL"))
	void searchBarBackgroundDeterminant(ItemGroup group, CallbackInfo ci) {
		displaySearchBarModification = (group.getType() == ItemGroup.Type.SEARCH) && hasAdditionalPages();
	}

	@Unique
	private static final Identifier CREATIVE_SEARCH_TAB = ItemGroup.getTabTextureId("item_search");

	@Inject(method = "drawBackground", at = @At("TAIL"))
	public void renderSearchBar(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
		if (displaySearchBarModification) {
			context.drawTexture(RenderLayer::getGuiTextured, CREATIVE_SEARCH_TAB, x + 161, y + 4, 166, 4, 5, 12, 256, 256);
		}
	}

	@WrapOperation(
			method = "init",
			at = @At(
					value = "NEW",
					target = "net/minecraft/client/gui/widget/TextFieldWidget"
			)
	)
	private TextFieldWidget shortenSearchBarTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text text, Operation<TextFieldWidget> original) {
		// Modify only the available text space width to be 5 pixels shorter
		if (hasAdditionalPages()) {
			return original.call(textRenderer, x, y, width - 5, height, text);
		} else {
			return original.call(textRenderer, x, y, width, height, text);
		}
	}
}
