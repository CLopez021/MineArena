package com.clopez021.mine_arena.packets;

import com.clopez021.mine_arena.client.speech_recognition.VoiceSidecar;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client: Start sidecar or update its config (lang/spells).
 */
public class VoiceSidecarConfigPacket {
	private final String lang;
	private final List<String> spells;

	public VoiceSidecarConfigPacket(String lang, List<String> spells) {
		this.lang = lang;
		this.spells = spells != null ? spells : List.of();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(lang != null ? lang : "en-US");
		buf.writeVarInt(spells.size());
		for (String s : spells) buf.writeUtf(s);
	}

	public static VoiceSidecarConfigPacket decode(FriendlyByteBuf buf) {
		String lang = buf.readUtf();
		int n = buf.readVarInt();
		List<String> spells = new ArrayList<>(n);
		for (int i = 0; i < n; i++) spells.add(buf.readUtf());
		return new VoiceSidecarConfigPacket(lang, spells);
	}

	public void handle(CustomPayloadEvent.Context ctx) {
		ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			var player = Minecraft.getInstance().player;
			if (player == null) return;
			try {
				var sidecar = VoiceSidecar.getInstance();
				if (!sidecar.isRunning()) {
					sidecar.start(spells, lang);
				} else {
					sidecar.sendConfig(lang, spells);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
		ctx.setPacketHandled(true);
	}
}