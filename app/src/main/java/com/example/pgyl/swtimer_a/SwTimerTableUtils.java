package com.example.pgyl.swtimer_a;

import android.content.Context;

import com.example.pgyl.pekislib_a.InputButtonsActivity;
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils;
import com.example.pgyl.pekislib_a.TimeDateUtils;

import static com.example.pgyl.pekislib_a.Constants.UNDEFINED;
import static com.example.pgyl.pekislib_a.StringShelfDatabase.TABLE_ID_INDEX;
import static com.example.pgyl.pekislib_a.TimeDateUtils.xhmsToMs;

public class SwTimerTableUtils {
    private enum SWTIMER_TABLES {   //  "Enum of enums"
        CHRONO_TIMERS(SwTimerTableAndFields.ChronoTimers.class, ""),
        PRESETS_CT(SwTimerTableAndFields.PresetsCT.class, ""),
        COLORS_DOT_MATRIX_DISPLAY(SwTimerTableAndFields.ColorsDotMatrixDisplay.class, "Dot matrix Display"),
        COLORS_BUTTONS(SwTimerTableAndFields.ColorsButtons.class, "CT Control buttons"),
        COLORS_BACK_SCREEN(SwTimerTableAndFields.ColorsBackScreen.class, "Back screen");

        private SwTimerTableAndFields[] tableFields;
        private int colorTypeIndex;
        private String valueColorTypeLabel;
        private final int COLOR_TYPE_INDEX_UNDEFINED = -2;
        private final int COLOR_TYPE_INDEX_NOT_FOUND = -1;

        SWTIMER_TABLES(Class<? extends SwTimerTableAndFields> swTimerTableFields, String valueColorTypeLabel) {
            tableFields = swTimerTableFields.getEnumConstants();
            this.valueColorTypeLabel = valueColorTypeLabel;
            colorTypeIndex = COLOR_TYPE_INDEX_UNDEFINED;
        }

        public SwTimerTableAndFields[] TABLE_FIELDS() {
            return tableFields;
        }

        public String COLOR_TYPE_LABEL() {
            return valueColorTypeLabel;
        }

        public int COLOR_TYPE_INDEX() {
            if (colorTypeIndex == COLOR_TYPE_INDEX_UNDEFINED) {   //  Au 1er appel (colorTypeIndex undefined), trouver l'index de la table dans colorTypes (qui doit attendre la construction complète de SWTIMERS_TABLES avant de pouvoir être utilisé)
                colorTypeIndex = COLOR_TYPE_INDEX_NOT_FOUND;
                for (int i = 0; i <= (colorTypes.length - 1); i = i + 1) {
                    if (this.toString().equals(colorTypes[i].toString())) {
                        colorTypeIndex = i;
                    }
                }
            }
            return colorTypeIndex;
        }
    }

    private static final SWTIMER_TABLES[] colorTypes = {SWTIMER_TABLES.COLORS_DOT_MATRIX_DISPLAY, SWTIMER_TABLES.COLORS_BUTTONS, SWTIMER_TABLES.COLORS_BACK_SCREEN};  //  Chaque type de couleur utilisée dans CtDisplayActivity est reliée à la table correspondante

    private interface SwTimerTableAndFields {
        enum ChronoTimers implements SwTimerTableAndFields {
            MODE, SELECTED, RUNNING, SPLITTED, ALARM_SET, MESSAGE, MESSAGE_INIT, TIME_START, TIME_ACC, TIME_ACC_UNTIL_SPLIT, TIME_DEF, TIME_DEF_INIT, TIME_EXP;

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur
        }

        enum PresetsCT implements SwTimerTableAndFields {
            TIME("Time"), MESSAGE("Message");

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

        enum ColorsDotMatrixDisplay implements SwTimerTableAndFields {
            ON_TIME("ON Time"), ON_MESSAGE("ON Message"), OFF("OFF"), BACK("Background");

            private String valueLabel;

            ColorsDotMatrixDisplay(String valueLabel) {
                this.valueLabel = valueLabel;
            }

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur

            public String LABEL() {
                return valueLabel;
            }
        }

        enum ColorsButtons implements SwTimerTableAndFields {
            ON("ON"), OFF("OFF"), BACK("Background");

            private String valueLabel;

            ColorsButtons(String valueLabel) {
                this.valueLabel = valueLabel;
            }

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur

            public String LABEL() {
                return valueLabel;
            }
        }

        enum ColorsBackScreen implements SwTimerTableAndFields {
            BACK("Background");

            private String valueLabel;

