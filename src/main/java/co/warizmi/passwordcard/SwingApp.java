// P A C K A G E ///////////////////////////////////////////////////////////////////////////////////
package co.warizmi.passwordcard;

// I M P O R T /////////////////////////////////////////////////////////////////////////////////////

import static java.lang.System.*;
import static javax.swing.SwingUtilities.invokeLater;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

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
    long mFocusLost;

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

    private JLabel createLabel (String aValue, Color aBg, Color aFg) {
        JLabel label = new JLabel (aValue) {
            @Override public void paint (Graphics aG) {
                Graphics2D g2d = (Graphics2D)aG;
                g2d.setRenderingHint (
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2d.setRenderingHint (
                    RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                super.paint (g2d);
            }
        };
        if (aBg != null) {
            label.setOpaque (true);
            label.setBackground (aBg);
        }
        if (aFg != null)
            label.setForeground (aFg);
        label.setBorder (new EmptyBorder (1, 4, 1, 4));
        label.setFont (Card.FONT);
        label.setHorizontalAlignment (SwingConstants.CENTER);
        return label;
    }

    private JPanel createGrid (Card aCard, boolean aShowLineNumbers) {
        int w = aShowLineNumbers? Card.WIDTH : Card.WIDTH + 1;
        JPanel pnlGrid = new JPanel (new GridLayout (Card.HEIGHT, w, 0, 0));
        pnlGrid.setBackground (Color.WHITE);
        char[] grid = aCard.getGrid ();

        // Header
        if (aShowLineNumbers)
            pnlGrid.add (createLabel (" ", null, null));

        for (int ii = 0; ii < Card.WIDTH; ii++)
            pnlGrid.add (createLabel (String.valueOf (grid[ii]), null, Color.DARK_GRAY));

        // Data
        for (int ii = Card.WIDTH; ii < grid.length; ii++) {
            int r = ii / Card.WIDTH;
            if (aShowLineNumbers && ii % Card.WIDTH == 0)
                pnlGrid.add (createLabel (String.valueOf (r), null, Color.DARK_GRAY));
            pnlGrid.add (createLabel (String.valueOf (grid[ii]), Card.COLORS[r - 1], null));
        }

        return pnlGrid;
    }

    private void createWindow () {
        mWnd = new JDialog ();
        mWnd.setUndecorated (true);
        mWnd.getRootPane ().setBorder (new BevelBorder (BevelBorder.RAISED));
        mWnd.setAlwaysOnTop (mOntop);
        mWnd.add (createGrid (mCard, true), BorderLayout.CENTER);
        mWnd.pack();
        mWnd.addWindowFocusListener (new WindowAdapter () {
            @Override
            public void windowLostFocus (WindowEvent e) {
                mWnd.dispose ();
                mFocusLost = currentTimeMillis ();
            }
        });
        mWnd.addMouseListener (new MouseAdapter () {
            @Override
            public void mouseClicked (MouseEvent e) {
                mWnd.dispose ();
            }
        });

        mWidth = mWnd.getWidth ();
        mHeight = mWnd.getHeight ();
    }

    private void showDialog () {
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();

        // Calculate the topleft coordinates depending of the quadrant
        int cursorX = mouseLocation.x, cursorY = mouseLocation.y;
        int screenWidth = screenSize.width, screenHeight = screenSize.height;
        boolean right = cursorX > screenWidth / 2, bottom = cursorY > screenHeight / 2;
        int dX = right? screenWidth - cursorX : cursorX;
        int dY = bottom? screenHeight - cursorY : cursorY;
        int x, y;

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

    private void run () {
        invokeLater (new Runnable () {
            @Override public void run () {
                try {
                    final SystemTray systemTray = SystemTray.getSystemTray ();

                    Dimension d = systemTray.getTrayIconSize ();
                    Image img = ImageIO.read (SwingApp.class.getResource (TRAY_IMAGE))
                        .getScaledInstance (d.width - 2, d.height - 2, Image.SCALE_SMOOTH);

                    TrayIcon trayIcon = new TrayIcon (img, "Press middle button to exit");
                    trayIcon.addMouseListener (new MouseAdapter () {
                        @Override public void mouseClicked (MouseEvent e) {
                            /*
                             * If focus lost and click are below the threadhold are the "same"
                             * event
                             */
                            long elapsedTime = currentTimeMillis () - mFocusLost;
                            if (e.getButton ()== MouseEvent.BUTTON2)
                                exit (0);
                            else {
                                if (!SwingApp.this.mWnd.isVisible () && elapsedTime > 200)
                                    showDialog ();
                            }
                        }
                    });

                    systemTray.add (trayIcon);
                }
                catch (Exception e) {
                    e.printStackTrace ();
                }
            }
        });
    }
}
// E O F ///////////////////////////////////////////////////////////////////////////////////////////
