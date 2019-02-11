package WayofTime.alchemicalWizardry.common.rituals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import WayofTime.alchemicalWizardry.AlchemicalWizardry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import WayofTime.alchemicalWizardry.api.Int3;
import WayofTime.alchemicalWizardry.api.alchemy.energy.ReagentRegistry;
import WayofTime.alchemicalWizardry.api.rituals.IMasterRitualStone;
import WayofTime.alchemicalWizardry.api.rituals.RitualComponent;
import WayofTime.alchemicalWizardry.api.rituals.RitualEffect;
import WayofTime.alchemicalWizardry.api.soulNetwork.SoulNetworkHandler;
import WayofTime.alchemicalWizardry.common.ItemType;
import WayofTime.alchemicalWizardry.common.block.BlockTeleposer;
import gregtech.common.blocks.GT_Block_Ores;

public class RitualEffectMagnetic extends RitualEffect
{
    private static final int potentiaDrain = 10;
    private static final int terraeDrain = 10;
    private static final int orbisTerraeDrain = 10;

    private static final Map<ItemType, Boolean> oreBlockCache = new HashMap<ItemType, Boolean>();
    
    public static boolean isBlockOre(Block block, int meta)
    {
        //Special case for lit redstone ore
        if (block instanceof BlockOre || block instanceof BlockRedstoneOre || block instanceof GT_Block_Ores)
            return true;
        
        if (block == null || Item.getItemFromBlock(block) == null)
            return false;
        
        ItemType type = new ItemType(block, meta);
        Boolean result = oreBlockCache.get(type);
        if (result == null)
        {
            result = computeIsItemOre(type);
            oreBlockCache.put(type, result);
        }
        return result;
    }
    
    private static boolean computeIsItemOre(ItemType type)
    {
        ItemStack itemStack = type.createStack(1);
        for (int id : OreDictionary.getOreIDs(itemStack))
        {
			// if (id == 1164)
				// return true; // current GT ore
            String oreName = OreDictionary.getOreName(id);
			// System.out.println(oreName + " " + id);
            if (oreName.contains("ore"))
                return true;
        }
        return false;
    }
    
    @Override
    public void performEffect(IMasterRitualStone ritualStone)
    {
        String owner = ritualStone.getOwner();

        int currentEssence = SoulNetworkHandler.getCurrentEssence(owner);
        World world = ritualStone.getWorld();
        int x = ritualStone.getXCoord();
        int y = ritualStone.getYCoord();
        int z = ritualStone.getZCoord();

        boolean hasPotentia = this.canDrainReagent(ritualStone, ReagentRegistry.potentiaReagent, potentiaDrain, false);

        if (world.getWorldTime() % (hasPotentia ? 10 : 40) != 0)
        {
            return;
        }

        boolean hasTerrae = this.canDrainReagent(ritualStone, ReagentRegistry.terraeReagent, terraeDrain, false);
        boolean hasOrbisTerrae = this.canDrainReagent(ritualStone, ReagentRegistry.orbisTerraeReagent, orbisTerraeDrain, false);

        int radius = this.getRadiusForReagents(hasTerrae, hasOrbisTerrae);

        if (currentEssence < this.getCostPerRefresh())
        {
            SoulNetworkHandler.causeNauseaToPlayer(owner);
        } else
        {
            int xRep = 0;
            int yRep = 0;
            int zRep = 0;
            boolean replace = false;

            outer:
            for (int j = 1; j <= 3; j++)
            {
                for (int i = -1; i <= 1; i++)
                {
                    for (int k = -1; k <= 1; k++)
                    {
                        if ((!replace) && world.isAirBlock(x + i, y + j, z + k))
                        {
                            xRep = x + i;
                            yRep = y + j;
                            zRep = z + k;
                            replace = true;
                            break outer;
                        }
                    }
                }
            }

            if (replace)
            {
            	Int3 lastPos = this.getLastPosition(ritualStone.getCustomRitualTag());
            	
            	int j = y - 1;
            	int i = 0;
            	int k = 0;
            	
            	if(lastPos != null)
            	{
            		j = lastPos.yCoord;
            		i = Math.min(radius, Math.max(-radius, lastPos.xCoord));
            		k = Math.min(radius, Math.max(-radius, lastPos.zCoord));
            	}
            	
                while(j >= 0)
                {
                    while(i <= radius)
                    {
                        while(k <= radius)
                        {
                            Block block = world.getBlock(x + i, j, z + k);
                            int meta = world.getBlockMetadata(x + i, j, z + k);

                            if (isBlockOre(block, meta))
                            {
                                //Allow swapping code. This means the searched block is an ore.
                                BlockTeleposer.swapBlocks(this, world, world, x + i, j, z + k, xRep, yRep, zRep);
                                SoulNetworkHandler.syphonFromNetwork(owner, this.getCostPerRefresh());

                                if (hasPotentia)
                                {
                                    this.canDrainReagent(ritualStone, ReagentRegistry.potentiaReagent, potentiaDrain, true);
                                }

                                if (hasTerrae)
                                {
                                    this.canDrainReagent(ritualStone, ReagentRegistry.terraeReagent, terraeDrain, true);
                                }

                                if (hasOrbisTerrae)
                                {
                                    this.canDrainReagent(ritualStone, ReagentRegistry.orbisTerraeReagent, orbisTerraeDrain, true);
                                }
                                
                                this.setLastPosition(ritualStone.getCustomRitualTag(), new Int3(i, j, k));

                                return;
                            }
                            k++;
                        }
                        k = -radius;
                        i++;
                    }
                    i = -radius;
                    j--;
                    this.setLastPosition(ritualStone.getCustomRitualTag(), new Int3(i, j, k));
                    return;
                }
                
                j = y - 1;
                this.setLastPosition(ritualStone.getCustomRitualTag(), new Int3(i, j, k));
                return;
            }
        }
    }

