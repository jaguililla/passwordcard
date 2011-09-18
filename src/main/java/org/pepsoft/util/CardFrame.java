package org.pepsoft.util;

import static java.awt.Color.*;
import static java.lang.Boolean.*;
import static java.lang.Integer.*;
import static org.pepsoft.util.Tool.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Properties;

import javax.swing.JDialog;

import org.pepsoft.passwordcard.PasswordCard;

/**
 * TODO Shadow, shape, tray icon location, hide taskbar button.
 * @author jam
 */
public class CardFrame extends JDialog {
    private static final long serialVersionUID = -9057030187624431810L;
    private static final Toolkit sTk = Toolkit.getDefaultToolkit ();
    private static final Dimension sSize = sTk.getScreenSize ();
//    private static final Insets sInsets = sTk.getScreenInsets (getGraphicsConfiguration ());
    private static final Color [] COLORS = { WHITE, GRAY, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN };

    private final PasswordCard mCard;

    CardFrame (Properties aConfiguration) {
        if (aConfiguration == null)
            throw new IllegalArgumentException ();

        mCard = new PasswordCard (
            parseUnsignedHexLong (aConfiguration.getProperty ("seed")),
            parseBoolean (aConfiguration.getProperty ("digits")),
            parseBoolean (aConfiguration.getProperty ("symbols")));

        String fstyle = aConfiguration.getProperty ("font.style").trim ();
        int style = Font.PLAIN;
        if (fstyle.equals ("BOLD"))
            style = Font.BOLD;
        if (fstyle.equals ("ITALIC"))
            style = Font.ITALIC;

        setFont (new Font (
            aConfiguration.getProperty ("font.face"),
            style,
            parseInt (aConfiguration.getProperty ("font.size"))));

        setSize (500, 200);
        setLocation (
            (sSize.width - getWidth ()) / 2,
            (sSize.height - getHeight ()) / 2);

        setUndecorated (true);
        setIconImage (sTk.getImage ((CardTray.iconUrl)));
        setTitle ("Password Card");
        setShape (new RoundRectangle2D.Float (0, 0, 500, 200, 20, 20));

        if (parseBoolean (aConfiguration.getProperty ("ontop")))
            setAlwaysOnTop (true);
        else
            addWindowListener (new WindowAdapter () {
                @Override
                public void windowDeactivated (WindowEvent aE) {
                    dispose ();
                }
            });
    }

    @Override
    public void paint (Graphics aG) {
        Graphics2D g = (Graphics2D)aG;
        g.setRenderingHint (
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        FontMetrics fm = aG.getFontMetrics ();

        int ii = 0;
        for (char[] row: mCard.getGrid()) {
            g.setBackground (COLORS[ii % COLORS.length]);
            g.drawChars (row, 0, row.length, 10, ((fm.getMaxAscent () + 1) * ii++) + 20);
        }

        g.dispose ();
    }

    public void toggle () {
        // setVisible (!isVisible ()) don't work properly
        if (isVisible ())
            dispose ();
        else
            setVisible (true);
    }
}
