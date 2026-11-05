# Návod pro učitele: jak rozjet arénu ve škole

Cíl: jeden počítač (u projektoru) je **server + aréna**, studenti připojují své boty ze svých
notebooků přes síť. Kód botů zůstává u studentů – mezi bitvami ho můžou upravovat.

```
 notebook studenta ──┐
 notebook studenta ──┼──  ws://IP-UČITELE:7654  ──▶  server + GUI (projektor)
 notebook studenta ──┘
```

Ověřeno 2026-11-04 na Tank Royale 1.4.0: boti připojení přes síťovou IP i přes ngrok tunel
odehráli bitvu, bot se špatným heslem byl odmítnut („Invalid secret“).

---

## 1. Příprava (den předem)

1. Na počítači u projektoru: **JDK 17+** (`java -version`).
2. Stáhni projekt (`git clone …` nebo ZIP) a v jeho složce jednou spusť:
   ```bash
   ./gradlew setup verifyBots
   ```
   (Windows: `gradlew.bat setup verifyBots`). Poprvé se stahuje Gradle a knihovny – chce to internet.
3. Vymysli **heslo** pro arénu, např. `delta2026`. Bez něj by se ti do arény mohl připojit kdokoli v síti.
4. Zjisti **IP adresu** svého počítače:
   - macOS: `ipconfig getifaddr en0`
   - Windows: `ipconfig` → „IPv4 Address“
   - Linux: `hostname -I`
5. **Otestuj síť**: na jiném počítači ve stejné Wi-Fi zkus připojit bota (krok 3 níž). Pokud se nepřipojí,
   viz *Řešení problémů*.

## 2. Spuštění arény

### a) Server
```bash
./gradlew server -Psecret=delta2026
```
Server poslouchá na portu **7654** na všech síťových rozhraních. Nech terminál otevřený.

### b) GUI na projektor
V druhém terminálu:
```bash
./gradlew gui
```
V GUI nastav, ať používá náš server (stačí jednou):
**Server → Server Options → Remote server** → přidej `ws://localhost:7654`,
jako *Controller Secret* i *Bot Secret* zadej heslo (`delta2026`) a zaškrtni *Use remote server*.

> Alternativa bez samostatného serveru: v GUI **Server → Start Local Server** a v *Server Options*
> zapni *Enable server secrets*. GUI pak heslo vygeneruje samo – musíš ho studentům opsat.

### c) Studenti připojí boty
Na tabuli napiš:
```
./gradlew connect -Pbot=JMENO_BOTA -Purl=ws://IP-UČITELE:7654 -Psecret=delta2026
```
V GUI je uvidíš v **Battle → Start Battle** v seznamu **Joined Bots (local/remote)**.

### d) Bitva
V *Start Battle* přidej připojené boty do bitvy (můžeš přidat i místní ze složky `bots/`) → **Start**.
Po bitvě zůstávají boti připojení, takže hned můžeš pustit další.

### e) Žebříček bez GUI (volitelné)
Rychlá melee všech připojených botů s výsledky v HTML (`vysledky/`):
```bash
./gradlew arena -Psecret=delta2026 -Prounds=10
```
Počká, až stiskneš Enter (= všichni jsou připojení), odehraje bitvu a vypíše tabulku.
`-Pwait=30` místo Enteru odstartuje automaticky po 30 s.

## 3. Přes internet (studenti z domova / blokovaná Wi-Fi)

Server vystavíš tunelem. **Ověřený je ngrok** (potřebuje bezplatný účet a `ngrok config add-authtoken …`):
```bash
ngrok http 7654
```
Vypíše adresu `https://xxxx.ngrok-free.dev` – studenti použijí **`wss://`**:
```
./gradlew connect -Pbot=JMENO_BOTA -Purl=wss://xxxx.ngrok-free.dev -Psecret=delta2026
```

Cloudflare quick tunnel (`cloudflared tunnel --url http://localhost:7654`) by měl fungovat taky,
ale v našem testu vracel 404 (požadavky se k serveru vůbec nedostaly) – pokud ho chceš používat,
vyzkoušej ho předem na své síti.

**Důležité – zvyš limit na tah.** Bot má na odpověď v každém tahu jen **30 ms**. Přes tunel to nestíhá
a „zamrzá“. V našem testu měl bot přes ngrok při 30 ms poškození 26, při 200 ms 162.
- V GUI: *Start Battle → Setup Rules → Turn timeout* nastav na **200000** µs.
- V aréně: `./gradlew arena -Psecret=delta2026 -Ptimeout=200000`

Tunnel vystavuje server na veřejnou adresu → **vždy s heslem** a po akci tunel vypni.

## 4. Den otevřených dveří – scénář

1. **Úvod:** melee všech botů v GUI (TPS 30–60).
2. **Jak bot přemýšlí:** `Stopar` vs `Tanecnik`, u obou klikni na bota → *Toggle Graphical Debugging*
   (predikce dráhy, zaměřovač, „vlny“ výstřelů).
3. **Zahraj si sám:** přidej `Hrac` + pár slabších botů (`SpinBot`, `Corners`). Otevře se okno *Ovladač* –
   návštěvník do něj klikne: WASD jízda, Q/E dělo, mezerník střelba, M asistent míření.
4. **Boti studentů:** studenti připojí boty ze svých notebooků, bitva na projektoru, nakonec `./gradlew arena`
   a HTML žebříček.
5. **Ukaž kód:** `bots/Sablona/Sablona.java` – „tohle je celý bot“.

## 5. Řešení problémů

| Problém | Řešení |
|---|---|
| Bot: `Could not create web socket` / timeout | Špatná IP nebo port, server neběží, nebo firewall. Zkus `ping IP-UČITELE`. |
| Bot: `Invalid secret` | Špatné heslo v `-Psecret`. |
| Ping jde, připojení ne | Firewall na počítači učitele: povol příchozí spojení pro **Javu** (macOS: Nastavení → Síť → Firewall; Windows: Defender Firewall → povolit Java/port 7654). |
| Ping nejde mezi notebooky | Školní Wi-Fi má *izolaci klientů*. Požádej správce o výjimku, použij vlastní router/hotspot, nebo tunel (kap. 3). |
| Bot v aréně stojí / skipuje tahy | Pomalá síť – zvyš *Turn timeout*. Nebo bot dělá v jednom tahu moc výpočtů. |
| Bot se v GUI neobjeví | Student nespustil `connect`, nebo GUI nemá nastavený náš server (kap. 2b). |
| `./gradlew` nejde spustit (macOS/Linux) | `chmod +x gradlew` |
