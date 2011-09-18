package org.pepsoft.util;

import static java.awt.GraphicsDevice.WindowTranslucency.*;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JDialog;

import org.pepsoft.passwordcard.PasswordCard;

/**
 * TODO Shadow, shape, tray icon location, hide taskbar button.
 * @author jam
 */
public class CardFrame extends JDialog {
    private static final long serialVersionUID = -9057030187624431810L;
    private static Toolkit sTk = Toolkit.getDefaultToolkit ();

    private static CardFrame mInstance;

    public static void toggle (PasswordCard aCard) {
        if (mInstance == null)
            mInstance = new CardFrame (aCard);

        // setVisible (!isVisible ()) don't work properly
        if (mInstance.isVisible ())
            mInstance.dispose ();
        else {
            mInstance.setVisible (true);
            Dimension size = sTk.getScreenSize ();
            Insets sSize = sTk.getScreenInsets (mInstance.getGraphicsConfiguration ());
            mInstance.setLocation (
                sSize.left, size.height - sSize.bottom - mInstance.getHeight ());
        }
    }

    private final PasswordCard mCard;

    private CardFrame (PasswordCard aCard) {
        if (aCard == null)
            throw new IllegalArgumentException ();

        mCard = aCard;

        setUndecorated (true);
        setFont (new Font ("DejaVu Sans Mono", Font.PLAIN, 20));
        setIconImage (sTk.getImage ((CardTray.iconUrl)));
        setTitle ("Password Card");
        Dimension sSize = sTk.getScreenSize ();
        setSize (500, 200);
        setLocation (
            (sSize.width - getWidth ()) / 2,
            (sSize.height - getHeight ()) / 2);
        setShape (new RoundRectangle2D.Float (0, 0, 500, 200, 20, 20));
        GraphicsDevice device = getGraphicsConfiguration ().getDevice ();

        System.out.println (
            "> TRANSLUCENT: " + device.isWindowTranslucencySupported (TRANSLUCENT) +
            "> PIXEL TRANSLUCENT: " + device.isWindowTranslucencySupported (PERPIXEL_TRANSLUCENT) +
            "> PIXEL TRANSPARENT: " + device.isWindowTranslucencySupported (PERPIXEL_TRANSPARENT));

        boolean ontop = true;
        if (ontop)
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
        for (char[] row: mCard.getGrid())
            g.drawChars (row, 0, row.length, 10, ((fm.getMaxAscent () + 1) * ii++) + 20);

        g.dispose ();
    }
}
