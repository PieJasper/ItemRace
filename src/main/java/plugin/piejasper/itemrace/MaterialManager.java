package plugin.piejasper.itemrace;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MaterialManager {
    private final List<Material> usableMaterials = new ArrayList<>();
    private final Random random = new Random();

    private FileConfiguration categoriesConfig;
    private File categoriesConfigFile;
    private final Map<Integer, List<Material>> materialsByDifficulty = new HashMap<>();

    public void generateUsableMaterials() {
        Material[] materials = Material.values();

        for (Material material : materials) {
            if (material.isItem() && !material.isAir()) {
                usableMaterials.add(material);
            }
        }
    }

    private int getRandomNumber(int max) {
        return random.nextInt(max);
    }

    public Material getRandomMaterial() {
        return usableMaterials.get(getRandomNumber(usableMaterials.size()));
    }

    public Material getRandomMaterialExcludingPlayerItems(List<Player> players) {
        List<Material> possibleItems = new ArrayList<>(usableMaterials);

        for (Player player : players) {
            PlayerInventory inventory = player.getInventory();
            ItemStack[] items = inventory.getStorageContents();
            for (ItemStack item : items) {
                if (item == null) {
                    continue;
                }
                Material itemType = item.getType();
                possibleItems.remove(itemType);
            }
        }

        // if players somehow have every item in their inventories, resort to using default list
        if(possibleItems.isEmpty()) {
            possibleItems = usableMaterials;
        }

        return possibleItems.get(getRandomNumber(possibleItems.size()));
    }

    public Material getRandomMaterialExcludingPlayerItems(Player player) {
        List<Material> possibleItems = new ArrayList<>(usableMaterials);

        PlayerInventory inventory = player.getInventory();
        ItemStack[] items = inventory.getStorageContents();
        for (ItemStack item : items) {
            if (item == null) {
                continue;
            }
            Material itemType = item.getType();
            possibleItems.remove(itemType);
        }

        if(possibleItems.isEmpty()) {
            possibleItems = usableMaterials;
        }

        // if player somehow has every item in their inventory, resort to using default list
        return possibleItems.get(getRandomNumber(possibleItems.size()));
    }

    public void setupConfigs(Plugin plugin) {
        categoriesConfigFile = createConfig("categories.yml", plugin);
        categoriesConfig = null; // fixme

        loadConfigs();
    }

    public void loadConfigs() {
        Map<Material, Integer> highestDifficulties = new HashMap<>();
        Set<String> keys = categoriesConfig.getKeys(false);

        // load categories
        for (String key : keys) {
            // the overall category name
            ConfigurationSection category = categoriesConfig.getConfigurationSection(key);
            assert category != null;
            // difficulty of items in the category
            int difficulty = category.getInt("difficulty", 0);
            // items in the category
            List<String> materialStrings = category.getStringList("items");

            for (String materialString : materialStrings) {
                Material material;
                try {
                    material = Material.valueOf(materialString.toUpperCase());
                } catch (Exception e) {
                    System.out.println("Invalid material in categories.yml: " + materialString);
                    continue;
                }

                int recordedDifficulty = highestDifficulties.getOrDefault(material, -1);
                // get highest difficulty for each material
                if (difficulty > recordedDifficulty) {
                    highestDifficulties.put(material, difficulty);
                }
            }
        }

        // copy highest difficulties for each material over to materialsByDifficulty
        for (Material material : highestDifficulties.keySet()) {
            int difficulty = highestDifficulties.get(material);

            List<Material> difficultyList = materialsByDifficulty.getOrDefault(difficulty, new ArrayList<>());
            difficultyList.add(material);
            materialsByDifficulty.put(difficulty, difficultyList);
        }
    }

    public void writeConfigs() {
        try {
            categoriesConfig.save(categoriesConfigFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File createConfig(String file, Plugin plugin) {
        File configFile = new File(plugin.getDataFolder(), file);
        if (!configFile.exists()) {
            assert configFile.getParentFile().mkdirs();
            plugin.saveResource(file, false);
        }

        return configFile;
    }

    private FileConfiguration loadFileConfiguration(File configFile) {
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        return config;
    }
}