            ColorsBackScreen(String valueLabel) {
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

    private static final String TABLE_COLORS_REGEXP_HEX_DEFAULT = ".{6}";  // Pour valider 6 caractères HEX dans INPUT_BUTTONS pour la table COLORS (RRGGBB)

    public static int getSwtimerTableDataFieldsCount(String tableName) {
        return SWTIMER_TABLES.valueOf(tableName).TABLE_FIELDS().length;
    }

    //region CHRONO_TIMERS
    public static String getChronoTimersTableName() {
        return SWTIMER_TABLES.CHRONO_TIMERS.toString();
    }

    public static CtRecord chronoTimerRowToCtRecord(String[] chronoTimerRow, Context context) {
        CtRecord ret = new CtRecord(
                context,
                Integer.parseInt(chronoTimerRow[TABLE_ID_INDEX]),
                CtRecord.MODE.valueOf(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.MODE.INDEX()]),
                (Integer.parseInt(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.SELECTED.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.RUNNING.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.SPLITTED.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.ALARM_SET.INDEX()]) == 1),
                chronoTimerRow[SwTimerTableAndFields.ChronoTimers.MESSAGE.INDEX()],
                chronoTimerRow[SwTimerTableAndFields.ChronoTimers.MESSAGE_INIT.INDEX()],
                Long.parseLong(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.TIME_START.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.TIME_ACC.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.TIME_ACC_UNTIL_SPLIT.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.TIME_DEF.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.TIME_DEF_INIT.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableAndFields.ChronoTimers.TIME_EXP.INDEX()]));
        return ret;
    }

