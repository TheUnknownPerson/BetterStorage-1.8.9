package de.dbone.betterstorage.item.tile;

import java.util.List;

import de.dbone.betterstorage.tile.entity.TileEntityPresent;
import de.dbone.betterstorage.utils.StackUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPresent extends ItemCardboardBox {
	
	public ItemPresent(Block block) {
		super(block);
	}
	
	@Override
	public EnumRarity getRarity(ItemStack stack) { return EnumRarity.UNCOMMON; }
	
	@Override
	public boolean showDurabilityBar(ItemStack stack) { return false; }
	
	@Override
	@SideOnly(Side.CLIENT)
	public int getColorFromItemStack(ItemStack stack, int renderPass) { return 0xFFFFFF; }
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advancedTooltips) {
		String nameTag = StackUtils.get(stack, null, TileEntityPresent.TAG_NAMETAG);
		if (nameTag != null) list.add("for " + nameTag);
	}
	
	@Override
	public boolean canBeStoredInContainerItem(ItemStack item) { return true; }
	
}
