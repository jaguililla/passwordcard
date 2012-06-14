// P A C K A G E ///////////////////////////////////////////////////////////////////////////////////
package co.warizmi.passwordcard;

// I M P O R T /////////////////////////////////////////////////////////////////////////////////////
import static java.lang.System.*;
import static org.eclipse.swt.SWT.*;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Properties;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

// C L A S S ///////////////////////////////////////////////////////////////////////////////////////
public class App {
    private static final String TRAY_IMAGE = "/icon.png";
    private static final String DEFAULT_PROPERTIES = "/passwordcard.properties";
    private static final String JS_GET_TABLE = "return document.getElementsByTagName ('table')[0]";
    private static final int POPUP_MARGIN = 10;

    public static void main(String[] aArgs) {
        try {
            String config = DEFAULT_PROPERTIES;
            boolean html = false;
            boolean print = false;

            for (String arg : aArgs)
                if (!arg.startsWith ("-"))
                    config = arg;
                else if (arg.equals ("--html"))
                    html = true;
                else if (arg.equals ("--print"))
                    print = true;
                else
                    throw new IllegalArgumentException (arg);

            if (print) {
                setOut (new PrintStream (new FileOutputStream (FileDescriptor.out), true, "UTF-8"));
                Card card = new Card (config);
                out.println (html? card.toHtml () : card.toString ());
            }
            else {
                new App (config).run ();
            }
        }
        catch (Exception e) {
            err.println ("Unhandled exception. Closing application");
            e.printStackTrace ();
            exit (-1);
        }
    }

    Properties mConfig = new Properties ();
    Card mCard;
    boolean mOntop, mRounded;
    int mTransparency, mWidth, mHeight;

    Display mDisplay;
    Image mImage;
    TrayItem mCardTray;
    Shell mCardWindow;
    Browser mBrowser;

    private App (String aConfigProperties) throws IOException {
        super ();
        String props = aConfigProperties;
        try {
            File f = new File (aConfigProperties);
            if (!f.exists () || f.isDirectory ()) {
                f = new File ("passwordcard.properties");
                props = !f.exists () || f.isDirectory ()?
                    System.getProperty ("user.home") + "/.passwordcard" :
                    "passwordcard.properties";
            }
            mConfig.load (new FileReader (props));
        }
        catch (IOException e) {
            InputStream stream = App.class.getResourceAsStream (DEFAULT_PROPERTIES);
            mConfig.load (new InputStreamReader (stream));
            props = DEFAULT_PROPERTIES;
        }
        mOntop = Boolean.valueOf (mConfig.getProperty ("ontop"));
        mTransparency = Integer.valueOf (mConfig.getProperty ("transparency"));
        mRounded = Boolean.valueOf (mConfig.getProperty ("rounded"));

        mCard = new Card (props);
        mDisplay = new Display ();
        mCardTray = createTray ();
        mCardWindow = createWindow ();
        mBrowser = createBrowser ();
    }

    void toggleWindow () {
        if (mCardWindow.isVisible ()) {
            Shell wnd = mCardWindow;
            mCardWindow = createWindow ();
            mBrowser.setParent (mCardWindow);
            wnd.close ();
        }
        else {
            if (mWidth == 0 || mHeight == 0) {
                mCardWindow.open (); // We need to open to know the size
                mWidth =
                    ((Double)mBrowser.evaluate (JS_GET_TABLE + ".clientWidth")).intValue () + 2;
                mHeight =
                    ((Double)mBrowser.evaluate (JS_GET_TABLE + ".clientHeight")).intValue () + 2;
                mCardWindow.setSize (mWidth, mHeight);
            }
            Rectangle screenSize = mDisplay.getBounds ();
            Point mouseLocation = mDisplay.getCursorLocation ();

            // Calculate the topleft coordinates depending of the quadrant
            int cursorX = mouseLocation.x, cursorY = mouseLocation.y;
            int screenWidth = screenSize.width, screenHeight = screenSize.height;
            boolean right = cursorX > screenWidth / 2, bottom = cursorY > screenHeight / 2;
            int dX = right? screenWidth - cursorX : cursorX;
            int dY = bottom? screenHeight - cursorY : cursorY;
            int x = 0, y = 0;
//            System.out.println (
//                "Screen (" + screenWidth + ", " + screenHeight +
//                ") Size (" + mWidth + ", " + mHeight +
//                ") Click (" + cursorX + ", " + cursorY +
//                ") D (" + dX + ", " + dY + ")");

            // If taskbar horizontal (center x, margin y)
            // TODO This way of finding out is not accurate at all! allow to set this in properties
            if (dY < dX) {
                y = bottom? cursorY - mHeight - POPUP_MARGIN : cursorY + POPUP_MARGIN;
                x = cursorX - (mWidth / 2);
                if (!right && x < POPUP_MARGIN)
                    x = POPUP_MARGIN;
                else if (right && x + mWidth > screenWidth - POPUP_MARGIN)
                    x = screenWidth - POPUP_MARGIN - mWidth;
            }
            else { // If taskbar vertical (center y, margin x)
                x = right? cursorX - mWidth - POPUP_MARGIN : cursorX + POPUP_MARGIN;
                y = cursorY - (mHeight / 2);
                if (!bottom && y < POPUP_MARGIN)
                    y = POPUP_MARGIN;
                else if (bottom && y + mHeight > screenHeight - POPUP_MARGIN)
                    y = screenHeight - POPUP_MARGIN - mHeight;
            }

            mCardWindow.setLocation (x, y);
            mCardWindow.open ();
        }
    }

    private Browser createBrowser () throws IOException {
        Browser browser = new Browser (mCardWindow, NONE);
        browser.setText (mCard.toHtml ());
        browser.setJavascriptEnabled (false);
        browser.setEnabled (false);
        browser.addMouseListener (new MouseAdapter () {
            @Override public void mouseUp (MouseEvent aArg0) {
                toggleWindow ();
            }
        });
        return browser;
    }

    private TrayItem createTray () {
        final Tray tray = mDisplay.getSystemTray ();

        if (tray == null)
            throw new IllegalStateException ("The system tray is not available");

        mImage = new Image (mDisplay, App.class.getResourceAsStream (TRAY_IMAGE));

        final TrayItem item = new TrayItem (tray, NONE);
        item.setToolTipText ("Password Card Tray\nPress CTRL + Click to exit");
        item.setImage (mImage);

        item.addSelectionListener (new SelectionListener () {
            @Override public void widgetDefaultSelected (SelectionEvent aEvent) {
                widgetSelected (aEvent);
            }

            @Override public void widgetSelected (SelectionEvent aEvent) {
                if (aEvent.stateMask == CTRL)
                    item.dispose ();
                else
                    toggleWindow ();
            }
        });

        return item;
    }

    private Shell createWindow () {
        final Shell shell = new Shell (mDisplay, mOntop? NO_TRIM | ON_TOP : NO_TRIM);
        shell.setLayout (new FillLayout ());
        shell.setAlpha (mTransparency);
        shell.setImage (mImage);
        shell.setText ("Password Card");
        shell.setSize (mWidth, mHeight);
        if (mRounded) {
            Region r = new Region ();
            shell.setRegion (r);
        }

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
