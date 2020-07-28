package plugin.piejasper.itemrace;

import java.util.*;

public class GameSettings {

    private boolean racing = false;
    private boolean shared = true;

    private int startingTime = 300;
    private int maxLives = 3;

    private int winningPoints = 5;

    private final HashMap<String, List<String>> tabCompletions = new HashMap<>();

    public GameSettings() {
        tabCompletions.put("mode", new ArrayList<>(Arrays.asList("race", "time")));
        tabCompletions.put("shared", new ArrayList<>(Arrays.asList("true", "false")));
        tabCompletions.put("time", new ArrayList<>());
        tabCompletions.put("points", new ArrayList<>());
        tabCompletions.put("lives", new ArrayList<>());
    }

    public List<String> getPossibleSettings() {
        return new ArrayList<>(tabCompletions.keySet());
    }

    public List<String> getPossibleCompletions(String setting) {
        return tabCompletions.getOrDefault(setting, new ArrayList<>());
    }

    public boolean isRacing() {
        return racing;
    }

    public void setRacing(boolean racing) {
        this.racing = racing;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public int getStartingTime() {
        return startingTime;
    }

    public void setStartingTime(int startingTime) {
        this.startingTime = startingTime;
    }

    public int getMaxLives() {
        return maxLives;
    }

    public void setMaxLives(int maxLives) {
        this.maxLives = maxLives;
    }

    public int getWinningPoints() {
        return winningPoints;
    }

    public void setWinningPoints(int winningPoints) {
        this.winningPoints = winningPoints;
    }
}
