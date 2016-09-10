package de.dbone.betterstorage.addon.minetweaker;

import minetweaker.api.item.IIngredient;
import de.dbone.betterstorage.api.crafting.IRecipeInput;
import de.dbone.betterstorage.api.crafting.RecipeBounds;
import de.dbone.betterstorage.api.crafting.ShapelessStationRecipe;
import de.dbone.betterstorage.api.crafting.StationCrafting;
import net.minecraft.item.ItemStack;

public class MTShapelessStationRecipe extends ShapelessStationRecipe {
	
	public MTShapelessStationRecipe(IIngredient[] ingredients, ItemStack[] output, int experience, int craftingTime) {
		super(toRecipeInputs(ingredients), output);
		setRequiredExperience(experience);
		setCraftingTime(craftingTime);
	}
	private static IRecipeInput[] toRecipeInputs(IIngredient[] ingredients) {
		IRecipeInput[] inputs = new IRecipeInput[ingredients.length];
		for (int i = 0; i < inputs.length; i++)
			if (ingredients[i] != null)
				inputs[i] = new RecipeInputIngredient(ingredients[i]);
		return inputs;
	}
	
	@Override
	public StationCrafting checkMatch(ItemStack[] input, RecipeBounds bounds) {
		StationCrafting crafting = super.checkMatch(input, bounds);
		return ((crafting != null) ? new MTStationCrafting(crafting) : null);
	}
	
}
