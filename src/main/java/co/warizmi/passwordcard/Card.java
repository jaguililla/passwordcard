package co.warizmi.passwordcard;

import static java.awt.Font.PLAIN;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;
import static java.lang.System.*;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;

public class Card {
    private static final int WIDTH = 29, HEIGHT = 9;
    private static final int BODY_HEIGHT = HEIGHT - 1, HALF_HEIGHT = 1 + (BODY_HEIGHT / 2);

    private static final String LOWERCASE_LETTERS = "abcdefghjkmnpqrstuvwxyz";
    private static final String UPPERCASE_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    public static final String HEADER_CHARS = "■□▲△○●★☂☀☁☹☺♠♣♥♦♫€¥£$!?¡¿⊙◐◩�";
    public static final String DIGITS_LETTERS = "23456789" + LOWERCASE_LETTERS + UPPERCASE_LETTERS;
    public static final String DIGITS_LETTERS_SYMBOLS = DIGITS_LETTERS + "@#$%&*<>?€+{}[]()/\\";

    public static final String HTML_TEMPLATE = "/template.html";
    public static final Font FONT = new Font ("FreeMono", PLAIN, 10);
    public static final Color [] COLORS = {
        new Color (0xFFFFFF), // White
        new Color (0xC0C0C0), // Gray
        new Color (0xFFC0C0), // Red
        new Color (0xC0FFC0), // Green
        new Color (0xFFFFC0), // Yellow
        new Color (0xC0C0FF), // Blue
        new Color (0xFFC0FF), // Magenta
        new Color (0xC0FFFF)  // Cyan
    };

    private static long parseUnsignedHex (String aHex) {
        if (aHex == null || (aHex = aHex.trim ()).length () < 1 || aHex.length () > 16)
            throw new IllegalArgumentException ();

        int idx = aHex.length () - 8;
        return idx > 0?
            (parseLong (aHex.substring (0, idx), 16) << 32) | parseLong (aHex.substring (idx), 16) :
            parseLong (aHex, 16);
    }

    private final long mNumber;
    private final boolean mDigitArea, mIncludeSymbols;
    private char[] mGrid;

    public Card (boolean aDigitArea, boolean aIncludeSymbols) {
        this (new SecureRandom ().nextLong (), aDigitArea, aIncludeSymbols);
    }

    public Card (long aNumber, boolean aDigitArea, boolean aIncludeSymbols) {
        mNumber = aNumber;
        mDigitArea = aDigitArea;
        mIncludeSymbols = aIncludeSymbols;
    }

    public Card (String aConfigProperties) throws IOException {
        Properties config = new Properties ();
        try {
            // TODO Check property existence properly
            config.load (new FileReader (aConfigProperties));
        }
        catch (IOException e) {
            config.load (new InputStreamReader (Class.class.getResourceAsStream (aConfigProperties)));
        }

        mNumber = parseUnsignedHex (config.getProperty ("seed"));
        mDigitArea = parseBoolean (config.getProperty ("digits"));
        mIncludeSymbols = parseBoolean (config.getProperty ("symbols"));
    }

    public Card (String aHexNumber, boolean aDigitArea, boolean aIncludeSymbols) {
        this (parseUnsignedHex (aHexNumber), aDigitArea, aIncludeSymbols);
    }

    private void generateGrid () {
        Random rnd = new Random (mNumber);
        mGrid = new char[HEIGHT * WIDTH];

        arraycopy (shuffle (HEADER_CHARS.toCharArray (), rnd), 0, mGrid, 0, WIDTH);

        // TODO Change '% WIDTH' by '- 1' or somothing like that
        for (int ii = WIDTH; ii < (mDigitArea? HALF_HEIGHT : HEIGHT) * WIDTH; ii++)
            mGrid[ii] = (mIncludeSymbols && (((ii % WIDTH) % 2) == 0))?
                DIGITS_LETTERS_SYMBOLS.charAt (rnd.nextInt (DIGITS_LETTERS_SYMBOLS.length ())) :
                DIGITS_LETTERS.charAt (rnd.nextInt (DIGITS_LETTERS.length ()));

        if (mDigitArea)
            for (int y = HALF_HEIGHT * WIDTH; y < HEIGHT * WIDTH; y++)
                mGrid[y] = Character.forDigit (rnd.nextInt (10), 10);
    }

