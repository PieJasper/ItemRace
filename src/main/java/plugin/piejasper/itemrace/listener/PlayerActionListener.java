package plugin.piejasper.itemrace.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import plugin.piejasper.itemrace.GameManager;

public class PlayerActionListener implements Listener {

    private final GameManager manager;

    public PlayerActionListener(GameManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        manager.checkItemForWin(player, event.getItem().getItemStack());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        manager.removePlayer(event.getPlayer());
    }
}
