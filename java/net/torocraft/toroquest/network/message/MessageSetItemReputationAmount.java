package net.torocraft.toroquest.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.torocraft.toroquest.gui.VillageLordGuiContainer;

public class MessageSetItemReputationAmount implements IMessage {

	private int reputation;
	
	public MessageSetItemReputationAmount() {

	}

	public MessageSetItemReputationAmount(int reputation) {
		this.reputation = reputation;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		reputation = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(reputation);
	}
	
	public static class Worker {
		public void work(MessageSetItemReputationAmount message) {
			Minecraft minecraft = Minecraft.getMinecraft();
			final EntityPlayer player = minecraft.player;

			if (player == null) {
				return;
			}
			
			VillageLordGuiContainer.setAvailableReputation(message.reputation);
		}
	}

	public static class Handler implements IMessageHandler<MessageSetItemReputationAmount, IMessage> {

		@Override
		public IMessage onMessage(final MessageSetItemReputationAmount message, MessageContext ctx) {
			if (ctx.side != Side.CLIENT) {
				return null;
			}

			Minecraft.getMinecraft().addScheduledTask(new Runnable() {
				@Override
				public void run() {
					new Worker().work(message);
				}
			});

			return null;
		}	
	}
}