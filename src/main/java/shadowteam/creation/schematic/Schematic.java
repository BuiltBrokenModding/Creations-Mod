package shadowteam.creation.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;
import shadowteam.creation.vec.Cube;
import shadowteam.creation.vec.Vec;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;

/** Set of blocks that make up the instructions for building something
 * 
 * @author Darkguardsman */
public class Schematic
{
    @Getter
    private String name = "Schematic";

    @Getter
    private BiMap<Vec, BlockMeta> blocks;

    @Getter
    private Vec size;

    @Getter
    private Vec center;

    public Schematic()
    {

    }

    public Schematic(File file)
    {
        load(file);
    }

    /** Loads the selection from the world
     * 
     * @param world - world to load from
     * @param cube - area to load from */
    public Schematic load(World world, Cube cube)
    {
        blocks = HashBiMap.create();
        size = cube.getSize();
        center = new Vec(cube.getXLength() / 2, 0, cube.getYLength() / 2);

        for (int y = cube.getLowPoint().yi(); y < cube.getYLength(); y++)
        {
            for (int x = cube.getLowPoint().xi(); x < cube.getXLength(); x++)
            {
                for (int z = cube.getLowPoint().zi(); z < cube.getZLength(); z++)
                {
                    Vec vec = new Vec(x, y, z).sub(cube.getLowPoint());
                    Block block = vec.getBlock(world);
                    if (block != null && !block.isAirBlock(world, x, y, z) && !(block instanceof IFluidBlock))
                    {
                        BlockMeta blockMeta = new BlockMeta(block, vec.getBlockMeta(world));
                        blocks.put(vec, blockMeta);
                    }
                }
            }
        }
        return this;
    }

    /** Loads a schematic from a NBTTagCompound, auto converts block ids and catchs missing blocks
     * 
     * @param nbt - NBTTagCompound to load from, must contain the correct data
     * @return list of missing blocks if they are not present in this instance of the game
     */
    public List<MissingBlock> load(NBTTagCompound nbt)
    {
        HashMap<Integer, MissingBlock> missingBlocks = new HashMap();
        byte[] loadedIDs = nbt.getByteArray("Blocks");
        byte[] metaLoaded = nbt.getByteArray("Data");

        HashMap<Integer, Integer> idToNewId = new HashMap<Integer, Integer>();
        NBTTagCompound idTag = nbt.getCompoundTag("idMap");
        int mapSize = idTag.getInteger("size");
        for (int i = 0; i < mapSize; i++)
        {
            String save = idTag.getString("s" + i);
            String[] split = save.split(":");
            String modName = split[0];
            String blockName = split[1];
            int blockId = Integer.getInteger(split[2]);
            Block block = GameRegistry.findBlock(modName, blockName);
            if (block != null)
            {
                idToNewId.put(blockId, block.blockID);
            }
            else
            {
                for(ModContainer mod : Loader.instance().getActiveModList())
                {
                    block = GameRegistry.findBlock(mod.getModId(), blockName);
                    if (block != null)
                    {
                        idToNewId.put(blockId, block.blockID);
                        break;
                    }
                }
                if(block == null)
                {
                    missingBlocks.put(blockId, new MissingBlock(modName, blockName));
                }
            }
        }

        //Load ids & meta
        int index = 0;
        for (int y = 0; y < size.yi(); y++)
        {
            for (int z = 0; z < size.zi(); z++)
            {
                for (int x = 0; x < size.xi(); x++)
                {
                    Vec vec = new Vec(x, y, z);
                    int id = loadedIDs[index];
                    int meta = metaLoaded[index];
                    
                    if(idToNewId.containsKey(id))
                    {
                        blocks.put(vec, new BlockMeta(Block.blocksList[idToNewId.get(id)], meta));
                    }
                    else if(missingBlocks.containsKey(id))
                    {
                        missingBlocks.get(id).add(vec);
                    }
                    index++;

                }
            }
        }
        return new ArrayList<MissingBlock>(missingBlocks.values());
    }

    public void save(NBTTagCompound nbt)
    {
        //Save size
        nbt.setShort("sizeX", (short) size.xi());
        nbt.setShort("sizeY", (short) size.yi());
        nbt.setShort("sizeZ", (short) size.zi());

        //Save center
        nbt.setShort("centerX", (short) center.xi());
        nbt.setShort("centerY", (short) center.yi());
        nbt.setShort("centerZ", (short) center.zi());

        //Save ids and meta
        byte[] setIDs = new byte[size.xi() * size.yi() * size.zi()];
        byte[] setMetas = new byte[size.xi() * size.yi() * size.zi()];
        int index = 0;

        List<Block> blockList = new LinkedList<Block>();

        for (int y = 0; y < size.yi(); y++)
        {
            for (int z = 0; z < size.zi(); z++)
            {
                for (int x = 0; x < size.xi(); x++)
                {
                    Vec vec = new Vec(x, y, z);
                    BlockMeta block = blocks.get(vec);
                    if (block != null)
                    {
                        if (!blockList.contains(block.getBlock()))
                        {
                            blockList.add(block.getBlock());
                        }
                        setIDs[index] = (byte) (block.getBlock().blockID & 0xff);
                        setMetas[index] = (byte) (block.getMeta() & 0xff);
                    }
                    index++;
                }
            }
        }
        nbt.setByteArray("Blocks", setIDs);
        nbt.setByteArray("Data", setMetas);

        //Save ids to names for translating during load time
        NBTTagCompound idTag = new NBTTagCompound();
        idTag.setShort("size", (short) blockList.size());
        int o = 0;
        for (Block block : blockList)
        {
            UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(block);
            idTag.setString("s" + o, id.modId + ":" + id.name + ":" + block.blockID);
            o++;
        }
        nbt.setTag("idMap", idTag);

    }

    public void load(File file)
    {
        if (file.exists())
        {
            try
            {
                load(CompressedStreamTools.readCompressed(new FileInputStream(file)));
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    public void save(File file)
    {
        File tempFile = new File(file.getParent(), file.getName() + "_tmp.dat");
        
        NBTTagCompound tag = new NBTTagCompound();
        save(tag);
        
        try
        {
            CompressedStreamTools.writeCompressed(tag, new FileOutputStream(tempFile));       
    
            if (file.exists())
            {
                file.delete();
            }
    
            tempFile.renameTo(file);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}