import dev.robocode.tankroyale.botapi.*;
import dev.robocode.tankroyale.botapi.events.*;
import dev.robocode.tankroyale.botapi.graphics.Color;
import dev.robocode.tankroyale.botapi.graphics.IGraphics;

// ---------------------------------------------------------------------------
// Tanecnik – pozná, že soupeř vystřelil, a uhne
// ---------------------------------------------------------------------------
// Trik: výstřel stojí střelce energii (0.1 až 3). Když soupeři energie klesne
// o takovou hodnotu a my jsme ho netrefili, nejspíš právě vystřelil → změníme
// směr jízdy. V Graphical Debugging uvidíš oranžovou „vlnu“ – kruh, jak daleko
// už mohla střela doletět.
// ---------------------------------------------------------------------------
public class Tanecnik extends Bot {

    double cilX, cilY, cilEnergie = 100;
    int cilId = -1;
    int naposledyVidenVTahu = -100;
    int smer = 1;

    // Poslední zaznamenaná vlna (výstřel soupeře)
    double vlnaX, vlnaY, vlnaRychlost;
    int vlnaTah = -1;

    public static void main(String[] args) {
        new Tanecnik().start();
    }

    @Override
    public void run() {
        setBodyColor(Color.fromRgb(0x89, 0x57, 0xe5));
        setTurretColor(Color.fromRgb(0xd2, 0xa8, 0xff));
        setRadarColor(Color.fromRgb(0xff, 0xa6, 0x57));
        setScanColor(Color.fromRgb(0xd2, 0xa8, 0xff));
        setBulletColor(Color.fromRgb(0xff, 0xa6, 0x57));
        setAdjustGunForBodyTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForBodyTurn(true);
        cilId = -1;
        cilEnergie = 100;
        vlnaTah = -1;

        while (isRunning()) {
            boolean vidim = getTurnNumber() - naposledyVidenVTahu < 5;
            if (vidim) {
                double b = radarBearingTo(cilX, cilY);
                setRadarTurnRate(b + Math.signum(b) * 15);

                // Krouživý pohyb kolem soupeře, trochu se k němu přibližujeme/vzdalujeme
                double vzdalenost = distanceTo(cilX, cilY);
                double priblizeni = vzdalenost > 350 ? -20 : (vzdalenost < 200 ? 20 : 0);
                setTurnRate(normalizeRelativeAngle(bearingTo(cilX, cilY) + 90 + priblizeni * smer));

                // Střílíme přímo na soupeře, slaběji – šetříme energii na tancování
                double natoceni = gunBearingTo(cilX, cilY);
                setGunTurnRate(natoceni);
                if (getGunHeat() == 0 && Math.abs(natoceni) < 4 && getEnergy() > 5) {
                    setFire(vzdalenost < 250 ? 2 : 1.2);
                }
            } else {
                setRadarTurnRate(Constants.MAX_RADAR_TURN_RATE);
            }

            if (blizkoZdi(70)) {
                setTurnRate(bearingTo(getArenaWidth() / 2.0, getArenaHeight() / 2.0));
                setTargetSpeed(8);
            } else {
                setTargetSpeed(8 * smer);
            }

            nakresliVlnu();
            go();
        }
    }

    @Override
    public void onScannedBot(ScannedBotEvent e) {
        if (cilId != -1 && e.getScannedBotId() != cilId && getTurnNumber() - naposledyVidenVTahu < 5
                && distanceTo(e.getX(), e.getY()) > distanceTo(cilX, cilY)) {
            return;
        }
        if (e.getScannedBotId() == cilId) {
            double pokles = cilEnergie - e.getEnergy();
            if (pokles >= 0.1 && pokles <= 3.0) {
                // Soupeř vystřelil → uhýbáme na druhou stranu
                smer = -smer;
                vlnaX = e.getX();
                vlnaY = e.getY();
                vlnaRychlost = 20 - 3 * pokles;
                vlnaTah = e.getTurnNumber();
            }
        }
        cilId = e.getScannedBotId();
        cilX = e.getX();
        cilY = e.getY();
        cilEnergie = e.getEnergy();
        naposledyVidenVTahu = e.getTurnNumber();
    }

    // Náš zásah soupeři sebral energii – nesmíme si to splést s jeho výstřelem
    @Override
    public void onBulletHit(BulletHitBotEvent e) {
        if (e.getVictimId() == cilId) cilEnergie = e.getEnergy();
    }

    @Override
    public void onHitBot(HitBotEvent e) {
        smer = -smer;
        if (e.getVictimId() == cilId) cilEnergie = e.getEnergy();
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        smer = -smer;
    }

    @Override
    public void onBotDeath(BotDeathEvent e) {
        if (e.getVictimId() == cilId) {
            cilId = -1;
            cilEnergie = 100;
            naposledyVidenVTahu = -100;
        }
    }

    void nakresliVlnu() {
        if (vlnaTah < 0) return;
        double polomer = (getTurnNumber() - vlnaTah) * vlnaRychlost;
        if (polomer > distanceTo(vlnaX, vlnaY) + 50) return;
        IGraphics g = getGraphics();
        g.setStrokeWidth(3);
        g.setStrokeColor(Color.fromRgba(0xff, 0xa6, 0x57, 0xc0));
        g.drawCircle(vlnaX, vlnaY, polomer);
        g.setFillColor(Color.fromRgb(0xff, 0xa6, 0x57));
        g.setFont("Arial", 14);
        g.drawText("uhýbám!", getX() + 25, getY() + 25);
    }

    boolean blizkoZdi(double okraj) {
        return getX() < okraj || getY() < okraj
                || getX() > getArenaWidth() - okraj || getY() > getArenaHeight() - okraj;
    }
}
