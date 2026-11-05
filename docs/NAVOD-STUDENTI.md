# Návod pro studenty: tvůj první tank v Robocode

Naprogramuješ tank v Javě. V aréně pak bojuje sám – ty ho neovládáš, rozhoduje jen tvůj kód.

---

## Krok 1: Nainstaluj nástroje

1. **Java JDK 17 nebo novější**
   - Stáhni z https://adoptium.net (Temurin, verze 21 LTS) a nainstaluj.
   - Ověř v terminálu (Windows: *cmd* nebo *PowerShell*, macOS: *Terminál*):
     ```bash
     java -version
     ```
     Musí vypsat verzi 17 nebo vyšší.
2. **Editor** – doporučujeme **IntelliJ IDEA Community** (zdarma): https://www.jetbrains.com/idea/download
   Jde to i ve VS Code s rozšířením *Extension Pack for Java*.

## Krok 2: Stáhni projekt

Od učitele dostaneš odkaz:
- přes git: `git clone <odkaz>`
- nebo jako ZIP – rozbal ho třeba do `Dokumenty/robocode`.

V terminálu přejdi do složky projektu a spusť:
```bash
./gradlew setup verifyBots
```
Na **Windows** piš všude `gradlew.bat` místo `./gradlew`.
Poprvé to chvíli trvá (stahuje se Gradle a knihovny). Musí skončit `BUILD SUCCESSFUL`.

V IntelliJ: **File → Open** → vyber složku projektu → potvrď *Trust project*. IntelliJ pozná Gradle
projekt sám a bude ti napovídat metody tanku.

## Krok 3: Vytvoř svého bota

Bot = **jedna složka** v `bots/` se dvěma soubory. Název složky, souborů i třídy musí být **stejný**.

1. Zkopíruj složku `bots/Sablona` a pojmenuj ji svým jménem bez diakritiky a mezer, např. `NovakTank`.
2. Přejmenuj soubory uvnitř: `NovakTank.java` a `NovakTank.json`.
3. V `NovakTank.java` přepiš:
   ```java
   public class NovakTank extends Bot {
       public static void main(String[] args) {
           new NovakTank().start();
       }
   ```
4. V `NovakTank.json` přepiš `name`, `authors` a `description`:
   ```json
   {
     "name": "NovakTank",
     "version": "1.0",
     "authors": ["Jan Novák"],
     "description": "Můj první tank",
     ...
   }
   ```
5. Zkontroluj, že je všechno v pořádku:
   ```bash
   ./gradlew verifyBots
   ```

## Krok 4: Vyzkoušej ho doma (na svém počítači)

```bash
./gradlew gui
```
1. V okně klikni **Battle → Start Battle**.
2. Vlevo nahoře uvidíš boty ze složky `bots/`. Vyber svého a třeba `SpinBot` → **Boot →**.
3. Nabootovaní boti se objeví v *Joined Bots*. Přidej je do bitvy (**Add →**) a klikni **Start Battle**.

Tipy:
- Kliknutím na tank v pravém panelu otevřeš jeho **konzoli** – vidíš tam `System.out.println(...)` z tvého kódu.
- V konzoli je **Toggle Graphical Debugging** – zobrazí, co tvůj bot kreslí přes `getGraphics()`.
- Rychlost hry měníš posuvníkem TPS (tahy za sekundu).

## Krok 5: Připoj se do školní arény

Učitel ti řekne **adresu serveru** a **heslo**. Pak spusť:
```bash
./gradlew connect -Pbot=NovakTank -Purl=ws://192.168.1.50:7654 -Psecret=HESLO
```
(adresu a heslo nahraď těmi od učitele)

Když vidíš `Connected to: ws://…`, jsi v aréně – učitel tě uvidí a přidá do bitvy.
Nech terminál otevřený. Chceš-li bota upravit: **Ctrl+C**, změň kód, spusť `connect` znovu.

Bez Gradlu to jde taky (ve složce svého bota):
- macOS / Linux:
  ```bash
  SERVER_URL=ws://192.168.1.50:7654 SERVER_SECRET=HESLO java -cp "../lib/*" NovakTank.java
  ```
- Windows (cmd):
  ```bat
  set SERVER_URL=ws://192.168.1.50:7654
  set SERVER_SECRET=HESLO
  java -cp "..\lib\*" NovakTank.java
  ```

**Chyby při připojení:**
- `Invalid secret` → špatné heslo.
- `Could not create web socket` → špatná adresa, nebo nejsi ve stejné Wi-Fi jako učitel.
- `Could not find or load main class` → název složky / souboru / třídy nesedí, nebo jsi nespustil `setup`.

