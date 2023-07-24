package com.example.pgyl.swtimer_a;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.example.pgyl.pekislib_a.ClockAppAlarmUtils;
import com.example.pgyl.pekislib_a.ColorBox;
import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.HelpActivity;
import com.example.pgyl.pekislib_a.ImageButtonView;
import com.example.pgyl.pekislib_a.PresetsActivity;
import com.example.pgyl.pekislib_a.StringDB;
import com.example.pgyl.pekislib_a.StringDBTables.ACTIVITY_START_STATUS;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.pgyl.pekislib_a.ColorUtils.BUTTON_COLOR_TYPES;
import static com.example.pgyl.pekislib_a.Constants.ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.Constants.COLOR_PREFIX;
import static com.example.pgyl.pekislib_a.Constants.CRLF;
import static com.example.pgyl.pekislib_a.Constants.NOT_FOUND;
import static com.example.pgyl.pekislib_a.Constants.PEKISLIB_ACTIVITIES;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.Constants.SWITCHES;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.MiscUtils.beep;
import static com.example.pgyl.pekislib_a.MiscUtils.capitalize;
import static com.example.pgyl.pekislib_a.MiscUtils.getStringIndexOf;
import static com.example.pgyl.pekislib_a.MiscUtils.toastLong;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_DISPLAY_TYPE;
import static com.example.pgyl.pekislib_a.StringDBTables.TABLE_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringDBUtils.getCurrentsFromActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.getCurrentsFromMultipleTablesFromActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.isColdStartStatusOfActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.setCurrentsForActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.setCurrentsForMultipleTablesForActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.setDefaults;
import static com.example.pgyl.pekislib_a.StringDBUtils.setStartStatusOfActivity;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmm;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmmss;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.getFormattedTimeZoneLongTimeDate;
import static com.example.pgyl.pekislib_a.TimeDateUtils.msToTimeUnit;
import static com.example.pgyl.pekislib_a.TimeDateUtils.timeFormatDToMs;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER;
import static com.example.pgyl.swtimer_a.CtRecord.MODES;
import static com.example.pgyl.swtimer_a.MainActivity.SWTIMER_SHP_KEY_NAMES;
import static com.example.pgyl.swtimer_a.StringDBTables.chronoTimerRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringDBTables.copyPresetCTRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringDBTables.ctRecordToChronoTimerRow;
import static com.example.pgyl.swtimer_a.StringDBTables.getBackScreenColorsBackIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getBackScreenColorsTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.getButtonsColorsBackIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getButtonsColorsOffIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getButtonsColorsOnIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getButtonsColorsTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.getColorTableNames;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsDotCornerRadiusIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsDotSpacingIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsScrollSpeedIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayColorsTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.getPresetsCTTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.timeLabelToPresetCTRow;
import static com.example.pgyl.swtimer_a.StringDBUtils.getDBChronoTimerById;
import static com.example.pgyl.swtimer_a.StringDBUtils.saveDBChronoTimer;

