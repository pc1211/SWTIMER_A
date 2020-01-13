package com.example.pgyl.swtimer_a;

import android.content.Context;

import com.example.pgyl.pekislib_a.InputButtonsActivity;
import com.example.pgyl.pekislib_a.TimeDateUtils;

import static com.example.pgyl.pekislib_a.StringShelfDatabase.TABLE_ID_INDEX;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.TABLE_IDS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.xhmsToMs;

public class StringShelfDatabaseTables {
    private enum SWTIMER_TABLES {   //  "Enum of enums"
        CHRONO_TIMERS(SwTimerTableFields.ChronoTimers.class, ""),
        PRESETS_CT(SwTimerTableFields.PresetsCT.class, ""),
        COLORS_DOT_MATRIX_DISPLAY(SwTimerTableFields.ColorsDotMatrixDisplay.class, "Dot matrix Display"),
        COLORS_BUTTONS(SwTimerTableFields.ColorsButtons.class, "CT Control buttons"),
        COLORS_BACK_SCREEN(SwTimerTableFields.ColorsBackScreen.class, "Back screen");

        private SwTimerTableFields[] tableFields;
        private int colorTableIndex;
        private String colorTableLabel;
        private final int COLOR_TABLE_INDEX_UNDEFINED = -2;
        private final int COLOR_TABLE_INDEX_NOT_FOUND = -1;

        SWTIMER_TABLES(Class<? extends SwTimerTableFields> swTimerTableFields, String colorTableLabel) {
            tableFields = swTimerTableFields.getEnumConstants();
            this.colorTableLabel = colorTableLabel;
            colorTableIndex = COLOR_TABLE_INDEX_UNDEFINED;
        }

        public SwTimerTableFields[] TABLE_FIELDS() {
            return tableFields;
        }

        public String COLOR_TABLE_LABEL() {
            return colorTableLabel;
        }

        public int COLOR_TABLE_INDEX() {
            if (colorTableIndex == COLOR_TABLE_INDEX_UNDEFINED) {   //  Lazy ... Au 1er appel de COLOR_TYPE_INDEX() (après initialisation complète de SWTIMERS_TABLES), trouver l'index de la table dans SWTIMER_COLOR_TABLES
                colorTableIndex = COLOR_TABLE_INDEX_NOT_FOUND;
                for (int i = 0; i <= (SWTIMER_COLOR_TABLES.length - 1); i = i + 1) {
                    if (this.toString().equals(SWTIMER_COLOR_TABLES[i].toString())) {   //  Trouvé; on connaît maintenant l'index de la table dans SWTIMER_COLOR_TABLES
                        colorTableIndex = i;
                        break;
                    }
                }
            }
            return colorTableIndex;
        }
    }

    private static final SWTIMER_TABLES[] SWTIMER_COLOR_TABLES = {SWTIMER_TABLES.COLORS_DOT_MATRIX_DISPLAY, SWTIMER_TABLES.COLORS_BUTTONS, SWTIMER_TABLES.COLORS_BACK_SCREEN};  //  Chaque type de couleur utilisée dans CtDisplayActivity est reliée à la table correspondante

    private interface SwTimerTableFields {
        enum ChronoTimers implements SwTimerTableFields {
            MODE, SELECTED, RUNNING, SPLITTED, ALARM_SET, MESSAGE, MESSAGE_INIT, TIME_START, TIME_ACC, TIME_ACC_UNTIL_SPLIT, TIME_DEF, TIME_DEF_INIT, TIME_EXP;

            public int INDEX() {
                return ordinal() + 1;
            }   //  INDEX 0 pour identifiant utilisateur
        }

        enum PresetsCT implements SwTimerTableFields {
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

        enum ColorsDotMatrixDisplay implements SwTimerTableFields {
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

        enum ColorsButtons implements SwTimerTableFields {
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

        enum ColorsBackScreen implements SwTimerTableFields {
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

