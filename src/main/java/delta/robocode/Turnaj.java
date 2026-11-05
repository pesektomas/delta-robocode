package delta.robocode;

import dev.robocode.tankroyale.runner.BattleResults;
import dev.robocode.tankroyale.runner.BattleRunner;
import dev.robocode.tankroyale.runner.BattleSetup;
import dev.robocode.tankroyale.runner.BotEntry;
import dev.robocode.tankroyale.runner.BotResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Odehraje turnaj všech botů ze složky bots/ bez grafiky (co nejrychleji) a vypíše žebříček.
 *
 * <pre>
 *   ./gradlew tournament                     # melee: všichni proti všem v jedné aréně, 10 kol
 *   ./gradlew tournament --args="melee 20"   # melee, 20 kol
 *   ./gradlew tournament --args="duel 5"     # každý s každým 1 na 1, 5 kol na zápas
 * </pre>
 *
 * Výsledky se uloží i jako HTML do složky vysledky/ a záznamy bitev do vysledky/zaznamy/
 * (dají se přehrát v GUI přes Battle → Replay from File...).
 */
public class Turnaj {

    /** Boti, kteří do turnaje nepatří (ovládání klávesnicí, šablona). */
    private static final Set<String> VYNECHAT = Set.of("Hrac", "Sablona");

    public static void main(String[] args) throws Exception {
        String rezim = args.length > 0 ? args[0] : "melee";
        int kola = args.length > 1 ? Integer.parseInt(args[1]) : ("duel".equals(rezim) ? 5 : 10);
        Logger.getLogger("dev.robocode.tankroyale").setLevel(Level.WARNING);

        Path botsDir = Path.of(System.getProperty("bots.dir", "bots"));
        List<Path> boti = najdiBoty(botsDir);
        if (boti.size() < 2) {
            System.err.println("Na turnaj jsou potřeba aspoň 2 boti, ve složce " + botsDir + " jsem našel " + boti.size() + ".");
            System.exit(1);
        }
        System.out.println("Turnaj (" + rezim + ", " + kola + " kol), boti: "
                + boti.stream().map(p -> p.getFileName().toString()).toList());

        Path vysledky = Path.of("vysledky");
        Path zaznamy = vysledky.resolve("zaznamy");
        Files.createDirectories(zaznamy);

        Map<String, Skore> tabulka = new LinkedHashMap<>();
        try (BattleRunner runner = BattleRunner.create(b -> b
                .embeddedServer()
                .suppressServerOutput()
                .enableRecording(zaznamy))) {

            if ("duel".equals(rezim)) {
                for (int i = 0; i < boti.size(); i++) {
                    for (int j = i + 1; j < boti.size(); j++) {
                        Path a = boti.get(i), c = boti.get(j);
                        System.out.printf("  %s vs %s ... ", a.getFileName(), c.getFileName());
                        BattleResults r = runner.runBattle(
                                BattleSetup.oneVsOne(s -> s.setNumberOfRounds(kola)),
                                List.of(BotEntry.of(a), BotEntry.of(c)));
                        BotResult vitez = r.getResults().stream().filter(x -> x.getRank() == 1).findFirst().orElseThrow();
                        System.out.println("vyhrál " + vitez.getName());
                        zapocitej(tabulka, r, true);
                    }
                }
            } else {
                BattleResults r = runner.runBattle(
                        BattleSetup.melee(s -> {
                            s.setNumberOfRounds(kola);
                            s.setMinNumberOfParticipants(2); // preset melee jinak chce aspoň 10 botů
                        }),
                        boti.stream().map(BotEntry::of).toList());
                zapocitej(tabulka, r, false);
            }
        }

        List<Skore> poradi = new ArrayList<>(tabulka.values());
        // V duelu rozhodují vyhrané zápasy, v melee celkové body (jako v oficiálních výsledcích Robocode)
        Comparator<Skore> podle = "duel".equals(rezim)
                ? Comparator.comparingInt((Skore s) -> s.vitezstvi).thenComparingInt(s -> s.body)
                : Comparator.comparingInt((Skore s) -> s.body);
        poradi.sort(podle.reversed());
        vypis(poradi, rezim);
        Path html = ulozHtml(poradi, rezim, kola, vysledky);
        System.out.println("\nVýsledky uloženy do " + html.toAbsolutePath());
    }

