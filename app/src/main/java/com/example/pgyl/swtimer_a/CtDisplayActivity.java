package com.example.pgyl.swtimer_a;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.example.pgyl.pekislib_a.ClockAppAlarmUtils;
import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.HelpActivity;
import com.example.pgyl.pekislib_a.PresetsActivity;
import com.example.pgyl.pekislib_a.StringDB;
import com.example.pgyl.pekislib_a.StringDBTables.ACTIVITY_START_STATUS;
import com.example.pgyl.pekislib_a.SymbolButtonView;

import java.util.logging.Level;
import java.util.logging.Logger;

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
import static com.example.pgyl.pekislib_a.MiscUtils.toastLong;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_DISPLAY_TYPE;
import static com.example.pgyl.pekislib_a.StringDBTables.TABLE_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringDBUtils.getCurrentsFromActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.getCurrentsFromMultipleTablesFromActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.getTableIndex;
import static com.example.pgyl.pekislib_a.StringDBUtils.isColdStartStatusOfActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.setCurrentsForActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.setCurrentsForMultipleTablesForActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.setDefaults;
import static com.example.pgyl.pekislib_a.StringDBUtils.setStartStatusOfActivity;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmm;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmmss;
import static com.example.pgyl.pekislib_a.TimeDateUtils.ROUND_TO_TIME_UNIT_PRECISION;
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
import static com.example.pgyl.swtimer_a.StringDBTables.getColorTableNames;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsDotCornerRadiusIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsDotSpacingIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsScrollSpeedIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayColorsTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.getPresetsCTTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.getStateButtonsColorsBackIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getStateButtonsColorsOffIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getStateButtonsColorsOnIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getStateButtonsColorsTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.timeLabelToPresetCTRow;
import static com.example.pgyl.swtimer_a.StringDBUtils.getDBChronoTimerById;
import static com.example.pgyl.swtimer_a.StringDBUtils.saveDBChronoTimer;

public class CtDisplayActivity extends Activity {
    //region Constantes
    private enum STATE_COMMANDS {
        RUN(R.raw.ct_run), SPLIT(R.raw.ct_split), CLOCK_APP_ALARM(R.raw.ct_bell), RESET(R.raw.ct_reset), CHRONO_MODE(R.raw.ct_chrono), TIMER_MODE(R.raw.ct_timer);

        private int valueId;

        STATE_COMMANDS(int valueId) {
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
    private SymbolButtonView[] stateButtons;
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
        setupOrientationLayout();
        setupStateButtons();
        setupBackLayout();
        setupDotMatrixDisplay();
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
        updateCurrentRecord(nowm);
        getActionBar().setTitle(currentCtRecord.getLabel());
        setupDotMatrixDisplayUpdater(currentCtRecord);
        setupDotMatrixDisplayColors();
        setupDotMatrixDisplayCoeffs();
        rebuildDotMatrixDisplayStructure();
        updateDisplayDotMatrixDisplay();
        updateDisplayStateButtonColors();
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
            launchCtDisplayDotMatrixDisplayCoeffsActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    private void onStateButtonCustomClick(STATE_COMMANDS command) {
        long nowm = System.currentTimeMillis();
        if (command.equals(STATE_COMMANDS.RUN)) {
            onStateButtonClickRun(nowm);
        }
        if (command.equals(STATE_COMMANDS.SPLIT)) {
            onStateButtonClickSplit(nowm);
        }
        if (command.equals(STATE_COMMANDS.CLOCK_APP_ALARM)) {
            onStateButtonClickClockAppAlarm();
        }
        if (command.equals(STATE_COMMANDS.RESET)) {
            onStateButtonClickReset();
        }
        if (command.equals(STATE_COMMANDS.CHRONO_MODE)) {
            onStateButtonClickMode(MODES.CHRONO);
        }
        if (command.equals(STATE_COMMANDS.TIMER_MODE)) {
            onStateButtonClickMode(MODES.TIMER);
        }
        updateCurrentRecord(nowm);
        updateDisplayStateButtonColors();
        updateDisplayDotMatrixDisplay();
    }

    private void onStateButtonClickRun(long nowm) {
        if (!currentCtRecord.isRunning()) {
            currentCtRecord.start(nowm, setClockAppAlarmOnStartTimer);
        } else {
            currentCtRecord.stop(nowm);
        }
    }

    private void onStateButtonClickSplit(long nowm) {
        currentCtRecord.split(nowm);
    }

    private void onStateButtonClickClockAppAlarm() {
        currentCtRecord.setClockAppAlarmOn(!currentCtRecord.isClockAppAlarmOn());
    }

    private void onStateButtonClickReset() {
        currentCtRecord.reset();
    }

    private void onStateButtonClickMode(MODES newMode) {
        MODES oldMode = currentCtRecord.getMode();
        if (!currentCtRecord.setMode(newMode)) {
            if (!newMode.equals(oldMode)) {
                toastLong("First stop " + capitalize(oldMode.toString()), this);
            }
        }
    }

    private void onRequestClockAppAlarmSwitch(CtRecord ctRecord, SWITCHES clockAppAlarmSwitch) {   //  Créer ou désactiver une alarme dans Clock App; Evénement normalement déclenché par CtRecord
        if (clockAppAlarmSwitch.equals(SWITCHES.ON)) {
            long gap = msToTimeUnit(timeFormatDToMs(getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmmss)) - timeFormatDToMs(getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmm)), TIME_UNITS.SEC, ROUND_TO_TIME_UNIT_PRECISION);
            String message = "Setting " + ctRecord.getClockAppAlarmDescription() + CRLF + "(" + gap + "s before exact end)";
            ClockAppAlarmUtils.setClockAppAlarm(this, ctRecord.getTimeExp(), ctRecord.getLabel(), message);
        } else {   //  OFF
            ClockAppAlarmUtils.dismissClockAppAlarm(this, ctRecord.getLabel(), "Dismissing " + ctRecord.getClockAppAlarmDescription());
        }
    }

