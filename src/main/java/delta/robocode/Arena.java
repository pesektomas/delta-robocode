package delta.robocode;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.robocode.tankroyale.runner.BattleSetup;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * „Rozhodčí“ pro boty připojené přes síť: připojí se k běžícímu serveru, ukazuje, kdo se připojil,
 * po stisku Enter odstartuje melee všech připojených botů a vypíše žebříček.
 *
 * <pre>
 *   ./gradlew arena -Psecret=TAJNE                          # server na tomto počítači
 *   ./gradlew arena -Purl=ws://10.0.0.5:7654 -Psecret=TAJNE -Prounds=5 -Ptimeout=150000
 * </pre>
 *
 * Runner z Tank Royale umí jen boty, které si sám spustí – proto vlastní controller přes WebSocket protokol.
 */
public class Arena {

    private static final Gson GSON = new Gson();

    private final String url;
    private final String heslo;
    private final AtomicReference<JsonArray> boti = new AtomicReference<>(new JsonArray());
    private final CompletableFuture<JsonObject> konecHry = new CompletableFuture<>();
    private WebSocket controller;

    Arena(String url, String heslo) {
        this.url = url;
        this.heslo = heslo;
    }

    public static void main(String[] args) throws Exception {
        String url = System.getProperty("arena.url", "ws://localhost:7654");
        String heslo = System.getProperty("arena.heslo", "");
        int kola = Integer.parseInt(System.getProperty("arena.kola", "10"));
        int timeout = Integer.parseInt(System.getProperty("arena.timeout", "0"));
        int cekat = Integer.parseInt(System.getProperty("arena.cekat", "0"));

        Arena arena = new Arena(url, heslo);
        arena.pripoj();
        System.out.println("Připojeno k " + url + ". Čekám na boty…");

        if (cekat > 0) {
            System.out.println("Bitva začne automaticky za " + cekat + " s.");
            Thread.sleep(cekat * 1000L);
        } else {
            System.out.println("Až budou všichni připojení, stiskni Enter.");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        }

        JsonArray pripojeni = arena.boti.get();
        if (pripojeni.size() < 2) {
            System.err.println("Na bitvu jsou potřeba aspoň 2 připojení boti, je jich " + pripojeni.size() + ".");
            System.exit(1);
        }
        arena.start(kola, timeout, pripojeni);

        JsonObject vysledek = arena.konecHry.get(2, TimeUnit.HOURS);
        if (!"GameEndedEventForObserver".equals(vysledek.get("type").getAsString())) {
            System.err.println("Bitva byla přerušena.");
            System.exit(1);
        }

        List<Turnaj.Skore> poradi = new ArrayList<>();
        for (JsonElement el : vysledek.getAsJsonArray("results")) {
            JsonObject r = el.getAsJsonObject();
            Turnaj.Skore s = new Turnaj.Skore(r.get("name").getAsString());
            s.body = r.get("totalScore").getAsInt();
            s.poskozeni = r.get("bulletDamage").getAsInt() + r.get("ramDamage").getAsInt();
            s.preziti = r.get("survival").getAsInt();
            s.vitezstvi = r.get("firstPlaces").getAsInt();
            poradi.add(s);
        }
        poradi.sort(Comparator.comparingInt((Turnaj.Skore s) -> s.body).reversed());
        Turnaj.vypis(poradi, "melee");
        Path vysledky = Path.of("vysledky");
        Files.createDirectories(vysledky);
        System.out.println("\nVýsledky uloženy do " + Turnaj.ulozHtml(poradi, "melee", kola, vysledky).toAbsolutePath());
        System.exit(0);
    }

    void pripoj() throws InterruptedException {
        CountDownLatch pripraveno = new CountDownLatch(2);
        controller = otevri("ControllerHandshake", pripraveno);
        otevri("ObserverHandshake", pripraveno);
        if (!pripraveno.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Server na " + url + " neodpověděl (běží? sedí heslo?)");
        }
    }

