package game.save;

import java.io.*;

public class SaveManager {

    private static File getSaveFile() {
        String home = System.getProperty("user.home");
        return new File(home, ".vamsur_save.dat");
    }

    public static boolean hasSave() {
        File f = getSaveFile();
        return f.exists() && f.isFile() && f.length() > 0;
    }

    public static void clearSave() {
        File f = getSaveFile();
        if (f.exists()) f.delete();
    }

    public static boolean save(SaveState state) {
        if (state == null) return false;
        File f = getSaveFile();

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(f)))) {
            oos.writeObject(state);
            oos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static SaveState load() {
        File f = getSaveFile();
        if (!hasSave()) return null;

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {
            Object obj = ois.readObject();
            if (obj instanceof SaveState) return (SaveState) obj;
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
