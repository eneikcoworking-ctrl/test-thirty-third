package com.leadgen.bot.service;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpintaxParser {

    private static final Random DEFAULT_RANDOM = new Random();
    private static final Pattern SPINTAX_PATTERN = Pattern.compile("\\{([^{}]*)\\}");

    /**
     * Parses a spintax string containing expressions like "{option1|option2}"
     * and returns a randomized variation. It handles nested spintax patterns recursively
     * from the inside out.
     *
     * @param template The spintax template string
     * @return A randomized version of the template
     */
    public static String parse(String template) {
        return parse(template, DEFAULT_RANDOM);
    }

    /**
     * Parses a spintax string containing expressions like "{option1|option2}"
     * using the provided Random instance.
     *
     * @param template The spintax template string
     * @param random Custom Random instance for predictability in tests
     * @return A randomized version of the template
     */
    public static String parse(String template, Random random) {
        if (template == null) {
            return "";
        }
        String result = template;
        while (true) {
            Matcher matcher = SPINTAX_PATTERN.matcher(result);
            if (!matcher.find()) {
                break;
            }
            String group = matcher.group(1);
            String[] options = group.split("\\|", -1);
            String selection = options[random.nextInt(options.length)];

            // Build the replacement without regex group replacement issues (e.g. if selection contains $ or \)
            result = result.substring(0, matcher.start()) + selection + result.substring(matcher.end());
        }
        return result;
    }
}
