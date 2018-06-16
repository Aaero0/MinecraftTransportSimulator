package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PlayerCraftingPacket implements IMessage{
	private int playerID;
	private String selectedPart;

	public PlayerCraftingPacket(){}
	
	public PlayerCraftingPacket(EntityPlayer player, String selectedPart){
		this.playerID = player.getEntityId();
		this.selectedPart = selectedPart;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.playerID = buf.readInt();
		this.selectedPart = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.playerID);
		ByteBufUtils.writeUTF8String(buf, this.selectedPart);
	}
	
	protected static EntityPlayer getPlayer(PlayerCraftingPacket message, MessageContext ctx){
		if(message.playerID != -1){
			if(ctx.side.isServer()){
				return (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.playerID);
			}else{
				return (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.playerID);
			}
		}else{
			return null;
		}
	}
	
	public static boolean doesPlayerHaveMaterials(EntityPlayer player, String partToCraft){
		if(!player.capabilities.isCreativeMode){
			for(ItemStack materialStack : PackParserSystem.getMaterials(partToCraft)){
				int requiredMaterialCount = materialStack.stackSize;
				for(ItemStack stack : player.inventory.mainInventory){
					if(ItemStack.areItemsEqual(stack, materialStack)){
						requiredMaterialCount -= stack.stackSize;
					}
				}
				if(requiredMaterialCount > 0){
					return false;
				}
			}
		}
		return true;
	}
	
	protected static void removeMaterials(EntityPlayer player, String partToCraft){
		if(!player.capabilities.isCreativeMode){
			for(ItemStack materialStack : PackParserSystem.getMaterials(partToCraft)){
				player.inventory.clearMatchingItems(materialStack.getItem(), materialStack.getMetadata(), materialStack.stackSize, null);
			}
		}
	}
	
	public static class Handler implements IMessageHandler<PlayerCraftingPacket, IMessage>{
		public IMessage onMessage(final PlayerCraftingPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityPlayer player = getPlayer(message, ctx);
					if(player != null){
						//Start button was clicked.  Remove materials and start process.
						if(doesPlayerHaveMaterials(player, message.selectedPart)){
							removeMaterials(player, message.selectedPart);
							ItemStack partStack = new ItemStack(MTSRegistry.partItemMap.get(message.selectedPart));
							partStack.setTagCompound(new NBTTagCompound());
							player.getEntityWorld().spawnEntityInWorld(new EntityItem(player.getEntityWorld(), player.posX, player.posY, player.posZ, partStack));
						}
					}
				}
			});
			return null;
		}
	}
}
