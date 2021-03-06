package de.dbone.betterstorage.client.model;


import net.minecraft.client.model.ModelBase;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import de.dbone.betterstorage.misc.Resources;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.lib.models.AdvancedModelLoader;
import thaumcraft.client.lib.models.IModelCustom;

@SideOnly(Side.CLIENT)
public class ModelLocker extends ModelBase {
	
	private IModelCustom model;
	
	protected ResourceLocation modelPath() { return Resources.modelLocker; }
	
	public ModelLocker() {
		model = AdvancedModelLoader.loadModel(modelPath());
	}
	
	public void renderAll(boolean mirror, float doorAngle) {
		
		model.renderPart("box");
		
		if (doorAngle > 0) {
			float rotatePointX = (mirror ? -7 : 7);
			float rotatePointZ = -7;
			if (mirror) doorAngle = -doorAngle;
			
			GL11.glTranslated(-rotatePointX, 0, -rotatePointZ);
			GL11.glRotatef(-doorAngle, 0, 1, 0);
			GL11.glTranslated(rotatePointX, 0, rotatePointZ);
		}
		model.renderPart("door");
		
		if (mirror)
			GL11.glTranslated(-11, 0, 0);
		model.renderPart("handle");
		
	}
	
}
