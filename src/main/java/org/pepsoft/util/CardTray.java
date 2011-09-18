package org.pepsoft.util;

import static java.awt.SystemTray.*;
import static java.awt.Toolkit.*;
import static java.awt.event.MouseEvent.*;
import static java.lang.System.*;
import static javax.swing.UIManager.*;

import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import org.pepsoft.passwordcard.PasswordCard;

public class CardTray extends TrayIcon {
    private static CardTray mInstance;
    static URL iconUrl = Class.class.getResource ("/passwordcard32.png");

    private static void start (PasswordCard aCard) {
        try {
            setLookAndFeel (getSystemLookAndFeelClassName ());

            if (aCard == null)
                throw new IllegalArgumentException ();

            getSystemTray ().add (mInstance == null? mInstance = new CardTray (aCard) : mInstance);
        }
        catch (Exception e) {
            err.println ("TrayIcon could not be added.");
            e.printStackTrace();
        }
    }

    public static void main (String[] args) throws IOException {
        setOut (new PrintStream (new FileOutputStream (FileDescriptor.out), true, "UTF-8"));

        long seed = Tool.parseUnsignedHexLong (args[0]);
        boolean digitArea = false;
        boolean includeSymbols = false;

        for (int i = 1; i < args.length; i++)
            if (args[i].equals ("--digitArea"))
                digitArea = true;
            else if (args[i].equals ("--includeSymbols"))
                includeSymbols = true;
            else
                throw new IllegalArgumentException (args[i]);

        PasswordCard passwordCard = new PasswordCard (seed, digitArea, includeSymbols);
        if (isSupported ())
            CardTray.start (passwordCard);
        else
            err.println ("Tray icon not supported!");
    }

    final PasswordCard mCard;

    private CardTray (PasswordCard aCard) {
        super (getDefaultToolkit ().getImage (iconUrl));

        mCard = aCard;

        // TODO resize image manually to avoid visual artifacts
        setImageAutoSize (true);

        addMouseListener (new MouseAdapter() {
            @Override
            public void mouseClicked (MouseEvent aE) {
                int btn = aE.getButton ();
                if (btn == BUTTON1)
                    CardFrame.toggle (mCard);
                else if (btn == BUTTON2)
                    exit (0);
            }
        });
    }
}
