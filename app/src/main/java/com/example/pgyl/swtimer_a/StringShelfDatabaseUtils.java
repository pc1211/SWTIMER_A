package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.Constants.PEKISLIB_ACTIVITIES;
import com.example.pgyl.pekislib_a.InputButtonsActivity.KEYBOARDS;
import com.example.pgyl.pekislib_a.StringShelfDatabase;
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.PEKISLIB_TABLES;
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.TABLE_ACTIVITY_INFOS_DATA_FIELDS;
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.TABLE_IDS;
import com.example.pgyl.pekislib_a.TimeDateUtils.TIMEUNITS;
import com.example.pgyl.swtimer_a.CtDisplayActivity.COLOR_ITEMS;

import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.ACTIVITY_START_TYPE;
import static com.example.pgyl.pekislib_a.TimeDateUtils.convertXhmsToMs;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;

public class StringShelfDatabaseUtils {
    //region Constantes
    public enum SWTIMER_TABLES {
        CHRONO_TIMERS, PRESETS_CT, COLORS_TIME_BUTTONS, COLORS_BACK_SCREEN
    }

    public enum TABLE_CHRONO_TIMERS_DATA_FIELDS {
        MODE(1), SELECTED(2), RUNNING(3), SPLITTED(4), ALARM_SET(5), MESSAGE(6), MESSAGE_INIT(7), TIME_START(8), TIME_ACC(9), TIME_ACC_UNTIL_SPLIT(10), TIME_DEF(11), TIME_DEF_INIT(12), TIME_EXP(13);

        private int valueIndex;

        TABLE_CHRONO_TIMERS_DATA_FIELDS(int valueIndex) {
            this.valueIndex = valueIndex;
        }

        public int INDEX() {
            return valueIndex;
        }
    }

    public enum TABLE_PRESETS_CT_DATA_FIELDS {
        TIME("Time", 1), MESSAGE("Message", 2);

        private int valueIndex;
        private String valueLabel;

        TABLE_PRESETS_CT_DATA_FIELDS(String valueLabel, int valueIndex) {
            this.valueIndex = valueIndex;
            this.valueLabel = valueLabel;
        }

        public int INDEX() {
            return valueIndex;
        }

        public String LABEL() {
            return valueLabel;
        }
    }

    public enum TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS {
        ON("Light On", 1), OFF("Light Off", 2), BACK("Background", 3);

        private int valueIndex;
        private String valueLabel;

        TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS(String valueLabel, int valueIndex) {
            this.valueIndex = valueIndex;
            this.valueLabel = valueLabel;
        }

        public int INDEX() {
            return valueIndex;
        }

        public String LABEL() {
            return valueLabel;
        }
    }

    public enum TABLE_COLORS_BACK_SCREEN_DATA_FIELDS {
        BACK("Background", 1);

        private int valueIndex;
        private String valueLabel;

        TABLE_COLORS_BACK_SCREEN_DATA_FIELDS(String valueLabel, int valueIndex) {
            this.valueIndex = valueIndex;
            this.valueLabel = valueLabel;
        }

        public int INDEX() {
            return valueIndex;
        }

        public String LABEL() {
            return valueLabel;
        }
    }

    public static final String TABLE_COLORS_REGEXP_HEX_DEFAULT = ".{6}";  // Pour valider 6 caractères HEX dans INPUT_BUTTONS pour la table COLORS (RRGGBB)
    //endregion

    //region CREATE_TABLES
    public static void createTableChronoTimers(StringShelfDatabase stringShelfDatabase) {
        if (!stringShelfDatabase.tableExists(SWTIMER_TABLES.CHRONO_TIMERS.toString())) {
            stringShelfDatabase.createTable(SWTIMER_TABLES.CHRONO_TIMERS.toString(), 1 + TABLE_CHRONO_TIMERS_DATA_FIELDS.values().length);   //  Champ ID + Données
        }
    }

    public static void createTablePresetsCT(StringShelfDatabase stringShelfDatabase) {
        final String[][] TABLE_PRESETS_CT_INITS = {
                {TABLE_IDS.LABEL.toString(), TABLE_PRESETS_CT_DATA_FIELDS.TIME.LABEL(), TABLE_PRESETS_CT_DATA_FIELDS.MESSAGE.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), KEYBOARDS.TIME_XHMS.toString(), KEYBOARDS.ALPHANUM.toString()},
                {TABLE_IDS.REGEXP.toString(), "^([0-9]+(" + TIMEUNITS.HOUR.SYMBOL() + "|$))?([0-9]+(" + TIMEUNITS.MIN.SYMBOL() + "|$))?([0-9]+(" + TIMEUNITS.SEC.SYMBOL() + "|$))?([0-9]+(" + TIMEUNITS.CS.SYMBOL() + "|$))?$", null},
                {TABLE_IDS.MAX.toString(), String.valueOf(convertXhmsToMs("23" + TIMEUNITS.HOUR.SYMBOL() + "59" + TIMEUNITS.MIN.SYMBOL() + "59" + TIMEUNITS.SEC.SYMBOL() + "99" + TIMEUNITS.CS.SYMBOL())), null},    //  23h59m59s99c
                {TABLE_IDS.TIMEUNIT.toString(), TIMEUNITS.CS.toString(), null}};

