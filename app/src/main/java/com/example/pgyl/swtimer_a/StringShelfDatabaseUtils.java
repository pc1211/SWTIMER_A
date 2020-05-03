package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.StringShelfDatabase;

import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenColorsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getChronoTimersTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotCornerRadiusCoeffInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotCornerRadiusCoeffTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotSpacingCoeffsInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotSpacingCoeffsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayScrollSpeedInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayScrollSpeedTableName;
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

    public static void initializeTableDotMatrixDisplayDotCornerRadiusCoeff(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getDotMatrixDisplayDotCornerRadiusCoeffTableName(), getDotMatrixDisplayDotCornerRadiusCoeffInits());
    }

    public static void initializeTableDotMatrixDisplayScrollSpeed(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getDotMatrixDisplayScrollSpeedTableName(), getDotMatrixDisplayScrollSpeedInits());
    }

    public static void initializeTableStateButtonsColors(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getStateButtonsColorsTableName(), getStateButtonsColorsInits());
    }

    public static void initializeTableBackScreenColors(StringShelfDatabase stringShelfDatabase) {
        stringShelfDatabase.insertOrReplaceRows(getBackScreenColorsTableName(), getBackScreenColorsInits());
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
