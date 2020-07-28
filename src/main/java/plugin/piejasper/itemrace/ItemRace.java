package plugin.piejasper.itemrace;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.piejasper.itemrace.command.ItemRaceCommand;
import plugin.piejasper.itemrace.listener.PlayerActionListener;

@SuppressWarnings("ALL")
public class ItemRace extends JavaPlugin {

    private GameSettings settings;
    private GameManager gameManager;
    private ItemRaceCommand itemRaceCommand;

    @Override
    public void onEnable() {
        settings = new GameSettings();
        gameManager = new GameManager(this, settings);
        itemRaceCommand = new ItemRaceCommand(gameManager, settings);

        getCommand("itemrace").setExecutor(itemRaceCommand);
        getCommand("itemrace").setTabCompleter(itemRaceCommand);
        Bukkit.getPluginManager().registerEvents(new PlayerActionListener(gameManager), this);

        super.onEnable();
    }
}
