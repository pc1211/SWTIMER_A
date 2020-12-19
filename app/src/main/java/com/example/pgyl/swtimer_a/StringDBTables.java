package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.InputButtonsActivity;

import java.util.ArrayList;

import static com.example.pgyl.pekislib_a.StringDB.TABLE_ID_INDEX;
import static com.example.pgyl.pekislib_a.StringDBTables.TABLE_IDS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.getFirstTimeUnit;
import static com.example.pgyl.swtimer_a.Constants.TIME_UNIT_PRECISION;

public class StringDBTables {

    private static final String TABLE_COLORS_REGEXP_HEX_DEFAULT = ".{6}";  //  Pour valider 6 caractères HEX dans INPUT_BUTTONS pour les tables decouleur (RRGGBB ou HHSSVV (dégradé))
    private static final String TABLE_PERCENT_REGEXP_DEFAULT = "^(100|[1-9]?[0-9])$";  //  Nombre entier de 0 à 100, sans décimales

    public static String[] getColorTableNames() {
        return new String[]{SWTIMER_TABLES.DOT_MATRIX_DISPLAY_COLORS.toString(), SWTIMER_TABLES.STATE_BUTTONS_COLORS.toString(), SWTIMER_TABLES.BACK_SCREEN_COLORS.toString()};
    }

    enum SWTIMER_TABLES {   // Les tables, rattachées à leurs champs de data
        DOT_MATRIX_DISPLAY_COLORS(SwTimerTableDataFields.DotMatrixDisplayColors.class, "Dot matrix display"),
        STATE_BUTTONS_COLORS(SwTimerTableDataFields.StateButtonsColors.class, "CT Control buttons"),
        BACK_SCREEN_COLORS(SwTimerTableDataFields.BackScreenColors.class, "Back screen"),
        CHRONO_TIMERS(SwTimerTableDataFields.ChronoTimers.class, ""),   //  Table des Chronos et Timers
        PRESETS_CT(SwTimerTableDataFields.PresetsCT.class, ""),
        DOT_MATRIX_DISPLAY_COEFFS(SwTimerTableDataFields.DotMatrixDisplayCoeffs.class, "");

        private int dataFieldsCount;
        private String description;

        SWTIMER_TABLES(Class<? extends SwTimerTableDataFields> swTimerTableFields, String description) {
            dataFieldsCount = swTimerTableFields.getEnumConstants().length;
            this.description = description;
        }

        public String DESCRIPTION() {
            return description;
        }

        public int INDEX() {
            return ordinal();
        }

        public int getDataFieldsCount() {
            return dataFieldsCount;
        }
    }

    private interface SwTimerTableDataFields {  //  Les champs de data, par table

        enum DotMatrixDisplayColors implements SwTimerTableDataFields {
            ON_TIME("ON Time"), ON_LABEL("ON Label"), OFF("OFF"), BACK("Background");

            private String valueLabel;

            DotMatrixDisplayColors(String valueLabel) {
                this.valueLabel = valueLabel;
            }

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur

            public String LABEL() {
                return valueLabel;
            }
        }

        enum StateButtonsColors implements SwTimerTableDataFields {
            ON("ON"), OFF("OFF"), BACK("Background");

            private String valueLabel;

            StateButtonsColors(String valueLabel) {
                this.valueLabel = valueLabel;
            }

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur

            public String LABEL() {
                return valueLabel;
            }
        }

        enum BackScreenColors implements SwTimerTableDataFields {
            BACK("Background");

            private String valueLabel;

            BackScreenColors(String valueLabel) {
                this.valueLabel = valueLabel;
            }

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur

            public String LABEL() {
                return valueLabel;
            }
        }

        enum ChronoTimers implements SwTimerTableDataFields {   //  Les champs de data de la table CHRONO_TIMERS
            MODE, SELECTED, RUNNING, SPLITTED, CLOCK_APP_ALARM_REQUESTED, CLOCK_APP_ALARM_OUTDATED, LABEL, LABEL_INIT, TIME_START, TIME_ACC, TIME_ACC_UNTIL_SPLIT, TIME_DEF, TIME_DEF_INIT, TIME_EXP;

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur
        }

        enum PresetsCT implements SwTimerTableDataFields {
            TIME("Time"), LABEL("Label");

            private String valueLabel;

            PresetsCT(String valueLabel) {
                this.valueLabel = valueLabel;
            }

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur

            public String LABEL() {
                return valueLabel;
            }
        }

        enum DotMatrixDisplayCoeffs implements SwTimerTableDataFields {
            DOT_SPACING("Dot spacing"), DOT_CORNER_RADIUS("Dot corner"), SCROLL_SPEED("Scroll speed");

