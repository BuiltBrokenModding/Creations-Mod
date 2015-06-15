package com.builtbroken.creation;

import com.builtbroken.creation.content.ItemGlove;
import com.builtbroken.creation.content.forge.TileFireChannel;
import com.builtbroken.creation.content.tests.TileOrbit;
import com.builtbroken.creation.content.tests.TileSphere;
import com.builtbroken.creation.content.tests.TileSphereMorph;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.lib.mod.AbstractMod;
import com.builtbroken.mc.lib.mod.AbstractProxy;
import com.builtbroken.mc.lib.mod.ModCreativeTab;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Created by robert on 10/1/2014.
 */

@Mod(name = "Creations: Builder's Toolkit", modid = Creation.DOMAIN, version = "@VERSION@", dependencies = "required-after:VoltzEngine")
public class Creation extends AbstractMod
{
    public static final String DOMAIN = "creationsbt";
    public static final String PREFIX = DOMAIN + ":";

    @SidedProxy(clientSide = "com.builtbroken.creation.client.ClientProxy", serverSide = "com.builtbroken.creation.CommonProxy")
    public static CommonProxy proxy;

    @Instance(DOMAIN)
    public static Creation INSTANCE;
    public static ModCreativeTab creativeTab;

    //Settings
    /** If true it will force animation updates to be server side only */
    public static boolean CONTROL_SPHERE_ANIMATION_SERVER_SIDE = false;
    /** Number of buckets each meter of the sphere can contain, controlls volume of the sphere */
    public static int FORGE_BUCKETS_PER_METER = 16;
    /** Conversion ratio of ingot to fluid volume, based on Tinkers *in theory* */
    public static int INGOT_VOLUME = 144;


    //Content
    public static Item glove;

    public static  Block blockFireChannel;

    public static Block blockSphere;
    public static Block blockSphereMorph;
    public static Block blockOrbitTest;

    public Creation()
    {
        super(DOMAIN, "Creations");
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        super.preInit(e);
        // TODO re-enabled when we have more items
        creativeTab = new ModCreativeTab(DOMAIN);
        getManager().setTab(creativeTab);

        glove = getManager().newItem(ItemGlove.class);
        creativeTab.itemStack = new ItemStack(glove);
        blockFireChannel = getManager().newBlock(TileFireChannel.class);
        if(Engine.runningAsDev)
        {
            blockSphere = getManager().newBlock(TileSphere.class);
            //blockSphereMorph = getManager().newBlock(TileSphereMorph.class);
            //blockOrbitTest = getManager().newBlock(TileOrbit.class);
        }

    }

    @EventHandler
    public void init(FMLInitializationEvent e)
    {
        super.init(e);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e)
    {
        super.postInit(e);
    }

    @Override
    public AbstractProxy getProxy()
    {
        return proxy;
    }
}