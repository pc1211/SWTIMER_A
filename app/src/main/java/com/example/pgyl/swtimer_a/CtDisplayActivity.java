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

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.HelpActivity;
import com.example.pgyl.pekislib_a.PresetsActivity;
import com.example.pgyl.pekislib_a.StringShelfDatabase;
import com.example.pgyl.pekislib_a.StringShelfDatabaseTables.ACTIVITY_START_STATUS;
import com.example.pgyl.pekislib_a.SymbolButtonView;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.pgyl.pekislib_a.Constants.ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.Constants.COLOR_PREFIX;
import static com.example.pgyl.pekislib_a.Constants.CRLF;
import static com.example.pgyl.pekislib_a.Constants.NOT_FOUND;
import static com.example.pgyl.pekislib_a.Constants.PEKISLIB_ACTIVITIES;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.MiscUtils.beep;
import static com.example.pgyl.pekislib_a.MiscUtils.capitalize;
import static com.example.pgyl.pekislib_a.MiscUtils.toastLong;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_DISPLAY_TYPE;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseTables.TABLE_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentsFromActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentsFromMultipleTablesFromActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getTableIndex;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.isColdStartStatusOfActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentsForActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentsForMultipleTablesForActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setDefaults;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusOfActivity;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmmss;
import static com.example.pgyl.pekislib_a.TimeDateUtils.formattedTimeZoneLongTimeDate;
import static com.example.pgyl.pekislib_a.TimeDateUtils.msToTimeFormatD;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER;
import static com.example.pgyl.swtimer_a.Constants.TIME_UNIT_PRECISION;
import static com.example.pgyl.swtimer_a.CtRecord.MODE;
import static com.example.pgyl.swtimer_a.CtRecord.VIA_CLOCK_APP;
import static com.example.pgyl.swtimer_a.MainActivity.SWTIMER_SHP_KEY_NAMES;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.chronoTimerRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.copyPresetCTRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.ctRecordToChronoTimerRow;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenColorsBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTableNames;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayCoeffsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayCoeffsDotCornerRadiusIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayCoeffsDotSpacingIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayCoeffsScrollSpeedIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getPresetsCTTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getStateButtonsColorsBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getStateButtonsColorsOffIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getStateButtonsColorsOnIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getStateButtonsColorsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.timeLabelToPresetCTRow;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getChronoTimerById;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.saveChronoTimer;

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
        CURRENT_CHRONO_TIMER_ID, FIRST_PORTRAIT_VALUE_INDEX, FIRST_LANDSCAPE_VALUE_INDEX, SEEKBAR_MAX_VALUE
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
    private StringShelfDatabase stringShelfDatabase;
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

        dotMatrixDisplayUpdater.stopAutomatic();
        dotMatrixDisplayUpdater.close();
        dotMatrixDisplayUpdater = null;
        saveChronoTimer(stringShelfDatabase, ctRecordToChronoTimerRow(currentCtRecord));
        currentCtRecord = null;
        setCurrentsForMultipleTablesForActivity(stringShelfDatabase, colorTableNames, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), colors);
        setCurrentsForActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getDotMatrixDisplayCoeffsTableName(), coeffs);
        stringShelfDatabase.close();
        stringShelfDatabase = null;
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
        int idct = getIntent().getIntExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), NOT_FOUND);
        setupStringShelfDatabase();
        currentCtRecord = chronoTimerRowToCtRecord(getChronoTimerById(stringShelfDatabase, idct), this);
        setDefaults(stringShelfDatabase, getPresetsCTTableName(), timeLabelToPresetCTRow(currentCtRecord.getTimeDefInit(), currentCtRecord.getLabelInit()));
        colorTableNames = getColorTableNames();
        colors = getCurrentsFromMultipleTablesFromActivity(stringShelfDatabase, colorTableNames, SWTIMER_ACTIVITIES.CT_DISPLAY.toString());
        coeffs = getCurrentsFromActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getDotMatrixDisplayCoeffsTableName());

        if (isColdStartStatusOfActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString())) {
            setStartStatusOfActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), ACTIVITY_START_STATUS.HOT);
        } else {
            if (validReturnFromCalledActivity) {
                validReturnFromCalledActivity = false;
                if (calledActivityName.equals(PEKISLIB_ACTIVITIES.PRESETS.toString())) {
                    if (!copyPresetCTRowToCtRecord(getCurrentsFromActivity(stringShelfDatabase, PEKISLIB_ACTIVITIES.PRESETS.toString(), getPresetsCTTableName()), currentCtRecord, nowm)) {
                        toastLong("Error updating Timer", this);
                    }
                }
                if (calledActivityName.equals(SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString())) {
                    colors = getCurrentsFromMultipleTablesFromActivity(stringShelfDatabase, colorTableNames, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString());
                }
                if (calledActivityName.equals(SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.toString())) {
                    coeffs = getCurrentsFromActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.toString(), getDotMatrixDisplayCoeffsTableName());
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
        if (requestCode == (SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.INDEX() + 1) * SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER) {
            calledActivityName = SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.toString();
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
            onStateButtonClickMode(MODE.CHRONO);
        }
        if (command.equals(STATE_COMMANDS.TIMER_MODE)) {
            onStateButtonClickMode(MODE.TIMER);
        }
        updateCurrentRecord(nowm);
        updateDisplayStateButtonColors();
        updateDisplayDotMatrixDisplay();
    }

    private void onStateButtonClickRun(long nowm) {
        if (!currentCtRecord.isRunning()) {
            if (!currentCtRecord.start(nowm)) {
                if (setClockAppAlarmOnStartTimer) {
                    currentCtRecord.setClockAppAlarmOn(VIA_CLOCK_APP);
                }
            }
        } else {
            if (!currentCtRecord.stop(nowm)) {
                currentCtRecord.setClockAppAlarmOff(VIA_CLOCK_APP);
            }
        }
    }

    private void onStateButtonClickSplit(long nowm) {
        currentCtRecord.split(nowm);
    }

    private void onStateButtonClickClockAppAlarm() {
        if (currentCtRecord.getMode().equals(MODE.TIMER)) {
            if (currentCtRecord.isRunning()) {
                if (!currentCtRecord.hasClockAppAlarm()) {
                    currentCtRecord.setClockAppAlarmOn(VIA_CLOCK_APP);
                } else {
                    currentCtRecord.setClockAppAlarmOff(VIA_CLOCK_APP);
                }
            }
        }
    }

    private void onStateButtonClickReset() {
        if (!currentCtRecord.reset()) {
            currentCtRecord.setClockAppAlarmOff(VIA_CLOCK_APP);
        }
    }

    private void onStateButtonClickMode(MODE newMode) {
        MODE oldMode = currentCtRecord.getMode();
        if (!currentCtRecord.setMode(newMode)) {
            if (!newMode.equals(oldMode)) {
                toastLong("First stop " + capitalize(oldMode.toString()), this);
            }
        }
    }

    private void onExpiredTimerCurrentChronoTimer() {
        toastLong("Timer " + currentCtRecord.getLabel() + CRLF + "expired @ " + formattedTimeZoneLongTimeDate(currentCtRecord.getTimeExp(), HHmmss), this);
        updateDisplayDotMatrixDisplay();
        updateDisplayStateButtonColors();
        beep(this);
    }


    private void onDotMatrixDisplayCustomClick() {
        launchPresetsActivity();
    }

    private void updateDisplayDotMatrixDisplay() {
        dotMatrixDisplayUpdater.displayTimeAndLabel(msToTimeFormatD(currentCtRecord.getTimeDisplay(), TIME_UNIT_PRECISION), currentCtRecord.getLabel());
        if ((currentCtRecord.isRunning() && (!currentCtRecord.isSplitted())) || (currentCtRecord.isReset())) {   //  Besoin de rafraichissement continu
            dotMatrixDisplayUpdater.startAutomatic();
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
            return currentCtRecord.getMode().equals(MODE.CHRONO);
        }
        if (command.equals(STATE_COMMANDS.TIMER_MODE)) {
            return currentCtRecord.getMode().equals(MODE.TIMER);
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
            return currentCtRecord.hasClockAppAlarm();
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
        dotMatrixDisplayUpdater.setOnExpiredTimerListener(new CtDisplayDotMatrixDisplayUpdater.onExpiredTimerListener() {
            @Override
            public void onExpiredTimer() {
                onExpiredTimerCurrentChronoTimer();
            }
        });
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

    private void setupStringShelfDatabase() {
        stringShelfDatabase = new StringShelfDatabase(this);
        stringShelfDatabase.open();
    }

    private void updateCurrentRecord(long nowm) {
        if (!currentCtRecord.updateTime(nowm)) {    //  Le timer a expiré
            onExpiredTimerCurrentChronoTimer();
        }
    }

    private void launchPresetsActivity() {
        final String SEPARATOR = " - ";

        setCurrentsForActivity(stringShelfDatabase, PEKISLIB_ACTIVITIES.PRESETS.toString(), getPresetsCTTableName(), timeLabelToPresetCTRow(currentCtRecord.getTimeDef(), currentCtRecord.getLabel()));
        setStartStatusOfActivity(stringShelfDatabase, PEKISLIB_ACTIVITIES.PRESETS.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, PresetsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), capitalize("CT Presets"));
        callingIntent.putExtra(PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS.SEPARATOR.toString(), SEPARATOR);
        callingIntent.putExtra(PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS.DISPLAY_TYPE.toString(), PRESETS_ACTIVITY_DISPLAY_TYPE.NO_COLORS.toString());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getPresetsCTTableName());
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.PRESETS.INDEX());
    }

    private void launchCtDisplayColorsActivity() {
        setCurrentsForMultipleTablesForActivity(stringShelfDatabase, colorTableNames, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), colors);
        setStartStatusOfActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, CtDisplayColorsActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), currentCtRecord.getIdct());
        startActivityForResult(callingIntent, (SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.INDEX() + 1) * SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER);
    }

    private void launchCtDisplayDotMatrixDisplayCoeffsActivity() {
        setCurrentsForActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.toString(), getDotMatrixDisplayCoeffsTableName(), coeffs);
        setStartStatusOfActivity(stringShelfDatabase, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, CtDisplayDotMatrixDisplayCoeffsActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), currentCtRecord.getIdct());
        startActivityForResult(callingIntent, (SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.INDEX() + 1) * SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER);
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpctdisplayactivity);
        startActivity(callingIntent);
    }

}