    void start(int kola, int timeout, JsonArray pripojeni) {
        BattleSetup preset = BattleSetup.melee();
        JsonObject setup = new JsonObject();
        setup.addProperty("gameType", preset.getGameType().getDisplayName());
        setup.addProperty("arenaWidth", preset.getArenaWidth());
        setup.addProperty("isArenaWidthLocked", false);
        setup.addProperty("arenaHeight", preset.getArenaHeight());
        setup.addProperty("isArenaHeightLocked", false);
        setup.addProperty("minNumberOfParticipants", 2);
        setup.addProperty("isMinNumberOfParticipantsLocked", false);
        setup.addProperty("isMaxNumberOfParticipantsLocked", false);
        setup.addProperty("numberOfRounds", kola);
        setup.addProperty("isNumberOfRoundsLocked", false);
        setup.addProperty("gunCoolingRate", preset.getGunCoolingRate());
        setup.addProperty("isGunCoolingRateLocked", false);
        setup.addProperty("maxInactivityTurns", preset.getMaxInactivityTurns());
        setup.addProperty("isMaxInactivityTurnsLocked", false);
        setup.addProperty("turnTimeout", timeout > 0 ? timeout : preset.getTurnTimeoutMicros());
        setup.addProperty("isTurnTimeoutLocked", false);
        setup.addProperty("readyTimeout", preset.getReadyTimeoutMicros());
        setup.addProperty("isReadyTimeoutLocked", false);
        setup.addProperty("defaultTurnsPerSecond", preset.getDefaultTurnsPerSecond());

        JsonArray adresy = new JsonArray();
        for (JsonElement b : pripojeni) {
            JsonObject bot = b.getAsJsonObject();
            JsonObject adresa = new JsonObject();
            adresa.add("host", bot.get("host"));
            adresa.add("port", bot.get("port"));
            adresy.add(adresa);
        }

        JsonObject zprava = new JsonObject();
        zprava.addProperty("type", "StartGame");
        zprava.add("gameSetup", setup);
        zprava.add("botAddresses", adresy);
        System.out.println("Startuji melee: " + pripojeni.size() + " botů, " + kola + " kol, timeout tahu "
                + setup.get("turnTimeout").getAsInt() / 1000 + " ms");
        controller.sendText(GSON.toJson(zprava), true);
    }

    private WebSocket otevri(String handshake, CountDownLatch pripraveno) {
        WebSocket.Listener listener = new WebSocket.Listener() {
            final StringBuilder buffer = new StringBuilder();

            @Override
            public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                buffer.append(data);
                if (last) {
                    zpracuj(ws, GSON.fromJson(buffer.toString(), JsonObject.class), handshake, pripraveno);
                    buffer.setLength(0);
                }
                ws.request(1);
                return null;
            }
        };
        return HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(url), listener).join();
    }

    private void zpracuj(WebSocket ws, JsonObject msg, String handshake, CountDownLatch pripraveno) {
        String typ = msg.get("type").getAsString();
        boolean observer = "ObserverHandshake".equals(handshake);
        switch (typ) {
            case "ServerHandshake" -> {
                JsonObject odpoved = new JsonObject();
                odpoved.addProperty("type", handshake);
                odpoved.add("sessionId", msg.get("sessionId"));
                odpoved.addProperty("name", "Delta Arena");
                odpoved.addProperty("version", "1.0");
                odpoved.addProperty("secret", heslo);
                ws.sendText(GSON.toJson(odpoved), true);
                pripraveno.countDown();
            }
            case "BotListUpdate" -> {
                if (!observer) return;
                JsonArray seznam = msg.getAsJsonArray("bots");
                boti.set(seznam);
                StringJoiner jmena = new StringJoiner(", ");
                for (JsonElement b : seznam) {
                    JsonObject bot = b.getAsJsonObject();
                    jmena.add(bot.get("name").getAsString() + " (" + bot.get("host").getAsString() + ")");
                }
                System.out.println("Připojení boti [" + seznam.size() + "]: " + jmena);
            }
            case "RoundStartedEvent" -> {
                if (observer) System.out.println("  kolo " + msg.get("roundNumber").getAsInt());
            }
            case "GameEndedEventForObserver", "GameAbortedEvent" -> {
                if (observer) konecHry.complete(msg);
            }
            default -> { }
        }
    }
}
