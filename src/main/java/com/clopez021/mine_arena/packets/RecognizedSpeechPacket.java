package com.clopez021.mine_arena.packets;

import com.clopez021.mine_arena.player.PlayerManager;
import com.clopez021.mine_arena.speech_recognition.SpeechCommand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> Server: Carry recognized speech result.
 */
public class RecognizedSpeechPacket {
	private final String spell;

	public RecognizedSpeechPacket(String spell) {
		this.spell = spell;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(spell != null ? spell : "");
	}

	public static RecognizedSpeechPacket decode(FriendlyByteBuf buf) {
		String spell = buf.readUtf();
		return new RecognizedSpeechPacket(spell);
	}

	public void handle(CustomPayloadEvent.Context ctx) {
		ServerPlayer player = ctx.getSender();
		if (player == null) return;
		ctx.enqueueWork(() -> {
			// Create simplified SpeechCommand and delegate to PlayerManager
			SpeechCommand command = new SpeechCommand(player.getUUID(), spell);
			PlayerManager.getInstance().handleSpeechCommand(player, command);
		});
		ctx.setPacketHandled(true);
	}
} 