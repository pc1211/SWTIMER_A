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
import com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.ACTIVITY_START_STATUS;
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
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.TABLE_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentPresetInPresetsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getDefaults;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentPresetInPresetsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setDefaults;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusInPresetsActivity;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmmss;
import static com.example.pgyl.pekislib_a.TimeDateUtils.formattedTimeZoneLongTimeDate;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER;
import static com.example.pgyl.swtimer_a.CtDisplayDotMatrixDisplayUpdater.DISPLAY_INITIALIZE;
import static com.example.pgyl.swtimer_a.CtRecord.MODE;
import static com.example.pgyl.swtimer_a.CtRecord.VIA_CLOCK_APP;
import static com.example.pgyl.swtimer_a.MainActivity.SWTIMER_SHP_KEY_NAMES;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getChronoTimerById;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getCurrentValuesInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getCurrentValuesInCtDisplayColorsActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.isColdStartStatusInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.saveChronoTimer;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setCurrentValuesInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setCurrentValuesInCtDisplayColorsActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setStartStatusInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setStartStatusInCtDisplayColorsActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.chronoTimerRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.copyPresetCTRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.ctRecordToChronoTimerRow;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getBackScreenColorBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getButtonsColorBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getButtonsColorOffIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getButtonsColorOnIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTypeIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorTypesCount;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorsBackScreenTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorsButtonsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getColorsDotMatrixDisplayTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getPresetsCTTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.timeMessageToPresetCTRow;

