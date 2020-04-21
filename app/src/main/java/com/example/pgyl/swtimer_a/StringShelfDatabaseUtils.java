package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.StringShelfDatabase;

import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.TABLE_IDS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getLabels;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenColorsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getChronoTimersTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTablesCount;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotFormInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotFormTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotSpacingCoeffsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotSpacingCoeffsTableName;
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

    public static void initializeTableDotMatrixDisplayDotSpacingCoeffs(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getDotMatrixDisplayDotSpacingCoeffsTableName(), getDotMatrixDisplayDotSpacingCoeffsInits());
    }

    public static void initializeTableDotMatrixDisplayDotForm(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getDotMatrixDisplayDotFormTableName(), getDotMatrixDisplayDotFormInits());
    }

    public static void initializeTableStateButtonsColors(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getStateButtonsColorsTableName(), getStateButtonsColorsInits());
    }

    public static void initializeTableBackScreenColors(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getBackScreenColorsTableName(), getBackScreenColorsInits());
    }
    //endregion

    //region COLORS
    public static String[][] getCurrentColorsOfMultipleTablesInActivity(StringShelfDatabase stringShelfDatabase, String activityName) {
        String values[][] = new String[getColorTablesCount()][];
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            values[i] = stringShelfDatabase.selectRowByIdOrCreate(getColorTableName(i), TABLE_IDS.CURRENT.toString() + activityName);
        }
        return values;
    }

    public static void setCurrentColorsOfMultipleTablesInActivity(StringShelfDatabase stringShelfDatabase, String activityName, String[][] values) {
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            stringShelfDatabase.insertOrReplaceRowById(getColorTableName(i), TABLE_IDS.CURRENT.toString() + activityName, values[i]);
        }
    }

    public static String[][] getColorTableFieldLabelsOfMultipleTables(StringShelfDatabase stringShelfDatabase) {
        String values[][] = new String[getColorTablesCount()][];
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            values[i] = getLabels(stringShelfDatabase, getColorTableName(i));
        }
        return values;
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
