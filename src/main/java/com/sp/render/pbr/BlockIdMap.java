package com.sp.render.pbr;

import com.sp.SPBRevamped;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.registry.Registries;           

/**
 * In case I need a certain block to be binded to a certain ID. Kinda like how iris does it.<br>
 * Also keeps me from making a new render layer every time I need to access only a specific block. <br>
 * Lets me easily access certain blocks when running the shaders.
 */

public class BlockIdMap {
    private static Object2IntMap<Block> BlockIDs = null;
    public static boolean init = false;
    private static int numOfBlocks;

    public static void init(){
        BlockIDs = new Object2IntOpenHashMap<>();
        BlockIDs.clear();
        init = true;

        //For every block, assign an ID
        for(Block block : Registries.BLOCK){
            RenderLayer renderLayer = RenderLayers.getBlockLayer(block.getDefaultState());
            if(renderLayer == RenderLayer.getSolid()){
                BlockIDs.put(block, 0);
            } else if(renderLayer == RenderLayer.getCutout()){
                BlockIDs.put(block, 1);
            } else if(renderLayer == RenderLayer.getCutoutMipped()){
                BlockIDs.put(block, 2);
            } else if(renderLayer == RenderLayer.getTranslucent()){
                BlockIDs.put(block, 3);
            }
            numOfBlocks++;
        }

        SPBRevamped.LOGGER.info("Loaded {} Default Block IDs", numOfBlocks);

        //Custom IDs
    }

    public static int getBlockID(Block block) {
        if(BlockIDs == null || !init){
            return -1;
        }

        return BlockIDs.getOrDefault(block, -1);
    }
}