    private static final String TABLE_COLORS_REGEXP_HEX_DEFAULT = ".{6}";  // Pour valider 6 caractères HEX dans INPUT_BUTTONS pour les tables decouleur (RRGGBB ou HHSSVV (dégradé))

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
                CtRecord.MODE.valueOf(chronoTimerRow[SwTimerTableFields.ChronoTimers.MODE.INDEX()]),
                (Integer.parseInt(chronoTimerRow[SwTimerTableFields.ChronoTimers.SELECTED.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableFields.ChronoTimers.RUNNING.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableFields.ChronoTimers.SPLITTED.INDEX()]) == 1),
                (Integer.parseInt(chronoTimerRow[SwTimerTableFields.ChronoTimers.ALARM_SET.INDEX()]) == 1),
                chronoTimerRow[SwTimerTableFields.ChronoTimers.MESSAGE.INDEX()],
                chronoTimerRow[SwTimerTableFields.ChronoTimers.MESSAGE_INIT.INDEX()],
                Long.parseLong(chronoTimerRow[SwTimerTableFields.ChronoTimers.TIME_START.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableFields.ChronoTimers.TIME_ACC.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableFields.ChronoTimers.TIME_ACC_UNTIL_SPLIT.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableFields.ChronoTimers.TIME_DEF.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableFields.ChronoTimers.TIME_DEF_INIT.INDEX()]),
                Long.parseLong(chronoTimerRow[SwTimerTableFields.ChronoTimers.TIME_EXP.INDEX()]));
        return ret;
    }

    public static String[] ctRecordToChronoTimerRow(CtRecord ctRecord) {
        String[] ret = new String[1 + SWTIMER_TABLES.CHRONO_TIMERS.TABLE_FIELDS().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = String.valueOf(ctRecord.getIdct());
        ret[SwTimerTableFields.ChronoTimers.MODE.INDEX()] = ctRecord.getMode().toString();
        ret[SwTimerTableFields.ChronoTimers.SELECTED.INDEX()] = String.valueOf(ctRecord.isSelected() ? 1 : 0);
        ret[SwTimerTableFields.ChronoTimers.RUNNING.INDEX()] = String.valueOf(ctRecord.isRunning() ? 1 : 0);
        ret[SwTimerTableFields.ChronoTimers.SPLITTED.INDEX()] = String.valueOf(ctRecord.isSplitted() ? 1 : 0);
        ret[SwTimerTableFields.ChronoTimers.ALARM_SET.INDEX()] = String.valueOf(ctRecord.hasClockAppAlarm() ? 1 : 0);
        ret[SwTimerTableFields.ChronoTimers.MESSAGE.INDEX()] = ctRecord.getMessage();
        ret[SwTimerTableFields.ChronoTimers.MESSAGE_INIT.INDEX()] = ctRecord.getMessageInit();
        ret[SwTimerTableFields.ChronoTimers.TIME_START.INDEX()] = String.valueOf(ctRecord.getTimeStart());
        ret[SwTimerTableFields.ChronoTimers.TIME_ACC.INDEX()] = String.valueOf(ctRecord.getTimeAcc());
        ret[SwTimerTableFields.ChronoTimers.TIME_ACC_UNTIL_SPLIT.INDEX()] = String.valueOf(ctRecord.getTimeAccUntilSplit());
        ret[SwTimerTableFields.ChronoTimers.TIME_DEF.INDEX()] = String.valueOf(ctRecord.getTimeDef());
        ret[SwTimerTableFields.ChronoTimers.TIME_DEF_INIT.INDEX()] = String.valueOf(ctRecord.getTimeDefInit());
        ret[SwTimerTableFields.ChronoTimers.TIME_EXP.INDEX()] = String.valueOf(ctRecord.getTimeExp());
        return ret;
    }
    //endregion

    //region PRESETS_CT
    public static String getPresetsCTTableName() {
        return SWTIMER_TABLES.PRESETS_CT.toString();
    }

    public static String[][] getPresetsCTInits() {
        final String[][] TABLE_PRESETS_CT_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableFields.PresetsCT.TIME.LABEL(), SwTimerTableFields.PresetsCT.MESSAGE.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.TIME_XHMS.toString(), InputButtonsActivity.KEYBOARDS.ASCII.toString()},
                {TABLE_IDS.REGEXP.toString(), "^([0-9]+(h|$))?([0-9]+(m|$))?([0-9]+(s|$))?([0-9]+(c|$))?$", null},
                {TABLE_IDS.MAX.toString(), String.valueOf(xhmsToMs("23h59m59s99c")), null},
                {TABLE_IDS.TIMEUNIT.toString(), TimeDateUtils.TIMEUNITS.CS.toString(), null}
        };
        return TABLE_PRESETS_CT_INITS;
    }

    public static boolean copyPresetCTRowToCtRecord(String[] presetCTRow, CtRecord ctRecord, long nowm) {
        boolean ret = true;
        if (!ctRecord.setTimeDef(Long.parseLong(presetCTRow[SwTimerTableFields.PresetsCT.TIME.INDEX()]), nowm)) {
            ret = false;
        }
        if (!ctRecord.setMessage(presetCTRow[SwTimerTableFields.PresetsCT.MESSAGE.INDEX()])) {
            ret = false;
        }
        return ret;
    }

