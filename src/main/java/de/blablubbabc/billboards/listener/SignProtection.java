package de.blablubbabc.billboards.listener;

import de.blablubbabc.billboards.BillboardsPlugin;
import de.blablubbabc.billboards.entry.BillboardSign;
import de.blablubbabc.billboards.message.Message;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import de.blablubbabc.billboards.util.SoftBlockLocation;

public class SignProtection implements Listener {

	private final BillboardsPlugin plugin;

	public SignProtection(BillboardsPlugin plugin) {
		this.plugin = plugin;
	}

	public void onPluginEnable() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void onPluginDisable() {
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent event) {
		// only allow breaking if it has permission and is sneaking
		Player player = event.getPlayer();
		Block block = event.getBlock();
		SoftBlockLocation blockLocation = new SoftBlockLocation(block);
		BillboardSign billboard = plugin.getBillboard(blockLocation);
		if (billboard == null) return;
		if (!plugin.refreshSign(billboard)) return; // billboard is no longer valid

		boolean breakFailed = false;
		if (!player.isSneaking()) {
			breakFailed = true;
			player.sendMessage(Message.YOU_HAVE_TO_SNEAK.get());
		} else if (!billboard.canBreak(player)) {
			breakFailed = true;
			player.sendMessage(Message.NO_PERMISSION.get());
		}

		if (breakFailed) {
			event.setCancelled(true);
			Location location = billboard.getLocation().getBukkitLocation();
			if (location != null) {
				plugin.getScheduler().runAtLocation(location, (t) -> {
					// refresh sign to display text:
					plugin.refreshSign(billboard);
				});
			}
		} else {
			// remove billboard:
			plugin.removeBillboard(billboard);
			plugin.saveBillboards();
			player.sendMessage(Message.SIGN_REMOVED.get());
		}
	}
}
