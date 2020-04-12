package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.StringShelfDatabase;

import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.ACTIVITY_START_STATUS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.TABLE_IDS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getDefaults;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getLabels;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.isColdStartStatusInActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusInActivity;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenColorsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getChronoTimersTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTablesCount;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDimensionsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDimensionsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getPresetsCTInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getPresetsCTTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getStateButtonsColorsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getStateButtonsColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getSwTimerTableDataFieldsCount;

public class StringShelfDatabaseUtils {

    //region TABLES
    public static void createSwtimerTableIfNotExists(StringShelfDatabase stringShelfDatabase, String tableName) {
        stringShelfDatabase.createTableIfNotExists(tableName, 1 + getSwTimerTableDataFieldsCount(tableName));   //  Champ ID + Données;
    }

    public static void initializeTablePresetsCT(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getPresetsCTTableName(), getPresetsCTInits());
    }

    public static void initializeTableDotMatrixDisplayColors(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getDotMatrixDisplayColorsTableName(), getDotMatrixDisplayColorsInits());
    }

    public static void initializeTableDotMatrixDisplayDimensions(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getDotMatrixDisplayDimensionsTableName(), getDotMatrixDisplayDimensionsInits());
    }

    public static void initializeTableStateButtonsColors(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getStateButtonsColorsTableName(), getStateButtonsColorsInits());
    }

    public static void initializeTableBackScreenColors(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getBackScreenColorsTableName(), getBackScreenColorsInits());
    }
    //endregion

    //region COLORS
    public static String[][] getCurrentOrDefaultColorsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase) {
        return getCurrentOrDefaultColorsInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
    }

    public static String[][] getCurrentOrDefaultColorsInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase) {
        return getCurrentOrDefaultColorsInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString());
    }

    public static void setCurrentColorsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, String[][] values) {
        setCurrentColorsInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), values);
    }

    public static void setCurrentColorsInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase, String[][] values) {
        setCurrentColorsInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), values);
    }

    public static String[][] getCurrentOrDefaultColorsInActivity(StringShelfDatabase stringShelfDatabase, String activityName) {
        String values[][] = new String[getColorTablesCount()][];
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            values[i] = stringShelfDatabase.selectRowById(getColorTableName(i), TABLE_IDS.CURRENT.toString() + activityName);
            if (values[i] == null) {
                values[i] = getDefaults(stringShelfDatabase, getColorTableName(i));
            }
        }
        return values;
    }

    public static void setCurrentColorsInActivity(StringShelfDatabase stringShelfDatabase, String activityName, String[][] values) {
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            stringShelfDatabase.insertOrReplaceRowById(getColorTableName(i), TABLE_IDS.CURRENT.toString() + activityName, values[i]);
        }
    }

    public static String[][] getColorTablesFieldLabels(StringShelfDatabase stringShelfDatabase) {
        String values[][] = new String[getColorTablesCount()][];
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            values[i] = getLabels(stringShelfDatabase, getColorTableName(i));
        }
        return values;
    }
    //endregion

    //region DIMENSIONS
    public static String[] getCurrentOrDefaultDotMatrixDisplayDimensionsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase) {
        return getCurrentOrDefaultDotMatrixDisplayDimensionsInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
    }

    public static String[] getCurrentOrDefaultDotMatrixDisplayDimensionsInCtDisplayDimensionsActivity(StringShelfDatabase stringShelfDatabase) {
        return getCurrentOrDefaultDotMatrixDisplayDimensionsInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_DIMENSIONS.toString());
    }

    public static void setCurrentDotMatrixDisplayDimensionsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, String[] values) {
        stringShelfDatabase.insertOrReplaceRowById(getDotMatrixDisplayDimensionsTableName(), TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), values);
    }

    public static void setCurrentDotMatrixDisplayDimensionsInCtDisplayDimensionsActivity(StringShelfDatabase stringShelfDatabase, String[] values) {
        stringShelfDatabase.insertOrReplaceRowById(getDotMatrixDisplayDimensionsTableName(), TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY_DIMENSIONS.toString(), values);
    }

    public static String[] getDotMatrixDisplayDimensionsLabels(StringShelfDatabase stringShelfDatabase) {
        return getLabels(stringShelfDatabase, getDotMatrixDisplayDimensionsTableName());
    }

    public static String[] getCurrentOrDefaultDotMatrixDisplayDimensionsInActivity(StringShelfDatabase stringShelfDatabase, String activityName) {
        String[] ret = stringShelfDatabase.selectRowById(getDotMatrixDisplayDimensionsTableName(), TABLE_IDS.CURRENT.toString() + activityName);
        if (ret == null) {
            ret = getDefaults(stringShelfDatabase, getDotMatrixDisplayDimensionsTableName());
        }
        return ret;
    }
    //endregion

    //region ACTIVITY_INFOS
    public static boolean isColdStartStatusInCtDisplayActivity(StringShelfDatabase stringShelfDatabase) {
        return isColdStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
    }

    public static boolean isColdStartStatusInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase) {
        return isColdStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString());
    }

    public static boolean isColdStartStatusInCtDisplayDimensionsActivity(StringShelfDatabase stringShelfDatabase) {
        return isColdStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_DIMENSIONS.toString());
    }

    public static void setStartStatusInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_STATUS activityStartStatus) {
        setStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), activityStartStatus);
    }

    public static void setStartStatusInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_STATUS activityStartStatus) {
        setStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), activityStartStatus);
    }

    public static void setStartStatusInCtDisplayDimensionsActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_STATUS activityStartStatus) {
        setStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_DIMENSIONS.toString(), activityStartStatus);
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