public class CtDisplayActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        RUN(R.raw.ct_run), SPLIT(R.raw.ct_split), INVERT_CLOCK_APP_ALARM(R.raw.ct_bell), RESET(R.raw.ct_reset), CHRONO_MODE(R.raw.ct_chrono), TIMER_MODE(R.raw.ct_timer);

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
    private SymbolButtonView[] buttons;
    private Menu menu;
    private MenuItem barMenuItemSetClockAppAlarmOnStartTimer;
    private MenuItem barMenuItemKeepScreen;
    private LinearLayout backLayout;
    private boolean setClockAppAlarmOnStartTimer;
    private boolean keepScreen;
    private String[][] colors = new String[getColorTypesCount()][];  //  Couleurs de DotMatrixDisplay, Boutons, Backscreen
    private boolean validReturnFromCalledActivity;
    private String calledActivity;
    private StringShelfDatabase stringShelfDatabase;
    private String shpFileName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setupOrientationLayout();
        setupButtons();
        setupDotMatrixDisplay();
        setupBackLayout();
        validReturnFromCalledActivity = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        dotMatrixDisplayUpdater.stopAutomatic();
        dotMatrixDisplayUpdater.close();
        dotMatrixDisplayUpdater = null;
        saveCurrentChronoTimer();
        currentCtRecord = null;
        saveCurrentColorsInDB();
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
        setupStringShelfDatabase();
        int idct = getIntent().getIntExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), NOT_FOUND);
        currentCtRecord = chronoTimerRowToCtRecord(getChronoTimerById(stringShelfDatabase, idct), this);
        setupDotMatrixDisplayUpdater();
        getDBCurrentOrDefaultColors();

        if (isColdStartStatusInCtDisplayActivity(stringShelfDatabase)) {
            setStartStatusInCtDisplayActivity(stringShelfDatabase, ACTIVITY_START_STATUS.HOT);
        } else {
            if (validReturnFromCalledActivity) {
                validReturnFromCalledActivity = false;
                if (returnsFromPresetsActivity()) {
                    if (!copyPresetCTRowToCtRecord(getCurrentPresetInPresetsActivity(stringShelfDatabase, getPresetsCTTableName()), currentCtRecord, nowm)) {
                        toastLong("Error updating Timer", this);
                    }
                }
                if (returnsFromCtDisplayColorsActivity()) {
                    getDBCurrentColorsFromCtDisplayColorsActivity();
                }
            }
        }

        getActionBar().setTitle(currentCtRecord.getMessage());
        dotMatrixDisplayUpdater.setGridDimensions();
        dotMatrixDisplayUpdater.setGridColors(colors[getColorTypeIndex(getColorsDotMatrixDisplayTableName())]);
        updateDotMatrixDisplay();
        dotMatrixDisplayUpdater.startAutomatic();
        updateDisplayButtonColors();
        updateDisplayBackScreenColor();
        updateDisplayKeepScreen();
        invalidateOptionsMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        validReturnFromCalledActivity = false;
        if (requestCode == PEKISLIB_ACTIVITIES.PRESETS.INDEX()) {
            calledActivity = PEKISLIB_ACTIVITIES.PRESETS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == (SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.INDEX() + 1) * SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER) {
            calledActivity = SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString();
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
            saveCurrentColorsInDBCtDisplayColorsActivity();
            launchCtDisplayColorsActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    private void onButtonCustomClick(COMMANDS command) {
        long nowm = System.currentTimeMillis();
        if (command.equals(COMMANDS.RUN)) {
            onButtonClickRun(nowm);
        }
        if (command.equals(COMMANDS.SPLIT)) {
            onButtonClickSplit(nowm);
        }
        if (command.equals(COMMANDS.INVERT_CLOCK_APP_ALARM)) {
            onButtonClickInvertClockAppAlarm();
        }
        if (command.equals(COMMANDS.RESET)) {
            onButtonClickReset();
        }
        if (command.equals(COMMANDS.CHRONO_MODE)) {
            onButtonClickMode(MODE.CHRONO);
        }
        if (command.equals(COMMANDS.TIMER_MODE)) {
            onButtonClickMode(MODE.TIMER);
        }
        currentCtRecord.updateTime(nowm);
        updateDisplayButtonColors();
        updateDotMatrixDisplay();
    }

    private void onButtonClickRun(long nowm) {
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

    private void onButtonClickSplit(long nowm) {
        currentCtRecord.split(nowm);
    }

    private void onButtonClickInvertClockAppAlarm() {
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

    private void onButtonClickReset() {
        if (!currentCtRecord.reset()) {
            currentCtRecord.setClockAppAlarmOff(VIA_CLOCK_APP);
        }
    }

    private void onButtonClickMode(MODE newMode) {
        MODE oldMode = currentCtRecord.getMode();
        if (!currentCtRecord.setMode(newMode)) {
            if (!newMode.equals(oldMode)) {
                toastLong("First stop " + capitalize(oldMode.toString()), this);
            }
        }
    }

    private void onExpiredTimerCurrentChronoTimer() {
        toastLong("Timer " + currentCtRecord.getMessage() + CRLF + "expired @ " + formattedTimeZoneLongTimeDate(currentCtRecord.getTimeExp(), HHmmss), this);
        updateDisplayButtonColors();
        beep(this);
    }


    private void onDotMatrixDisplayCustomClick() {
        setCurrentPresetInPresetsActivity(stringShelfDatabase, getPresetsCTTableName(), timeMessageToPresetCTRow(currentCtRecord.getTimeDef(), currentCtRecord.getMessage()));
        setDefaults(stringShelfDatabase, getPresetsCTTableName(), timeMessageToPresetCTRow(currentCtRecord.getTimeDefInit(), currentCtRecord.getMessageInit()));
        launchPresetsActivity();
    }

    private void updateDotMatrixDisplay() {
        dotMatrixDisplayUpdater.update(DISPLAY_INITIALIZE);
    }

    private void updateDisplayBackScreenColor() {
        backLayout.setBackgroundColor(Color.parseColor(COLOR_PREFIX + colors[getColorTypeIndex(getColorsBackScreenTableName())][getBackScreenColorBackIndex()]));
    }

    private void updateDisplayButtonColor(COMMANDS command) {  //   ON/BACK ou OFF/BACK
        int colorTypeIndex = getColorTypeIndex(getColorsButtonsTableName());
        buttons[command.INDEX()].setFrontColor(((getButtonState(command)) ? colors[colorTypeIndex][getButtonsColorOnIndex()] : colors[colorTypeIndex][getButtonsColorOffIndex()]));
        buttons[command.INDEX()].setBackColor(colors[colorTypeIndex][getButtonsColorBackIndex()]);
        buttons[command.INDEX()].setExtraColor(((getButtonState(command)) ? colors[colorTypeIndex][getButtonsColorOffIndex()] : colors[colorTypeIndex][getButtonsColorOnIndex()]));
        buttons[command.INDEX()].invalidate();
    }

    private void updateDisplayButtonColors() {
        for (COMMANDS command : COMMANDS.values()) {
            updateDisplayButtonColor(command);
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

    private boolean getButtonState(COMMANDS command) {
        if (command.equals(COMMANDS.CHRONO_MODE)) {
            return currentCtRecord.getMode().equals(MODE.CHRONO);
        }
        if (command.equals(COMMANDS.TIMER_MODE)) {
            return currentCtRecord.getMode().equals(MODE.TIMER);
        }
        if (command.equals(COMMANDS.RUN)) {
            return currentCtRecord.isRunning();
        }
        if (command.equals(COMMANDS.SPLIT)) {
            return currentCtRecord.isSplitted();
        }
        if (command.equals(COMMANDS.RESET)) {
            return false;
        }
        if (command.equals(COMMANDS.INVERT_CLOCK_APP_ALARM)) {
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

    private void setupButtons() {
        final String BUTTON_XML_NAME_PREFIX = "BTN_";
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        buttons = new SymbolButtonView[COMMANDS.values().length];
        Class rid = R.id.class;
        for (COMMANDS command : COMMANDS.values()) {
            try {
                buttons[command.INDEX()] = findViewById(rid.getField(BUTTON_XML_NAME_PREFIX + command.toString()).getInt(rid));
                buttons[command.INDEX()].setSVGImageResource(command.ID());
                if (!command.equals(COMMANDS.RUN)) {   //  Start/Stop doit pouvoir cliquer sans délai
                    buttons[command.INDEX()].setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                }
                final COMMANDS fcommand = command;
                buttons[command.INDEX()].setCustomOnClickListener(new SymbolButtonView.onCustomClickListener() {
                    @Override
                    public void onCustomClick() {
                        onButtonCustomClick(fcommand);
                    }
                });
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void setupDotMatrixDisplay() {  //  Pour Afficher HH:MM:SS.CC et éventuellement un message
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

    private void setupDotMatrixDisplayUpdater() {
        dotMatrixDisplayUpdater = new CtDisplayDotMatrixDisplayUpdater(dotMatrixDisplayView, currentCtRecord);
        dotMatrixDisplayUpdater.setOnExpiredTimerListener(new CtDisplayDotMatrixDisplayUpdater.onExpiredTimerListener() {
            @Override
            public void onExpiredTimer() {
                onExpiredTimerCurrentChronoTimer();
            }
        });
    }

    private void setupBarMenuItems() {
        final String BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER_NAME = "BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER";
        final String BAR_MENU_ITEM_KEEP_SCREEN_NAME = "BAR_MENU_ITEM_KEEP_SCREEN";

        Class rid = R.id.class;
        try {
            barMenuItemSetClockAppAlarmOnStartTimer = menu.findItem(rid.getField(BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER_NAME).getInt(rid));
            barMenuItemKeepScreen = menu.findItem(rid.getField(BAR_MENU_ITEM_KEEP_SCREEN_NAME).getInt(rid));
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setupStringShelfDatabase() {
        stringShelfDatabase = new StringShelfDatabase(this);
        stringShelfDatabase.open();
    }

    private void setupBackLayout() {
        backLayout = findViewById(R.id.BACK_LAYOUT);
    }

    private void saveCurrentChronoTimer() {
        saveChronoTimer(stringShelfDatabase, ctRecordToChronoTimerRow(currentCtRecord));
    }

    private void saveCurrentColorsInDB() {
        for (int i = 0; i <= (getColorTypesCount() - 1); i = i + 1) {
            setCurrentValuesInCtDisplayActivity(stringShelfDatabase, getColorTableName(i), colors[i]);
        }
    }

    private void saveCurrentColorsInDBCtDisplayColorsActivity() {
        for (int i = 0; i <= (getColorTypesCount() - 1); i = i + 1) {
            setCurrentValuesInCtDisplayColorsActivity(stringShelfDatabase, getColorTableName(i), colors[i]);
        }
    }

    private void getDBCurrentOrDefaultColors() {
        for (int i = 0; i <= (getColorTypesCount() - 1); i = i + 1) {
            colors[i] = getCurrentValuesInCtDisplayActivity(stringShelfDatabase, getColorTableName(i));
            if (colors[i] == null) {
                colors[i] = getDefaults(stringShelfDatabase, getColorTableName(i));
            }
        }
    }

    private void getDBCurrentColorsFromCtDisplayColorsActivity() {
        for (int i = 0; i <= (getColorTypesCount() - 1); i = i + 1) {
            colors[i] = getCurrentValuesInCtDisplayColorsActivity(stringShelfDatabase, getColorTableName(i));
        }
    }

    private void launchPresetsActivity() {
        final String SEPARATOR = " - ";

        setStartStatusInPresetsActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, PresetsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), capitalize("CT Presets"));
        callingIntent.putExtra(PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS.SEPARATOR.toString(), SEPARATOR);
        callingIntent.putExtra(PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS.DISPLAY_TYPE.toString(), PRESETS_ACTIVITY_DISPLAY_TYPE.NO_COLORS.toString());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getPresetsCTTableName());
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.PRESETS.INDEX());
    }

    private void launchCtDisplayColorsActivity() {
        setStartStatusInCtDisplayColorsActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, CtDisplayColorsActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), currentCtRecord.getIdct());
        startActivityForResult(callingIntent, (SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.INDEX() + 1) * SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER);
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpctdisplayactivity);
        startActivity(callingIntent);
    }

    private boolean returnsFromPresetsActivity() {
        return (calledActivity.equals(PEKISLIB_ACTIVITIES.PRESETS.toString()));
    }

    private boolean returnsFromCtDisplayColorsActivity() {
        return (calledActivity.equals(SWTIMER_ACTIVITIES.CT_DISPLAY_COLORS.toString()));
    }

}