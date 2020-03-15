package com.example.pgyl.swtimer_a;

import android.content.Context;

import com.example.pgyl.pekislib_a.InputButtonsActivity;

import static com.example.pgyl.pekislib_a.Constants.NOT_FOUND;
import static com.example.pgyl.pekislib_a.StringShelfDatabase.TABLE_ID_INDEX;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.TABLE_IDS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.timeFormatDLToMs;
import static com.example.pgyl.swtimer_a.Constants.TIME_UNIT_PRECISION;

public class StringShelfDatabaseTables {

    private interface SwTimerTables {  // Les tables, par type (Couleur ou non), rattachées à leurs champs de data
        int getDataFieldsCount();

        enum ColorYes implements SwTimerTables {   //  Les tables Couleur  (utilisées dans CtDisplayActivity et CtDisplayColorsActivity)
            DOT_MATRIX_DISPLAY(SwTimerTableDataFields.DotMatrixDisplay.class, "Dot matrix Display"),   //  Table avec les couleurs du Dot Matrix Display
            STATE_BUTTONS(SwTimerTableDataFields.StateButtons.class, "CT Control buttons"),
            BACK_SCREEN(SwTimerTableDataFields.BackScreen.class, "Back screen");

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
            PRESETS_CT(SwTimerTableDataFields.PresetsCT.class);

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

        enum DotMatrixDisplay implements SwTimerTableDataFields {
            ON_TIME("ON Time"), ON_LABEL("ON Label"), OFF("OFF"), BACK("Background");

            private String valueLabel;

            DotMatrixDisplay(String valueLabel) {
                this.valueLabel = valueLabel;
            }

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur

            public String LABEL() {
                return valueLabel;
            }
        }

        enum StateButtons implements SwTimerTableDataFields {
            ON("ON"), OFF("OFF"), BACK("Background");

            private String valueLabel;

            StateButtons(String valueLabel) {
                this.valueLabel = valueLabel;
            }

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur

            public String LABEL() {
                return valueLabel;
            }
        }

        enum BackScreen implements SwTimerTableDataFields {
            BACK("Background");

            private String valueLabel;

            BackScreen(String valueLabel) {
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
        TIME_UNITS tu = TIME_UNITS.HOUR;   //  1e unité à décoder
        do {   //  Construire une regexp adaptée à TIME_UNIT_PRECISION
            timeFormatDLRegExp = timeFormatDLRegExp + TU_REG_EXP_BEGIN + tu.SEPARATOR_DL() + TU_REG_EXP_END + TF_REG_EXP_MID;
            if (tu.equals(TIME_UNIT_PRECISION)) {
                break;
            }
            tu = tu.getNextDecodeUnit();
        } while (tu != null);
        timeFormatDLRegExp = timeFormatDLRegExp + TF_REG_EXP_END;    //  Si TIME_UNIT_PRECISION = TS => "^([0-9]+(h|$))?([0-9]+(m|$))?([0-9]+(s|$))?([0-9]+(t|$))?$"  cad [...h][...m][...s][...t]

        final String[][] TABLE_PRESETS_CT_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.PresetsCT.TIME.LABEL(), SwTimerTableDataFields.PresetsCT.LABEL.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.TIME_FORMAT_DL.toString(), InputButtonsActivity.KEYBOARDS.ASCII.toString()},
                {TABLE_IDS.REGEXP.toString(), timeFormatDLRegExp, null},
                {TABLE_IDS.MAX.toString(), String.valueOf(timeFormatDLToMs("23h59m59s999")), null},
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

    //region DOT_MATRIX_DISPLAY
    public static String getDotMatrixDisplayTableName() {
        return SwTimerTables.ColorYes.DOT_MATRIX_DISPLAY.toString();
    }

    public static String[][] getDotMatrixDisplayInits() {
        final String[][] TABLE_COLORS_DOT_MATRIX_DISPLAY_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.DotMatrixDisplay.ON_TIME.LABEL(), SwTimerTableDataFields.DotMatrixDisplay.ON_LABEL.LABEL(), SwTimerTableDataFields.DotMatrixDisplay.OFF.LABEL(), SwTimerTableDataFields.DotMatrixDisplay.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "999900", "00B777", "303030", "000000"}
        };
        return TABLE_COLORS_DOT_MATRIX_DISPLAY_INITS;
    }

    public static int getDotMatrixDisplayOnTimeIndex() {
        return SwTimerTableDataFields.DotMatrixDisplay.ON_TIME.INDEX();
    }

    public static int getDotMatrixDisplayOnLabelIndex() {
        return SwTimerTableDataFields.DotMatrixDisplay.ON_LABEL.INDEX();
    }

    public static int getDotMatrixDisplayOffIndex() {
        return SwTimerTableDataFields.DotMatrixDisplay.OFF.INDEX();
    }

    public static int getDotMatrixDisplayBackIndex() {
        return SwTimerTableDataFields.DotMatrixDisplay.BACK.INDEX();
    }
    //endregion

    //region STATE_BUTTONS
    public static String getStateButtonsTableName() {
        return SwTimerTables.ColorYes.STATE_BUTTONS.toString();
    }

    public static String[][] getStateButtonsInits() {
        final String[][] TABLE_COLOR_STATE_BUTTONS_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.StateButtons.ON.LABEL(), SwTimerTableDataFields.StateButtons.OFF.LABEL(), SwTimerTableDataFields.StateButtons.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "0061F3", "404040", "000000"}
        };
        return TABLE_COLOR_STATE_BUTTONS_INITS;
    }

    public static int getStateButtonsOnIndex() {
        return SwTimerTableDataFields.StateButtons.ON.INDEX();
    }

    public static int getStateButtonsOffIndex() {
        return SwTimerTableDataFields.StateButtons.OFF.INDEX();
    }

    public static int getStateButtonsBackIndex() {
        return SwTimerTableDataFields.StateButtons.BACK.INDEX();
    }
    //endregion

    //region COLORS_BACKSCREEN
    public static String getBackScreenTableName() {
        return SwTimerTables.ColorYes.BACK_SCREEN.toString();
    }

    public static String[][] getBackScreenInits() {
        final String[][] TABLE_COLORS_BACK_SCREEN_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableDataFields.BackScreen.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "000000"}
        };
        return TABLE_COLORS_BACK_SCREEN_INITS;
    }

    public static int getBackScreenBackIndex() {
        return SwTimerTableDataFields.BackScreen.BACK.INDEX();
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

    public static String getColorTableLabel(String colorTableName) {
        return SwTimerTables.ColorYes.valueOf(colorTableName).getLabel();
    }
    //endregion

}