    @Override
    public int getCostPerRefresh()
    {
        return AlchemicalWizardry.ritualCostMagnetism[1];
    }
    
    public Int3 getLastPosition(NBTTagCompound tag)
    {
    	if(tag != null)
    	{
    		return Int3.readFromNBT(tag);
    	}
    	
    	return new Int3(0, 0, 0);
    }
    
    public void setLastPosition(NBTTagCompound tag, Int3 pos)
    {
    	if(tag != null)
    	{
    		pos.writeToNBT(tag);
    	}
    }

    @Override
    public List<RitualComponent> getRitualComponentList()
    {
        ArrayList<RitualComponent> magneticRitual = new ArrayList();
        magneticRitual.add(new RitualComponent(1, 0, 1, RitualComponent.EARTH));
        magneticRitual.add(new RitualComponent(1, 0, -1, RitualComponent.EARTH));
        magneticRitual.add(new RitualComponent(-1, 0, 1, RitualComponent.EARTH));
        magneticRitual.add(new RitualComponent(-1, 0, -1, RitualComponent.EARTH));
        magneticRitual.add(new RitualComponent(2, 1, 0, RitualComponent.EARTH));
        magneticRitual.add(new RitualComponent(0, 1, 2, RitualComponent.EARTH));
        magneticRitual.add(new RitualComponent(-2, 1, 0, RitualComponent.EARTH));
        magneticRitual.add(new RitualComponent(0, 1, -2, RitualComponent.EARTH));
        magneticRitual.add(new RitualComponent(2, 1, 2, RitualComponent.AIR));
        magneticRitual.add(new RitualComponent(2, 1, -2, RitualComponent.AIR));
        magneticRitual.add(new RitualComponent(-2, 1, 2, RitualComponent.AIR));
        magneticRitual.add(new RitualComponent(-2, 1, -2, RitualComponent.AIR));
        magneticRitual.add(new RitualComponent(2, 2, 0, RitualComponent.FIRE));
        magneticRitual.add(new RitualComponent(0, 2, 2, RitualComponent.FIRE));
        magneticRitual.add(new RitualComponent(-2, 2, 0, RitualComponent.FIRE));
        magneticRitual.add(new RitualComponent(0, 2, -2, RitualComponent.FIRE));
        return magneticRitual;
    }

    public int getRadiusForReagents(boolean hasTerrae, boolean hasOrbisTerrae)
    {
        if (hasTerrae)
        {
            if (hasOrbisTerrae)
            {
                return 31;
            } else
            {
                return 7;
            }
        } else
        {
            if (hasOrbisTerrae)
            {
                return 12;
            } else
            {
                return 3;
            }
        }
    }
}
