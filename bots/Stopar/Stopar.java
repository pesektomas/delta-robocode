import dev.robocode.tankroyale.botapi.*;
import dev.robocode.tankroyale.botapi.events.*;
import dev.robocode.tankroyale.botapi.graphics.Color;
import dev.robocode.tankroyale.botapi.graphics.IGraphics;

// ---------------------------------------------------------------------------
// Stopar – drží cíl radarem a míří tam, kde soupeř BUDE (kruhová predikce)
// ---------------------------------------------------------------------------
// V GUI klikni na Stopaře (otevře se jeho konzole), dej „Toggle Graphical Debugging“ a uvidíš:
//   - zelenou dráhu, po které podle Stopaře soupeř pojede,
//   - červený zaměřovač v bodě, kam letí střela,
//   - čáru od děla k cíli.
// ---------------------------------------------------------------------------
public class Stopar extends Bot {

    // Poslední známý stav cíle
    int cilId = -1;
    double cilX, cilY, cilSmer, cilRychlost, cilOtaceni, cilEnergie;
    int naposledyVidenVTahu = -100;

    public static void main(String[] args) {
        new Stopar().start();
    }

    @Override
    public void run() {
        setBodyColor(Color.fromRgb(0x23, 0x86, 0x36));
        setTurretColor(Color.fromRgb(0x2e, 0xa0, 0x43));
        setRadarColor(Color.fromRgb(0x7e, 0xe7, 0x87));
        setScanColor(Color.fromRgb(0x7e, 0xe7, 0x87));
        setBulletColor(Color.fromRgb(0xff, 0x7b, 0x72));

        // Tělo, dělo a radar se otáčejí nezávisle na sobě
        setAdjustGunForBodyTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForBodyTurn(true);
        cilId = -1;

        int smerJizdy = 1;
        while (isRunning()) {
            boolean cilJeCerstvy = getTurnNumber() - naposledyVidenVTahu < 5;

            if (!cilJeCerstvy) {
                // Nikoho nevidíme → radar dokola
                setRadarTurnRate(Constants.MAX_RADAR_TURN_RATE);
            } else {
                zamerARadar();
                zamerAStrilej();
                // Jezdíme kolmo k soupeři (hůř se na nás míří) a občas změníme směr
                double kolmo = normalizeRelativeAngle(bearingTo(cilX, cilY) + 90);
                setTurnRate(kolmo);
                if (getTurnNumber() % 40 == 0) smerJizdy = -smerJizdy;
            }

            // Blízko zdi → otoč se ke středu arény
            if (blizkoZdi(80)) {
                setTurnRate(bearingTo(getArenaWidth() / 2.0, getArenaHeight() / 2.0));
                setTargetSpeed(6);
            } else {
                setTargetSpeed(6 * smerJizdy);
            }

            go();
        }
    }

    @Override
    public void onScannedBot(ScannedBotEvent e) {
        // V melee se držíme nejbližšího soupeře
        boolean jinyBlizsi = cilId != -1 && e.getScannedBotId() != cilId
                && getTurnNumber() - naposledyVidenVTahu < 5
                && distanceTo(e.getX(), e.getY()) > distanceTo(cilX, cilY);
        if (jinyBlizsi) return;

        int dt = Math.max(1, e.getTurnNumber() - naposledyVidenVTahu);
        if (e.getScannedBotId() == cilId) {
            cilOtaceni = normalizeRelativeAngle(e.getDirection() - cilSmer) / dt;
        } else {
            cilOtaceni = 0;
        }
        cilId = e.getScannedBotId();
        cilX = e.getX();
        cilY = e.getY();
        cilSmer = e.getDirection();
        cilRychlost = e.getSpeed();
        cilEnergie = e.getEnergy();
        naposledyVidenVTahu = e.getTurnNumber();
    }

    @Override
    public void onBotDeath(BotDeathEvent e) {
        if (e.getVictimId() == cilId) {
            cilId = -1;
            naposledyVidenVTahu = -100;
        }
    }

    void zamerARadar() {
        // Radar přetáčíme kousek za cíl, aby ho neztratil
        double b = radarBearingTo(cilX, cilY);
        setRadarTurnRate(b + Math.signum(b) * 15);
    }

    void zamerAStrilej() {
        double vzdalenost = distanceTo(cilX, cilY);
        double sila = Math.min(3, Math.max(0.5, 500 / vzdalenost));
        if (getEnergy() < 15) sila = Math.min(sila, 1);
        double rychlostStrely = 20 - 3 * sila;

        // Simulujeme pohyb soupeře, dokud ho střela „nedohoní“
        double px = cilX, py = cilY, smer = cilSmer;
        IGraphics g = getGraphics();
        g.setStrokeColor(Color.fromRgba(0x7e, 0xe7, 0x87, 0xb0));
        g.setStrokeWidth(2);
        for (int t = 1; t < 100; t++) {
            smer += cilOtaceni;
            double nx = px + Math.cos(Math.toRadians(smer)) * cilRychlost;
            double ny = py + Math.sin(Math.toRadians(smer)) * cilRychlost;
            nx = Math.max(18, Math.min(getArenaWidth() - 18, nx));
            ny = Math.max(18, Math.min(getArenaHeight() - 18, ny));
            g.drawLine(px, py, nx, ny);
            px = nx;
            py = ny;
            if (t * rychlostStrely >= distanceTo(px, py)) break;
        }

        double natoceni = gunBearingTo(px, py);
        setGunTurnRate(natoceni);

        // Zaměřovač
        g.setStrokeColor(Color.fromRgb(0xff, 0x7b, 0x72));
        g.drawCircle(px, py, 12);
        g.drawLine(px - 20, py, px + 20, py);
        g.drawLine(px, py - 20, px, py + 20);
        g.setStrokeColor(Color.fromRgba(0xff, 0x7b, 0x72, 0x60));
        g.drawLine(getX(), getY(), px, py);

        if (getGunHeat() == 0 && Math.abs(natoceni) < 3) {
            setFire(sila);
        }
    }

    boolean blizkoZdi(double okraj) {
        return getX() < okraj || getY() < okraj
                || getX() > getArenaWidth() - okraj || getY() > getArenaHeight() - okraj;
    }
}
