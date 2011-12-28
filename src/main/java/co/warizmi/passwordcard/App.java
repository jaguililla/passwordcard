// P A C K A G E ///////////////////////////////////////////////////////////////////////////////////
package co.warizmi.passwordcard;

// I M P O R T /////////////////////////////////////////////////////////////////////////////////////
import static java.lang.System.*;
import static org.eclipse.swt.SWT.*;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Properties;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

// C L A S S ///////////////////////////////////////////////////////////////////////////////////////
public class App {
    private static final String TRAY_IMAGE = "/icon.png";
    private static final String DEFAULT_PROPERTIES = "/passwordcard.properties";

    public static void main (String[] aArgs) {
        try {
            String config = DEFAULT_PROPERTIES;
            boolean html = false;
            boolean tray = false;

            for (String arg : aArgs)
                if (!arg.startsWith ("-"))
                    config = arg;
                else if (arg.equals ("--html"))
                    html = true;
                else if (arg.equals ("--tray"))
                    tray = true;
                else
                    throw new IllegalArgumentException (arg);

            if (tray) {
                new App (config).run ();
            }
            else {
                setOut (new PrintStream (new FileOutputStream (FileDescriptor.out), true, "UTF-8"));
                Card card = new Card (config);
                out.println (html? card.toHtml () : card.toString ());
            }
        }
        catch (Exception e) {
            err.println ("Unhandled exception. Closing application");
            exit (-1);
        }
    }

    Display mDisplay;
    Shell mCardWindow;
    TrayItem mCardTray;
    Image mImage;
    Properties mConfig;

    private App (String aConfigProperties) throws IOException {
        super ();
        mDisplay = new Display ();
        mCardTray = createTray ();
        mCardWindow = createWindow ();
        mConfig = new Properties ();
        try {
            // TODO Check property existence properly
            mConfig.load (new FileReader (aConfigProperties));
        }
        catch (IOException e) {
            InputStream stream = App.class.getResourceAsStream (DEFAULT_PROPERTIES);
            mConfig.load (new InputStreamReader (stream));
        }
    }

    private TrayItem createTray () {
        final Tray tray = mDisplay.getSystemTray ();

        if (tray == null)
            throw new IllegalStateException ("The system tray is not available");

        mImage = new Image (mDisplay, App.class.getResourceAsStream (TRAY_IMAGE));


        final TrayItem item = new TrayItem (tray, NONE);
        item.setToolTipText ("Password Card Tray");
        item.setImage (mImage);

        item.addListener (Selection, new Listener () {
            @Override
            public void handleEvent (Event event) {
                mCardWindow.setVisible (!mCardWindow.getVisible ());
            }
        });

        item.addListener (DefaultSelection, new Listener () {
            @Override
            public void handleEvent (Event event) {
                item.dispose ();
            }
        });

        return item;
    }

    private Shell createWindow () {
        final Shell shell = new Shell (mDisplay, NO_TRIM);
        shell.setLayout (new FillLayout ());
        shell.setAlpha (196);
        shell.setImage (mImage);
        shell.setText ("Password Card");
        shell.setSize (550, 250);
//        Region r = new Region ();
//        shell.setRegion (r);

        Browser browser = new Browser (shell, NONE);
        browser.setUrl ("file:///home/jam/Projects/oss/passwordcard/src/main/resources/template.html");
        browser.setJavascriptEnabled (false);
        browser.setEnabled (false);

        return shell;
    }

    private void run () {
        while (!mCardTray.isDisposed ())
            if (!mDisplay.readAndDispatch ())
                mDisplay.sleep ();

        mCardWindow.dispose ();
        mImage.dispose ();
        mDisplay.dispose ();
    }
}
// E O F ///////////////////////////////////////////////////////////////////////////////////////////