public class CtDisplayActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        START_STOP(R.raw.ct_start_stop), SPLIT(R.raw.ct_split), CLOCK_APP_ALARM(R.raw.ct_bell), RESET(R.raw.ct_reset), CHRONO_MODE(R.raw.ct_chrono), TIMER_MODE(R.raw.ct_timer);

        private int valueId;

        COMMANDS(int valueId) {
            this.valueId = valueId;
        }

        public int ID() {
            return valueId;
        }

        public int INDEX() {
            return ordinal();
        }
    }

    public enum CTDISPLAY_EXTRA_KEYS {
        CURRENT_CHRONO_TIMER_ID
    }

    //endregion
    //region Variables
    private CtRecord currentCtRecord;
    private DotMatrixDisplayView dotMatrixDisplayView;
    private CtDisplayDotMatrixDisplayUpdater dotMatrixDisplayUpdater;
    private ImageButtonView[] buttons;
    private Menu menu;
    private MenuItem barMenuItemSetClockAppAlarmOnStartTimer;
    private MenuItem barMenuItemKeepScreen;
    private LinearLayout backLayout;
    private boolean setClockAppAlarmOnStartTimer;
    private boolean keepScreen;
    private String[][] colors;   //  Couleurs de DotMatrixDisplay, Boutons, Backscreen
    private String[] colorTableNames;
    private String[] coeffs;   //  Espacement des points de DotMatrixDisplay, forme des points et vitesse de défilement
    private boolean validReturnFromCalledActivity;
    private String calledActivityName;
    private StringDB stringDB;
    private String shpFileName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        validReturnFromCalledActivity = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        dotMatrixDisplayUpdater.close();
        dotMatrixDisplayUpdater = null;
        saveDBChronoTimer(stringDB, ctRecordToChronoTimerRow(currentCtRecord));
        currentCtRecord = null;
        setCurrentsForMultipleTablesForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), colorTableNames, colors);
        setCurrentsForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getDotMatrixDisplayCoeffsTableName(), coeffs);
        stringDB.close();
        stringDB = null;
        menu = null;
        savePreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupOrientationLayout();
        setupButtons();
        setupBackLayout();
        setupDotMatrixDisplay();

        long nowm = System.currentTimeMillis();
        shpFileName = getPackageName() + SHP_FILE_NAME_SUFFIX;   //  Sans nom d'activité car partagé avec MainActivity
        setClockAppAlarmOnStartTimer = getSHPSetClockAppAlarmOnStartTimer();
        keepScreen = getSHPKeepScreen();
        setupStringDB();
        int idct = getIntent().getIntExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), NOT_FOUND);
        currentCtRecord = chronoTimerRowToCtRecord(getDBChronoTimerById(stringDB, idct));
        setupCurrentCtRecord();
        setDefaults(stringDB, getPresetsCTTableName(), timeLabelToPresetCTRow(currentCtRecord.getTimeDefInit(), currentCtRecord.getLabelInit()));   //  "Label" -> "Label<idct>"
        colorTableNames = getColorTableNames();
        colors = getCurrentsFromMultipleTablesFromActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), colorTableNames);
        coeffs = getCurrentsFromActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getDotMatrixDisplayCoeffsTableName());

        if (isColdStartStatusOfActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString())) {
            setStartStatusOfActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), ACTIVITY_START_STATUS.HOT);
        } else {
            if (validReturnFromCalledActivity) {
                validReturnFromCalledActivity = false;
                if (calledActivityName.equals(PEKISLIB_ACTIVITIES.PRESETS.toString())) {
                    if (!copyPresetCTRowToCtRecord(getCurrentsFromActivity(stringDB, PEKISLIB_ACTIVITIES.PRESETS.toString(), getPresetsCTTableName()), currentCtRecord, nowm)) {
                        toastLong("Error updating Timer", this);
                    }
                }
                if (calledActivityName.equals(SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString())) {
                    colors = getCurrentsFromMultipleTablesFromActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), colorTableNames);
                }
                if (calledActivityName.equals(SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY.toString())) {
                    coeffs = getCurrentsFromActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY.toString(), getDotMatrixDisplayCoeffsTableName());
                }
            }
        }
        getActionBar().setTitle(currentCtRecord.getLabel());
        setupDotMatrixDisplayUpdater(currentCtRecord);
        setupDotMatrixDisplayColors();
        setupDotMatrixDisplayCoeffs();
        rebuildDotMatrixDisplayStructure();
        updateDisplayDotMatrixDisplay(nowm);
        updateDisplayButtonColorsAndVisibility();
        updateDisplayBackScreenColor();
        updateDisplayKeepScreen();
        invalidateOptionsMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        validReturnFromCalledActivity = false;
        if (requestCode == PEKISLIB_ACTIVITIES.PRESETS.INDEX()) {
            calledActivityName = PEKISLIB_ACTIVITIES.PRESETS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == (SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.INDEX() + 1) * SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER) {
            calledActivityName = SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == (SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY.INDEX() + 1) * SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER) {
            calledActivityName = SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {  //  Non appelé après changement d'orientation
        getMenuInflater().inflate(R.menu.menu_ct_display, menu);
        this.menu = menu;
        setupBarMenuItems();
        updateDisplaySetClockAppAlarmOnStartTimerBarMenuItemIcon(setClockAppAlarmOnStartTimer);
        updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {  // appelé par invalideOptionsMenu après changement d'orientation
        updateDisplaySetClockAppAlarmOnStartTimerBarMenuItemIcon(setClockAppAlarmOnStartTimer);
        updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.HELP) {
            launchHelpActivity();
            return true;
        }
        if (item.getItemId() == R.id.BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER) {
            setClockAppAlarmOnStartTimer = !setClockAppAlarmOnStartTimer;
            updateDisplaySetClockAppAlarmOnStartTimerBarMenuItemIcon(setClockAppAlarmOnStartTimer);
        }
        if (item.getItemId() == R.id.BAR_MENU_ITEM_KEEP_SCREEN) {
            keepScreen = !keepScreen;
            updateDisplayKeepScreen();
            updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        }
        if (item.getItemId() == R.id.SET_COLORS) {
            launchCtDisplayColorsActivity();
            return true;
        }
        if (item.getItemId() == R.id.SET_DOT_MATRIX_DISPLAY_COEFFS) {
            launchCtDisplayDotMatrixDisplayActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    private void onButtonCustomClick(COMMANDS command) {
        long nowm = System.currentTimeMillis();
        if (command.equals(COMMANDS.START_STOP)) {
            if (!currentCtRecord.isRunning()) {
                currentCtRecord.start(nowm, setClockAppAlarmOnStartTimer);
            } else {
                currentCtRecord.stop(nowm);
            }
        }
        if (command.equals(COMMANDS.SPLIT)) {
            currentCtRecord.split(nowm);
        }
        if (command.equals(COMMANDS.CLOCK_APP_ALARM)) {
            currentCtRecord.setClockAppAlarmOn(!currentCtRecord.isClockAppAlarmOn());
        }
        if (command.equals(COMMANDS.RESET)) {
            currentCtRecord.reset();
        }
        if (command.equals(COMMANDS.CHRONO_MODE)) {
            currentCtRecord.setMode(MODES.CHRONO);
        }
        if (command.equals(COMMANDS.TIMER_MODE)) {
            currentCtRecord.setMode(MODES.TIMER);
        }
        updateDisplayButtonColorsAndVisibility();
        updateDisplayDotMatrixDisplay(nowm);
    }

    private void onRequestClockAppAlarmSwitch(CtRecord ctRecord, SWITCHES clockAppAlarmSwitch) {   //  Créer ou désactiver une alarme dans Clock App; Evénement normalement déclenché par CtRecord
        if (clockAppAlarmSwitch.equals(SWITCHES.ON)) {
            long gap = msToTimeUnit(timeFormatDToMs(getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmmss)) - timeFormatDToMs(getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmm)), TIME_UNITS.SEC, TIME_UNITS.SEC);
            String message = "Setting " + ctRecord.getClockAppAlarmDescription() + CRLF + "(" + gap + "s before exact end)";
            ClockAppAlarmUtils.setClockAppAlarm(this, ctRecord.getTimeExp(), ctRecord.getLabel(), message);
        } else {   //  OFF
            ClockAppAlarmUtils.dismissClockAppAlarm(this, ctRecord.getLabel(), "Dismissing " + ctRecord.getClockAppAlarmDescription());
        }
    }

    private void onExpiredTimer(CtRecord ctRecord) {
        toastLong("Timer " + ctRecord.getLabel() + CRLF + "expired @ " + getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmmss), this);
        long nowm = System.currentTimeMillis();
        updateDisplayDotMatrixDisplay(nowm);
        updateDisplayButtonColorsAndVisibility();
        beep(this);
    }

    private void onDotMatrixDisplayCustomClick() {
        launchPresetsActivity();
    }

    private boolean getButtonState(COMMANDS command) {
        if (command.equals(COMMANDS.CHRONO_MODE)) {
            return currentCtRecord.getMode().equals(MODES.CHRONO);
        }
        if (command.equals(COMMANDS.TIMER_MODE)) {
            return currentCtRecord.getMode().equals(MODES.TIMER);
        }
        if (command.equals(COMMANDS.START_STOP)) {
            return currentCtRecord.isRunning();
        }
        if (command.equals(COMMANDS.SPLIT)) {
            return currentCtRecord.isSplitted();
        }
        if (command.equals(COMMANDS.RESET)) {
            return false;
        }
        if (command.equals(COMMANDS.CLOCK_APP_ALARM)) {
            return currentCtRecord.isClockAppAlarmOn();
        }
        return false;
    }

    private boolean getButtonVisibility(COMMANDS command) {
        if (command.equals(COMMANDS.CHRONO_MODE)) {
            return (currentCtRecord.getMode().equals(MODES.CHRONO) || currentCtRecord.isReset());
        }
        if (command.equals(COMMANDS.TIMER_MODE)) {
            return (currentCtRecord.getMode().equals(MODES.TIMER) || currentCtRecord.isReset());
        }
        if (command.equals(COMMANDS.START_STOP)) {
            return (currentCtRecord.getMode().equals(MODES.CHRONO) || !currentCtRecord.isReset() || (currentCtRecord.getTimeDef() > 0));
        }
        if (command.equals(COMMANDS.SPLIT)) {
            return (currentCtRecord.isRunning() || currentCtRecord.isSplitted());
        }
        if (command.equals(COMMANDS.RESET)) {
            return (!currentCtRecord.isRunning() && !currentCtRecord.isReset());
        }
        if (command.equals(COMMANDS.CLOCK_APP_ALARM)) {
            return (currentCtRecord.getMode().equals(MODES.TIMER) && currentCtRecord.isRunning());
        }
        return false;
    }

    private void updateDisplayDotMatrixDisplay(long nowm) {
        dotMatrixDisplayUpdater.stopAutomatic();
        dotMatrixDisplayUpdater.displayTimeAndLabel(nowm);
        if (currentCtRecord.isReset() || (currentCtRecord.isRunning() && ((currentCtRecord.getMode().equals(MODES.TIMER)) || (!currentCtRecord.isSplitted())))) {   //  Besoin de rafraichissement continu
            dotMatrixDisplayUpdater.resetScroll();
            dotMatrixDisplayUpdater.startAutomatic(nowm, currentCtRecord.isReset());   //  Appelé même si Timer en Split car sinon pas de constatation que timer écoulé (car currentCtRecord n'est plus mis à jour)
        }
    }

    private void updateDisplayBackScreenColor() {
        backLayout.setBackgroundColor(Color.parseColor(COLOR_PREFIX + colors[getStringIndexOf(getBackScreenColorsTableName(), colorTableNames)][getBackScreenColorsBackIndex()]));
    }

    private void updateDisplayButtonColorsAndVisibility() {
        int colorTableIndex = getStringIndexOf(getButtonsColorsTableName(), colorTableNames);
        int onColorIndex = getButtonsColorsOnIndex();
        int offColorIndex = getButtonsColorsOffIndex();
        int backColorIndex = getButtonsColorsBackIndex();
        for (COMMANDS command : COMMANDS.values()) {
            boolean buttonState = getButtonState(command);
            ColorBox colorBox = buttons[command.INDEX()].getColorBox();
            colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT.INDEX(), buttonState ? colors[colorTableIndex][onColorIndex] : colors[colorTableIndex][offColorIndex]);
            colorBox.setColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK.INDEX(), colors[colorTableIndex][backColorIndex]);
            colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_FRONT.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_BACK.INDEX()).RGBString);
            colorBox.setColor(BUTTON_COLOR_TYPES.PRESSED_BACK.INDEX(), colorBox.getColor(BUTTON_COLOR_TYPES.UNPRESSED_FRONT.INDEX()).RGBString);
            buttons[command.INDEX()].updateDisplay();
            buttons[command.INDEX()].setVisibility(getButtonVisibility(command) ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void updateDisplaySetClockAppAlarmOnStartTimerBarMenuItemIcon(boolean setClockAppAlarmOnStartTimer) {
        barMenuItemSetClockAppAlarmOnStartTimer.setIcon((setClockAppAlarmOnStartTimer ? R.drawable.main_bell_start_on : R.drawable.main_bell_start_off));
    }

    private void updateDisplayKeepScreenBarMenuItemIcon(boolean keepScreen) {
        barMenuItemKeepScreen.setIcon((keepScreen ? R.drawable.main_light_on : R.drawable.main_light_off));
    }

    private void updateDisplayKeepScreen() {
        if (keepScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void savePreferences() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        SharedPreferences.Editor shpEditor = shp.edit();
        shpEditor.putBoolean(SWTIMER_SHP_KEY_NAMES.SET_CLOCK_APP_ALARM_ON_START_TIMER.toString(), setClockAppAlarmOnStartTimer);
        shpEditor.putBoolean(SWTIMER_SHP_KEY_NAMES.KEEP_SCREEN.toString(), keepScreen);
        shpEditor.commit();
    }

    private boolean getSHPSetClockAppAlarmOnStartTimer() {
        final boolean SET_CLOCK_APP_ALARM_ON_START_TIMER_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.SET_CLOCK_APP_ALARM_ON_START_TIMER.toString(), SET_CLOCK_APP_ALARM_ON_START_TIMER_DEFAULT_VALUE);
    }

    private boolean getSHPKeepScreen() {
        final boolean KEEP_SCREEN_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.KEEP_SCREEN.toString(), KEEP_SCREEN_DEFAULT_VALUE);
    }

    private void setupOrientationLayout() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.ctdisplay_p);
        } else {
            setContentView(R.layout.ctdisplay_l);
        }
    }

    private void setupButtons() {
        final String BUTTON_XML_NAME_PREFIX = "BTN_";
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        buttons = new ImageButtonView[COMMANDS.values().length];
        Class rid = R.id.class;
        for (COMMANDS command : COMMANDS.values()) {
            try {
                buttons[command.INDEX()] = findViewById(rid.getField(BUTTON_XML_NAME_PREFIX + command.toString()).getInt(rid));
                buttons[command.INDEX()].setOutlineStrokeWidthDp(0);
                buttons[command.INDEX()].setSVGImageResource(command.ID());
                if (!command.equals(COMMANDS.START_STOP)) {   //  Start/Stop doit pouvoir cliquer sans délai
                    buttons[command.INDEX()].setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                }
                final COMMANDS fCommand = command;
                buttons[command.INDEX()].setOnCustomClickListener(new ImageButtonView.onCustomClickListener() {
                    @Override
                    public void onCustomClick() {
                        onButtonCustomClick(fCommand);
                    }
                });
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void setupDotMatrixDisplay() {  //  Pour Afficher HH:MM:SS.C et éventuellement un label
        final long DOT_MATRIX_DISPLAY_VIEW_MIN_CLICK_TIME_INTERVAL_MS = 500;

        dotMatrixDisplayView = findViewById(R.id.DOT_MATRIX_DISPLAY);
        dotMatrixDisplayView.setMinClickTimeInterval(DOT_MATRIX_DISPLAY_VIEW_MIN_CLICK_TIME_INTERVAL_MS);
        dotMatrixDisplayView.setOnCustomClickListener(new DotMatrixDisplayView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onDotMatrixDisplayCustomClick();
            }
        });
    }

    private void setupDotMatrixDisplayColors() {
        dotMatrixDisplayUpdater.setColors(colors[getStringIndexOf(getDotMatrixDisplayColorsTableName(), colorTableNames)]);
    }

    private void setupDotMatrixDisplayCoeffs() {
        dotMatrixDisplayUpdater.setDotSpacingCoeff(coeffs[getDotMatrixDisplayCoeffsDotSpacingIndex()]);
        dotMatrixDisplayUpdater.setDotCornerRadiusCoeff(coeffs[getDotMatrixDisplayCoeffsDotCornerRadiusIndex()]);
        dotMatrixDisplayUpdater.setScrollSpeed(coeffs[getDotMatrixDisplayCoeffsScrollSpeedIndex()]);
    }

    private void rebuildDotMatrixDisplayStructure() {
        dotMatrixDisplayUpdater.rebuildStructure();   //  Reconstruction générale
    }

    private void setupDotMatrixDisplayUpdater(CtRecord currentCtRecord) {
        dotMatrixDisplayUpdater = new CtDisplayDotMatrixDisplayUpdater(dotMatrixDisplayView, currentCtRecord);
    }

    private void setupBackLayout() {
        backLayout = findViewById(R.id.BACK_LAYOUT);
    }

    private void setupBarMenuItems() {
        final String BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER_NAME = "BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER";
        final String BAR_MENU_ITEM_KEEP_SCREEN_NAME = "BAR_MENU_ITEM_KEEP_SCREEN";

        Class rid = R.id.class;
        try {
            barMenuItemSetClockAppAlarmOnStartTimer = menu.findItem(rid.getField(BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER_NAME).getInt(rid));
            barMenuItemKeepScreen = menu.findItem(rid.getField(BAR_MENU_ITEM_KEEP_SCREEN_NAME).getInt(rid));
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setupStringDB() {
        stringDB = new StringDB(this);
        stringDB.open();
    }

    private void setupCurrentCtRecord() {
        currentCtRecord.setOnRequestClockAppAlarmSwitchListener(new CtRecord.onRequestClockAppAlarmSwitchListener() {
            @Override
            public void onRequestClockAppAlarmSwitch(CtRecord ctRecord, SWITCHES clockAppAlarmSwitch) {
                CtDisplayActivity.this.onRequestClockAppAlarmSwitch(ctRecord, clockAppAlarmSwitch);
            }
        });
        currentCtRecord.setOnExpiredTimerListener(new CtRecord.onExpiredTimerListener() {
            @Override
            public void onExpiredTimer(CtRecord ctRecord) {
                CtDisplayActivity.this.onExpiredTimer(ctRecord);
            }
        });
    }

    private void launchPresetsActivity() {
        final String SEPARATOR = " - ";

        setCurrentsForActivity(stringDB, PEKISLIB_ACTIVITIES.PRESETS.toString(), getPresetsCTTableName(), timeLabelToPresetCTRow(currentCtRecord.getTimeDef(), currentCtRecord.getLabel()));
        setStartStatusOfActivity(stringDB, PEKISLIB_ACTIVITIES.PRESETS.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, PresetsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), capitalize("CT Presets"));
        callingIntent.putExtra(PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS.SEPARATOR.toString(), SEPARATOR);
        callingIntent.putExtra(PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS.DISPLAY_TYPE.toString(), PRESETS_ACTIVITY_DISPLAY_TYPE.NO_COLORS.toString());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getPresetsCTTableName());
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.PRESETS.INDEX());
    }

    private void launchCtDisplayColorsActivity() {
        setCurrentsForMultipleTablesForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), colorTableNames, colors);
        setStartStatusOfActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, CtDisplayColorsActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), currentCtRecord.getIdct());
        startActivityForResult(callingIntent, (SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.INDEX() + 1) * SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER);
    }

    private void launchCtDisplayDotMatrixDisplayActivity() {
        setCurrentsForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY.toString(), getDotMatrixDisplayCoeffsTableName(), coeffs);
        setCurrentsForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY.toString(), getDotMatrixDisplayColorsTableName(), colors[getStringIndexOf(getDotMatrixDisplayColorsTableName(), colorTableNames)]);
        setStartStatusOfActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, CtDisplayDotMatrixDisplayActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), currentCtRecord.getIdct());
        startActivityForResult(callingIntent, (SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY.INDEX() + 1) * SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER);
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpctdisplayactivity);
        startActivity(callingIntent);
    }

}