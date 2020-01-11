package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.StringShelfDatabase;
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.ACTIVITY_START_STATUS;

import static com.example.pgyl.pekislib_a.PekislibTableUtils.getActivityInfosStartStatusIndex;
import static com.example.pgyl.pekislib_a.PekislibTableUtils.getActivityInfosTableName;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.TABLE_IDS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getDefaults;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.SwTimerTableUtils.getChronoTimersTableName;
import static com.example.pgyl.swtimer_a.SwTimerTableUtils.getColorsBackScreenInits;
import static com.example.pgyl.swtimer_a.SwTimerTableUtils.getColorsBackScreenTableName;
import static com.example.pgyl.swtimer_a.SwTimerTableUtils.getColorsButtonsInits;
import static com.example.pgyl.swtimer_a.SwTimerTableUtils.getColorsButtonsTableName;
import static com.example.pgyl.swtimer_a.SwTimerTableUtils.getColorsDotMatrixDisplayInits;
import static com.example.pgyl.swtimer_a.SwTimerTableUtils.getColorsDotMatrixDisplayTableName;
import static com.example.pgyl.swtimer_a.SwTimerTableUtils.getPresetsCTInits;
import static com.example.pgyl.swtimer_a.SwTimerTableUtils.getPresetsCTTableName;
import static com.example.pgyl.swtimer_a.SwTimerTableUtils.getSwtimerTableDataFieldsCount;

public class StringShelfDatabaseUtils {

    //region TABLES
    public static void createSwtimerTableIfNotExists(StringShelfDatabase stringShelfDatabase, String tableName) {
        stringShelfDatabase.createTableIfNotExists(tableName, 1 + getSwtimerTableDataFieldsCount(tableName));   //  Champ ID + Données;
    }

    public static void initializeTablePresetsCT(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getPresetsCTTableName(), getPresetsCTInits());
    }

    public static void initializeTableColorsDotMatrixDisplay(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getColorsDotMatrixDisplayTableName(), getColorsDotMatrixDisplayInits());
        stringShelfDatabase.insertOrReplaceRowById(getColorsDotMatrixDisplayTableName(), TABLE_IDS.PRESET.toString() + "1", getDefaults(stringShelfDatabase, getColorsDotMatrixDisplayTableName()));   //  PRESET1 = DEFAULT
    }

    public static void initializeTableColorsButtons(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getColorsButtonsTableName(), getColorsButtonsInits());
        stringShelfDatabase.insertOrReplaceRowById(getColorsButtonsTableName(), TABLE_IDS.PRESET.toString() + "1", getDefaults(stringShelfDatabase, getColorsButtonsTableName()));   //  PRESET1 = DEFAULT
    }

    public static void initializeTableColorsBackScreen(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getColorsBackScreenTableName(), getColorsBackScreenInits());
        stringShelfDatabase.insertOrReplaceRowById(getColorsBackScreenTableName(), TABLE_IDS.PRESET.toString() + "1", getDefaults(stringShelfDatabase, getColorsBackScreenTableName()));   //  PRESET1 = DEFAULT
    }
    //endregion

    //region CURRENT
    public static String[] getCurrentValuesInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, String tableName) {
        return stringShelfDatabase.selectRowById(tableName, TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
    }

    public static void setCurrentValuesInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, String tableName, String[] values) {
        stringShelfDatabase.insertOrReplaceRowById(tableName, TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), values);
    }

    public static String[] getCurrentValuesInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase, String tableName) {
        return stringShelfDatabase.selectRowById(tableName, TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString());
    }

    public static void setCurrentValuesInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase, String tableName, String[] colors) {
        stringShelfDatabase.insertOrReplaceRowById(tableName, TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), colors);
    }
    //endregion

    //region START_STATUS
    public static boolean isColdStartStatusInCtDisplayActivity(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectFieldByIdOrCreate(getActivityInfosTableName(), SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getActivityInfosStartStatusIndex()).equals(ACTIVITY_START_STATUS.COLD.toString());
    }

    public static boolean isColdStartStatusInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectFieldByIdOrCreate(getActivityInfosTableName(), SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), getActivityInfosStartStatusIndex()).equals(ACTIVITY_START_STATUS.COLD.toString());
    }

    public static void setStartStatusInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_STATUS activityStartStatus) {
        stringShelfDatabase.insertOrReplaceFieldById(getActivityInfosTableName(), SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getActivityInfosStartStatusIndex(), activityStartStatus.toString());
    }

    public static void setStartStatusInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_STATUS activityStartStatus) {
        stringShelfDatabase.insertOrReplaceFieldById(getActivityInfosTableName(), SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), getActivityInfosStartStatusIndex(), activityStartStatus.toString());
    }
    //endregion

    //region CT
    public static String[] getChronoTimerById(StringShelfDatabase stringShelfDatabase, int idct) {
        return stringShelfDatabase.selectRowByIdOrCreate(getChronoTimersTableName(), String.valueOf(idct));
    }

    public static String[][] getChronoTimers(StringShelfDatabase stringShelfDatabase) {
        return stringShelfDatabase.selectRows(getChronoTimersTableName(), null);
    }

    public static void saveChronoTimer(StringShelfDatabase stringShelfDatabase, String[] values) {
        stringShelfDatabase.insertOrReplaceRow(getChronoTimersTableName(), values);
    }

    public static void saveChronoTimers(StringShelfDatabase stringShelfDatabase, String[][] values) {
        stringShelfDatabase.deleteRows(getChronoTimersTableName(), null);
        stringShelfDatabase.insertOrReplaceRows(getChronoTimersTableName(), values);
    }
    //endregion

}
