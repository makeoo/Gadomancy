package makeo.gadomancy.common.blocks.tiles;

import makeo.gadomancy.common.utils.NBTHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ITickable;
import thaumcraft.common.tiles.essentia.TileJarFillable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class is part of the Gadomancy Mod
 * Gadomancy is Open Source and distributed under the
 * GNU LESSER GENERAL PUBLIC LICENSE
 * for more read the LICENSE file
 *
 * Created by makeo @ 14.10.2015 15:06
 */
public class TileRemoteJar extends TileJarFillable implements ITickable {

    public UUID networkId = null;

    private int count = 0;

    @Override
    public void update() {
        super.update();
        if (count % 3 == 0 && !getWorld().isRemote && networkId != null && amount < maxAmount) {
            count = 0;

            JarNetwork network = getNetwork(networkId);

            if(!network.jars.contains(this)) {
                network.jars.add((TileJarFillable) worldObj.getTileEntity(getPos()));
            }

            network.update();
        }
        count++;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);

        networkId = NBTHelper.getUUID(compound, "networkId");
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);

        if(networkId != null) {
            NBTHelper.setUUID(compound, "networkId", networkId);
        }
    }

    private static Map<UUID, JarNetwork> networks = new HashMap<UUID, JarNetwork>();

    private static class JarNetwork {
        private long lastTime = 0;
        private List<TileJarFillable> jars = new ArrayList<TileJarFillable>();

        private void update() {
            long time = MinecraftServer.getServer().getEntityWorld().getTotalWorldTime();
            if(time > lastTime) {
                if(jars.size() > 1) {
                    Collections.sort(jars, new Comparator<TileJarFillable>() {
                        @Override
                        public int compare(TileJarFillable o1, TileJarFillable o2) {
                            return o2.amount - o1.amount;
                        }
                    });

                    TileJarFillable jar1 = jars.get(0);
                    if(!isValid(jar1)) {
                        jars.remove(0);
                        return;
                    }

                    TileJarFillable jar2 = jars.get(jars.size() - 1);
                    if(!isValid(jar2)) {
                        jars.remove(jars.size() - 1);
                        return;
                    }

                    if((jar2.amount+1) < jar1.amount && jar2.addToContainer(jar1.aspect, 1) == 0) {
                        jar1.takeFromContainer(jar1.aspect, 1);
                    }
                }
                lastTime = time + 3;
            }
        }

        private static boolean isValid(TileJarFillable jar) {
            return jar != null && jar.getWorld() != null && !jar.isInvalid()
                    && jar.getWorld().isBlockLoaded(jar.getPos());
        }
    }

    private static JarNetwork getNetwork(UUID id) {
        JarNetwork network = networks.get(id);

        if(network == null) {
            network = new JarNetwork();
            networks.put(id, network);
        }
        return network;
    }

    public void markForUpdate() {
        markDirty();
        getWorld().markBlockForUpdate(getPos());
    }
}
