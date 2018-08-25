package gregtech.common.cable.net;

import gregtech.api.pipenet.Node;
import gregtech.api.pipenet.PipeNet;
import gregtech.api.pipenet.WorldPipeNet;
import gregtech.common.cable.RoutePath;
import gregtech.common.cable.WireProperties;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;
import java.util.Map.Entry;

public class EnergyNet extends PipeNet<WireProperties> {

    protected EnergyNet(WorldPipeNet<WireProperties, EnergyNet> world) {
        super(world);
    }

    public List<RoutePath> computePatches(BlockPos startPos) {
        ArrayList<RoutePath> readyPaths = new ArrayList<>();
        RoutePath currentPath = new RoutePath();
        Node<WireProperties> firstNode = allNodes.get(startPos);
        currentPath.path.put(startPos, firstNode.data);
        readyPaths.add(currentPath.cloneAndCompute(startPos));
        HashSet<BlockPos> observedSet = new HashSet<>();
        observedSet.add(startPos);
        MutableBlockPos currentPos = new MutableBlockPos(startPos);
        Stack<EnumFacing> moveStack = new Stack<>();
        main: while(true) {
            for(EnumFacing facing : EnumFacing.VALUES) {
                currentPos.move(facing);
                Node<WireProperties> secondNode = allNodes.get(currentPos);
                if(secondNode != null && canNodesConnect(firstNode, facing, secondNode) && !observedSet.contains(currentPos)) {
                    BlockPos immutablePos = currentPos.toImmutable();
                    observedSet.add(immutablePos);
                    firstNode = secondNode;
                    moveStack.push(facing.getOpposite());
                    currentPath.path.put(immutablePos, allNodes.get(immutablePos).data);
                    if(secondNode.isActive) {
                        //if we are on active node, this is end of our path
                        RoutePath finalizedPath = currentPath.cloneAndCompute(immutablePos);
                        readyPaths.add(finalizedPath);
                    }
                    continue main;
                } else {
                    currentPos.move(facing.getOpposite());
                }
            }
            if(!moveStack.isEmpty()) {
                currentPos.move(moveStack.pop());
                //also remove already visited block from path
                currentPath.path.remove(currentPos);
            } else break;
        }
        return readyPaths;
    }


    @Override
    protected void writeNodeData(WireProperties nodeData, NBTTagCompound tagCompound) {
        tagCompound.setInteger("voltage", nodeData.voltage);
        tagCompound.setInteger("amperage", nodeData.amperage);
        tagCompound.setInteger("loss", nodeData.lossPerBlock);
    }

    @Override
    protected WireProperties readNodeData(NBTTagCompound tagCompound) {
        int voltage = tagCompound.getInteger("voltage");
        int amperage = tagCompound.getInteger("amperage");
        int lossPerBlock = tagCompound.getInteger("loss");
        return new WireProperties(voltage, amperage, lossPerBlock);
    }
}
