package org.springframework.scheduling.support;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.temporal.*;
import java.util.Objects;
import java.util.function.BiFunction;


/**
 * Original spring code. (spring-context-5.3.29.jar)
 * Copied from:
 * org.springframework.scheduling.support.CronField
 * org.springframework.scheduling.support.BitsCronField
 * org.springframework.scheduling.support.QuartzCronField
 */
abstract class CronField {

    private static final String[] MONTHS = new String[]{"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP",
            "OCT", "NOV", "DEC"};

    private static final String[] DAYS = new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    private final Type type;


    protected CronField(Type type) {
        this.type = type;
    }

    /**
     * Parse the given value into a seconds {@code CronField}, the first entry of a cron expression.
     */
    public static void parseSeconds(String value) {
        parseField(value, Type.SECOND);
    }

    /**
     * Parse the given value into a minutes {@code CronField}, the second entry of a cron expression.
     */
    public static void parseMinutes(String value) {
        parseField(value, Type.MINUTE);
    }

    /**
     * Parse the given value into an hours {@code CronField}, the third entry of a cron expression.
     */
    public static void parseHours(String value) {
        parseField(value, Type.HOUR);
    }

    /**
     * Parse the given value into a days of months {@code CronField}, the fourth entry of a cron expression.
     */
    public static void parseDaysOfMonth(String value) {
        if (!isQuartzDaysOfMonthField(value)) {
            parseDate(value, Type.DAY_OF_MONTH);
        } else {
            parseList(value, Type.DAY_OF_MONTH, (field, type) -> {
                if (isQuartzDaysOfMonthField(field)) {
                    parseDaysOfMonthQuartz(field);
                } else {
                    parseDate(field, Type.DAY_OF_MONTH);
                }
                return null;
            });
        }
    }

    public static boolean isQuartzDaysOfMonthField(String value) {
        return value.contains("L") || value.contains("W");
    }

    private static void parseDate(String value, Type type) {
        if (value.equals("?")) {
            value = "*";
        }
        parseField(value, type);
    }

    public static void parseDaysOfMonthQuartz(String value) {
        int idx = value.lastIndexOf('L');
        if (idx != -1) {
            TemporalAdjuster adjuster;
            if (idx != 0) {
                throw new IllegalArgumentException("Unrecognized characters before 'L' in '" + value + "'");
            } else if (value.length() == 2 && value.charAt(1) == 'W') { // "LW"
                adjuster = lastWeekdayOfMonth();
            } else {
                if (value.length() == 1) { // "L"
                    adjuster = lastDayOfMonth();
                } else { // "L-[0-9]+"
                    int offset = Integer.parseInt(value.substring(idx + 1));
                    if (offset >= 0) {
                        throw new IllegalArgumentException("Offset '" + offset + " should be < 0 '" + value + "'");
                    }
                    adjuster = lastDayWithOffset(offset);
                }
            }
            return;
        }
        idx = value.lastIndexOf('W');
        if (idx != -1) {
            if (idx == 0) {
                throw new IllegalArgumentException("No day-of-month before 'W' in '" + value + "'");
            } else if (idx != value.length() - 1) {
                throw new IllegalArgumentException("Unrecognized characters after 'W' in '" + value + "'");
            } else { // "[0-9]+W"
                int dayOfMonth = Integer.parseInt(value.substring(0, idx));
                dayOfMonth = Type.DAY_OF_MONTH.checkValidValue(dayOfMonth);
                TemporalAdjuster adjuster = weekdayNearestTo(dayOfMonth);
                return;
            }
        }
        throw new IllegalArgumentException("No 'L' or 'W' found in '" + value + "'");
    }

    private static TemporalAdjuster lastWeekdayOfMonth() {
        TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
        return temporal -> {
            Temporal lastDom = adjuster.adjustInto(temporal);
            Temporal result;
            int dow = lastDom.get(ChronoField.DAY_OF_WEEK);
            if (dow == 6) { // Saturday
                result = lastDom.minus(1, ChronoUnit.DAYS);
            } else if (dow == 7) { // Sunday
                result = lastDom.minus(2, ChronoUnit.DAYS);
            } else {
                result = lastDom;
            }
            return rollbackToMidnight(temporal, result);
        };
    }

    private static TemporalAdjuster lastDayOfMonth() {
        TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
        return temporal -> {
            Temporal result = adjuster.adjustInto(temporal);
            return rollbackToMidnight(temporal, result);
        };
    }

