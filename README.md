# 🤖 Robocode Tank Royale – Delta

Programujeme tanky v Javě a necháme je bojovat v aréně.
Postaveno na [Robocode Tank Royale](https://robocode.dev) **1.4.0**.

Potřebuješ jen **Javu 17+** (JDK). Gradle se stáhne sám přes `./gradlew` (Windows: `gradlew.bat`).

- 🎓 **Studenti:** [docs/NAVOD-STUDENTI.md](docs/NAVOD-STUDENTI.md) – instalace, první bot, připojení do arény, tahák
- 🏫 **Učitel:** [docs/NAVOD-SKOLA.md](docs/NAVOD-SKOLA.md) – server ve škole, síť, tunel, scénář dne otevřených dveří

## Příkazy

| Příkaz | Co dělá |
|---|---|
| `./gradlew setup` | nakopíruje Bot API do `bots/lib` (potřeba jednou) |
| `./gradlew verifyBots` | přeloží všechny boty a ukáže chyby |
| `./gradlew gui` | spustí arénu (GUI) se složkou `bots/` už nastavenou |
| `./gradlew tournament` | turnaj místních botů bez grafiky – melee, 10 kol |
| `./gradlew tournament --args="duel 5"` | každý s každým 1 na 1, 5 kol na zápas |
| `./gradlew server -Psecret=HESLO` | samostatný server pro boty přes síť (port 7654, `-Pport=…`) |
| `./gradlew connect -Pbot=NovakTank -Purl=ws://IP:7654 -Psecret=HESLO` | připojí bota k serveru |
| `./gradlew arena -Psecret=HESLO` | bitva všech připojených botů + žebříček (`-Prounds`, `-Ptimeout`, `-Pwait`, `-Purl`) |

Výsledky turnajů a arény (HTML žebříček) a záznamy bitev se ukládají do `vysledky/`.
Záznam přehraješ v GUI přes **Battle → Replay from File…**

## Struktura

```
bots/
  lib/            Bot API (vytvoří ./gradlew setup – necommitovat)
  Sablona/        šablona pro vlastního bota
  Stopar/         ukázka: radar lock + kruhová predikce + kreslení zaměřovače
  Tanecnik/       ukázka: pozná výstřel soupeře podle poklesu energie a uhne
  Beran/          ukázka: taranuje nejslabšího soupeře
  Hrac/           tank ovládaný klávesnicí (pro návštěvníky)
  SpinBot, Walls, Crazy, Corners, TrackFire, RamFire   oficiální sample boti na trénink
src/main/java/    nástroje: Turnaj, Arena (controller pro síťové boty), SpustGui
docs/             návody
vysledky/         žebříčky a záznamy bitev
```

## Poznámky

- Turnaj vynechává `Hrac` a `Sablona` (seznam `VYNECHAT` v `Turnaj.java`).
- Nastavení GUI (složka botů) se ukládá do uživatelského profilu, `./gradlew gui` ho nastaví při každém spuštění.
- `Arena` mluví se serverem přímo WebSocket protokolem – runner z Tank Royale umí jen boty, které si sám spustí.
- Verzi Tank Royale měníš v `build.gradle` (`tankRoyaleVersion`).
- Runner a GUI z Tank Royale 1.4.0 jsou napsané v Kotlinu 2.4. Starší IntelliJ (2025.2 a dřív) jejich třídy
  nepřečte a v `Arena`, `Turnaj` a `SpustGui` hlásí *Cannot resolve symbol* – stačí aktualizovat IntelliJ.
  Překlad přes Gradle funguje vždy. Boty se to netýká (Bot API je čistá Java).
