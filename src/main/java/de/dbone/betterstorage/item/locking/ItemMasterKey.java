package de.dbone.betterstorage.item.locking;

import java.util.List;

import de.dbone.betterstorage.api.BetterStorageEnchantment;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
//import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemMasterKey extends ItemKey {
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs creativeTab, List list) {
		ItemStack stack = new ItemStack(item, 1, 0);
		Enchantment ench = BetterStorageEnchantment.get("unlocking");
		if (ench != null) stack.addEnchantment(ench, 10);
		list.add(stack);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public EnumRarity getRarity(ItemStack stack) { return EnumRarity.RARE; }

	@Override
	public boolean hasEffect(ItemStack stack) {
		return true;
	}
	
	@Override
	public void onCreated(ItemStack stack, World world, EntityPlayer player) {  }
	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isBeingHeld) {  }

	@SideOnly(Side.CLIENT)
	@Override
	public int getColorFromItemStack(ItemStack stack, int renderPass) { return 0xFFFFFF; }
	
	// IKey implementation
	
	@Override
	public boolean unlock(ItemStack key, ItemStack lock, boolean useAbility) { return true; }
	@Override
	public boolean canApplyEnchantment(ItemStack key, Enchantment enchantment) { return false; }
	
}
