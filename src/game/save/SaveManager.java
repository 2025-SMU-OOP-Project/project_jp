package game.save;

import java.io.*;

public class SaveManager {

    private static final String SAVE_FILE = "save.dat";

    public static boolean hasSave() {
        File f = new File(SAVE_FILE);
        return f.exists() && f.isFile() && f.length() > 0;
    }

    public static void clearSave() {
        File f = new File(SAVE_FILE);
        if (f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    public static boolean save(SaveState st) {
        if (st == null) return false;
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(st);
            oos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static SaveState load() {
        if (!hasSave()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            Object obj = ois.readObject();
            if (obj instanceof SaveState) {
                return (SaveState) obj;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}