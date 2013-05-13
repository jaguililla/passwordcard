// P A C K A G E ///////////////////////////////////////////////////////////////////////////////////
package co.warizmi.passwordcard;

// I M P O R T /////////////////////////////////////////////////////////////////////////////////////

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Properties;

import static java.lang.System.*;

// C L A S S ///////////////////////////////////////////////////////////////////////////////////////
public class SwingApp {
    private static final String TRAY_IMAGE = "/icon-square.png";
    private static final String DEFAULT_PROPERTIES = "/passwordcard.properties";
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
                new SwingApp (config).run ();
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

    JDialog mWnd = new JDialog ();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize ();

    private SwingApp (String aConfigProperties) throws IOException {
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
            InputStream stream = SwingApp.class.getResourceAsStream (DEFAULT_PROPERTIES);
            mConfig.load (new InputStreamReader (stream));
            props = DEFAULT_PROPERTIES;
        }
        mOntop = Boolean.valueOf (mConfig.getProperty ("ontop"));
        mTransparency = Integer.valueOf (mConfig.getProperty ("transparency"));
        mRounded = Boolean.valueOf (mConfig.getProperty ("rounded"));

        mCard = new Card (props);
        createWindow ();
    }

    private void createWindow () {
        mWnd = new JDialog ();
        mWnd.setAlwaysOnTop (true);
        mWnd.getContentPane ().setLayout (new GridLayout (0, 29, 0, 0));
        for (int ii = 0; ii < 29 * 9; ii++)
            mWnd.add (new JLabel (String.valueOf (ii)));
        mWnd.pack();
        mWidth = mWnd.getWidth ();
        mHeight = mWnd.getHeight ();
        mWnd.addWindowFocusListener (new WindowAdapter () {
            @Override public void windowLostFocus (WindowEvent e) {
                toggleDialog ();
            }
        });
        mWnd.addMouseListener (new MouseAdapter () {
            @Override
            public void mouseClicked (MouseEvent e) {
                toggleDialog ();
            }
        });
    }

    private void toggleDialog () {
        if (mWnd.isVisible ()) {
            mWnd.dispose ();
        }
        else {
            Point mouseLocation = MouseInfo.getPointerInfo().getLocation();

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
            mWnd.setLocation (x, y);
            mWnd.setVisible (true);
        }
    }

    private void run () {
        SwingUtilities.invokeLater (new Runnable () {
            @Override public void run () {
                try {
                    final SystemTray systemTray = SystemTray.getSystemTray ();
                    Dimension d = systemTray.getTrayIconSize ();
                    Image img = ImageIO.read (SwingApp.class.getResource(TRAY_IMAGE))
                        .getScaledInstance (d.width - 2, d.height - 2,  Image.SCALE_SMOOTH);

                    TrayIcon trayIcon = new TrayIcon (img, "Press middle button to exit");
                    trayIcon.addMouseListener (new MouseAdapter () {
                        @Override public void mouseClicked (MouseEvent e) {
                            if (e.getButton ()== MouseEvent.BUTTON2)
                                System.exit (0);
                            else
                                toggleDialog ();
                        }
                    });

                    systemTray.add (trayIcon);
                }
                catch (Exception e) {
                    e.printStackTrace ();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        });
    }
}
// E O F ///////////////////////////////////////////////////////////////////////////////////////////
