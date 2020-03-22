package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.StringShelfDatabase;

import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.ACTIVITY_START_STATUS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.TABLE_IDS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.getActivityInfosStartStatusIndex;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.getActivityInfosTableName;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getDefaults;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getLabels;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getChronoTimersTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTablesCount;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getPresetsCTInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getPresetsCTTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getStateButtonsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getStateButtonsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getSwTimerTableDataFieldsCount;

public class StringShelfDatabaseUtils {

    //region TABLES
    public static void createSwtimerTableIfNotExists(StringShelfDatabase stringShelfDatabase, String tableName) {
        stringShelfDatabase.createTableIfNotExists(tableName, 1 + getSwTimerTableDataFieldsCount(tableName));   //  Champ ID + Données;
    }

    public static void initializeTablePresetsCT(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getPresetsCTTableName(), getPresetsCTInits());
    }

    public static void initializeTableDotMatrixDisplay(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getDotMatrixDisplayTableName(), getDotMatrixDisplayInits());
    }

    public static void initializeTableStateButtons(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getStateButtonsTableName(), getStateButtonsInits());
    }

    public static void initializeTableBackScreen(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getBackScreenTableName(), getBackScreenInits());
    }
    //endregion

    //region COLORS
    public static String[][] getCurrentOrDefaultColorsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase) {
        String values[][] = new String[getColorTablesCount()][];
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            values[i] = stringShelfDatabase.selectRowById(getColorTableName(i), TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
            if (values[i] == null) {
                values[i] = getDefaults(stringShelfDatabase, getColorTableName(i));
            }
        }
        return values;
    }

    public static void setCurrentColorsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, String[][] values) {
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            stringShelfDatabase.insertOrReplaceRowById(getColorTableName(i), TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), values[i]);
        }
    }

    public static String[][] getCurrentColorsInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase) {
        String values[][] = new String[getColorTablesCount()][];
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            values[i] = stringShelfDatabase.selectRowById(getColorTableName(i), TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString());
        }
        return values;
    }

    public static void setCurrentColorsInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase, String[][] values) {
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            stringShelfDatabase.insertOrReplaceRowById(getColorTableName(i), TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), values[i]);
        }
    }

    public static String[][] getColorTableFieldLabels(StringShelfDatabase stringShelfDatabase) {
        String values[][] = new String[getColorTablesCount()][];
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            values[i] = getLabels(stringShelfDatabase, getColorTableName(i));
        }
        return values;
    }
    //endregion

    //region ACTIVITY_INFOS
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

    //region CHRONO_TIMERS
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