    private static TemporalAdjuster lastDayWithOffset(int offset) {
        if (!(offset < 0)) throw new IllegalArgumentException("Offset should be < 0");
        TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
        return temporal -> {
            Temporal result = adjuster.adjustInto(temporal).plus(offset, ChronoUnit.DAYS);
            return rollbackToMidnight(temporal, result);
        };
    }

    private static TemporalAdjuster weekdayNearestTo(int dayOfMonth) {
        return temporal -> {
            int current = Type.DAY_OF_MONTH.get(temporal);
            DayOfWeek dayOfWeek = DayOfWeek.from(temporal);

            if ((current == dayOfMonth && isWeekday(dayOfWeek)) || // dayOfMonth is a weekday
                    (dayOfWeek == DayOfWeek.FRIDAY && current == dayOfMonth - 1) || // dayOfMonth is a Saturday, so Friday before
                    (dayOfWeek == DayOfWeek.MONDAY && current == dayOfMonth + 1) || // dayOfMonth is a Sunday, so Monday after
                    (dayOfWeek == DayOfWeek.MONDAY && dayOfMonth == 1 && current == 3)) { // dayOfMonth is Saturday 1st, so Monday 3rd
                return temporal;
            }
            int count = 0;
            while (count++ < CronExpression.MAX_ATTEMPTS) {
                if (current == dayOfMonth) {
                    dayOfWeek = DayOfWeek.from(temporal);

                    if (dayOfWeek == DayOfWeek.SATURDAY) {
                        if (dayOfMonth != 1) {
                            temporal = temporal.minus(1, ChronoUnit.DAYS);
                        } else {
                            // exception for "1W" fields: execute on next Monday
                            temporal = temporal.plus(2, ChronoUnit.DAYS);
                        }
                    } else if (dayOfWeek == DayOfWeek.SUNDAY) {
                        temporal = temporal.plus(1, ChronoUnit.DAYS);
                    }
                    return atMidnight().adjustInto(temporal);
                } else {
                    temporal = Type.DAY_OF_MONTH.elapseUntil(cast(temporal), dayOfMonth);
                    current = Type.DAY_OF_MONTH.get(temporal);
                }
            }
            return null;
        };
    }

    private static TemporalAdjuster atMidnight() {
        return temporal -> {
            if (temporal.isSupported(ChronoField.NANO_OF_DAY)) {
                return temporal.with(ChronoField.NANO_OF_DAY, 0);
            } else {
                return temporal;
            }
        };
    }

    private static boolean isWeekday(DayOfWeek dayOfWeek) {
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * Parse the given value into a month {@code CronField}, the fifth entry of a cron expression.
     */
    public static CronField parseMonth(String value) {
        value = replaceOrdinals(value, MONTHS);
        parseField(value, Type.MONTH);
        return null;
    }

    private static Temporal rollbackToMidnight(Temporal current, Temporal result) {
        if (result.get(ChronoField.DAY_OF_MONTH) == current.get(ChronoField.DAY_OF_MONTH)) {
            return current;
        } else {
            return atMidnight().adjustInto(result);
        }
    }

    /**
     * Parse the given value into a days of week {@code CronField}, the sixth entry of a cron expression.
     */
    public static void parseDaysOfWeek(String value) {
        value = replaceOrdinals(value, DAYS);
        if (!isQuartzDaysOfWeekField(value)) {
            parseDate(value, Type.DAY_OF_WEEK);
        } else {
            parseList(value, Type.DAY_OF_WEEK, (field, type) -> {
                if (isQuartzDaysOfWeekField(field)) {
                    parseDaysOfWeekQuartz(field);
                } else {
                    parseDate(field, Type.DAY_OF_WEEK);
                }
                return null;
            });
        }
    }

    public static boolean isQuartzDaysOfWeekField(String value) {
        return value.contains("L") || value.contains("#");
    }

    public static void parseDaysOfWeekQuartz(String value) {
        int idx = value.lastIndexOf('L');
        if (idx != -1) {
            if (idx != value.length() - 1) {
                throw new IllegalArgumentException("Unrecognized characters after 'L' in '" + value + "'");
            } else {
                TemporalAdjuster adjuster;
                if (idx == 0) {
                    throw new IllegalArgumentException("No day-of-week before 'L' in '" + value + "'");
                } else { // "[0-7]L"
                    DayOfWeek dayOfWeek = parseDayOfWeek(value.substring(0, idx));
                    adjuster = lastInMonth(dayOfWeek);
                }
                return;
            }
        }
        idx = value.lastIndexOf('#');
        if (idx != -1) {
            if (idx == 0) {
                throw new IllegalArgumentException("No day-of-week before '#' in '" + value + "'");
            } else if (idx == value.length() - 1) {
                throw new IllegalArgumentException("No ordinal after '#' in '" + value + "'");
            }
            // "[0-7]#[0-9]+"
            DayOfWeek dayOfWeek = parseDayOfWeek(value.substring(0, idx));
            int ordinal = Integer.parseInt(value.substring(idx + 1));
            if (ordinal <= 0) {
                throw new IllegalArgumentException("Ordinal '" + ordinal + "' in '" + value +
                        "' must be positive number ");
            }

            TemporalAdjuster adjuster = dayOfWeekInMonth(ordinal, dayOfWeek);
            return;
        }
        throw new IllegalArgumentException("No 'L' or '#' found in '" + value + "'");
    }

    private static TemporalAdjuster lastInMonth(DayOfWeek dayOfWeek) {
        TemporalAdjuster adjuster = TemporalAdjusters.lastInMonth(dayOfWeek);
        return temporal -> {
            Temporal result = adjuster.adjustInto(temporal);
            return rollbackToMidnight(temporal, result);
        };
    }

    private static DayOfWeek parseDayOfWeek(String value) {
        int dayOfWeek = Integer.parseInt(value);
        if (dayOfWeek == 0) {
            dayOfWeek = 7; // cron is 0 based; java.time 1 based
        }
        try {
            return DayOfWeek.of(dayOfWeek);
        } catch (DateTimeException ex) {
            String msg = ex.getMessage() + " '" + value + "'";
            throw new IllegalArgumentException(msg, ex);
        }
    }

    private static TemporalAdjuster dayOfWeekInMonth(int ordinal, DayOfWeek dayOfWeek) {
        TemporalAdjuster adjuster = TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek);
        return temporal -> {
            Temporal result = adjuster.adjustInto(temporal);
            return rollbackToMidnight(temporal, result);
        };
    }