    public static String[] ctRecordToChronoTimerRow(CtRecord ctRecord) {
        String[] ret = new String[1 + SWTIMER_TABLES.CHRONO_TIMERS.TABLE_FIELDS().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = String.valueOf(ctRecord.getIdct());
        ret[SwTimerTableAndFields.ChronoTimers.MODE.INDEX()] = ctRecord.getMode().toString();
        ret[SwTimerTableAndFields.ChronoTimers.SELECTED.INDEX()] = String.valueOf(ctRecord.isSelected() ? 1 : 0);
        ret[SwTimerTableAndFields.ChronoTimers.RUNNING.INDEX()] = String.valueOf(ctRecord.isRunning() ? 1 : 0);
        ret[SwTimerTableAndFields.ChronoTimers.SPLITTED.INDEX()] = String.valueOf(ctRecord.isSplitted() ? 1 : 0);
        ret[SwTimerTableAndFields.ChronoTimers.ALARM_SET.INDEX()] = String.valueOf(ctRecord.hasClockAppAlarm() ? 1 : 0);
        ret[SwTimerTableAndFields.ChronoTimers.MESSAGE.INDEX()] = ctRecord.getMessage();
        ret[SwTimerTableAndFields.ChronoTimers.MESSAGE_INIT.INDEX()] = ctRecord.getMessageInit();
        ret[SwTimerTableAndFields.ChronoTimers.TIME_START.INDEX()] = String.valueOf(ctRecord.getTimeStart());
        ret[SwTimerTableAndFields.ChronoTimers.TIME_ACC.INDEX()] = String.valueOf(ctRecord.getTimeAcc());
        ret[SwTimerTableAndFields.ChronoTimers.TIME_ACC_UNTIL_SPLIT.INDEX()] = String.valueOf(ctRecord.getTimeAccUntilSplit());
        ret[SwTimerTableAndFields.ChronoTimers.TIME_DEF.INDEX()] = String.valueOf(ctRecord.getTimeDef());
        ret[SwTimerTableAndFields.ChronoTimers.TIME_DEF_INIT.INDEX()] = String.valueOf(ctRecord.getTimeDefInit());
        ret[SwTimerTableAndFields.ChronoTimers.TIME_EXP.INDEX()] = String.valueOf(ctRecord.getTimeExp());
        return ret;
    }
    //endregion

    //region PRESETS_CT
    public static String getPresetsCTTableName() {
        return SWTIMER_TABLES.PRESETS_CT.toString();
    }

    public static String[][] getPresetsCTInits() {
        final String[][] TABLE_PRESETS_CT_INITS = {
                {StringShelfDatabaseUtils.TABLE_IDS.LABEL.toString(), SwTimerTableAndFields.PresetsCT.TIME.LABEL(), SwTimerTableAndFields.PresetsCT.MESSAGE.LABEL()},
                {StringShelfDatabaseUtils.TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.TIME_XHMS.toString(), InputButtonsActivity.KEYBOARDS.ASCII.toString()},
                {StringShelfDatabaseUtils.TABLE_IDS.REGEXP.toString(), "^([0-9]+(h|$))?([0-9]+(m|$))?([0-9]+(s|$))?([0-9]+(c|$))?$", null},
                {StringShelfDatabaseUtils.TABLE_IDS.MAX.toString(), String.valueOf(xhmsToMs("23h59m59s99c")), null},
                {StringShelfDatabaseUtils.TABLE_IDS.TIMEUNIT.toString(), TimeDateUtils.TIMEUNITS.CS.toString(), null}
        };
        return TABLE_PRESETS_CT_INITS;
    }

    public static boolean copyPresetCTRowToCtRecord(String[] presetCTRow, CtRecord ctRecord, long nowm) {
        boolean ret = true;
        if (!ctRecord.setTimeDef(Long.parseLong(presetCTRow[SwTimerTableAndFields.PresetsCT.TIME.INDEX()]), nowm)) {
            ret = false;
        }
        if (!ctRecord.setMessage(presetCTRow[SwTimerTableAndFields.PresetsCT.MESSAGE.INDEX()])) {
            ret = false;
        }
        return ret;
    }

    public static String[] timeMessageToPresetCTRow(long time, String message) {
        String[] ret = new String[1 + SwTimerTableAndFields.PresetsCT.values().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = null;
        ret[SwTimerTableAndFields.PresetsCT.TIME.INDEX()] = String.valueOf(time);
        ret[SwTimerTableAndFields.PresetsCT.MESSAGE.INDEX()] = message;
        return ret;
    }
    //endregion

    //region COLORS_DOT_MATRIX_DISPLAY
    public static String getColorsDotMatrixDisplayTableName() {
        return SWTIMER_TABLES.COLORS_DOT_MATRIX_DISPLAY.toString();
    }

    public static String[][] getColorsDotMatrixDisplayInits() {
        final String[][] TABLE_COLORS_DOT_MATRIX_DISPLAY_INITS = {
                {StringShelfDatabaseUtils.TABLE_IDS.LABEL.toString(), SwTimerTableAndFields.ColorsDotMatrixDisplay.ON_TIME.LABEL(), SwTimerTableAndFields.ColorsDotMatrixDisplay.ON_MESSAGE.LABEL(), SwTimerTableAndFields.ColorsDotMatrixDisplay.OFF.LABEL(), SwTimerTableAndFields.ColorsDotMatrixDisplay.BACK.LABEL()},
                {StringShelfDatabaseUtils.TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {StringShelfDatabaseUtils.TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {StringShelfDatabaseUtils.TABLE_IDS.DEFAULT.toString(), "999900", "00B777", "303030", "000000"}
        };
        return TABLE_COLORS_DOT_MATRIX_DISPLAY_INITS;
    }

    public static int getDotMatrixDisplayColorOnTimeIndex() {
        return SwTimerTableAndFields.ColorsDotMatrixDisplay.ON_TIME.INDEX();
    }

    public static int getDotMatrixDisplayColorOnMessageIndex() {
        return SwTimerTableAndFields.ColorsDotMatrixDisplay.ON_MESSAGE.INDEX();
    }

    public static int getDotMatrixDisplayColorOffIndex() {
        return SwTimerTableAndFields.ColorsDotMatrixDisplay.OFF.INDEX();
    }

    public static int getDotMatrixDisplayColorBackIndex() {
        return SwTimerTableAndFields.ColorsDotMatrixDisplay.BACK.INDEX();
    }
    //endregion

    //region COLORS_BUTTONS
    public static String getColorsButtonsTableName() {
        return SWTIMER_TABLES.COLORS_BUTTONS.toString();
    }

    public static String[][] getColorsButtonsInits() {
        final String[][] TABLE_COLOR_BUTTONS_INITS = {
                {StringShelfDatabaseUtils.TABLE_IDS.LABEL.toString(), SwTimerTableAndFields.ColorsButtons.ON.LABEL(), SwTimerTableAndFields.ColorsButtons.OFF.LABEL(), SwTimerTableAndFields.ColorsButtons.BACK.LABEL()},
                {StringShelfDatabaseUtils.TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {StringShelfDatabaseUtils.TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {StringShelfDatabaseUtils.TABLE_IDS.DEFAULT.toString(), "0061F3", "696969", "000000"}
        };
        return TABLE_COLOR_BUTTONS_INITS;
    }

    public static int getButtonsColorOnIndex() {
        return SwTimerTableAndFields.ColorsButtons.ON.INDEX();
    }

    public static int getButtonsColorOffIndex() {
        return SwTimerTableAndFields.ColorsButtons.OFF.INDEX();
    }

    public static int getButtonsColorBackIndex() {
        return SwTimerTableAndFields.ColorsButtons.BACK.INDEX();
    }
    //endregion

    //region COLORS_BACKSCREEN
    public static String getColorsBackScreenTableName() {
        return SWTIMER_TABLES.COLORS_BACK_SCREEN.toString();
    }

    public static String[][] getColorsBackScreenInits() {
        final String[][] TABLE_COLORS_BACK_SCREEN_INITS = {
                {StringShelfDatabaseUtils.TABLE_IDS.LABEL.toString(), SwTimerTableAndFields.ColorsBackScreen.BACK.LABEL()},
                {StringShelfDatabaseUtils.TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {StringShelfDatabaseUtils.TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {StringShelfDatabaseUtils.TABLE_IDS.DEFAULT.toString(), "000000"}
        };
        return TABLE_COLORS_BACK_SCREEN_INITS;
    }

    public static int getBackScreenColorBackIndex() {
        return SwTimerTableAndFields.ColorsBackScreen.BACK.INDEX();
    }
    //endregion

    //region COLOR_TYPES
    public static int getColorTypesCount() {
        return colorTypes.length;
    }

    public static String getColorTableName(int colorTypeIndex) {
        return colorTypes[colorTypeIndex].toString();
    }

    public static int getColorTypeIndex(String colorTableName) {
        return SWTIMER_TABLES.valueOf(colorTableName).COLOR_TYPE_INDEX();
    }

    public static String getColorTypeLabel(String colorTableName) {
        return SWTIMER_TABLES.valueOf(colorTableName).COLOR_TYPE_LABEL();
    }
    //endregion

}



