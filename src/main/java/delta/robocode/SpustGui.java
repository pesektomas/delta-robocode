package delta.robocode;

import dev.robocode.tankroyale.gui.GuiAppKt;
import dev.robocode.tankroyale.gui.settings.BotDirectoryConfig;
import dev.robocode.tankroyale.gui.settings.ConfigSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Spustí Tank Royale GUI a předtím do jeho nastavení zapíše složku s našimi boty,
 * aby se nemusela na každém počítači naklikávat ručně (Config → Bot Root Directories).
 */
public class SpustGui {

    public static void main(String[] args) {
        String botsDir = new File(args[0]).getAbsolutePath().replace('\\', '/');

        ConfigSettings settings = ConfigSettings.INSTANCE;
        List<BotDirectoryConfig> dirs = new ArrayList<>();
        for (BotDirectoryConfig dir : settings.getBotDirectories()) {
            if (!dir.getPath().equals(botsDir)) {
                dirs.add(dir);
            }
        }
        dirs.add(0, new BotDirectoryConfig(botsDir, true));
        settings.setBotDirectories(dirs);

        GuiAppKt.main(new String[0]);
    }
}
