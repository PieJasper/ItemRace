package plugin.piejasper.itemrace.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import plugin.piejasper.itemrace.GameManager;
import plugin.piejasper.itemrace.GameSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemRaceCommand implements CommandExecutor, TabCompleter {

    private final GameManager gameManager;
    private final GameSettings settings;

    public ItemRaceCommand(GameManager gameManager, GameSettings settings) {
        this.gameManager = gameManager;
        this.settings = settings;
    }

    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        String subcommand;

        if (strings.length < 1) {
            subcommand = "help";
        } else {
            subcommand = strings[0];
        }

        switch (subcommand) {
            case "help":
                help(strings, commandSender);
                break;
            case "start":
                start(commandSender);
                break;
            case "join":
                join(commandSender);
                break;
            case "leave":
                leave(commandSender);
                break;
            case "stop":
                stop(commandSender);
                break;
            case "list":
                list(commandSender);
                break;
            case "skip":
                skip(commandSender);
                break;
            case "set":
                set(strings, commandSender);
                break;
            case "conf":
                conf(strings, commandSender);
                break;
            default:
                return false;
        }
        return true;
    }

    private void help(String[] arguments, CommandSender sender) {
        String command = "";
        if (arguments.length > 1) {
            command = arguments[1].toLowerCase();
        }

        String[] commands = {"help", "start", "join", "leave", "stop", "list", "skip", "set"};
        String[] helpResponses = {
                "/itemrace help [command]:" + ChatColor.WHITE + " this list",
                "/itemrace start:" + ChatColor.WHITE + " starts the race.",
                "/itemrace join:" + ChatColor.WHITE + " joins the race.",
                "/itemrace leave:" + ChatColor.WHITE + " leaves the race.",
                "/itemrace stop:" + ChatColor.WHITE + " stops the race.",
                "/itemrace list:" + ChatColor.WHITE + " lists players in the game.",
                "/itemrace skip:" + ChatColor.WHITE + " skips the current block.",
                "/itemrace set <setting> <value>:" + ChatColor.WHITE + " set a game setting."
        };

        boolean isValidCommand = false;
        for (int i = 0; i < commands.length; i++) {
            if (commands[i].equals(command)) {
                isValidCommand = true;
                sender.sendMessage(ChatColor.GOLD + helpResponses[i]);
            }
        }

        // if command isn't in the list of commands, show all commands
        if (!isValidCommand) {
            sender.sendMessage(ChatColor.WHITE + "--- " + ChatColor.GOLD + "ItemRace subcommands:" + ChatColor.WHITE + " ---");
            for (String response : helpResponses) {
                sender.sendMessage(ChatColor.GOLD + response);
            }
        }
    }

    private void start(CommandSender sender) {
        // don't start game if game is already started
        if (gameManager.isStarted()) {
            sender.sendMessage(ChatColor.RED + "Game is already started!");
            return;
        }

        gameManager.start();
    }

    private void join(CommandSender sender) {
        // must be a player to join
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to join the race.");
            return;
        }

        // must not already be in the game
        if (gameManager.isPlayerJoined((Player) sender)) {
            sender.sendMessage(ChatColor.RED + "You are already in the game.");
            return;
        }

        // game must not be started
        if (gameManager.isStarted()) {
            sender.sendMessage(ChatColor.RED + "The game has already started.");
            return;
        }

        gameManager.addPlayer((Player) sender);
    }

    private void leave(CommandSender sender) {
        // must be in the game to leave
        if (!(sender instanceof Player) || !gameManager.isPlayerJoined((Player) sender)) {
            sender.sendMessage(ChatColor.RED + "You are not in the game.");
            return;
        }

        gameManager.removePlayer((Player) sender);
    }

    private void stop(CommandSender sender) {
        if (!gameManager.isStarted()) {
            sender.sendMessage(ChatColor.RED + "Game isn't started yet!");
            return;
        }

        gameManager.stop();
    }

    private void list(CommandSender sender) {
        gameManager.listPlayers(sender);
    }

    private void skip(CommandSender sender) {
        if (!(sender instanceof Player) || !gameManager.isPlayerJoined((Player) sender)) {
            sender.sendMessage(ChatColor.RED + "You must be in the game to skip.");
            return;
        }

        gameManager.skip((Player) sender);
    }

    private void set(String[] arguments, CommandSender sender) {

        if (arguments.length < 2) {
            sender.sendMessage("Available settings: mode, shared, timer, lives, points");
            return;
        }

        if (gameManager.isStarted()) {
            sender.sendMessage(ChatColor.RED + "Cannot change settings while game is active.");
            return;
        }

        String setting = arguments[1].toLowerCase();
        if (arguments.length < 3) {
            switch (setting) {
                case "mode":
                    String currentMode = settings.isRacing() ? "race" : "time";
                    sender.sendMessage(ChatColor.GREEN + "The game is currently in " + currentMode + " mode.");
                    sender.sendMessage(ChatColor.GREEN + "Possible values: race, time.");
                    break;
                case "shared":
                    String currentSharing = settings.isShared() ? "shared" : "separate";
                    sender.sendMessage(ChatColor.GREEN + "Players currently have " + currentSharing + " items.");
                    sender.sendMessage(ChatColor.GREEN + "Possible values: true, false");
                    break;
                case "time":
                    int startingTime = settings.getStartingTime();
                    sender.sendMessage(ChatColor.GREEN + "Players in time mode have " + startingTime + " seconds to get their item.");
                    break;
                case "lives":
                    int lives = settings.getMaxLives();
                    sender.sendMessage(ChatColor.GREEN + "Players in time mode have " + lives + " lives.");
                    break;
                case "points":
                    int points = settings.getWinningPoints();
                    sender.sendMessage(ChatColor.GREEN + "Players in race mode have to get " + points + " points to win.");
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "No such setting.");
            }
        } else {
            switch (setting) {
                case "mode":
                    // must provide a valid mode
                    String mode = arguments[2].toLowerCase();
                    if (!mode.equals("race") && !mode.equals("time")) {
                        sender.sendMessage(ChatColor.RED + "Invalid mode.");
                        return;
                    }
                    settings.setRacing(mode.equals("race"));
                    break;
                case "shared":
                    // must provide either true or false
                    String enabled = arguments[2].toLowerCase();
                    if (!enabled.equals("true") && !enabled.equals("false")) {
                        sender.sendMessage(ChatColor.RED + "Invalid boolean.");
                        return;
                    }
                    settings.setShared(enabled.equals("true"));
                    break;
                case "time":
                    String timeString = arguments[2];
                    int time;
                    try {
                        time = Integer.parseInt(timeString);
                    } catch (Exception ignored) {
                        sender.sendMessage(ChatColor.RED + "Invalid number.");
                        return;
                    }
                    settings.setStartingTime(time);
                    break;
                case "lives":
                    String livesString = arguments[2];
                    int lives;
                    try {
                        lives = Integer.parseInt(livesString);
                    } catch (Exception ignored) {
                        sender.sendMessage(ChatColor.RED + "Invalid number.");
                        return;
                    }
                    settings.setMaxLives(lives);
                    break;
                case "points":
                    String pointsString = arguments[2];
                    int points;
                    try {
                        points = Integer.parseInt(pointsString);
                    } catch (Exception ignored) {
                        sender.sendMessage(ChatColor.RED + "Invalid number.");
                        return;
                    }
                    settings.setWinningPoints(points);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "No such setting.");
                    return;
            }

            gameManager.changeSetting(sender.getName(), arguments[1], arguments[2]);
        }
    }

    private void conf(String[] args, CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use config commands.");
            return;
        }

        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("list")) {
                StringBuilder listBuilder = new StringBuilder(ChatColor.GOLD + "Current Categories:\n");
                for (String category : gameManager.getCategoryNames()) {
                    listBuilder
                            .append(category)
                            .append(" (")
                            .append(gameManager.getCategory(category).getInt("difficulty"))
                            .append("), ");
                }
                sender.sendMessage(listBuilder.toString());
                return;
            } else {
                String categoryName = args[1].toLowerCase();
                int difficulty = -1;
                if (args.length > 2) {
                    difficulty = Integer.parseInt(args[2]);
                }
                sender.sendMessage(ChatColor.GREEN + "Successfully added category.");
                gameManager.addOrChangeCategory(categoryName, difficulty, (Player) sender);
                return;
            }
        } else {
            gameManager.startConfiguring((Player) sender);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> commandCompletions = new ArrayList<>(Arrays.asList(
                "join",
                "skip",
                "start",
                "list",
                "stop",
                "leave",
                "help",
                "set",
                "conf"
        ));

        List<String> possibleCompletions = new ArrayList<>();
        if (args.length > 1) {
            if (args[0].equalsIgnoreCase("set")) {
                if (args.length == 3) {
                    possibleCompletions = settings.getPossibleCompletions(args[1]);
                } else if (args.length > 3) {
                    // too many args
                    possibleCompletions = new ArrayList<>();
                } else {
                    possibleCompletions = settings.getPossibleSettings();
                }
            } else if (args[0].equalsIgnoreCase("conf")) {
                if (args.length == 2) {
                    possibleCompletions = gameManager.getCategoryNames();
                }
            }
        } else {
            possibleCompletions = commandCompletions;
        }

        String relevantArg = args[args.length - 1];
        List<String> usableCompletions = new ArrayList<>();
        for (String completion : possibleCompletions) {
            if (completion.toLowerCase().startsWith(relevantArg.toLowerCase())) {
                usableCompletions.add(completion);
            }
        }

        return usableCompletions;
    }
}
