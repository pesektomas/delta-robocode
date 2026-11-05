import dev.robocode.tankroyale.botapi.*;
import dev.robocode.tankroyale.botapi.events.*;
import dev.robocode.tankroyale.botapi.graphics.Color;

// ---------------------------------------------------------------------------
// Sablona – výchozí bod pro tvého vlastního bota
// ---------------------------------------------------------------------------
// Jak z ní udělat svého bota:
//   1. Zkopíruj celou složku Sablona a pojmenuj ji třeba NovakTank.
//   2. Přejmenuj oba soubory na NovakTank.java a NovakTank.json.
//   3. V tomto souboru přepiš "Sablona" na "NovakTank" (název třídy a v main).
//   4. V NovakTank.json přepiš "name", "authors" a "description".
//
// Souřadnice: [0,0] je vlevo dole, x roste doprava, y nahoru.
// Úhly: 0° = doprava (východ), 90° = nahoru. Kladné otáčení = doleva.
// ---------------------------------------------------------------------------
public class Sablona extends Bot {

    public static void main(String[] args) {
        new Sablona().start();
    }

    // Spustí se na začátku každého kola. Dokud kolo běží, opakuje se smyčka.
    @Override
    public void run() {
        // Barvy tanku – vyber si vlastní!
        setBodyColor(Color.fromRgb(0x1f, 0x6f, 0xeb));
        setTurretColor(Color.fromRgb(0xff, 0xff, 0xff));
        setRadarColor(Color.fromRgb(0xff, 0xa6, 0x57));
        setBulletColor(Color.fromRgb(0xff, 0xa6, 0x57));

        while (isRunning()) {
            forward(150);       // jeď dopředu 150 jednotek
            turnGunRight(360);  // otoč dělem dokola – radar se točí s ním a hledá soupeře
            back(100);          // couvni
            turnRight(45);      // natoč se o 45° doprava
        }
    }

    // Radar zahlédl jiného bota → vystřel.
    @Override
    public void onScannedBot(ScannedBotEvent e) {
        double vzdalenost = distanceTo(e.getX(), e.getY());
        // Blízko střílej silně (3 = max), daleko slabě (rychlejší střela, menší ztráta energie).
        fire(vzdalenost < 200 ? 3 : 1);
    }

    // Trefila nás střela → uhni stranou.
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        turnLeft(90);
        forward(100);
    }

    // Narazili jsme do zdi → otoč se a jeď pryč.
    @Override
    public void onHitWall(HitWallEvent e) {
        back(50);
        turnRight(90);
    }
}
