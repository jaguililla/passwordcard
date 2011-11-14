package co.warizmi.passwordcard;

import static java.lang.System.*;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Tool {
    private static final String DEFAULT_PROPERTIES = "/passwordcard.properties";

    public static void main (String[] aArgs) throws IOException {
        String config = null;
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

        }
        else {
            setOut (new PrintStream (new FileOutputStream (FileDescriptor.out), true, "UTF-8"));
            PasswordCard card = new PasswordCard (config == null? aArgs[0] : DEFAULT_PROPERTIES);
            out.println (html? card.toHtml () : card.toString ());
        }
    }
}
