package fr.leomelki.loupgarou.listeners;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

public class VoteListener implements Listener {
    private final MainLg plugin;

    public VoteListener(MainLg mainLg) {
        this.plugin = mainLg;
    }

    @EventHandler
    public void onClick(PlayerAnimationEvent e) {
        if (e.getAnimationType() == PlayerAnimationType.ARM_SWING)
            LGPlayer.thePlayer(plugin, e.getPlayer()).chooseAction();
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        e.setCancelled(true);
    }
}
