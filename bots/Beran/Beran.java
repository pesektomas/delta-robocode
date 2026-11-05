import dev.robocode.tankroyale.botapi.*;
import dev.robocode.tankroyale.botapi.events.*;
import dev.robocode.tankroyale.botapi.graphics.Color;

// ---------------------------------------------------------------------------
// Beran – najde nejslabšího soupeře a vrazí do něj
// ---------------------------------------------------------------------------
// Taranování taky dává body (ram damage). Beran jede plnou rychlostí přímo
// na cíl a z blízka pálí plnou silou.
// ---------------------------------------------------------------------------
public class Beran extends Bot {

    int cilId = -1;
    double cilX, cilY, cilEnergie;
    int naposledyVidenVTahu = -100;

    public static void main(String[] args) {
        new Beran().start();
    }

    @Override
    public void run() {
        setBodyColor(Color.fromRgb(0xda, 0x36, 0x33));
        setTurretColor(Color.fromRgb(0x30, 0x36, 0x3d));
        setRadarColor(Color.fromRgb(0xff, 0xd3, 0x3d));
        setScanColor(Color.fromRgb(0xff, 0x7b, 0x72));
        setBulletColor(Color.fromRgb(0xff, 0xd3, 0x3d));
        setAdjustGunForBodyTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForBodyTurn(true);
        cilId = -1;

        while (isRunning()) {
            if (getTurnNumber() - naposledyVidenVTahu < 6) {
                double b = radarBearingTo(cilX, cilY);
                setRadarTurnRate(b + Math.signum(b) * 20);
                setTurnRate(bearingTo(cilX, cilY));
                setTargetSpeed(Constants.MAX_SPEED);

                double natoceni = gunBearingTo(cilX, cilY);
                setGunTurnRate(natoceni);
                double vzdalenost = distanceTo(cilX, cilY);
                if (getGunHeat() == 0 && Math.abs(natoceni) < 5 && vzdalenost < 300) {
                    setFire(vzdalenost < 120 ? 3 : 1.5);
                }
            } else {
                setRadarTurnRate(Constants.MAX_RADAR_TURN_RATE);
                setTurnRate(5);
                setTargetSpeed(4);
            }
            go();
        }
    }

    @Override
    public void onScannedBot(ScannedBotEvent e) {
        boolean cilStale = getTurnNumber() - naposledyVidenVTahu < 6;
        // Přepneme na slabšího soupeře, ale jen když je výrazně slabší
        if (cilId == -1 || !cilStale || e.getScannedBotId() == cilId || e.getEnergy() + 15 < cilEnergie) {
            cilId = e.getScannedBotId();
            cilX = e.getX();
            cilY = e.getY();
            cilEnergie = e.getEnergy();
            naposledyVidenVTahu = e.getTurnNumber();
        }
    }

    @Override
    public void onHitBot(HitBotEvent e) {
        // Narazili jsme → hned pálíme a tlačíme dál
        cilId = e.getVictimId();
        cilX = e.getX();
        cilY = e.getY();
        cilEnergie = e.getEnergy();
        naposledyVidenVTahu = getTurnNumber();
    }

    @Override
    public void onBotDeath(BotDeathEvent e) {
        if (e.getVictimId() == cilId) {
            cilId = -1;
            naposledyVidenVTahu = -100;
        }
    }
}