    private static void parseList(String value, Type type, BiFunction<String, Type, CronField> parseFieldFunction) {
        Objects.requireNonNull(value, "Value must not be empty");
        String[] fields = SpringStringUtils.delimitedListToStringArray(value, ",");
        for (int i = 0; i < fields.length; i++) {
            parseFieldFunction.apply(fields[i], type);
        }
    }

    private static String replaceOrdinals(String value, String[] list) {
        value = value.toUpperCase();
        for (int i = 0; i < list.length; i++) {
            String replacement = Integer.toString(i + 1);
            value = SpringStringUtils.replace(value, list[i], replacement);
        }
        return value;
    }

    protected Type type() {
        return this.type;
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Temporal & Comparable<? super T>> T cast(Temporal temporal) {
        return (T) temporal;
    }


    /**
     * Represents the type of cron field, i.e. seconds, minutes, hours,
     * day-of-month, month, day-of-week.
     */
    protected enum Type {
        NANO(ChronoField.NANO_OF_SECOND, ChronoUnit.SECONDS),
        SECOND(ChronoField.SECOND_OF_MINUTE, ChronoUnit.MINUTES, ChronoField.NANO_OF_SECOND),
        MINUTE(ChronoField.MINUTE_OF_HOUR, ChronoUnit.HOURS, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
        HOUR(ChronoField.HOUR_OF_DAY, ChronoUnit.DAYS, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
        DAY_OF_MONTH(ChronoField.DAY_OF_MONTH, ChronoUnit.MONTHS, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
        MONTH(ChronoField.MONTH_OF_YEAR, ChronoUnit.YEARS, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
        DAY_OF_WEEK(ChronoField.DAY_OF_WEEK, ChronoUnit.WEEKS, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND);


        private final ChronoField field;

        private final ChronoUnit higherOrder;

        private final ChronoField[] lowerOrders;


        Type(ChronoField field, ChronoUnit higherOrder, ChronoField... lowerOrders) {
            this.field = field;
            this.higherOrder = higherOrder;
            this.lowerOrders = lowerOrders;
        }


        /**
         * Return the value of this type for the given temporal.
         *
         * @return the value of this type
         */
        public int get(Temporal date) {
            return date.get(this.field);
        }

        /**
         * Return the general range of this type. For instance, this method
         * will return 0-31 for {@link #MONTH}.
         *
         * @return the range of this field
         */
        public ValueRange range() {
            return this.field.range();
        }

        /**
         * Check whether the given value is valid, i.e. whether it falls in
         * {@linkplain #range() range}.
         *
         * @param value the value to check
         * @return the value that was passed in
         * @throws IllegalArgumentException if the given value is invalid
         */
        public int checkValidValue(int value) {
            if (this == DAY_OF_WEEK && value == 0) {
                return value;
            } else {
                try {
                    return this.field.checkValidIntValue(value);
                } catch (DateTimeException ex) {
                    throw new IllegalArgumentException(ex.getMessage(), ex);
                }
            }
        }

        /**
         * Elapse the given temporal for the difference between the current
         * value of this field and the goal value. Typically, the returned
         * temporal will have the given goal as the current value for this type,
         * but this is not the case for {@link #DAY_OF_MONTH}.
         *
         * @param temporal the temporal to elapse
         * @param goal     the goal value
         * @param <T>      the type of temporal
         * @return the elapsed temporal, typically with {@code goal} as value
         * for this type.
         */
        public <T extends Temporal & Comparable<? super T>> T elapseUntil(T temporal, int goal) {
            int current = get(temporal);
            ValueRange range = temporal.range(this.field);
            if (current < goal) {
                if (range.isValidIntValue(goal)) {
                    return cast(temporal.with(this.field, goal));
                } else {
                    // goal is invalid, eg. 29th Feb, so roll forward
                    long amount = range.getMaximum() - current + 1;
                    return this.field.getBaseUnit().addTo(temporal, amount);
                }
            } else {
                long amount = goal + range.getMaximum() - current + 1 - range.getMinimum();
                return this.field.getBaseUnit().addTo(temporal, amount);
            }
        }

        /**
         * Roll forward the give temporal until it reaches the next higher
         * order field. Calling this method is equivalent to calling
         * {@link #elapseUntil(Temporal, int)} with goal set to the
         * minimum value of this field's range.
         *
         * @param temporal the temporal to roll forward
         * @param <T>      the type of temporal
         * @return the rolled forward temporal
         */
        public <T extends Temporal & Comparable<? super T>> T rollForward(T temporal) {
            T result = this.higherOrder.addTo(temporal, 1);
            ValueRange range = result.range(this.field);
            return this.field.adjustInto(result, range.getMinimum());
        }

        /**
         * Reset this and all lower order fields of the given temporal to their
         * minimum value. For instance for {@link #MINUTE}, this method
         * resets nanos, seconds, <strong>and</strong> minutes to 0.
         *
         * @param temporal the temporal to reset
         * @param <T>      the type of temporal
         * @return the reset temporal
         */
        public <T extends Temporal> T reset(T temporal) {
            for (ChronoField lowerOrder : this.lowerOrders) {
                if (temporal.isSupported(lowerOrder)) {
                    temporal = lowerOrder.adjustInto(temporal, temporal.range(lowerOrder).getMinimum());
                }
            }
            return temporal;
        }

        @Override
        public String toString() {
            return this.field.toString();
        }
    }

    private static void parseField(String value, Type type) {
        Objects.requireNonNull(value, "Value must not be empty");
        Objects.requireNonNull(type, "Type must not be null");
        try {
            String[] fields = SpringStringUtils.delimitedListToStringArray(value, ",");
            for (String field : fields) {
                int slashPos = field.indexOf('/');
                if (slashPos == -1) {
                    ValueRange range = parseRange(field, type);
                } else {
                    String rangeStr = field.substring(0, slashPos);
                    String deltaStr = field.substring(slashPos + 1);
                    ValueRange range = parseRange(rangeStr, type);
                    if (rangeStr.indexOf('-') == -1) {
                        range = ValueRange.of(range.getMinimum(), type.range().getMaximum());
                    }
                    int delta = Integer.parseInt(deltaStr);
                    if (delta <= 0) {
                        throw new IllegalArgumentException("Incrementer delta must be 1 or higher");
                    }
                }
            }
        } catch (DateTimeException | IllegalArgumentException ex) {
            String msg = ex.getMessage() + " '" + value + "'";
            throw new IllegalArgumentException(msg, ex);
        }
    }

    private static ValueRange parseRange(String value, Type type) {
        if (value.equals("*")) {
            return type.range();
        } else {
            int hyphenPos = value.indexOf('-');
            if (hyphenPos == -1) {
                int result = type.checkValidValue(Integer.parseInt(value));
                return ValueRange.of(result, result);
            } else {
                int min = Integer.parseInt(value.substring(0, hyphenPos));
                int max = Integer.parseInt(value.substring(hyphenPos + 1));
                min = type.checkValidValue(min);
                max = type.checkValidValue(max);
                if (type == Type.DAY_OF_WEEK && min == 7) {
                    // If used as a minimum in a range, Sunday means 0 (not 7)
                    min = 0;
                }
                return ValueRange.of(min, max);
            }
        }
    }
}

