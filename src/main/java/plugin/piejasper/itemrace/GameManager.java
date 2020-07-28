package plugin.piejasper.itemrace;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GameManager {
    private final MaterialManager materialManager;

    private final List<Player> joinedPlayers = new ArrayList<>();
    private final List<Player> activePlayers = new ArrayList<>();
    private final List<Player> completedPlayers = new ArrayList<>();

    private final List<Player> configuringPlayers = new ArrayList<>();
    private final HashMap<Player, Material> materialsForPlayers = new HashMap<>();

    private final HashMap<Player, Material> playerItems = new HashMap<>();

    // racing mode
    private final HashMap<Player, Integer> scores = new HashMap<>();

    // time mode
    private final HashMap<Player, Integer> lives = new HashMap<>();

    private boolean started = false;

    private long time = -1;
    private int round = 0;

    private final GameSettings settings;

    public GameManager(Plugin plugin, GameSettings settings) {
        materialManager = new MaterialManager();
        materialManager.generateUsableMaterials();
        materialManager.setupConfig(plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (started) {
                    time++;
                    if (!settings.isRacing()) {
                        long timeLeft = settings.getStartingTime() - time;
                        if (timeLeft == settings.getStartingTime() / 2 || timeLeft == 60 || timeLeft == 30 || (timeLeft <= 10 && timeLeft > 0)) {
                            for (Player joinedPlayer : joinedPlayers) {
                                joinedPlayer.sendMessage("" + ChatColor.BLUE + getTimeFormatted(timeLeft) + " left!");
                            }
                        }
                        if (timeLeft <= 0) {
                            endRound();
                        }
                    }
                    for (Player joinedPlayer : joinedPlayers) {
                        checkPlayerInventoryForItem(joinedPlayer);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20);

        this.settings = settings;
    }

    public void startConfiguring(Player player) {
        if (configuringPlayers.contains(player)) {
            player.sendMessage(ChatColor.GREEN + "You are no longer configuring.");
            configuringPlayers.remove(player);
        } else {
            player.sendMessage(ChatColor.GREEN + "You are now configuring.");
            configuringPlayers.add(player);
            Material material = materialManager.getUncategorizedItem();
            materialsForPlayers.put(player, material);
            player.sendMessage(ChatColor.GOLD + "New item: " + cleanName(material));
        }
    }

    public List<String> getCategoryNames() {
        return materialManager.getCategoryNames();
    }

    public ConfigurationSection getCategory(String category) {
        return materialManager.getCategory(category);
    }

    public void addOrChangeCategory(String category, int difficulty, Player player) {
        if (configuringPlayers.contains(player)) {
            materialManager.addOrChangeCategory(category, difficulty, materialsForPlayers.get(player));
            Material material = materialManager.getUncategorizedItem();
            materialsForPlayers.put(player, material);
            player.sendMessage(ChatColor.GOLD + "New item: " + cleanName(material));
        } else {
            materialManager.addOrChangeCategory(category, difficulty, null);
        }
    }

    public void addPlayer(Player player) {
        player.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + "You joined the race!");

        for (Player joinedPlayer : joinedPlayers) {
            // tell all players already in the game about the new player
            joinedPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " has joined the race!");
        }

        // tell joining player all players in the game, as long as there is somebody else in the game
        if (!joinedPlayers.isEmpty()) {
            listPlayers(player);
        } else {
            player.sendMessage(ChatColor.GRAY + "You are currently the only player in the race.");
        }

        joinedPlayers.add(player);
        if (started) {
            activePlayers.add(player);
        }
    }

    public void listPlayers(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "-- Current players in the race: --");

        StringBuilder currentPlayers = new StringBuilder();
        for (int i = 0; i < joinedPlayers.size(); i++) {
            currentPlayers.append(joinedPlayers.get(i).getName());
            if (!(i == joinedPlayers.size() - 1)) {
                currentPlayers.append(", ");
            }
        }

        sender.sendMessage(currentPlayers.toString());

        if (joinedPlayers.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "There is currently nobody in the race.");
        }

        sender.sendMessage(ChatColor.AQUA + "-- End List --");
    }

    public void skip(Player player) {
        Material chosenMaterial;
        if (!settings.isShared()) {
            chosenMaterial = materialManager.getRandomMaterialExcludingPlayerItems(player, round);
        } else {
            chosenMaterial = materialManager.getRandomMaterialExcludingPlayerItems(joinedPlayers, round);
        }

        String chosenItemName = cleanName(chosenMaterial);

        for (Player joinedPlayer : joinedPlayers) {
            // readable item name
            String itemName = cleanName(playerItems.get(player));
            if (!settings.isShared()) {
                joinedPlayer.sendMessage(ChatColor.GREEN + player.getName() + " has skipped their item (" + itemName + ").");
            } else {
                joinedPlayer.sendMessage(ChatColor.GREEN + player.getName() + " has skipped the item (" + itemName + ").");
                playerItems.put(joinedPlayer, chosenMaterial);
                joinedPlayer.sendMessage("" + ChatColor.BOLD + ChatColor.GOLD + "The item: " + ChatColor.RESET + chosenItemName);
            }
        }

        playerItems.put(player, chosenMaterial);
        if (!settings.isShared()) {
            player.sendMessage("" + ChatColor.BOLD + ChatColor.GOLD + "Your item: " + ChatColor.RESET + chosenItemName);
        }
    }

    public void removePlayer(Player player) {

        joinedPlayers.remove(player);
        activePlayers.remove(player);

        player.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + "You left the race!");
        for (Player joinedPlayer : joinedPlayers) {

            // tell all players already in the game about the leaving player
            joinedPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " has left the race!");
        }
    }

    public boolean isPlayerJoined(Player player) {
        return joinedPlayers.contains(player);
    }

    public void start() {
        round = 0;
        started = true;
        activePlayers.clear();
        activePlayers.addAll(joinedPlayers);

        if (settings.isRacing()) {
            scores.clear();
            for (Player joinedPlayer : joinedPlayers) {
                scores.put(joinedPlayer, 0);
            }
        } else {
            lives.clear();
            for (Player joinedPlayer : joinedPlayers) {
                lives.put(joinedPlayer, settings.getMaxLives());
            }
        }

        startRound();
    }

    private void generateNewPlayerItems() {
        playerItems.clear();

        // material common between players excluding all items in their inventories, for when separate is false
        Material commonMaterial = materialManager.getRandomMaterialExcludingPlayerItems(joinedPlayers, round);
        for (Player joinedPlayer : joinedPlayers) {
            Material chosenMaterial;

            // set player's item
            if (!settings.isShared()) {
                // random item excluding all items in the player's inventory
                chosenMaterial = materialManager.getRandomMaterialExcludingPlayerItems(joinedPlayer, round);
            } else {
                chosenMaterial = commonMaterial;
            }
            playerItems.put(joinedPlayer, chosenMaterial);

            String itemName = cleanName(chosenMaterial);
            joinedPlayer.sendMessage("" + ChatColor.BOLD + ChatColor.GOLD + (!settings.isShared() ? "Your" : "The") + " item: " + ChatColor.RESET + itemName);
        }
    }

    public void stop() {
        started = false;
        activePlayers.clear();

        time = -1;

        for (Player joinedPlayer : joinedPlayers) {
            joinedPlayer.sendMessage(ChatColor.AQUA + "-- The game is over! --");
        }
    }

    public void changeSetting(String name, String setting, String value) {
        String text = name + " changed \"" + setting + "\" to \"" + value + "\".";

        for (Player joinedPlayer : joinedPlayers) {
            joinedPlayer.sendMessage(ChatColor.GRAY + text);
        }
    }

    public boolean isStarted() {
        return started;
    }

    private String cleanName(Material material) {
        return material.toString().toLowerCase().replace("_", " ");
    }

    private void startRound() {
        round++;
        completedPlayers.clear();
        time = 0;

        for (Player joinedPlayer : joinedPlayers) {
            joinedPlayer.sendMessage(ChatColor.AQUA + "-- A new round begins! --");
        }

        generateNewPlayerItems();

        if (!settings.isRacing()) {
            // send time message after normal message
            for (Player joinedPlayer : joinedPlayers) {
                joinedPlayer.sendMessage("" + ChatColor.BLUE + getTimeFormatted(settings.getStartingTime() - time) + " left!");
            }
        }
    }

    private void endRound() {

        List<Player> eliminatedPlayers = new ArrayList<>();
        HashMap<Player, Integer> differences = new HashMap<>();

        boolean gameOver = false;
        Player winningPlayer = null;

        for (Player joinedPlayer : joinedPlayers) {
            differences.put(joinedPlayer, 0);
        }

        if (settings.isRacing()) {
            for (Player completedPlayer : completedPlayers) {
                scores.put(completedPlayer, scores.get(completedPlayer) + 1);
                differences.put(completedPlayer, 1);
            }
        } else {
            // remove lives from players who haven't completed their goal
            for (Player activePlayer : activePlayers) {
                if (!completedPlayers.contains(activePlayer)) {
                    lives.put(activePlayer, lives.get(activePlayer) - 1);
                    differences.put(activePlayer, -1);
                }

                if (lives.get(activePlayer) <= 0) {
                    eliminatedPlayers.add(activePlayer);
                }
            }
            for (Player eliminatedPlayer : eliminatedPlayers) {
                activePlayers.remove(eliminatedPlayer);
            }
        }

        StringBuilder leaderboardBuilder = new StringBuilder(ChatColor.AQUA + "-- Here's what's happened this round: --\n");

        for (Player activePlayer : activePlayers) {

            if (settings.isRacing() || lives.get(activePlayer) > 0) {
                int numerator;
                int denominator;

                if (settings.isRacing()) {
                    numerator = scores.get(activePlayer);
                    denominator = settings.getWinningPoints();
                } else {
                    numerator = lives.get(activePlayer);
                    denominator = settings.getMaxLives();
                }

                ChatColor differenceColor;
                String sign;

                if (differences.get(activePlayer) > 0) {
                    // positive
                    differenceColor = ChatColor.GREEN;
                    sign = "+";
                } else if (differences.get(activePlayer) < 0) {
                    // negative
                    differenceColor = ChatColor.RED;
                    sign = "";
                } else {
                    // 0
                    differenceColor = ChatColor.YELLOW;
                    sign = "+";
                }

                ChatColor playerNameColor = ChatColor.DARK_GREEN;

                if (settings.isRacing() && scores.get(activePlayer) >= settings.getWinningPoints()) {
                    playerNameColor = ChatColor.GOLD;
                    winningPlayer = activePlayer;
                    gameOver = true;
                }

                leaderboardBuilder
                        .append(playerNameColor)
                        .append(activePlayer.getName())
                        .append(ChatColor.WHITE)
                        .append(": ")
                        .append(numerator)
                        .append("/")
                        .append(denominator)
                        .append(" (")
                        .append(differenceColor)
                        .append(sign)
                        .append(differences.get(activePlayer))
                        .append(ChatColor.WHITE)
                        .append(")\n");
            }
        }

        for (Player eliminatedPlayer : eliminatedPlayers) {
            leaderboardBuilder
                    .append(ChatColor.RED)
                    .append(eliminatedPlayer.getName())
                    .append(" was eliminated from the game!\n");
        }

        if (gameOver && settings.isRacing()) {
            leaderboardBuilder
                    .append(ChatColor.GOLD)
                    .append(ChatColor.BOLD)
                    .append(winningPlayer.getName())
                    .append(" is the first to ")
                    .append(settings.getWinningPoints())
                    .append(" points!\n");
        }

        // end game if there is one player left in time mode and game isn't singleplayer
        if (!settings.isRacing() && !(joinedPlayers.size() == 1) && activePlayers.size() == 1) {
            gameOver = true;

            leaderboardBuilder
                    .append(ChatColor.GOLD)
                    .append(ChatColor.BOLD)
                    .append(activePlayers.get(0).getName())
                    .append(" is the last player standing!\n");
        }

        String leaderboard = leaderboardBuilder.toString();

        for (Player joinedPlayer : joinedPlayers) {
            joinedPlayer.sendMessage(leaderboard);
        }

        if (gameOver || activePlayers.isEmpty()) {
            stop();
        } else {
            startRound();
        }
    }

    private String getTimeFormatted(long time) {
        long hours = time / 3600;
        long minutes = (time - hours * 3600) / 60;
        long seconds = time - minutes * 60 - hours * 3600;

        String formattedTime = "";
        if (hours > 0) {
            formattedTime = formattedTime + hours + "h";
        }
        if (minutes > 0) {
            formattedTime = formattedTime + minutes + "m";
        }
        if (seconds > 0) {
            formattedTime = formattedTime + seconds + "s";
        }

        return formattedTime;
    }

    private void finishGoal(Player player) {
        completedPlayers.add(player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // tell all players that the player completed their goal
        for (Player joinedPlayer : joinedPlayers) {
            String collectedItemMessage = ChatColor.DARK_GREEN + player.getName();
            collectedItemMessage += ChatColor.GREEN + " has collected their item (";
            collectedItemMessage += ChatColor.DARK_GREEN + cleanName(playerItems.get(player));
            collectedItemMessage += ChatColor.GREEN + ") in " + ChatColor.DARK_GREEN + getTimeFormatted(time) + ChatColor.GREEN + ".";
            joinedPlayer.sendMessage(collectedItemMessage);

            if (settings.isRacing()) {
                joinedPlayer.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + player.getName() + " wins this round!");
            }
        }

        // if racing, make that player win
        if (settings.isRacing()) {
            endRound();
        }
        // otherwise, if all players have won, end the round
        else if (completedPlayers.containsAll(joinedPlayers)) {
            endRound();
        }
    }

    public void checkPlayerInventoryForItem(Player player) {
        // only continue if player is active and game is started and player hasn't won
        if (!started || !activePlayers.contains(player) || completedPlayers.contains(player)) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] items = inventory.getStorageContents();
        for (ItemStack item : items) {
            checkItemForWin(player, item);
        }
    }

    public void checkItemForWin(Player player, ItemStack item) {
        Material targetItem = playerItems.get(player);

        if (item == null) {
            return;
        }
        if (targetItem.equals(item.getType())) {
            finishGoal(player);
        }
    }
}
