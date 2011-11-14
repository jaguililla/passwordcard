package co.warizmi.passwordcard;

import static java.awt.SystemTray.getSystemTray;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.event.MouseEvent.*;
import static java.lang.System.*;
import static javax.swing.UIManager.*;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

public class CardTray extends TrayIcon {
    static final URL iconUrl = Class.class.getResource ("/passwordcard.png");
    private static final String DEFAULT_PROPERTIES = "/passwordcard.properties";
    private static final String USAGE =
        "Syntax exception. Usage: java -jar PasswordCard.jar <config file>";

    private static CardTray mInstance;

    public static void main (String[] args) {
        if (args.length > 1) {
            err.println (USAGE);
            exit (-1);
        }
        if (!SystemTray.isSupported ()) {
            err.println ("Tray icon not supported!");
            exit (-2);
        }

        try {
            // TODO Check property existence properly
            Properties config = new Properties ();
            try {
                config.load (new FileReader (args.length == 1? args[0] : DEFAULT_PROPERTIES));
            }
            catch (IOException e) {
                config.load (new InputStreamReader (Class.class.getResourceAsStream (
                    args.length == 1? args[0] : DEFAULT_PROPERTIES)));
            }

            setLookAndFeel (getSystemLookAndFeelClassName ());

            getSystemTray ().add (
                mInstance == null? mInstance = new CardTray (config) : mInstance);
        }
        catch (Exception e) {
            err.println ("TrayIcon could not be added.");
            e.printStackTrace();
        }
    }

    final CardFrame mFrame;

    private CardTray (Properties aConfiguration) {
        super (getDefaultToolkit ().getImage (iconUrl));

        mFrame = new CardFrame (aConfiguration);

        // TODO resize image manually to avoid visual artifacts
        setImageAutoSize (true);

        addMouseListener (new MouseAdapter() {
            @Override
            public void mouseClicked (MouseEvent aE) {
                int btn = aE.getButton ();
                if (btn == BUTTON1)
                    mFrame.toggle ();
                else if (btn == BUTTON2)
                    exit (0);
            }
        });
    }
}
