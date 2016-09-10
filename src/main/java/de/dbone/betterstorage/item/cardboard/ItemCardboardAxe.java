package de.dbone.betterstorage.item.cardboard;

import de.dbone.betterstorage.BetterStorage;
import de.dbone.betterstorage.misc.Constants;
import de.dbone.betterstorage.utils.MiscUtils;
import de.dbone.betterstorage.utils.StackUtils;
import net.minecraft.block.Block;
//import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemCardboardAxe extends ItemAxe implements ICardboardItem {
	
	private String name;
	
	public ItemCardboardAxe() {
		super(ItemCardboardSheet.toolMaterial);
		setCreativeTab(BetterStorage.creativeTab);
		setUnlocalizedName(Constants.modId + "." + getItemName());
		GameRegistry.registerItem(this, getItemName());
	}
	
	/** Returns the name of this item, for example "drinkingHelmet". */
	public String getItemName() {
		return ((name != null) ? name : (name = MiscUtils.getName(this)));
	}
	
	/*@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconRegister) {
		itemIcon = iconRegister.registerIcon(Constants.modId + ":" + getItemName());
	}*/
	
	@Override
	@SideOnly(Side.CLIENT)
	public int getColorFromItemStack(ItemStack stack, int renderPass) {
		return StackUtils.get(stack, 0x705030, "display", "color");
	}
	
	@Override
	public boolean canDye(ItemStack stack) { return true; }
	
	// Makes sure cardboard tools don't get destroyed,
	// and are ineffective when durability is at 0.
	@Override public boolean canHarvestBlock(Block block, ItemStack stack) {
		return ItemCardboardSheet.canHarvestBlock(stack, super.canHarvestBlock(block, stack)); }
	@Override public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
		return !ItemCardboardSheet.isEffective(stack); }
	/*@Override public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase player) {
		return ItemCardboardSheet.onBlockDestroyed(world, block, x, y, z, stack, player); }*/
	@Override public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase player) {
		return ItemCardboardSheet.damageItem(stack, 1, player); }
	
}
