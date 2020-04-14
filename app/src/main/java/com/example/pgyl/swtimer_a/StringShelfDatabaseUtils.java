package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.StringShelfDatabase;

import static com.example.pgyl.pekislib_a.DotMatrixDisplayView.DOT_FORM;
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
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotFormInits;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotFormTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayDotFormValueIndex;
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

    //region DOT_SPACING_COEFFS
    public static String[] getCurrentOrDefaultDotMatrixDisplayDotSpacingCoeffsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase) {
        return getCurrentOrDefaultDotMatrixDisplayDotSpacingCoeffsInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
    }

    public static String[] getCurrentOrDefaultDotMatrixDisplayDotSpacingCoeffsInCtDisplayDotSpacingActivity(StringShelfDatabase stringShelfDatabase) {
        return getCurrentOrDefaultDotMatrixDisplayDotSpacingCoeffsInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_SPACING.toString());
    }

    public static void setCurrentDotMatrixDisplayDotSpacingCoeffsInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, String[] values) {
        stringShelfDatabase.insertOrReplaceRowById(getDotMatrixDisplayDotSpacingCoeffsTableName(), TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), values);
    }

    public static void setCurrentDotMatrixDisplayDotSpacingCoeffsInCtDisplayDotSpacingActivity(StringShelfDatabase stringShelfDatabase, String[] values) {
        stringShelfDatabase.insertOrReplaceRowById(getDotMatrixDisplayDotSpacingCoeffsTableName(), TABLE_IDS.CURRENT.toString() + SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_SPACING.toString(), values);
    }

    public static String[] getDotMatrixDisplayDotSpacingCoeffsLabels(StringShelfDatabase stringShelfDatabase) {
        return getLabels(stringShelfDatabase, getDotMatrixDisplayDotSpacingCoeffsTableName());
    }

    public static String[] getCurrentOrDefaultDotMatrixDisplayDotSpacingCoeffsInActivity(StringShelfDatabase stringShelfDatabase, String activityName) {
        String[] ret = stringShelfDatabase.selectRowById(getDotMatrixDisplayDotSpacingCoeffsTableName(), TABLE_IDS.CURRENT.toString() + activityName);
        if (ret == null) {
            ret = getDefaults(stringShelfDatabase, getDotMatrixDisplayDotSpacingCoeffsTableName());
        }
        return ret;
    }
    //endregion

    //region DOT_MATRIX_DISPLAY_DOT_FORM
    public static void saveDotMatrixDisplayDotForm(StringShelfDatabase stringShelfDatabase, DOT_FORM value) {
        stringShelfDatabase.insertOrReplaceFieldById(getDotMatrixDisplayDotFormTableName(), TABLE_IDS.CURRENT.toString(), getDotMatrixDisplayDotFormValueIndex(), value.toString());
    }

    public static DOT_FORM getCurrentOrDefaultDotMatrixDisplayDotForm(StringShelfDatabase stringShelfDatabase) {
        String[] ret = stringShelfDatabase.selectRowById(getDotMatrixDisplayDotFormTableName(), TABLE_IDS.CURRENT.toString());
        if (ret == null) {
            ret = getDefaults(stringShelfDatabase, getDotMatrixDisplayDotFormTableName());
        }
        return DOT_FORM.valueOf(ret[getDotMatrixDisplayDotFormValueIndex()]);
    }
    //endregion

    //region ACTIVITY_INFOS
    public static boolean isColdStartStatusInCtDisplayActivity(StringShelfDatabase stringShelfDatabase) {
        return isColdStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
    }

    public static boolean isColdStartStatusInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase) {
        return isColdStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString());
    }

    public static boolean isColdStartStatusInCtDisplayDotSpacingActivity(StringShelfDatabase stringShelfDatabase) {
        return isColdStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_SPACING.toString());
    }

    public static void setStartStatusInCtDisplayActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_STATUS activityStartStatus) {
        setStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), activityStartStatus);
    }

    public static void setStartStatusInCtDisplayColorsActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_STATUS activityStartStatus) {
        setStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), activityStartStatus);
    }

    public static void setStartStatusInCtDisplayDotSpacingActivity(StringShelfDatabase stringShelfDatabase, ACTIVITY_START_STATUS activityStartStatus) {
        setStartStatusInActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_SPACING.toString(), activityStartStatus);
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
