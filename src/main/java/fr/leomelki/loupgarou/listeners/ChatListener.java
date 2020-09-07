package fr.leomelki.loupgarou.listeners;

import fr.leomelki.loupgarou.MainLg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import fr.leomelki.loupgarou.classes.LGPlayer;

public class ChatListener implements Listener{

	private final MainLg plugin;

	public ChatListener(MainLg mainLg) {
		this.plugin = mainLg;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(AsyncPlayerChatEvent e) {
		if(!e.isCancelled()) {
			LGPlayer player = LGPlayer.thePlayer(plugin, e.getPlayer());
			player.onChat(e.getMessage());
			e.setCancelled(true);
		}
	}
}
