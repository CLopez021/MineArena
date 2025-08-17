package com.clopez021.mine_arena.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> Server: Carry recognized speech result.
 */
public class RecognizedSpeechPacket {
	private final String spell;
	private final String heard;
	private final String matchKind;
	private final double confidence;
	private final long timestamp;

	public RecognizedSpeechPacket(String spell, String heard, String matchKind, double confidence, long timestamp) {
		this.spell = spell;
		this.heard = heard;
		this.matchKind = matchKind;
		this.confidence = confidence;
		this.timestamp = timestamp;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(spell != null ? spell : "");
		buf.writeUtf(heard != null ? heard : "");
		buf.writeUtf(matchKind != null ? matchKind : "unknown");
		buf.writeDouble(confidence);
		buf.writeLong(timestamp);
	}

	public static RecognizedSpeechPacket decode(FriendlyByteBuf buf) {
		String spell = buf.readUtf();
		String heard = buf.readUtf();
		String matchKind = buf.readUtf();
		double confidence = buf.readDouble();
		long ts = buf.readLong();
		return new RecognizedSpeechPacket(spell, heard, matchKind, confidence, ts);
	}

	public void handle(CustomPayloadEvent.Context ctx) {
		ServerPlayer player = ctx.getSender();
		if (player == null) return;
		ctx.enqueueWork(() -> {
			String chatMessage = String.format("🎤 Voice: \"%s\" -> %s (%s)", heard, spell, matchKind);
			player.sendSystemMessage(Component.literal(chatMessage));
		});
		ctx.setPacketHandled(true);
	}
} 