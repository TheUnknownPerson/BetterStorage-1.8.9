package de.dbone.betterstorage.tile.crate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import de.dbone.betterstorage.BetterStorage;
import de.dbone.betterstorage.api.crate.ICrateStorage;
import de.dbone.betterstorage.api.crate.ICrateWatcher;
import de.dbone.betterstorage.config.GlobalConfig;
import de.dbone.betterstorage.container.ContainerBetterStorage;
import de.dbone.betterstorage.container.ContainerCrate;
import de.dbone.betterstorage.content.BetterStorageTiles;
import de.dbone.betterstorage.inventory.InventoryCratePlayerView;
import de.dbone.betterstorage.misc.Constants;
import de.dbone.betterstorage.misc.ItemIdentifier;
import de.dbone.betterstorage.tile.entity.TileEntityContainer;
import de.dbone.betterstorage.utils.GuiHandler;
import de.dbone.betterstorage.utils.WorldUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public class TileEntityCrate extends TileEntityContainer implements IInventory, ICrateStorage, ICrateWatcher {
	
	private static final EnumFacing[] sideDirections = {
			EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
	};
	
	public static final int slotsPerCrate = 18;
	
	private CratePileData data;
	/** Crate pile id of this crate, used only for saving/loading. */
	private int id = -1;
	
	public int getID() { return id; }
	
	/** Get the pile data for this tile entity. */
	public CratePileData getPileData() {
		if (worldObj.isRemote)
			throw new IllegalStateException("Can't be called client-side.");
		if (data == null) {
			CratePileCollection collection = CratePileCollection.getCollection(worldObj);
			if (id == -1)
				setPileData(collection.createCratePile(), true);
			else setPileData(collection.getCratePile(id), false);
		}
		return data;
	}
	/** Sets the pile data and adds the crate to it if desired. <br>
	 *  Removes the crate from the old pile data if it had one. */
	private void setPileData(CratePileData data, boolean addCrate) {
		if (this.data != null)
			this.data.removeCrate(this);
		this.data = data;
		if (data != null) {
			id = data.id;
			markForUpdate();
			if (addCrate) data.addCrate(this);
		} else id = -1;
	}
	/** Destroys all crates above, and makes sure when piles split,
	 *  each pile gets their own CratePileData object. */
	private void checkPileConnections(CratePileData data) {
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		
		// Destroy all crates above.
		TileEntityCrate crateAbove = WorldUtils.get(worldObj, new BlockPos(x, y + 1, z), TileEntityCrate.class);
		if ((crateAbove != null) && (crateAbove.data == data)) {
			worldObj.setBlockToAir(new BlockPos(x, y + 1, z));
			crateAbove.dropItem(new ItemStack(BetterStorageTiles.crate));
		}
		
		// If there's still some crates left and this is a
		// base crate, see which crates are still connected.
		if ((data.getNumCrates() <= 0) || (y != data.getRegion().minY)) return;
		
		// If there's more than one crate set, they need to split.
		List<HashSet<TileEntityCrate>> crateSets = getCrateSets(x, y, z, data);
		if (crateSets.size() <= 1) return;
		
		// The first crate set will keep the original pile data.
		// All other sets will get new pile data objects.
		for (int i = 1; i < crateSets.size(); i++) {
			HashSet<TileEntityCrate> set = crateSets.get(i);
			CratePileData newPileData = data.collection.createCratePile();
			int numCrates = set.size();
			// Add the base crates from the set.
			for (TileEntityCrate newPileCrate : set) {
				newPileCrate.setPileData(newPileData, true);
				// Add all crates above the base crate.
				while (true) {
					newPileCrate = WorldUtils.get(worldObj, new BlockPos(newPileCrate.getPos().getX(), newPileCrate.getPos().getY() + 1, newPileCrate.getPos().getZ()), TileEntityCrate.class);
					if (newPileCrate == null) break;
					newPileCrate.setPileData(newPileData, true);
					numCrates++;
				}
			}
			// Move some of the items over to the new crate pile.
			int count = numCrates * data.getOccupiedSlots() / (data.getNumCrates() + numCrates);
			for (ItemStack stack : data.getContents().getRandomStacks(count)) {
				data.removeItems(stack);
				newPileData.addItems(stack);
			}
		}
		
		// Trim the original map to the size it actually is.
		// This is needed because the crates may not be removed in
		// order, from outside to inside.
		data.trimMap();
	}
	
	private List<HashSet<TileEntityCrate>> getCrateSets(int x, int y, int z, CratePileData data) {
		List<HashSet<TileEntityCrate>> crateSets = new ArrayList<HashSet<TileEntityCrate>>();
		int checkedCrates = 0;
		
		neighborLoop: // Suck it :P
		for (EnumFacing dir : sideDirections) {
			int nx = x + dir.getFrontOffsetX();
			int nz = z + dir.getFrontOffsetZ();
			
			// Continue if this neighbor block is not part of the crate pile.
			TileEntityCrate neighborCrate = WorldUtils.get(worldObj, new BlockPos(nx, y, nz), TileEntityCrate.class);
			if ((neighborCrate == null) || (neighborCrate.data != data)) continue;
			
			// See if the neighbor crate is already in a crate set,
			// in that case continue with the next neighbor block.
			for (HashSet<TileEntityCrate> set : crateSets)
				if (set.contains(neighborCrate)) continue neighborLoop;
			
			// Create a new set of crates and fill it with all connecting crates.
			HashSet<TileEntityCrate> set = new HashSet<TileEntityCrate>();
			set.add(neighborCrate);
			for (EnumFacing ndir : sideDirections)
				checkConnections(nx + ndir.getFrontOffsetX(), y, nz + ndir.getFrontOffsetZ(), data, set);
			crateSets.add(set);
			
			// If we checked all crates, stop the loop.
			checkedCrates += set.size();
			if (checkedCrates >= data.getNumCrates()) break;
		}
		
		return crateSets;
	}
	
	private void checkConnections(int x, int y, int z, CratePileData data, HashSet<TileEntityCrate> set) {
		TileEntityCrate crate = WorldUtils.get(worldObj, new BlockPos(x, y, z), TileEntityCrate.class);
		if ((crate == null) || (data != crate.data) || set.contains(crate)) return;
		set.add(crate);
		for (EnumFacing ndir : sideDirections)
			checkConnections(x + ndir.getFrontOffsetX(), y, z + ndir.getFrontOffsetZ(), data, set);
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		if (worldObj.isRemote || (data != null)) return;
		if (!isInvalid()) getPileData();
	}
	
	public void attemptConnect(EnumFacing side) {
		if (worldObj.isRemote || (side == EnumFacing.UP)) return;
		int x = pos.getX() + side.getFrontOffsetX();
		int y = pos.getY() + side.getFrontOffsetY();
		int z = pos.getZ() + side.getFrontOffsetZ();
		TileEntityCrate crateClicked = WorldUtils.get(worldObj, new BlockPos(x, y, z), TileEntityCrate.class);
		if (crateClicked == null) return;
		CratePileData pileData = crateClicked.getPileData();
		if (pileData.canAdd(this))
			setPileData(pileData, true);
	}
	
	@Override
	public void invalidate() {
		super.invalidate();
		if (worldObj.isRemote) return;
		CratePileData data = getPileData();
		if (watcherRegistered)
			data.removeWatcher(this);
		setPileData(null, false);
		dropOverflowContents(data);
		checkPileConnections(data);
	}
	
	/** Drops a single item from the (destroyed) crate. */
	private void dropItem(ItemStack stack) {
		WorldUtils.dropStackFromBlock(worldObj, pos, stack);
	}
	/** Drops multiple item from the (destroyed) crate. */
	private void dropItems(List<ItemStack> stacks) {
		for (ItemStack stack : stacks)
			dropItem(stack);
	}
	/** Drops contents that don't fit into the
	 *  crate pile after a crate got destroyed. */
	private void dropOverflowContents(CratePileData data) {
		int amount = -data.getFreeSlots();
		if (amount <= 0) return;
		List<ItemStack> items = data.getContents().getRandomStacks(amount);
		for (ItemStack stack : items) data.removeItems(stack);
		dropItems(items);
	}
	
	// TileEntityContainer stuff
	
	@Override
	protected int getSizeContents() { return 0; }
	@Override
	public String getName() { return Constants.containerCrate; }
	
	@Override
	public void openGui(EntityPlayer playerIn, World worldIn, BlockPos pos) {
		if (!canPlayerUseContainer(playerIn)) return;
		//PlayerUtils.openGui(playerIn, getName(), getColumns(), 2 * Math.min(data.getNumCrates(), 3),
		//                    getContainerTitle(), createContainer(playerIn));
		if(data.getNumCrates() == 1)
			playerIn.openGui(BetterStorage.instance, GuiHandler.GUI_CRATE_0, worldIn, pos.getX(), pos.getY(), pos.getZ());
		else if(data.getNumCrates() == 2)
			playerIn.openGui(BetterStorage.instance, GuiHandler.GUI_CRATE_0, worldIn, pos.getX(), pos.getY(), pos.getZ());
		else if(data.getNumCrates() == 3)
			playerIn.openGui(BetterStorage.instance, GuiHandler.GUI_CRATE_0, worldIn, pos.getX(), pos.getY(), pos.getZ());
	}
	
	@Override
	public ContainerBetterStorage createContainer(EntityPlayer player) {
		return new ContainerCrate(player, new InventoryCratePlayerView(this));
	}
	
	// Comparator related
	
	private boolean watcherRegistered = false;
	
	@Override
	protected int getComparatorSignalStengthInternal() {
		if (worldObj.isRemote) return 0;
		CratePileData data = getPileData();
		return ((data.getOccupiedSlots() > 0)
				? (1 + data.getOccupiedSlots() * 14 / data.getCapacity()) : 0);
	}
	
	@Override
	protected void markComparatorAccessed() {
		super.markComparatorAccessed();
		if (!watcherRegistered && !worldObj.isRemote) {
			getPileData().addWatcher(this);
			watcherRegistered = true;
		}
	}
	
	@Override
	protected void comparatorUpdateAndReset() {
		super.comparatorUpdateAndReset();
		if (watcherRegistered && !hasComparatorAccessed()) {
			getPileData().removeWatcher(this);
			watcherRegistered = false;
		}
	}
	
	@Override
	public void onCrateItemsModified(ItemStack stack) {
		markContentsChanged();
	}
	
	// IInventory implementation

	//@Override
	public String getInventoryName() { return getName(); }
	@Override
	public int getInventoryStackLimit() { return 64; }
	
	@Override
	public int getSizeInventory() {
		if (!GlobalConfig.enableCrateInventoryInterfaceSetting.getValue()) return 0;
		if (worldObj.isRemote) return 1;
		return getPileData().blockView.getSizeInventory();
	}
	
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if (!GlobalConfig.enableCrateInventoryInterfaceSetting.getValue()) return false;
		return getPileData().blockView.isItemValidForSlot(slot, stack);
	}
	
	@Override
	public ItemStack getStackInSlot(int slot) {
		if (!GlobalConfig.enableCrateInventoryInterfaceSetting.getValue()) return null;
		return getPileData().blockView.getStackInSlot(slot);
	}
	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		if (GlobalConfig.enableCrateInventoryInterfaceSetting.getValue()) {
			getPileData().blockView.setInventorySlotContents(slot, stack);
			markDirty();
		}
	}	
	/*@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		if (!GlobalConfig.enableCrateInventoryInterfaceSetting.getValue()) return null;
			return getPileData().blockView.getStackInSlotOnClosing(slot);
	}*/
	@Override
	public ItemStack decrStackSize(int slot, int amount) {
		if (!GlobalConfig.enableCrateInventoryInterfaceSetting.getValue()) return null;
		markDirty();
		return getPileData().blockView.decrStackSize(slot, amount);
	}
	@Override
	public void markDirty() {
		if (GlobalConfig.enableCrateInventoryInterfaceSetting.getValue())
			getPileData().blockView.markDirty();
	}
	
	@Override
	public boolean isUseableByPlayer(EntityPlayer player) { return false; }

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public void openInventory(EntityPlayer player) {
		getPileData().blockView.openInventory(player);		
	}

	@Override
	public void closeInventory(EntityPlayer player) {
		getPileData().blockView.closeInventory(player);		
	}
	
	// ICrateStorage implementation
	
	private static boolean isEnabled() {
		return GlobalConfig.enableCrateStorageInterfaceSetting.getValue();
	}
	
	@Override
	public Object getCrateIdentifier() { return getPileData(); }
	
	@Override
	public int getCapacity() { return (isEnabled() ? getPileData().getCapacity() : 0); }
	@Override
	public int getOccupiedSlots() { return (isEnabled() ? getPileData().getOccupiedSlots() : 0); }
	@Override
	public int getUniqueItems() { return (isEnabled() ? getPileData().getUniqueItems() : 0);  }
	
	@Override
	public Iterable<ItemStack> getContents() {
		return (isEnabled() ? getPileData().getContents().getItems() : Collections.EMPTY_LIST);
	}
	@Override
	public Iterable<ItemStack> getRandomStacks() {
		return (isEnabled() ? getPileData().getContents().getRandomStacks() : Collections.EMPTY_LIST);
	}
	
	@Override
	public int getItemCount(ItemStack identifier) {
		return (isEnabled() ? getPileData().getContents().get(new ItemIdentifier(identifier)) : 0);
	}
	@Override
	public int getSpaceForItem(ItemStack identifier) {
		return (isEnabled() ? getPileData().getSpaceForItem(identifier) : 0);
	}
	
	@Override
	public ItemStack insertItems(ItemStack stack) {
		return (isEnabled() ? getPileData().addItems(stack) : stack);
	}
	@Override
	public ItemStack extractItems(ItemStack identifier, int amount) {
		return (isEnabled() ? getPileData().removeItems(new ItemIdentifier(identifier), amount) : null);
	}
	
	@Override
	public void registerCrateWatcher(ICrateWatcher watcher) { if (isEnabled()) getPileData().addWatcher(watcher); }
	@Override
	public void unregisterCrateWatcher(ICrateWatcher watcher) { if (isEnabled()) getPileData().removeWatcher(watcher); }
	
	// TileEntity synchronization
	
	
	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("crateId", id);
        return new S35PacketUpdateTileEntity(pos, 0, compound);
	}
	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		//id = packet.func_148857_g().getInteger("crateId");
		worldObj.markBlockForUpdate(pos);
	}
	
	// Reading from / writing to NBT
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		id = compound.getInteger("crateId");
	}
	@Override
	public void writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setInteger("crateId", id);
		// TODO: This might not be the best place to save the crate data.
		getPileData().save();
	}

	@Override
	public IChatComponent getDisplayName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getField(int id) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setField(int id, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getFieldCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}
	
}
