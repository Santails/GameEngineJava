package cz.cvut.fel.pjv.gameengine3000.savegame;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;

public class SaveLoadManager {
    private static final String SAVE_GAME_DIR = "saves";
    private static final String SAVE_FILE_EXTENSION = ".json";
    private final ObjectMapper objectMapper;

    public SaveLoadManager() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // For human-readable JSON
        new File(SAVE_GAME_DIR).mkdirs(); // Ensure save directory exists
    }

    public boolean saveGame(SaveGameData data, String slotName) {
        if (data == null || slotName == null || slotName.trim().isEmpty()) {
            System.err.println("SaveLoadManager: Invalid data or slot name for saving.");
            return false;
        }
        File saveFile = new File(SAVE_GAME_DIR, slotName + SAVE_FILE_EXTENSION);
        try {
            objectMapper.writeValue(saveFile, data);
            System.out.println("SaveLoadManager: Game saved to " + saveFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("SaveLoadManager: Error saving game to " + slotName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public SaveGameData loadGame(String slotName) {
        if (slotName == null || slotName.trim().isEmpty()) {
            System.err.println("SaveLoadManager: Invalid slot name for loading.");
            return null;
        }
        File saveFile = new File(SAVE_GAME_DIR, slotName + SAVE_FILE_EXTENSION);
        if (!saveFile.exists()) {
            System.err.println("SaveLoadManager: Save file not found: " + saveFile.getAbsolutePath());
            return null;
        }
        try {
            SaveGameData data = objectMapper.readValue(saveFile, SaveGameData.class);
            System.out.println("SaveLoadManager: Game loaded from " + saveFile.getAbsolutePath());
            return data;
        } catch (IOException e) {
            System.err.println("SaveLoadManager: Error loading game from " + slotName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String[] getAvailableSaveSlots() {
        File dir = new File(SAVE_GAME_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(SAVE_FILE_EXTENSION));
        if (files == null) {
            return new String[0];
        }
        String[] slotNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            slotNames[i] = files[i].getName().replace(SAVE_FILE_EXTENSION, "");
        }
        return slotNames;
    }
}