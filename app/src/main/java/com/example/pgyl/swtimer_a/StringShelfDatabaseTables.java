package com.example.pgyl.swtimer_a;

import android.content.Context;
import android.content.res.Configuration;

import com.example.pgyl.pekislib_a.InputButtonsActivity;

import static com.example.pgyl.pekislib_a.Constants.NOT_FOUND;
import static com.example.pgyl.pekislib_a.DotMatrixDisplayView.DOT_FORM;
import static com.example.pgyl.pekislib_a.StringShelfDatabase.TABLE_ID_INDEX;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.TABLE_IDS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.getFirstTimeUnit;
import static com.example.pgyl.swtimer_a.Constants.TIME_UNIT_PRECISION;

public class StringShelfDatabaseTables {

    private interface SwTimerTables {  // Les tables, par type (Couleur ou non), rattachées à leurs champs de data
        int getDataFieldsCount();

        enum ColorYes implements SwTimerTables {   //  Les tables Couleur  (utilisées dans CtDisplayActivity et CtDisplayColorsActivity)
            DOT_MATRIX_DISPLAY_COLORS(SwTimerTableDataFields.DotMatrixDisplayColors.class, "Dot matrix Display"),   //  Table avec les couleurs du Dot Matrix Display
            STATE_BUTTONS_COLORS(SwTimerTableDataFields.StateButtonsColors.class, "CT Control buttons"),
            BACK_SCREEN_COLORS(SwTimerTableDataFields.BackScreenColors.class, "Back screen");

            private int dataFieldsCount;
            private String label;

            ColorYes(Class<? extends SwTimerTableDataFields> swTimerTableFields, String label) {
                dataFieldsCount = swTimerTableFields.getEnumConstants().length;
                this.label = label;
            }

            public String getLabel() {
                return label;
            }

            @Override
            public int getDataFieldsCount() {
                return dataFieldsCount;
            }
        }

        enum ColorNo implements SwTimerTables {  //  Les tables Non Couleur
            CHRONO_TIMERS(SwTimerTableDataFields.ChronoTimers.class),   //  Table des Chronos et Timers
            PRESETS_CT(SwTimerTableDataFields.PresetsCT.class),
            DOT_MATRIX_DISPLAY_DOT_SPACING_COEFFS(SwTimerTableDataFields.DotMatrixDisplayDotSpacingCoeffs.class),
            DOT_MATRIX_DISPLAY_DOT_FORM(SwTimerTableDataFields.DotMatrixDisplayDotForm.class);

            private int dataFieldsCount;

            ColorNo(Class<? extends SwTimerTableDataFields> swTimerTableFields) {
                dataFieldsCount = swTimerTableFields.getEnumConstants().length;
            }

            @Override
            public int getDataFieldsCount() {
                return dataFieldsCount;
            }
        }
    }

    private interface SwTimerTableDataFields {  //  Les champs de data, par table
        enum ChronoTimers implements SwTimerTableDataFields {   //  Les champs de data de la table CHRONO_TIMERS
            MODE, SELECTED, RUNNING, SPLITTED, CLOCK_APP_ALARM, LABEL, LABEL_INIT, TIME_START, TIME_ACC, TIME_ACC_UNTIL_SPLIT, TIME_DEF, TIME_DEF_INIT, TIME_EXP;

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

        enum DotMatrixDisplayDotSpacingCoeffs implements SwTimerTableDataFields {
            PORTRAIT("Portrait"), LANDSCAPE("Landscape");

            private String valueLabel;

            DotMatrixDisplayDotSpacingCoeffs(String valueLabel) {
                this.valueLabel = valueLabel;
            }

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur

            public String LABEL() {
                return valueLabel;
            }
        }

        enum DotMatrixDisplayDotForm implements SwTimerTableDataFields {
            VALUE;

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur
        }


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
    }

    private static final String TABLE_COLORS_REGEXP_HEX_DEFAULT = ".{6}";  //  Pour valider 6 caractères HEX dans INPUT_BUTTONS pour les tables decouleur (RRGGBB ou HHSSVV (dégradé))
    private static final String TABLE_PERCENT_REGEXP_DEFAULT = "^(100|[1-9]?[0-9])$";  //  Nombre entier de 0 à 100, sans décimales

    public static int getSwTimerTableDataFieldsCount(String tableName) {   //  Rechercher nombre de champs de data de tableName (existant dans l'enum SwTimerTables.ColorYes ou SwTimerTables.ColorNo)
        int ret = NOT_FOUND;  //  Ne pas utiliser valueOf(tableName) avec ColorYes puis avec ColorNo à cause du risque d'exception générée si tableName absent de l'enum
        mainLoop:
        for (Class cl : SwTimerTables.class.getClasses()) {  //  Chaque classe de SwTimerTables
            if (cl.isEnum()) {   //  Filtrer sur les enum
                Class<? extends SwTimerTables> clEnum = (Class<? extends SwTimerTables>) cl;  //  Classe -> Classe de SwTimerTables (ColorYes ou ColorNo)
                for (SwTimerTables table : clEnum.getEnumConstants()) {   //  Chaque table de l'enum
                    if (table.toString().equals(tableName)) {   //  Table trouvée
                        ret = table.getDataFieldsCount();
                        break mainLoop;  // Go, Go, Go !!
                    }
                }
            }
        }
        return ret;
    }

