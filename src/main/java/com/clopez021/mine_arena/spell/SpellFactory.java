package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.packets.SpellCompletePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class SpellFactory {

    public static void createSpell(ServerPlayer player, String spellDescription, String castPhrase) {
        // Simulate async work and then notify client
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(10_000L);
            } catch (InterruptedException ignored) {}
            // Back to server thread to send packet safely
            if (player.server != null) {
                player.server.execute(() -> {
                    if (player.connection != null) {
                        PacketHandler.INSTANCE.send(new SpellCompletePacket(), PacketDistributor.PLAYER.with(player));
                    }
                });
            }
        }, "SpellFactoryDelay");
        t.setDaemon(true);
        t.start();
    }
}
