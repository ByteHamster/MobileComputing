package com.bytehamster.controller;

import android.content.Context;
import android.util.Log;

public class Vibrator {
    private static final String[] morseAlphabet = {".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....",
            "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-",
            "...-", ".--", "-..-", "-.--", "--.."};

    private int patternCounter = -1;
    private Context c;
    private String packageName;
    private static final int DURATION = 200;
    private static final int DURATION_LONG = 2000;
    private boolean hasNext = true;
    private boolean useMorse;
    private String morseTitle = "";
    private int lastDuration = 0;
    private boolean charPause = false;

    public Vibrator(String packagename, String title, Context context) {
        this.c = context;
        this.packageName = packagename;

        if (c.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt(packageName, 0) == 0) {
            hasNext = false;
        }
        useMorse = c.getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean("MORSE" + packageName, false);


        Log.d("Title", new String(title.toUpperCase().toCharArray()));
        for (char cc : title.toUpperCase().toCharArray()) {
            int c = cc - 'A';
            if (c >= morseAlphabet.length || c < 0) {
                morseTitle = morseTitle.concat("  ");
            } else {
                morseTitle = morseTitle.concat(" " + morseAlphabet[c]);
            }
        }
        Log.d("Title", morseTitle);
    }

    public int nextPattern() {
        patternCounter++;
        try { Thread.sleep(DURATION); } catch (Exception ignore) { }
        switch (patternCounter) {
            case 0: return 0b1000;
            case 1: return 0b0000;
            case 2: return 0b0100;
            case 3: return 0b0000;
            case 4: return 0b0010;
            case 5: return 0b0000;
            case 6: return 0b0001;
            case 7: return 0b0000;
            case 8: return c.getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt(packageName, 0b0000);
            case 9: try { Thread.sleep(DURATION_LONG); } catch (Exception ignore) { }
                if (!useMorse) {
                    hasNext = false;
                }
                return 0b0000;
        }

        // Morse
        if (charPause) {
            charPause = false;
            try { Thread.sleep(lastDuration); } catch (Exception ignore) { }
            return 0b0000;
        }

        if (morseTitle.equals("")) {
            hasNext = false;
            return 0b0000;
        }

        char c = morseTitle.charAt(0);
        Log.d("Title", "Executing " + c);
        morseTitle = morseTitle.substring(1);
        charPause = true;
        if (c == '.') {
            lastDuration = 0;
            return 0b1000;
        } else if(c == '-') {
            lastDuration = 300;
            return 0b1000;
        } else if(c == ' ') {
            lastDuration = 100;
            return 0b0000;
        }
        return 0b0000;
    }

    public boolean hasPattern() {
        return hasNext;
    }
}
