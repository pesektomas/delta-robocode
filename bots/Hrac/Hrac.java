import dev.robocode.tankroyale.botapi.*;
import dev.robocode.tankroyale.botapi.events.*;
import dev.robocode.tankroyale.botapi.graphics.Color;
import dev.robocode.tankroyale.botapi.graphics.IGraphics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

// ---------------------------------------------------------------------------
// Hrac – tank, kterého řídíš klávesnicí. Zkus porazit boty našich studentů!
// ---------------------------------------------------------------------------
// Po spuštění bitvy se otevře okno „Ovladač“. Klikni do něj a hraj:
//   W A S D / šipky ... jízda a zatáčení
//   Q / E ............. otáčení dělem
//   mezerník .......... výstřel
//   M ................. zapnout/vypnout asistenta míření
// ---------------------------------------------------------------------------
public class Hrac extends Bot {

    // Stav kláves – zapisuje ho okno ovladače, čte herní smyčka
    static volatile boolean vpred, vzad, vlevo, vpravo, delVlevo, delVpravo, strilet;
    static volatile boolean asistent = true;

    static JLabel stav;

    double cilX, cilY;
    int naposledyVidenVTahu = -100;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Hrac::otevriOvladac);
        new Hrac().start();
    }

    @Override
    public void run() {
        setBodyColor(Color.fromRgb(0xff, 0xd3, 0x3d));
        setTurretColor(Color.fromRgb(0x0d, 0x11, 0x17));
        setRadarColor(Color.fromRgb(0xff, 0xff, 0xff));
        setBulletColor(Color.fromRgb(0xff, 0xff, 0xff));
        setScanColor(Color.fromRgba(0xff, 0xff, 0xff, 0x40));
        setAdjustGunForBodyTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForBodyTurn(true);

        while (isRunning()) {
            setTargetSpeed(vpred ? Constants.MAX_SPEED : vzad ? -Constants.MAX_SPEED : 0);
            setTurnRate(vlevo ? Constants.MAX_TURN_RATE : vpravo ? -Constants.MAX_TURN_RATE : 0);

            // Radar se pořád točí, aby asistent věděl, kde jsou soupeři
            setRadarTurnRate(Constants.MAX_RADAR_TURN_RATE);

            boolean vidim = getTurnNumber() - naposledyVidenVTahu < 10;
            if (delVlevo || delVpravo) {
                setGunTurnRate(delVlevo ? Constants.MAX_GUN_TURN_RATE : -Constants.MAX_GUN_TURN_RATE);
            } else if (asistent && vidim) {
                setGunTurnRate(gunBearingTo(cilX, cilY));
            } else {
                setGunTurnRate(0);
            }

            if (strilet && getGunHeat() == 0) {
                setFire(2);
            }

            nakresliMiridla(vidim);
            aktualizujStav();
            go();
        }
    }

    @Override
    public void onScannedBot(ScannedBotEvent e) {
        // Asistent míří na nejbližšího viděného soupeře
        boolean stary = getTurnNumber() - naposledyVidenVTahu > 2;
        if (stary || distanceTo(e.getX(), e.getY()) <= distanceTo(cilX, cilY)) {
            cilX = e.getX();
            cilY = e.getY();
            naposledyVidenVTahu = e.getTurnNumber();
        }
    }

    void nakresliMiridla(boolean vidim) {
        IGraphics g = getGraphics();
        // Kam míří dělo
        double dx = Math.cos(Math.toRadians(getGunDirection()));
        double dy = Math.sin(Math.toRadians(getGunDirection()));
        g.setStrokeWidth(2);
        g.setStrokeColor(Color.fromRgba(0xff, 0xff, 0xff, 0x70));
        g.drawLine(getX() + dx * 30, getY() + dy * 30, getX() + dx * 600, getY() + dy * 600);
        if (asistent && vidim) {
            g.setStrokeColor(Color.fromRgb(0xff, 0xd3, 0x3d));
            g.drawCircle(cilX, cilY, 28);
        }
        g.setFillColor(Color.fromRgb(0xff, 0xd3, 0x3d));
        g.setFont("Arial", 16);
        g.drawText("TY", getX() - 10, getY() + 30);
    }

    void aktualizujStav() {
        if (stav == null) return;
        String text = String.format("<html><center>Energie: <b>%.0f</b><br>Asistent míření: <b>%s</b></center></html>",
                getEnergy(), asistent ? "ZAP" : "VYP");
        SwingUtilities.invokeLater(() -> stav.setText(text));
    }

    // ---------------------------------------------------------------- ovladač

    static void otevriOvladac() {
        JFrame okno = new JFrame("Ovladač – Hráč");
        okno.setAlwaysOnTop(true);
        okno.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(new java.awt.Color(0x0d1117));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel nadpis = new JLabel("<html><center><b style='font-size:20px'>🎮 Klikni sem a hraj!</b></center></html>",
                SwingConstants.CENTER);
        nadpis.setForeground(new java.awt.Color(0xffd33d));

        JLabel napoveda = new JLabel("<html><table style='font-size:14px'>"
                + "<tr><td><b>W A S D</b> / šipky</td><td>jízda</td></tr>"
                + "<tr><td><b>Q / E</b></td><td>dělo</td></tr>"
                + "<tr><td><b>mezerník</b></td><td>výstřel</td></tr>"
                + "<tr><td><b>M</b></td><td>asistent míření</td></tr></table></html>");
        napoveda.setForeground(new java.awt.Color(0xe6edf3));

        stav = new JLabel("Čekám na start bitvy…", SwingConstants.CENTER);
        stav.setForeground(new java.awt.Color(0x7ee787));
        stav.setFont(stav.getFont().deriveFont(16f));

        panel.add(nadpis, BorderLayout.NORTH);
        panel.add(napoveda, BorderLayout.CENTER);
        panel.add(stav, BorderLayout.SOUTH);
        okno.setContentPane(panel);
        okno.pack();
        okno.setLocation(40, 40);
        okno.setVisible(true);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            boolean dole = e.getID() == KeyEvent.KEY_PRESSED;
            if (!dole && e.getID() != KeyEvent.KEY_RELEASED) return false;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W, KeyEvent.VK_UP -> vpred = dole;
                case KeyEvent.VK_S, KeyEvent.VK_DOWN -> vzad = dole;
                case KeyEvent.VK_A, KeyEvent.VK_LEFT -> vlevo = dole;
                case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> vpravo = dole;
                case KeyEvent.VK_Q -> delVlevo = dole;
                case KeyEvent.VK_E -> delVpravo = dole;
                case KeyEvent.VK_SPACE -> strilet = dole;
                case KeyEvent.VK_M -> { if (dole) asistent = !asistent; }
                default -> { return false; }
            }
            return true;
        });
    }
}