        if (!stringShelfDatabase.tableExists(SWTIMER_TABLES.PRESETS_CT.toString())) {
            stringShelfDatabase.createTable(SWTIMER_TABLES.PRESETS_CT.toString(), 1 + TABLE_PRESETS_CT_DATA_FIELDS.values().length);   //  Champ ID + Données
            stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.PRESETS_CT.toString(), TABLE_PRESETS_CT_INITS);
        }
    }

    public static void createTableColors(StringShelfDatabase stringShelfDatabase) {
        final String[][] TABLE_COLOR_TIME_BUTTONS_INITS = {
                {TABLE_IDS.LABEL.toString(), TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS.ON.LABEL(), TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS.OFF.LABEL(), TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), KEYBOARDS.HEX.toString(), KEYBOARDS.HEX.toString(), KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString() + COLOR_ITEMS.TIME.toString(), "999900", "303030", "000000"},
                {TABLE_IDS.DEFAULT.toString() + COLOR_ITEMS.BUTTONS.toString(), "0061F3", "696969", "000000"}};

        final String[][] TABLE_COLOR_BACK_SCREEN_INITS = {
                {TABLE_IDS.LABEL.toString(), TABLE_COLORS_BACK_SCREEN_DATA_FIELDS.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString() + COLOR_ITEMS.BACK_SCREEN.toString(), "000000"}};

        if (!stringShelfDatabase.tableExists(SWTIMER_TABLES.COLORS_TIME_BUTTONS.toString())) {
            stringShelfDatabase.createTable(SWTIMER_TABLES.COLORS_TIME_BUTTONS.toString(), 1 + TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS.values().length);   //  Champ ID + Données;
            stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.COLORS_TIME_BUTTONS.toString(), TABLE_COLOR_TIME_BUTTONS_INITS);
            setCurrentColorsInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.TIME, getDefaultColors(stringShelfDatabase, COLOR_ITEMS.TIME));
            setCurrentColorsInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.BUTTONS, getDefaultColors(stringShelfDatabase, COLOR_ITEMS.BUTTONS));
        }
        if (!stringShelfDatabase.tableExists(SWTIMER_TABLES.COLORS_BACK_SCREEN.toString())) {
            stringShelfDatabase.createTable(SWTIMER_TABLES.COLORS_BACK_SCREEN.toString(), 1 + TABLE_COLORS_BACK_SCREEN_DATA_FIELDS.values().length);   //  Champ ID + Données;
            stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.COLORS_BACK_SCREEN.toString(), TABLE_COLOR_BACK_SCREEN_INITS);
            setCurrentColorsInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.BACK_SCREEN, getDefaultColors(stringShelfDatabase, COLOR_ITEMS.BACK_SCREEN));
        }
    }
    //endregion

    //region COLORS
    public static String getColorItemTableName(COLOR_ITEMS colorItem) {
        String ret;

        ret = null;
        if (colorItem.equals(COLOR_ITEMS.TIME)) {
            ret = SWTIMER_TABLES.COLORS_TIME_BUTTONS.toString();
        }
        if (colorItem.equals(COLOR_ITEMS.BUTTONS)) {
            ret = SWTIMER_TABLES.COLORS_TIME_BUTTONS.toString();
        }
        if (colorItem.equals(COLOR_ITEMS.BACK_SCREEN)) {
            ret = SWTIMER_TABLES.COLORS_BACK_SCREEN.toString();
        }
        return ret;
    }

    public static String[] getDefaultColors(StringShelfDatabase stringShelfDatabase, COLOR_ITEMS colorItem) {
        return stringShelfDatabase.selectRowByIdOrCreate(getColorItemTableName(colorItem), TABLE_IDS.DEFAULT.toString() + colorItem.toString());
    }

    public static String[] getCurrentColorsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, COLOR_ITEMS colorItem) {
        return stringShelfDatabase.selectRowByIdOrCreate(getColorItemTableName(colorItem), TABLE_IDS.CURRENT.toString() + colorItem.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
    }

    public static void setCurrentColorsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, COLOR_ITEMS colorItem, String[] colors) {
        stringShelfDatabase.insertOrReplaceRowById(getColorItemTableName(colorItem), TABLE_IDS.CURRENT.toString() + colorItem.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), colors);
    }
    //endregion

    //region START_STATUS
    public static boolean isColdStartStatusInCtDisplayActivity(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectFieldByIdOrCreate(PEKISLIB_TABLES.ACTIVITY_INFOS.toString(), SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), TABLE_ACTIVITY_INFOS_DATA_FIELDS.START_TYPE.INDEX()).equals(ACTIVITY_START_TYPE.COLD.toString());
    }

    public static void setStartStatusInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_TYPE activityStartType) {
        stringShelfDatabase.insertOrReplaceFieldById(PEKISLIB_TABLES.ACTIVITY_INFOS.toString(), SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), TABLE_ACTIVITY_INFOS_DATA_FIELDS.START_TYPE.INDEX(), activityStartType.toString());
    }
    //endregion

    //region CT
    public static String[] getChronoTimerById(StringShelfDatabase stringShelfDatabase, int idct) {
        return stringShelfDatabase.selectRowByIdOrCreate(SWTIMER_TABLES.CHRONO_TIMERS.toString(), String.valueOf(idct));
    }

    public static String[][] getChronoTimers(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectRows(SWTIMER_TABLES.CHRONO_TIMERS.toString(), null);
    }

    public static void saveChronoTimer(StringShelfDatabase stringShelfDatabase, String[] values) {
        stringShelfDatabase.insertOrReplaceRow(SWTIMER_TABLES.CHRONO_TIMERS.toString(), values);
    }

    public static void saveChronoTimers(StringShelfDatabase stringShelfDatabase, String[][] values) {
        stringShelfDatabase.deleteRows(SWTIMER_TABLES.CHRONO_TIMERS.toString(), null);
        stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.CHRONO_TIMERS.toString(), values);
    }
    //endregion

    //region PRESETS_CT
    public static String getCurrentMessageOfPresetCTInPresetsActivity(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectFieldByIdOrCreate(SWTIMER_TABLES.PRESETS_CT.toString(), TABLE_IDS.CURRENT.toString() + PEKISLIB_ACTIVITIES.PRESETS.toString(), TABLE_PRESETS_CT_DATA_FIELDS.MESSAGE.INDEX());
    }

    public static void setCurrentMessageOfPresetCTInPresetsActivity(StringShelfDatabase stringShelfDatabase, String value) {
        stringShelfDatabase.insertOrReplaceFieldById(SWTIMER_TABLES.PRESETS_CT.toString(), TABLE_IDS.CURRENT.toString() + PEKISLIB_ACTIVITIES.PRESETS.toString(), TABLE_PRESETS_CT_DATA_FIELDS.MESSAGE.INDEX(), value);
    }

    public static String getCurrentTimeOfPresetCTInPresetsActivity(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectFieldByIdOrCreate(SWTIMER_TABLES.PRESETS_CT.toString(), TABLE_IDS.CURRENT.toString() + PEKISLIB_ACTIVITIES.PRESETS.toString(), TABLE_PRESETS_CT_DATA_FIELDS.TIME.INDEX());
    }

    public static void setCurrentTimeOfPresetCTInPresetsActivity(StringShelfDatabase stringShelfDatabase, long value) {
        stringShelfDatabase.insertOrReplaceFieldById(SWTIMER_TABLES.PRESETS_CT.toString(), TABLE_IDS.CURRENT.toString() + PEKISLIB_ACTIVITIES.PRESETS.toString(), TABLE_PRESETS_CT_DATA_FIELDS.TIME.INDEX(), String.valueOf(value));
    }

    public static void setDefaultTimeOfPresetCT(StringShelfDatabase stringShelfDatabase, long value) {
        stringShelfDatabase.insertOrReplaceFieldById(SWTIMER_TABLES.PRESETS_CT.toString(), TABLE_IDS.DEFAULT.toString(), TABLE_PRESETS_CT_DATA_FIELDS.TIME.INDEX(), String.valueOf(value));
    }

    public static void setDefaultMessageOfPresetCT(StringShelfDatabase stringShelfDatabase, String value) {
        stringShelfDatabase.insertOrReplaceFieldById(SWTIMER_TABLES.PRESETS_CT.toString(), TABLE_IDS.DEFAULT.toString(), TABLE_PRESETS_CT_DATA_FIELDS.MESSAGE.INDEX(), value);
    }
    //endregion

}