    static List<Path> najdiBoty(Path botsDir) throws IOException {
        try (Stream<Path> dirs = Files.list(botsDir)) {
            return dirs.filter(Files::isDirectory)
                    .filter(d -> !VYNECHAT.contains(d.getFileName().toString()))
                    .filter(d -> Files.exists(d.resolve(d.getFileName() + ".json")))
                    .filter(d -> Files.exists(d.resolve(d.getFileName() + ".java")))
                    .sorted()
                    .toList();
        }
    }

    /** Body z bitvy. V duelu se za výhru zápasu počítá 1 vítězství, v melee se počítají vyhraná kola. */
    static void zapocitej(Map<String, Skore> tabulka, BattleResults r, boolean duel) {
        for (BotResult b : r.getResults()) {
            Skore s = tabulka.computeIfAbsent(b.getName(), Skore::new);
            s.body += b.getTotalScore();
            s.poskozeni += b.getBulletDamage() + b.getRamDamage();
            s.preziti += b.getSurvival();
            s.vitezstvi += duel ? (b.getRank() == 1 ? 1 : 0) : b.getFirstPlaces();
        }
    }

    static void vypis(List<Skore> poradi, String rezim) {
        String vyhry = "duel".equals(rezim) ? "Výhry" : "1. místa";
        System.out.printf("%n%-4s %-24s %9s %8s %10s %8s%n", "#", "Bot", vyhry, "Body", "Poškození", "Přežití");
        int i = 1;
        for (Skore s : poradi) {
            System.out.printf("%-4s %-24s %9d %8d %10d %8d%n", i++ + ".", s.jmeno, s.vitezstvi, s.body, s.poskozeni, s.preziti);
        }
    }

    static Path ulozHtml(List<Skore> poradi, String rezim, int kola, Path vysledky) throws IOException {
        String cas = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        String vyhry = "duel".equals(rezim) ? "Výhry" : "1. místa";
        int maxBody = poradi.stream().mapToInt(s -> s.body).max().orElse(1);

        StringBuilder radky = new StringBuilder();
        int i = 1;
        for (Skore s : poradi) {
            String medaile = switch (i) { case 1 -> "🥇"; case 2 -> "🥈"; case 3 -> "🥉"; default -> i + "."; };
            radky.append("<tr><td class=pos>").append(medaile).append("</td><td class=name>").append(esc(s.jmeno))
                    .append("</td><td>").append(s.vitezstvi).append("</td><td><div class=bar><span style=\"width:")
                    .append(100L * s.body / Math.max(1, maxBody)).append("%\"></span><b>").append(s.body)
                    .append("</b></div></td><td>").append(s.poskozeni).append("</td><td>").append(s.preziti).append("</td></tr>\n");
            i++;
        }

        String html = """
                <!doctype html>
                <html lang="cs"><head><meta charset="utf-8"><title>Robocode turnaj</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                  body{margin:0;font-family:system-ui,sans-serif;background:#0d1117;color:#e6edf3;display:flex;justify-content:center}
                  main{width:min(900px,100%%);padding:32px 16px}
                  h1{font-size:2.4rem;margin:0}.sub{color:#8b949e;margin:4px 0 24px}
                  table{width:100%%;border-collapse:collapse;font-size:1.15rem}
                  th{text-align:left;color:#8b949e;font-weight:500;border-bottom:1px solid #30363d;padding:8px}
                  td{padding:10px 8px;border-bottom:1px solid #21262d}
                  .pos{font-size:1.5rem;width:3rem}.name{font-weight:600}
                  .bar{position:relative;background:#21262d;border-radius:6px;height:28px;min-width:140px}
                  .bar span{position:absolute;inset:0 auto 0 0;background:linear-gradient(90deg,#f78166,#ffa657);border-radius:6px}
                  .bar b{position:relative;padding-left:8px;line-height:28px}
                </style></head><body><main>
                <h1>🤖 Robocode turnaj</h1>
                <p class=sub>%s · %d kol · %s</p>
                <table><tr><th></th><th>Bot</th><th>%s</th><th>Body</th><th>Poškození</th><th>Přežití</th></tr>
                %s</table></main></body></html>
                """.formatted("duel".equals(rezim) ? "Každý s každým 1 na 1" : "Všichni proti všem",
                kola, cas.replace('_', ' '), vyhry, radky);

        Path soubor = vysledky.resolve("turnaj-" + cas + ".html");
        Files.writeString(soubor, html, StandardCharsets.UTF_8);
        return soubor;
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static class Skore {
        final String jmeno;
        int vitezstvi, body, poskozeni, preziti;

        Skore(String jmeno) {
            this.jmeno = jmeno;
        }
    }
}