    public static String[] timeMessageToPresetCTRow(long time, String message) {
        String[] ret = new String[1 + SwTimerTableFields.PresetsCT.values().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = null;
        ret[SwTimerTableFields.PresetsCT.TIME.INDEX()] = String.valueOf(time);
        ret[SwTimerTableFields.PresetsCT.MESSAGE.INDEX()] = message;
        return ret;
    }
    //endregion

    //region COLORS_DOT_MATRIX_DISPLAY
    public static String getColorsDotMatrixDisplayTableName() {
        return SWTIMER_TABLES.COLORS_DOT_MATRIX_DISPLAY.toString();
    }

    public static String[][] getColorsDotMatrixDisplayInits() {
        final String[][] TABLE_COLORS_DOT_MATRIX_DISPLAY_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableFields.ColorsDotMatrixDisplay.ON_TIME.LABEL(), SwTimerTableFields.ColorsDotMatrixDisplay.ON_MESSAGE.LABEL(), SwTimerTableFields.ColorsDotMatrixDisplay.OFF.LABEL(), SwTimerTableFields.ColorsDotMatrixDisplay.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "999900", "00B777", "303030", "000000"}
        };
        return TABLE_COLORS_DOT_MATRIX_DISPLAY_INITS;
    }

    public static int getDotMatrixDisplayColorOnTimeIndex() {
        return SwTimerTableFields.ColorsDotMatrixDisplay.ON_TIME.INDEX();
    }

    public static int getDotMatrixDisplayColorOnMessageIndex() {
        return SwTimerTableFields.ColorsDotMatrixDisplay.ON_MESSAGE.INDEX();
    }

    public static int getDotMatrixDisplayColorOffIndex() {
        return SwTimerTableFields.ColorsDotMatrixDisplay.OFF.INDEX();
    }

    public static int getDotMatrixDisplayColorBackIndex() {
        return SwTimerTableFields.ColorsDotMatrixDisplay.BACK.INDEX();
    }
    //endregion

    //region COLORS_BUTTONS
    public static String getColorsButtonsTableName() {
        return SWTIMER_TABLES.COLORS_BUTTONS.toString();
    }

    public static String[][] getColorsButtonsInits() {
        final String[][] TABLE_COLOR_BUTTONS_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableFields.ColorsButtons.ON.LABEL(), SwTimerTableFields.ColorsButtons.OFF.LABEL(), SwTimerTableFields.ColorsButtons.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "0061F3", "696969", "000000"}
        };
        return TABLE_COLOR_BUTTONS_INITS;
    }

    public static int getButtonsColorOnIndex() {
        return SwTimerTableFields.ColorsButtons.ON.INDEX();
    }

    public static int getButtonsColorOffIndex() {
        return SwTimerTableFields.ColorsButtons.OFF.INDEX();
    }

    public static int getButtonsColorBackIndex() {
        return SwTimerTableFields.ColorsButtons.BACK.INDEX();
    }
    //endregion

    //region COLORS_BACKSCREEN
    public static String getColorsBackScreenTableName() {
        return SWTIMER_TABLES.COLORS_BACK_SCREEN.toString();
    }

    public static String[][] getColorsBackScreenInits() {
        final String[][] TABLE_COLORS_BACK_SCREEN_INITS = {
                {TABLE_IDS.LABEL.toString(), SwTimerTableFields.ColorsBackScreen.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString(), "000000"}
        };
        return TABLE_COLORS_BACK_SCREEN_INITS;
    }

    public static int getBackScreenColorBackIndex() {
        return SwTimerTableFields.ColorsBackScreen.BACK.INDEX();
    }
    //endregion

    //region COLOR_TABLES
    public static int getColorTablesCount() {
        return SWTIMER_COLOR_TABLES.length;
    }

    public static String getColorTableName(int colorTableIndex) {
        return SWTIMER_COLOR_TABLES[colorTableIndex].toString();
    }

    public static int getColorTableIndex(String colorTableName) {
        return SWTIMER_TABLES.valueOf(colorTableName).COLOR_TABLE_INDEX();
    }

    public static String getColorTableLabel(String colorTableName) {
        return SWTIMER_TABLES.valueOf(colorTableName).COLOR_TABLE_LABEL();
    }
    //endregion

}