    private void onExpiredTimer(CtRecord ctRecord) {
        toastLong("Timer " + ctRecord.getLabel() + CRLF + "expired @ " + getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmmss), this);
        updateDisplayDotMatrixDisplay();
        updateDisplayStateButtonColors();
        beep(this);
    }

    private void onDotMatrixDisplayCustomClick() {
        launchPresetsActivity();
    }

    private void updateDisplayDotMatrixDisplay() {
        dotMatrixDisplayUpdater.displayCurrentTimeAndLabel();
        if ((currentCtRecord.isRunning() && (!currentCtRecord.isSplitted())) || (currentCtRecord.isReset())) {   //  Besoin de rafraichissement continu
            dotMatrixDisplayUpdater.resetScroll();
            dotMatrixDisplayUpdater.startAutomatic(currentCtRecord.isReset());
        } else {  //  Pas besoin de rafraichissement continu
            dotMatrixDisplayUpdater.stopAutomatic();
        }
    }

    private void updateDisplayBackScreenColor() {
        backLayout.setBackgroundColor(Color.parseColor(COLOR_PREFIX + colors[getTableIndex(colorTableNames, getBackScreenColorsTableName())][getBackScreenColorsBackIndex()]));
    }

    private void updateDisplayStateButtonColor(STATE_COMMANDS command) {  //   ON/BACK ou OFF/BACK
        int colorTableIndex = getTableIndex(colorTableNames, getStateButtonsColorsTableName());
        String frontColor = ((getStateButtonState(command)) ? colors[colorTableIndex][getStateButtonsColorsOnIndex()] : colors[colorTableIndex][getStateButtonsColorsOffIndex()]);
        String backColor = colors[colorTableIndex][getStateButtonsColorsBackIndex()];
        String extraColor = ((getStateButtonState(command)) ? colors[colorTableIndex][getStateButtonsColorsOffIndex()] : colors[colorTableIndex][getStateButtonsColorsOnIndex()]);
        stateButtons[command.INDEX()].setColors(frontColor, backColor, extraColor);
    }

    private void updateDisplayStateButtonColors() {
        for (STATE_COMMANDS command : STATE_COMMANDS.values()) {
            updateDisplayStateButtonColor(command);
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

    private boolean getStateButtonState(STATE_COMMANDS command) {
        if (command.equals(STATE_COMMANDS.CHRONO_MODE)) {
            return currentCtRecord.getMode().equals(MODES.CHRONO);
        }
        if (command.equals(STATE_COMMANDS.TIMER_MODE)) {
            return currentCtRecord.getMode().equals(MODES.TIMER);
        }
        if (command.equals(STATE_COMMANDS.RUN)) {
            return currentCtRecord.isRunning();
        }
        if (command.equals(STATE_COMMANDS.SPLIT)) {
            return currentCtRecord.isSplitted();
        }
        if (command.equals(STATE_COMMANDS.RESET)) {
            return false;
        }
        if (command.equals(STATE_COMMANDS.CLOCK_APP_ALARM)) {
            return currentCtRecord.isClockAppAlarmOn();
        }
        return false;
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

    private void setupStateButtons() {
        final String STATE_BUTTON_XML_NAME_PREFIX = "STATE_BTN_";
        final long STATE_BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        stateButtons = new SymbolButtonView[STATE_COMMANDS.values().length];
        Class rid = R.id.class;
        for (STATE_COMMANDS stateCommand : STATE_COMMANDS.values()) {
            try {
                stateButtons[stateCommand.INDEX()] = findViewById(rid.getField(STATE_BUTTON_XML_NAME_PREFIX + stateCommand.toString()).getInt(rid));
                stateButtons[stateCommand.INDEX()].setSVGImageResource(stateCommand.ID());
                if (!stateCommand.equals(STATE_COMMANDS.RUN)) {   //  Start/Stop doit pouvoir cliquer sans délai
                    stateButtons[stateCommand.INDEX()].setMinClickTimeInterval(STATE_BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                }
                final STATE_COMMANDS fstatecommand = stateCommand;
                stateButtons[stateCommand.INDEX()].setCustomOnClickListener(new SymbolButtonView.onCustomClickListener() {
                    @Override
                    public void onCustomClick() {
                        onStateButtonCustomClick(fstatecommand);
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
        dotMatrixDisplayUpdater.setColors(colors[getTableIndex(colorTableNames, getDotMatrixDisplayColorsTableName())]);
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

    private void updateCurrentRecord(long nowm) {
        currentCtRecord.updateTime(nowm);
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

    private void launchCtDisplayDotMatrixDisplayCoeffsActivity() {
        setCurrentsForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY.toString(), getDotMatrixDisplayCoeffsTableName(), coeffs);
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