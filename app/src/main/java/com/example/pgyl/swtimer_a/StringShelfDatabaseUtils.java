package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.InputButtonsActivity.KEYBOARDS;
import com.example.pgyl.pekislib_a.StringShelfDatabase;
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.ACTIVITY_START_STATUS;
import com.example.pgyl.pekislib_a.TimeDateUtils.TIMEUNITS;

import static com.example.pgyl.pekislib_a.StringShelfDatabase.TABLE_ID_INDEX;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.TABLE_IDS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getActivityInfosStartStatusIndex;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getActivityInfosTableName;
import static com.example.pgyl.pekislib_a.TimeDateUtils.xhmsToMs;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;

public class StringShelfDatabaseUtils {
    //region Constantes
    private enum SWTIMER_TABLES {
        CHRONO_TIMERS, PRESETS_CT, COLORS_TIME, COLORS_BUTTONS, COLORS_BACK_SCREEN
    }

    private enum TABLE_CHRONO_TIMERS_DATA_FIELDS {
        MODE, SELECTED, RUNNING, SPLITTED, ALARM_SET, MESSAGE, MESSAGE_INIT, TIME_START, TIME_ACC, TIME_ACC_UNTIL_SPLIT, TIME_DEF, TIME_DEF_INIT, TIME_EXP;

        public int INDEX() {
            return ordinal() + 1;
        }   //  INDEX 0 pour identifiant utilisateur
    }

    private enum TABLE_PRESETS_CT_DATA_FIELDS {
        TIME("Time"), MESSAGE("Message");

        private String valueLabel;

        TABLE_PRESETS_CT_DATA_FIELDS(String valueLabel) {
            this.valueLabel = valueLabel;
        }

        public int INDEX() {
            return ordinal() + 1;
        }   //  INDEX 0 pour identifiant utilisateur

        public String LABEL() {
            return valueLabel;
        }
    }

    private enum TABLE_COLORS_TIME_DATA_FIELDS {
        ON_TIME("ON Time"), ON_MESSAGE("ON Message"), OFF("OFF"), BACK("Background");

        private String valueLabel;

        TABLE_COLORS_TIME_DATA_FIELDS(String valueLabel) {
            this.valueLabel = valueLabel;
        }

        public int INDEX() {
            return ordinal() + 1;
        }   //  INDEX 0 pour identifiant utilisateur

        public String LABEL() {
            return valueLabel;
        }
    }

    private enum TABLE_COLORS_BUTTONS_DATA_FIELDS {
        ON("ON"), OFF("OFF"), BACK("Background");

        private String valueLabel;

        TABLE_COLORS_BUTTONS_DATA_FIELDS(String valueLabel) {
            this.valueLabel = valueLabel;
        }

        public int INDEX() {
            return ordinal() + 1;
        }   //  INDEX 0 pour identifiant utilisateur

        public String LABEL() {
            return valueLabel;
        }
    }

    private enum TABLE_COLORS_BACK_SCREEN_DATA_FIELDS {
        BACK("Background");

        private String valueLabel;

        TABLE_COLORS_BACK_SCREEN_DATA_FIELDS(String valueLabel) {
            this.valueLabel = valueLabel;
        }

        public int INDEX() {
            return ordinal() + 1;
        }   //  INDEX 0 pour identifiant utilisateur

        public String LABEL() {
            return valueLabel;
        }
    }

    private static final String TABLE_COLORS_REGEXP_HEX_DEFAULT = ".{6}";  // Pour valider 6 caractères HEX dans INPUT_BUTTONS pour la table COLORS (RRGGBB)
    //endregion

    //region TABLES
    public static boolean tableChronoTimersExists(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.tableExists(SWTIMER_TABLES.CHRONO_TIMERS.toString());
    }

    public static void createTableChronoTimersIfNotExists(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.createTableIfNotExists(SWTIMER_TABLES.CHRONO_TIMERS.toString(), 1 + TABLE_CHRONO_TIMERS_DATA_FIELDS.values().length);   //  Champ ID + Données
    }

    public static boolean tablePresetsCTExists(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.tableExists(SWTIMER_TABLES.PRESETS_CT.toString());
    }

    public static void createTablePresetsCTIfNotExists(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.createTableIfNotExists(SWTIMER_TABLES.PRESETS_CT.toString(), 1 + TABLE_PRESETS_CT_DATA_FIELDS.values().length);   //  Champ ID + Données
    }

    public static void initializeTablePresetsCT(StringShelfDatabase stringShelfDatabase) {
        final String[][] TABLE_PRESETS_CT_INITS = {
                {TABLE_IDS.LABEL.toString(), TABLE_PRESETS_CT_DATA_FIELDS.TIME.LABEL(), TABLE_PRESETS_CT_DATA_FIELDS.MESSAGE.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), KEYBOARDS.TIME_XHMS.toString(), KEYBOARDS.ALPHANUM.toString()},
                {TABLE_IDS.REGEXP.toString(), "^([0-9]+(h|$))?([0-9]+(m|$))?([0-9]+(s|$))?([0-9]+(c|$))?$", null},
                {TABLE_IDS.MAX.toString(), String.valueOf(xhmsToMs("23h59m59s99c")), null},
                {TABLE_IDS.TIMEUNIT.toString(), TIMEUNITS.CS.toString(), null}};

        stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.PRESETS_CT.toString(), TABLE_PRESETS_CT_INITS);
    }

    public static String getPresetsCTTableName() {
        return SWTIMER_TABLES.PRESETS_CT.toString();
    }

    public static boolean tableColorsTimeExists(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.tableExists(SWTIMER_TABLES.COLORS_TIME.toString());
    }

    public static boolean tableColorsButtonsExists(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.tableExists(SWTIMER_TABLES.COLORS_BUTTONS.toString());
    }

    public static void createTableColorsTimeIfNotExists(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.createTableIfNotExists(SWTIMER_TABLES.COLORS_TIME.toString(), 1 + TABLE_COLORS_TIME_DATA_FIELDS.values().length);   //  Champ ID + Données;
    }

    public static void createTableColorsButtonsIfNotExists(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.createTableIfNotExists(SWTIMER_TABLES.COLORS_BUTTONS.toString(), 1 + TABLE_COLORS_BUTTONS_DATA_FIELDS.values().length);   //  Champ ID + Données;
    }

    public static void initializeTableColorsTime(StringShelfDatabase stringShelfDatabase) {
        final String[][] TABLE_COLOR_TIME_INITS = {
                {TABLE_IDS.LABEL.toString(), TABLE_COLORS_TIME_DATA_FIELDS.ON_TIME.LABEL(), TABLE_COLORS_TIME_DATA_FIELDS.ON_MESSAGE.LABEL(), TABLE_COLORS_TIME_DATA_FIELDS.OFF.LABEL(), TABLE_COLORS_TIME_DATA_FIELDS.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), KEYBOARDS.HEX.toString(), KEYBOARDS.HEX.toString(), KEYBOARDS.HEX.toString(), KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "999900", "00B777", "303030", "000000"}};

        stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.COLORS_TIME.toString(), TABLE_COLOR_TIME_INITS);
    }

    public static void initializeTableColorsButtons(StringShelfDatabase stringShelfDatabase) {
        final String[][] TABLE_COLOR_BUTTONS_INITS = {
                {TABLE_IDS.LABEL.toString(), TABLE_COLORS_BUTTONS_DATA_FIELDS.ON.LABEL(), TABLE_COLORS_BUTTONS_DATA_FIELDS.OFF.LABEL(), TABLE_COLORS_BUTTONS_DATA_FIELDS.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), KEYBOARDS.HEX.toString(), KEYBOARDS.HEX.toString(), KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "0061F3", "696969", "000000"}};

        stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.COLORS_BUTTONS.toString(), TABLE_COLOR_BUTTONS_INITS);
    }

    public static boolean tableColorsBackScreenExists(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.tableExists(SWTIMER_TABLES.COLORS_BACK_SCREEN.toString());
    }

    public static void createTableColorsBackScreenIfNotExists(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.createTableIfNotExists(SWTIMER_TABLES.COLORS_BACK_SCREEN.toString(), 1 + TABLE_COLORS_BACK_SCREEN_DATA_FIELDS.values().length);   //  Champ ID + Données;
    }

    public static void initializeTableColorsBackScreen(StringShelfDatabase stringShelfDatabase) {
        final String[][] TABLE_COLOR_BACK_SCREEN_INITS = {
                {TABLE_IDS.LABEL.toString(), TABLE_COLORS_BACK_SCREEN_DATA_FIELDS.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "000000"}};

        stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.COLORS_BACK_SCREEN.toString(), TABLE_COLOR_BACK_SCREEN_INITS);
    }

    public static String getColorsTimeTableName() {
        return SWTIMER_TABLES.COLORS_TIME.toString();
    }

    public static String getColorsButtonsTableName() {
        return SWTIMER_TABLES.COLORS_BUTTONS.toString();
    }

    public static String getColorsBackScreenTableName() {
        return SWTIMER_TABLES.COLORS_BACK_SCREEN.toString();
    }

    public static int getTimeColorOnTimeIndex() {
        return TABLE_COLORS_TIME_DATA_FIELDS.ON_TIME.INDEX();
    }

    public static int getTimeColorOnMessageIndex() {
        return TABLE_COLORS_TIME_DATA_FIELDS.ON_MESSAGE.INDEX();
    }

    public static int getTimeColorOffIndex() {
        return TABLE_COLORS_TIME_DATA_FIELDS.OFF.INDEX();
    }

    public static int getTimeColorBackIndex() {
        return TABLE_COLORS_TIME_DATA_FIELDS.BACK.INDEX();
    }

    public static int getButtonsColorOnIndex() {
        return TABLE_COLORS_BUTTONS_DATA_FIELDS.ON.INDEX();
    }

    public static int getButtonsColorOffIndex() {
        return TABLE_COLORS_BUTTONS_DATA_FIELDS.OFF.INDEX();
    }

    public static int getButtonsColorBackIndex() {
        return TABLE_COLORS_BUTTONS_DATA_FIELDS.BACK.INDEX();
    }

    public static int getBackScreenColorBackIndex() {
        return TABLE_COLORS_BACK_SCREEN_DATA_FIELDS.BACK.INDEX();
    }

    public static String[] getDefaultColors(StringShelfDatabase stringShelfDatabase, String tableName) {
        return stringShelfDatabase.selectRowByIdOrCreate(tableName, TABLE_IDS.DEFAULT.toString());
    }

    public static String[] getCurrentColorsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, String tableName) {
        return stringShelfDatabase.selectRowByIdOrCreate(tableName, TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
    }

    public static void setCurrentColorsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, String tableName, String[] colors) {
        stringShelfDatabase.insertOrReplaceRowById(tableName, TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), colors);
    }
    //endregion

    //region START_STATUS
    public static boolean isColdStartStatusInCtDisplayActivity(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectFieldByIdOrCreate(getActivityInfosTableName(), SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getActivityInfosStartStatusIndex()).equals(ACTIVITY_START_STATUS.COLD.toString());
    }

    public static void setStartStatusInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_STATUS activityStartStatus) {
        stringShelfDatabase.insertOrReplaceFieldById(getActivityInfosTableName(), SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getActivityInfosStartStatusIndex(), activityStartStatus.toString());
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

    public static void copyChronoTimerRowToCtRecord(String[] chronoTimerRow, CtRecord ctRecord) {
        ctRecord.fill(
                Integer.parseInt(chronoTimerRow[TABLE_ID_INDEX]),
                CtRecord.MODE.valueOf(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.MODE.INDEX()]),
                (Integer.parseInt(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.SELECTED.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.RUNNING.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.SPLITTED.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.ALARM_SET.INDEX()]) == 1),
                chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.MESSAGE.INDEX()],
                chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.MESSAGE_INIT.INDEX()],
                Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_START.INDEX()]),
                Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_ACC.INDEX()]),
                Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_ACC_UNTIL_SPLIT.INDEX()]),
                Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_DEF.INDEX()]),
                Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_DEF_INIT.INDEX()]),
                Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_EXP.INDEX()]),
                0,    //  Non stockés dans la table
                0);
    }

    public static String[] ctRecordToChronoTimerRow(CtRecord ctRecord) {
        String[] ret = new String[1 + TABLE_CHRONO_TIMERS_DATA_FIELDS.values().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = String.valueOf(ctRecord.getIdct());
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.MODE.INDEX()] = ctRecord.getMode().toString();
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.SELECTED.INDEX()] = String.valueOf(ctRecord.isSelected() ? 1 : 0);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.RUNNING.INDEX()] = String.valueOf(ctRecord.isRunning() ? 1 : 0);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.SPLITTED.INDEX()] = String.valueOf(ctRecord.isSplitted() ? 1 : 0);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.ALARM_SET.INDEX()] = String.valueOf(ctRecord.hasClockAppAlarm() ? 1 : 0);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.MESSAGE.INDEX()] = ctRecord.getMessage();
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.MESSAGE_INIT.INDEX()] = ctRecord.getMessageInit();
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_START.INDEX()] = String.valueOf(ctRecord.getTimeStart());
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_ACC.INDEX()] = String.valueOf(ctRecord.getTimeAcc());
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_ACC_UNTIL_SPLIT.INDEX()] = String.valueOf(ctRecord.getTimeAccUntilSplit());
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_DEF.INDEX()] = String.valueOf(ctRecord.getTimeDef());
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_DEF_INIT.INDEX()] = String.valueOf(ctRecord.getTimeDefInit());
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_EXP.INDEX()] = String.valueOf(ctRecord.getTimeExp());
        return ret;
    }
    //endregion

    //region PRESETS_CT
    public static boolean copyPresetCTRowToCtRecord(String[] presetCTRow, CtRecord ctRecord, long nowm) {
        boolean ret = true;
        if (!ctRecord.setTimeDef(Long.parseLong(presetCTRow[TABLE_PRESETS_CT_DATA_FIELDS.TIME.INDEX()]), nowm)) {
            ret = false;
        }
        if (!ctRecord.setMessage(presetCTRow[TABLE_PRESETS_CT_DATA_FIELDS.MESSAGE.INDEX()])) {
            ret = false;
        }
        return ret;
    }

    public static String[] timeMessageToPresetCTRow(long time, String message) {
        String[] ret = new String[1 + TABLE_PRESETS_CT_DATA_FIELDS.values().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = null;
        ret[TABLE_PRESETS_CT_DATA_FIELDS.TIME.INDEX()] = String.valueOf(time);
        ret[TABLE_PRESETS_CT_DATA_FIELDS.MESSAGE.INDEX()] = message;
        return ret;
    }
    //endregion

}