            private String valueLabel;

            DotMatrixDisplayCoeffs(String valueLabel) {
                this.valueLabel = valueLabel;
            }

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur

            public String LABEL() {
                return valueLabel;
            }
        }
    }

    public static int getSwTimerTableDataFieldsCount(String tableName) {
        return SWTIMER_TABLES.valueOf(tableName).getDataFieldsCount();
    }

    public static int getSwTimerTableIndex(String tableName) {
        return SWTIMER_TABLES.valueOf(tableName).INDEX();
    }

    public static String getSwTimerTableDescription(String tableName) {
        return SWTIMER_TABLES.valueOf(tableName).DESCRIPTION();
    }

    public static String[] getDescriptionsOfMultipleSwtimerTables(String[] tableNames) {
        String[] values = new String[tableNames.length];
        for (int i = 0; i <= (tableNames.length - 1); i = i + 1) {
            values[i] = getSwTimerTableDescription(tableNames[i]);
        }
        return values;
    }

    //region DOT_MATRIX_DISPLAY_COLORS
    public static String getDotMatrixDisplayColorsTableName() {
        return SWTIMER_TABLES.DOT_MATRIX_DISPLAY_COLORS.toString();
    }

    public static String[][] getDotMatrixDisplayColorsInits() {
        final String[][] TABLE_COLORS_DOT_MATRIX_DISPLAY_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.DotMatrixDisplayColors.ON_TIME.LABEL(), SwTimerTableDataFields.DotMatrixDisplayColors.ON_LABEL.LABEL(), SwTimerTableDataFields.DotMatrixDisplayColors.OFF.LABEL(), SwTimerTableDataFields.DotMatrixDisplayColors.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "999900", "00B777", "303030", "000000"}
        };
        return TABLE_COLORS_DOT_MATRIX_DISPLAY_INITS;
    }

    public static int getDotMatrixDisplayColorsOnTimeIndex() {
        return SwTimerTableDataFields.DotMatrixDisplayColors.ON_TIME.INDEX();
    }

    public static int getDotMatrixDisplayColorsOnLabelIndex() {
        return SwTimerTableDataFields.DotMatrixDisplayColors.ON_LABEL.INDEX();
    }

    public static int getDotMatrixDisplayColorsOffIndex() {
        return SwTimerTableDataFields.DotMatrixDisplayColors.OFF.INDEX();
    }

    public static int getDotMatrixDisplayColorsBackIndex() {
        return SwTimerTableDataFields.DotMatrixDisplayColors.BACK.INDEX();
    }
    //endregion

    //region STATE_BUTTONS_COLORS
    public static String getStateButtonsColorsTableName() {
        return SWTIMER_TABLES.STATE_BUTTONS_COLORS.toString();
    }

    public static String[][] getStateButtonsColorsInits() {
        final String[][] TABLE_COLOR_STATE_BUTTONS_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.StateButtonsColors.ON.LABEL(), SwTimerTableDataFields.StateButtonsColors.OFF.LABEL(), SwTimerTableDataFields.StateButtonsColors.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "0061F3", "404040", "000000"}
        };
        return TABLE_COLOR_STATE_BUTTONS_INITS;
    }

    public static int getStateButtonsColorsOnIndex() {
        return SwTimerTableDataFields.StateButtonsColors.ON.INDEX();
    }

    public static int getStateButtonsColorsOffIndex() {
        return SwTimerTableDataFields.StateButtonsColors.OFF.INDEX();
    }

    public static int getStateButtonsColorsBackIndex() {
        return SwTimerTableDataFields.StateButtonsColors.BACK.INDEX();
    }
    //endregion

    //region BACKSCREEN_COLORS
    public static String getBackScreenColorsTableName() {
        return SWTIMER_TABLES.BACK_SCREEN_COLORS.toString();
    }

    public static String[][] getBackScreenColorsInits() {
        final String[][] TABLE_COLORS_BACK_SCREEN_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.BackScreenColors.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "000000"}
        };
        return TABLE_COLORS_BACK_SCREEN_INITS;
    }

    public static int getBackScreenColorsBackIndex() {
        return SwTimerTableDataFields.BackScreenColors.BACK.INDEX();
    }
    //endregion

    //region CHRONO_TIMERS
    public static String getChronoTimersTableName() {
        return SWTIMER_TABLES.CHRONO_TIMERS.toString();
    }

    public static CtRecord chronoTimerRowToCtRecord(String[] chronoTimerRow) {
        return new CtRecord(
                Integer.parseInt(chronoTimerRow[TABLE_ID_INDEX]),
                CtRecord.MODES.valueOf(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.MODE.INDEX()]),
                (Integer.parseInt(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.SELECTED.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.RUNNING.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.SPLITTED.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.CLOCK_APP_ALARM_REQUESTED.INDEX()]) == 1),
                chronoTimerRow[SwTimerTableDataFields.ChronoTimers.LABEL.INDEX()],
                chronoTimerRow[SwTimerTableDataFields.ChronoTimers.LABEL_INIT.INDEX()],
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_START.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_ACC.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_ACC_UNTIL_SPLIT.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_DEF.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_DEF_INIT.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_EXP.INDEX()]));
    }

    public static String[] ctRecordToChronoTimerRow(CtRecord ctRecord) {
        String[] chronoTimerRow = new String[1 + SwTimerTableDataFields.ChronoTimers.values().length];  //  Champ ID + Données
        chronoTimerRow[TABLE_ID_INDEX] = String.valueOf(ctRecord.getIdct());
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.MODE.INDEX()] = ctRecord.getMode().toString();
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.SELECTED.INDEX()] = String.valueOf(ctRecord.isSelected() ? 1 : 0);
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.RUNNING.INDEX()] = String.valueOf(ctRecord.isRunning() ? 1 : 0);
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.SPLITTED.INDEX()] = String.valueOf(ctRecord.isSplitted() ? 1 : 0);
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.CLOCK_APP_ALARM_REQUESTED.INDEX()] = String.valueOf(ctRecord.isClockAppAlarmOn() ? 1 : 0);
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.LABEL.INDEX()] = ctRecord.getLabel();
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.LABEL_INIT.INDEX()] = ctRecord.getLabelInit();
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_START.INDEX()] = String.valueOf(ctRecord.getTimeStart());
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_ACC.INDEX()] = String.valueOf(ctRecord.getTimeAcc());
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_ACC_UNTIL_SPLIT.INDEX()] = String.valueOf(ctRecord.getTimeAccUntilSplit());
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_DEF.INDEX()] = String.valueOf(ctRecord.getTimeDef());
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_DEF_INIT.INDEX()] = String.valueOf(ctRecord.getTimeDefInit());
        chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_EXP.INDEX()] = String.valueOf(ctRecord.getTimeExp());
        return chronoTimerRow;
    }

    public static ArrayList<CtRecord> chronoTimerRowsToCtRecords(String[][] chronoTimerRows) {
        ArrayList<CtRecord> ctRecords = new ArrayList<CtRecord>();
        if (chronoTimerRows != null) {
            for (int i = 0; i <= (chronoTimerRows.length - 1); i = i + 1) {
                ctRecords.add(chronoTimerRowToCtRecord(chronoTimerRows[i]));
            }
        }
        return ctRecords;
    }

    public static String[][] ctRecordsToChronoTimerRows(ArrayList<CtRecord> ctRecords) {
        String[][] chronoTimerRows = null;
        if (!ctRecords.isEmpty()) {
            chronoTimerRows = new String[ctRecords.size()][];
            for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
                chronoTimerRows[i] = ctRecordToChronoTimerRow(ctRecords.get(i));
            }
        }
        return chronoTimerRows;
    }
    //endregion

    //region PRESETS_CT
    public static String getPresetsCTTableName() {
        return SWTIMER_TABLES.PRESETS_CT.toString();
    }

    public static String[][] getPresetsCTInits() {
        final String TF_REG_EXP_BEGIN = "^(?=.+)";   //  Lookahead: S'assurer qu'il y a au moins un caractère
        final String TF_REG_EXP_MID = "?";
        final String TF_REG_EXP_END = "$";
        final String TU_REG_EXP_BEGIN = "([0-9]+(";
        final String TU_REG_EXP_END = "|$))";

        String timeFormatDLRegExp = TF_REG_EXP_BEGIN;
        TIME_UNITS tu = getFirstTimeUnit();   //  1e unité à décoder
        do {   //  Construire une regexp adaptée à TIME_UNIT_PRECISION
            timeFormatDLRegExp = timeFormatDLRegExp + TU_REG_EXP_BEGIN + tu.FORMAT_DL_SEPARATOR() + TU_REG_EXP_END + TF_REG_EXP_MID;
            if (tu.equals(TIME_UNIT_PRECISION)) {
                break;
            }
            tu = tu.getNextTimeUnit();
        } while (tu != null);
        timeFormatDLRegExp = timeFormatDLRegExp + TF_REG_EXP_END;    //  Si TIME_UNIT_PRECISION = TS => "^(?=.+)([0-9]+(h|$))?([0-9]+(m|$))?([0-9]+(s|$))?([0-9]+(t|$))?$"  cad [...h][...m][...s][...t] et au moins 1 caractère

        final String[][] TABLE_PRESETS_CT_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.PresetsCT.TIME.LABEL(), SwTimerTableDataFields.PresetsCT.LABEL.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.TIME_FORMAT_DL.toString(), InputButtonsActivity.KEYBOARDS.ASCII.toString()},
                {TABLE_IDS.REGEXP.toString(), timeFormatDLRegExp, null},
                {TABLE_IDS.DEFAULT_BASE.toString(), "0", "Label"},    //  A la base des DEFAULT calculés par CtRecordsHandler et injectés dans PRESETS_CT par CtDisplayActivity en vue de PresetsActivity
                {TABLE_IDS.MAX.toString(), String.valueOf(TIME_UNITS.DAY.DURATION_MS() - TIME_UNIT_PRECISION.DURATION_MS()), null},       //  Si TS => Max 23:59:59.9
                {TABLE_IDS.TIMEUNIT.toString(), TIME_UNIT_PRECISION.toString(), null}
        };
        return TABLE_PRESETS_CT_INITS;
    }

    public static int getPresetsCTTimeIndex() {
        return SwTimerTableDataFields.PresetsCT.TIME.INDEX();
    }

    public static int getPresetsCTLabelIndex() {
        return SwTimerTableDataFields.PresetsCT.LABEL.INDEX();
    }

    public static boolean copyPresetCTRowToCtRecord(String[] presetCTRow, CtRecord ctRecord, long nowm) {
        boolean copyOK = true;
        if (!ctRecord.setTimeDef(Long.parseLong(presetCTRow[SwTimerTableDataFields.PresetsCT.TIME.INDEX()]), nowm)) {
            copyOK = false;
        }
        if (!ctRecord.setLabel(presetCTRow[SwTimerTableDataFields.PresetsCT.LABEL.INDEX()])) {
            copyOK = false;
        }
        return copyOK;
    }

    public static String[] timeLabelToPresetCTRow(long time, String label) {
        String[] presetCTRow = new String[1 + SwTimerTableDataFields.PresetsCT.values().length];  //  Champ ID + Données
        presetCTRow[TABLE_ID_INDEX] = null;
        presetCTRow[SwTimerTableDataFields.PresetsCT.TIME.INDEX()] = String.valueOf(time);
        presetCTRow[SwTimerTableDataFields.PresetsCT.LABEL.INDEX()] = label;
        return presetCTRow;
    }
    //endregion

    //region DOT_MATRIX_DISPLAY_COEFFS
    public static String getDotMatrixDisplayCoeffsTableName() {
        return SWTIMER_TABLES.DOT_MATRIX_DISPLAY_COEFFS.toString();
    }

    public static String[][] getDotMatrixDisplayCoeffsInits() {
        final String[][] TABLE_DOT_MATRIX_DISPLAY_COEFFS_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.DotMatrixDisplayCoeffs.DOT_SPACING.LABEL(), SwTimerTableDataFields.DotMatrixDisplayCoeffs.DOT_CORNER_RADIUS.LABEL(), SwTimerTableDataFields.DotMatrixDisplayCoeffs.SCROLL_SPEED.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.POSINT.toString(), InputButtonsActivity.KEYBOARDS.POSINT.toString(), InputButtonsActivity.KEYBOARDS.POSINT.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_PERCENT_REGEXP_DEFAULT, TABLE_PERCENT_REGEXP_DEFAULT, null},
                {TABLE_IDS.DEFAULT.toString(), "20", "0", "25"},    //  Points carrés par défaut ; 25 points par seconde cad +/- 4 caractères par secondes  (car un caractère avec marge droite a une largeur de 6 points)
                {TABLE_IDS.MAX.toString(), "100", "100", "100"}
        };
        return TABLE_DOT_MATRIX_DISPLAY_COEFFS_INITS;
    }

    public static int getDotMatrixDisplayCoeffsDotSpacingIndex() {
        return SwTimerTableDataFields.DotMatrixDisplayCoeffs.DOT_SPACING.INDEX();
    }

    public static int getDotMatrixDisplayCoeffsDotCornerRadiusIndex() {
        return SwTimerTableDataFields.DotMatrixDisplayCoeffs.DOT_CORNER_RADIUS.INDEX();
    }

    public static int getDotMatrixDisplayCoeffsScrollSpeedIndex() {
        return SwTimerTableDataFields.DotMatrixDisplayCoeffs.SCROLL_SPEED.INDEX();
    }
    //endregion

}