    /**
     * Private implementation of Collections.shuffle() algorithm, because the Android core classes
     * implement it differently.
     * @param list The list of characters to shuffle.
     * @param rnd The pseudorandom generator instance to use for the shuffle.
     */
    private char[] shuffle (char[] aList, Random aRnd) {
        for (int ii = aList.length; ii > 1; ii--)
            swap (aList, ii - 1, aRnd.nextInt (ii));

        return aList;
    }

    private void swap (char[] aList, int aI, int aJ) {
        char tmp = aList[aI];
        aList[aI] = aList[aJ];
        aList[aJ] = tmp;
    }

    public char[] getGrid () {
        if (mGrid == null)
            generateGrid ();

        return mGrid;
    }

    public long getNumber () {
        return mNumber;
    }

    public String readResource(InputStream aInput) throws IOException {
        if (aInput == null)
            throw new IllegalArgumentException ("Can't read a 'null' stream");

        Writer writer = new StringWriter();
        try {
            char[] buffer = new char[1024];
            Reader reader = new BufferedReader(new InputStreamReader(aInput));
            int n;
            while ((n = reader.read(buffer)) != -1)
                writer.write(buffer, 0, n);
        }
        finally {
            aInput.close();
        }

        return writer.toString();
    }

    public String toHtml () throws IOException {
        return toHtml (true);
    }

    public String toHtml (boolean aShowLineNumber) throws IOException {
        String eol = lineSeparator ();
        StringBuilder buffer = new StringBuilder ();
        char[] grid = getGrid ();

        // Header
        if (aShowLineNumber)
            buffer.append ("\t\t\t<th></th>").append (eol);
        for (int ii = 0; ii < WIDTH; ii++)
            buffer.append ("\t\t\t<th>").append (grid [ii]).append ("</th>").append (eol);

        // Load template
        String template = readResource (App.class.getResourceAsStream (HTML_TEMPLATE));
        template = template.replace ("<?header?>", buffer);
        buffer.delete (0, buffer.length ());

        // Rows
        for (int ii = WIDTH; ii < grid.length; ii++) {
            if (ii % WIDTH == 0) {
                buffer.append ("\t\t<tr>").append (eol);

                if (aShowLineNumber && ii % WIDTH == 0)
                    buffer.append ("\t\t\t<th>").append (ii / WIDTH).append ("</th>").append (eol);
            }

            buffer.append ("\t\t\t<td>").append (grid [ii]).append ("</td>").append (eol);

            if ( (ii + 1) % WIDTH == 0)
                buffer.append ("\t\t</tr>").append (eol);
        }

        return template.replace ("<?rows?>", buffer);
    }

    @Override
    public String toString () {
        return toString (false, false);
    }

    public String toString (boolean aShowLineNumber, boolean aPadLineNumber) {
        String eol = lineSeparator ();
        int lineLength = WIDTH + eol.length ();
        lineLength += (aShowLineNumber? 1 : 0) + (aPadLineNumber? 1 : 0);
        StringBuilder buffer = new StringBuilder (HEIGHT * lineLength);
        char[] grid = getGrid ();

        for (int ii = 0; ii < grid.length; ii += WIDTH) {
            if (aShowLineNumber) {
                buffer.append (ii > 0? Character.forDigit (ii, 10) : ' ');
                if (aPadLineNumber)
                    buffer.append (' ');
            }
            buffer.append (grid, ii, WIDTH);
            buffer.append (eol);
        }

        return buffer.toString ();
    }
}
