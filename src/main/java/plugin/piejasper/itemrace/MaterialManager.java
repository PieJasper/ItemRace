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

    private final Map<String, ConfigurationSection> categories = new HashMap<>();
    private final List<Material> itemsAlreadyCategorized = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();

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

    public Material getRandomMaterialExcludingPlayerItems(List<Player> players, int round) {
        List<ItemStack> excludedItems = new ArrayList<>();

        for (Player player : players) {
            PlayerInventory inventory = player.getInventory();
            ItemStack[] items = inventory.getStorageContents();
            for (ItemStack item : items) {
                if (item == null) {
                    continue;
                }
                excludedItems.add(item);
            }
        }

        return getRandomMaterialExcludingItems(excludedItems,  round);
    }

    public Material getRandomMaterialExcludingPlayerItems(Player player, int round) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] items = inventory.getStorageContents();

        return getRandomMaterialExcludingItems(Arrays.asList(items), round);
    }

    public Material getRandomMaterialExcludingItems(List<ItemStack> items, int round) {

        // todo: make this better
        List<Material> possibleItems = materialsByDifficulty.get(getRandomNumber(3) + 1);

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

        // if all items are excluded, use any item
        return possibleItems.get(getRandomNumber(possibleItems.size()));
    }

    public void setupConfig(Plugin plugin) {
        categoriesConfigFile = createConfig("categories.yml", plugin);
        categoriesConfig = loadFileConfiguration(categoriesConfigFile);

        loadConfig();
    }

    public void loadConfig() {
        Map<Material, Integer> highestDifficulties = new HashMap<>();
        Set<String> keys = categoriesConfig.getKeys(false);

        // load categories
        for (String key : keys) {
            // the overall category name
            ConfigurationSection category = categoriesConfig.getConfigurationSection(key);
            assert category != null;

            categories.put(key, category);
            categoryNames.add(key);

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

                if (!itemsAlreadyCategorized.contains(material)) {
                    itemsAlreadyCategorized.add(material);
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

    public Material getUncategorizedItem() {
        List<Material> uncategorizedItems = usableMaterials;
        uncategorizedItems.removeAll(itemsAlreadyCategorized);
        System.out.println(uncategorizedItems.size());
        return uncategorizedItems.get(getRandomNumber(uncategorizedItems.size()));
    }

    public void writeConfig() {
        for (String categoryName : categories.keySet()) {
            categoriesConfig.set(categoryName, categories.get(categoryName));
        }

        try {
            categoriesConfig.save(categoriesConfigFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getCategoryNames() {
        return categoryNames;
    }

    public ConfigurationSection getCategory(String name) {
        return categories.getOrDefault(name, null);
    }

    public void addOrChangeCategory(String categoryName, int difficulty, Material newMaterial) {
        ConfigurationSection categoryToBeChanged;
        boolean existedBefore = false;

        if (categories.containsKey(categoryName)) {
            categoryToBeChanged = categories.get(categoryName);
            existedBefore = true;
        } else {
            categoryToBeChanged = categoriesConfig.createSection(categoryName);
        }

        if (difficulty > 0) {
            categoryToBeChanged.set("difficulty", difficulty);
        }

        if (newMaterial != null) {
            List<String> alreadyExistingItems = categoryToBeChanged.getStringList("items");
            if (!alreadyExistingItems.contains(newMaterial.toString())) {
                alreadyExistingItems.add(newMaterial.toString());
            }
            categoryToBeChanged.set("items", alreadyExistingItems);
            itemsAlreadyCategorized.add(newMaterial);
        }

        if (!existedBefore) {
            categoryNames.add(categoryName);
            categories.put(categoryName, categoryToBeChanged);
        }

        writeConfig();
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
