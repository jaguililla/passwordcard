/*
 * This file is part of PasswordCard.
 *
 * PasswordCard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PasswordCard is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PasswordCard.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright © 2010 pepsoft.org.
 */

package org.pepsoft.util;

import static java.lang.Long.*;
import static java.lang.System.*;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.pepsoft.passwordcard.PasswordCard;

/**
 * A simple command line tool to generate PasswordCards.
 * @author pepsoft.org
 */
public class Tool {
    private static final String USAGE =
        "Syntax exception. Usage: " +
        "java -jar PasswordCard.jar <card number> [ --digitArea ] [ -- includeSymbols ]";

    static long parseUnsignedHexLong (String str) {
        str = str.trim ();
        if (str.length () > 16)
            throw new IllegalArgumentException ();

        StringBuilder sb = new StringBuilder (16);
        for (int i = str.length (); i < 16; i++)
            sb.append ('0');

        sb.append (str);
        String paddedStr = sb.toString ();
        return
            parseLong (paddedStr.substring (0, 8), 16) << 32
            | parseLong (paddedStr.substring (8), 16);
    }

    public static void main (String[] args) throws IOException {
        if (args.length == 0) {
            out.println (USAGE);
            exit (-1);
        }

        setOut (new PrintStream (new FileOutputStream (FileDescriptor.out), true, "UTF-8"));

        boolean digits = false;
        boolean symbols = false;

        for (int i = 1; i < args.length; i++)
            if (args[i].equals ("--digitArea"))
                digits = true;
            else if (args[i].equals ("--includeSymbols"))
                symbols = true;
            else
                throw new IllegalArgumentException (args[i]);

        PasswordCard card = new PasswordCard (parseUnsignedHexLong (args[0]), digits, symbols);
        for (char[] row : card.getGrid ())
            out.println (row);
    }
}