// P A C K A G E ///////////////////////////////////////////////////////////////////////////////////
package co.warizmi.passwordcard;

// I M P O R T /////////////////////////////////////////////////////////////////////////////////////
import static java.lang.System.out;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

// C L A S S ///////////////////////////////////////////////////////////////////////////////////////
public class PasswordCardApp {
    public static void run () {
        PasswordCardApp sCardApp = new PasswordCardApp ();
    }

    private Shell mCardWindow;
    private TrayItem mCardTray;

    private PasswordCardApp () {
        super ();
        mCardWindow = createWindow ();
        mCardTray = createTray ();
    }

    private Shell createWindow () {
        return null;
    }

    private TrayItem createTray () {
        return null;
    }

    public static void main (String[] args) {
        final Display display = new Display ();
        final Tray tray = display.getSystemTray ();

        if (tray == null)
            out.println ("The system tray is not available");
        else {
            final Image image = new Image (display, PasswordCardApp.class.getResourceAsStream ("/passwordcard.png"));

            final TrayItem item = new TrayItem (tray, SWT.NONE);
            item.setToolTipText ("SWT TrayItem");
            item.setImage (image);

            item.addListener (SWT.Selection, new Listener () {
                @Override
                public void handleEvent (Event event) {
                    out.println ("selection");
                }
            });
            item.addListener (SWT.DefaultSelection, new Listener () {
                @Override
                public void handleEvent (Event event) {
                    out.println ("default selection");
                    item.dispose ();
                }
            });

            while (!item.isDisposed ())
                if (!display.readAndDispatch ())
                    display.sleep ();

            image.dispose ();
        }
        display.dispose ();
    }
}
// E O F ///////////////////////////////////////////////////////////////////////////////////////////