    //region CHRONO_TIMERS
    public static String getChronoTimersTableName() {
        return SwTimerTables.ColorNo.CHRONO_TIMERS.toString();
    }

    public static CtRecord chronoTimerRowToCtRecord(String[] chronoTimerRow, Context context) {
        CtRecord ret = new CtRecord(
                context,
                Integer.parseInt(chronoTimerRow[TABLE_ID_INDEX]),
                CtRecord.MODE.valueOf(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.MODE.INDEX()]),
                (Integer.parseInt(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.SELECTED.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.RUNNING.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.SPLITTED.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.CLOCK_APP_ALARM.INDEX()]) == 1),
                chronoTimerRow[SwTimerTableDataFields.ChronoTimers.LABEL.INDEX()],
                chronoTimerRow[SwTimerTableDataFields.ChronoTimers.LABEL_INIT.INDEX()],
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_START.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_ACC.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_ACC_UNTIL_SPLIT.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_DEF.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_DEF_INIT.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableDataFields.ChronoTimers.TIME_EXP.INDEX()]));
        return ret;
    }

    public static String[] ctRecordToChronoTimerRow(CtRecord ctRecord) {
        String[] ret = new String[1 + SwTimerTableDataFields.ChronoTimers.values().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = String.valueOf(ctRecord.getIdct());
        ret[SwTimerTableDataFields.ChronoTimers.MODE.INDEX()] = ctRecord.getMode().toString();
        ret[SwTimerTableDataFields.ChronoTimers.SELECTED.INDEX()] = String.valueOf(ctRecord.isSelected() ? 1 : 0);
        ret[SwTimerTableDataFields.ChronoTimers.RUNNING.INDEX()] = String.valueOf(ctRecord.isRunning() ? 1 : 0);
        ret[SwTimerTableDataFields.ChronoTimers.SPLITTED.INDEX()] = String.valueOf(ctRecord.isSplitted() ? 1 : 0);
        ret[SwTimerTableDataFields.ChronoTimers.CLOCK_APP_ALARM.INDEX()] = String.valueOf(ctRecord.hasClockAppAlarm() ? 1 : 0);
        ret[SwTimerTableDataFields.ChronoTimers.LABEL.INDEX()] = ctRecord.getLabel();
        ret[SwTimerTableDataFields.ChronoTimers.LABEL_INIT.INDEX()] = ctRecord.getLabelInit();
        ret[SwTimerTableDataFields.ChronoTimers.TIME_START.INDEX()] = String.valueOf(ctRecord.getTimeStart());
        ret[SwTimerTableDataFields.ChronoTimers.TIME_ACC.INDEX()] = String.valueOf(ctRecord.getTimeAcc());
        ret[SwTimerTableDataFields.ChronoTimers.TIME_ACC_UNTIL_SPLIT.INDEX()] = String.valueOf(ctRecord.getTimeAccUntilSplit());
        ret[SwTimerTableDataFields.ChronoTimers.TIME_DEF.INDEX()] = String.valueOf(ctRecord.getTimeDef());
        ret[SwTimerTableDataFields.ChronoTimers.TIME_DEF_INIT.INDEX()] = String.valueOf(ctRecord.getTimeDefInit());
        ret[SwTimerTableDataFields.ChronoTimers.TIME_EXP.INDEX()] = String.valueOf(ctRecord.getTimeExp());
        return ret;
    }
    //endregion

    //region PRESETS_CT
    public static String getPresetsCTTableName() {
        return SwTimerTables.ColorNo.PRESETS_CT.toString();
    }

    public static String[][] getPresetsCTInits() {
        final String TF_REG_EXP_BEGIN = "^";
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
        timeFormatDLRegExp = timeFormatDLRegExp + TF_REG_EXP_END;    //  Si TIME_UNIT_PRECISION = TS => "^([0-9]+(h|$))?([0-9]+(m|$))?([0-9]+(s|$))?([0-9]+(t|$))?$"  cad [...h][...m][...s][...t]

        final String[][] TABLE_PRESETS_CT_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.PresetsCT.TIME.LABEL(), SwTimerTableDataFields.PresetsCT.LABEL.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.TIME_FORMAT_DL.toString(), InputButtonsActivity.KEYBOARDS.ASCII.toString()},
                {TABLE_IDS.REGEXP.toString(), timeFormatDLRegExp, null},
                {TABLE_IDS.MAX.toString(), String.valueOf(TIME_UNITS.DAY.DURATION_MS() - TIME_UNIT_PRECISION.DURATION_MS()), null},       //  Si TS => Max 23:59:59.9
                {TABLE_IDS.TIMEUNIT.toString(), TIME_UNIT_PRECISION.toString(), null}
        };
        return TABLE_PRESETS_CT_INITS;
    }

    public static boolean copyPresetCTRowToCtRecord(String[] presetCTRow, CtRecord ctRecord, long nowm) {
        boolean ret = true;
        if (!ctRecord.setTimeDef(Long.parseLong(presetCTRow[SwTimerTableDataFields.PresetsCT.TIME.INDEX()]), nowm)) {
            ret = false;
        }
        if (!ctRecord.setLabel(presetCTRow[SwTimerTableDataFields.PresetsCT.LABEL.INDEX()])) {
            ret = false;
        }
        return ret;
    }

    public static String[] timeLabelToPresetCTRow(long time, String label) {
        String[] ret = new String[1 + SwTimerTableDataFields.PresetsCT.values().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = null;
        ret[SwTimerTableDataFields.PresetsCT.TIME.INDEX()] = String.valueOf(time);
        ret[SwTimerTableDataFields.PresetsCT.LABEL.INDEX()] = label;
        return ret;
    }
    //endregion

    //region DOT_MATRIX_DISPLAY_DOT_SPACING_COEFFS
    public static String getDotMatrixDisplayDotSpacingCoeffsTableName() {
        return SwTimerTables.ColorNo.DOT_MATRIX_DISPLAY_DOT_SPACING_COEFFS.toString();
    }

    public static String[][] getDotMatrixDisplayDotSpacingCoeffsInits() {
        final String[][] TABLE_DOT_MATRIX_DISPLAY_DOT_SPACING_COEFFS_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.DotMatrixDisplayDotSpacingCoeffs.PORTRAIT.LABEL(), SwTimerTableDataFields.DotMatrixDisplayDotSpacingCoeffs.LANDSCAPE.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.POSINT.toString(), InputButtonsActivity.KEYBOARDS.POSINT.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_PERCENT_REGEXP_DEFAULT, TABLE_PERCENT_REGEXP_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "20", "20"}
        };
        return TABLE_DOT_MATRIX_DISPLAY_DOT_SPACING_COEFFS_INITS;
    }

    public static int getDotMatrixDisplayDotSpacingCoeffLandscapeIndex() {
        return SwTimerTableDataFields.DotMatrixDisplayDotSpacingCoeffs.LANDSCAPE.INDEX();
    }

    public static int getDotMatrixDisplayDotSpacingCoeffPortraitIndex() {
        return SwTimerTableDataFields.DotMatrixDisplayDotSpacingCoeffs.PORTRAIT.INDEX();
    }

    public static int getOrientationDotMatrixDisplayDotSpacingCoeffIndex(int orientation) {
        return (orientation == Configuration.ORIENTATION_PORTRAIT) ? getDotMatrixDisplayDotSpacingCoeffPortraitIndex() : getDotMatrixDisplayDotSpacingCoeffLandscapeIndex();
    }

    //region DOT_MATRIX_DISPLAY_DOT_FORM
    public static String getDotMatrixDisplayDotFormTableName() {
        return SwTimerTables.ColorNo.DOT_MATRIX_DISPLAY_DOT_FORM.toString();
    }

    public static String[][] getDotMatrixDisplayDotFormInits() {
        final String[][] TABLE_DOT_MATRIX_DISPLAY_DOT_FORM_INITS = {
                {TABLE_IDS.DEFAULT.toString(), DOT_FORM.SQUARE.toString()}};
        return TABLE_DOT_MATRIX_DISPLAY_DOT_FORM_INITS;
    }

    public static int getDotMatrixDisplayDotFormValueIndex() {
        return SwTimerTableDataFields.DotMatrixDisplayDotForm.VALUE.INDEX();
    }
    //endregion

    //region DOT_MATRIX_DISPLAY_COLORS
    public static String getDotMatrixDisplayColorsTableName() {
        return SwTimerTables.ColorYes.DOT_MATRIX_DISPLAY_COLORS.toString();
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
        return SwTimerTables.ColorYes.STATE_BUTTONS_COLORS.toString();
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
        return SwTimerTables.ColorYes.BACK_SCREEN_COLORS.toString();
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

    //region TABLES COLOR_YES
    public static int getColorTablesCount() {
        return SwTimerTables.ColorYes.values().length;
    }

    public static String getColorTableName(int colorTableIndex) {
        return SwTimerTables.ColorYes.values()[colorTableIndex].toString();
    }

    public static int getColorTableIndex(String colorTableName) {
        return SwTimerTables.ColorYes.valueOf(colorTableName).ordinal();
    }

    public static String[] getColorTablesLabels() {
        String[] values = new String[getColorTablesCount()];
        for (int i = 0; i <= (getColorTablesCount() - 1); i = i + 1) {
            values[i] = SwTimerTables.ColorYes.valueOf(getColorTableName(i)).getLabel();
        }
        return values;
    }
    //endregion

}