---

## Jak tank funguje – tahák

### Aréna a úhly
- Bod `[0, 0]` je **vlevo dole**, `x` roste doprava, `y` **nahoru**.
- Úhel **0° = doprava**, **90° = nahoru**. Kladné otáčení je **doleva** (proti směru hodinových ručiček).
- Tank má 3 části, které se otáčejí: **tělo**, **dělo** a **radar** (radar sedí na děle).

### Struktura bota
```java
public class NovakTank extends Bot {
    public static void main(String[] args) { new NovakTank().start(); }

    @Override
    public void run() {                    // začátek každého kola
        while (isRunning()) {               // opakuj, dokud kolo běží
            forward(100);
            turnGunRight(360);
        }
    }

    @Override
    public void onScannedBot(ScannedBotEvent e) { fire(1); }   // radar někoho viděl
    @Override
    public void onHitByBullet(HitByBulletEvent e) { turnLeft(90); }  // trefili nás
    @Override
    public void onHitWall(HitWallEvent e) { back(50); }        // narazili jsme do zdi
}
```

### Nejdůležitější příkazy
| Blokující (počká, než se dokončí) | Co dělá |
|---|---|
| `forward(d)` / `back(d)` | jeď dopředu / dozadu o `d` |
| `turnLeft(st)` / `turnRight(st)` | otoč tělo |
| `turnGunLeft(st)` / `turnGunRight(st)` | otoč dělo |
| `fire(sila)` | vystřel, `sila` 0.1 – 3 |

| Neblokující (nastav a pak zavolej `go()`) | Co dělá |
|---|---|
| `setTargetSpeed(v)` | rychlost −8 až 8 |
| `setTurnRate(st)` / `setGunTurnRate(st)` / `setRadarTurnRate(st)` | otáčení za tah |
| `setFire(sila)` | vystřel, až bude dělo připravené |
| `go()` | pošli všechno najednou a počkej na další tah |

Neblokující příkazy umožní jezdit, mířit a skenovat **zároveň** – podívej se na bota `Stopar`.

### Užitečné výpočty
| Metoda | Vrací |
|---|---|
| `getX()`, `getY()`, `getEnergy()`, `getDirection()` | kde jsem, kolik mám energie, kam se dívám |
| `distanceTo(x, y)` | vzdálenost k bodu |
| `bearingTo(x, y)` | o kolik stupňů natočit **tělo**, abych mířil na bod |
| `gunBearingTo(x, y)`, `radarBearingTo(x, y)` | totéž pro dělo a radar |
| `getArenaWidth()`, `getArenaHeight()` | rozměr arény |
| `getGraphics()` | kreslení do arény (čáry, kruhy, text) |

### Pravidla hry
- Každý tank začíná se **100 energie**. Při 0 je vyřazený.
- **Výstřel** stojí energii = síla. **Zásah** soupeře ti energii vrací (3× síla).
- Silná střela (3) dělá větší poškození, ale **letí pomaleji** (rychlost = 20 − 3 × síla) a dělo se déle chladí.
- Náraz do zdi i do tanku ubírá energii. Taranování ale přináší body.
- Bot musí v každém tahu odpovědět rychle (výchozí limit 30 ms) – žádné dlouhé výpočty ani `Thread.sleep`.

---

## Úrovně – jak se zlepšovat

1. **Začátečník:** uprav šablonu – barvy (`setBodyColor`), jiný pohyb, síla střely podle vzdálenosti.
2. **Radar lock:** drž radar na soupeři, místo aby se točil dokola. Nápověda:
   `setRadarTurnRate(radarBearingTo(x, y) * 2)`. Ukázka: `Stopar`.
3. **Míření dopředu:** soupeř se hýbe – miř tam, kde **bude**, až tam střela doletí. Ukázka: `Stopar`.
4. **Uhýbání:** když soupeři klesne energie o 0.1–3, právě vystřelil → změň směr. Ukázka: `Tanecnik`.
5. **Taktika v melee:** koho napadnout (nejbližšího? nejslabšího?), jak nebýt uprostřed. Ukázka: `Beran`.

Inspirace: https://robowiki.net (strategie z původního Robocode, fungují i tady),
dokumentace: https://robocode.dev

## Pravidla pro školní turnaj

- Jeden bot = **jeden `.java` soubor** (pomocné třídy dej dovnitř jako vnořené `static class`).
- Žádné soubory, síť ani vlákna navíc – bot jen hraje.
- Před odevzdáním vždy `./gradlew verifyBots`.
