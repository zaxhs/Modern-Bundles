package dev.modernbundles.mixin;

import dev.modernbundles.bundle.BundleContentsSelectionAccess;

import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BundleContents.class)
public abstract class BundleContentsMixin implements BundleContentsSelectionAccess {
    @Unique
    private int modernbundles$selectedItem = -1;

    @Override
    public int modernbundles$getSelectedItem() {
        return this.modernbundles$selectedItem;
    }

    @Override
    public void modernbundles$setSelectedItem(int selectedItem) {
        this.modernbundles$selectedItem = selectedItem;
    }
}

