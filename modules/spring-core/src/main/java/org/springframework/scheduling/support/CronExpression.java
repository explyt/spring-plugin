package org.springframework.scheduling.support;

import java.util.Objects;

/**
 * Original spring code. (spring-context-5.3.29.jar)
 * Copied from org.springframework.scheduling.support.CronExpression
 */
public final class CronExpression {

    static final int MAX_ATTEMPTS = 366;

    public static final String[] MACROS = new String[]{
            "@yearly", "0 0 0 1 1 *",
            "@annually", "0 0 0 1 1 *",
            "@monthly", "0 0 0 1 * *",
            "@weekly", "0 0 0 * * 0",
            "@daily", "0 0 0 * * *",
            "@midnight", "0 0 0 * * *",
            "@hourly", "0 0 * * * *"
    };

    public static void parse(String expression) {
        Objects.requireNonNull(expression, "Expression string must not be empty");

        expression = resolveMacros(expression);

        String[] fields = SpringStringUtils.tokenizeToStringArray(expression, " ", true, true);
        if (fields.length != 6) {
            throw new IllegalArgumentException(String.format(
                    "Cron expression must consist of 6 fields (found %d in \"%s\")", fields.length, expression));
        }
        try {
            CronField.parseSeconds(fields[0]);
            CronField.parseMinutes(fields[1]);
            CronField.parseHours(fields[2]);
            CronField.parseDaysOfMonth(fields[3]);
            CronField.parseMonth(fields[4]);
            CronField.parseDaysOfWeek(fields[5]);
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage() + " in cron expression \"" + expression + "\"";
            throw new IllegalArgumentException(msg, ex);
        }
    }

    /**
     * Determine whether the given string represents a valid cron expression.
     *
     * @param expression the expression to evaluate
     * @return {@code true} if the given expression is a valid cron expression
     * @since 5.3.8
     */
    public static boolean isValidExpression(String expression) {
        if (expression == null) {
            return false;
        }
        try {
            parse(expression);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }


    private static String resolveMacros(String expression) {
        expression = expression.trim();
        for (int i = 0; i < MACROS.length; i = i + 2) {
            if (MACROS[i].equalsIgnoreCase(expression)) {
                return MACROS[i + 1];
            }
        }
        return expression;
    }

}
