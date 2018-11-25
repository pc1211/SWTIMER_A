package com.example.pgyl.swtimer_a;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.example.pgyl.pekislib_a.ColorPickerActivity;
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
import static com.example.pgyl.pekislib_a.Constants.NOT_FOUND;
import static com.example.pgyl.pekislib_a.Constants.PEKISLIB_ACTIVITIES;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.MiscUtils.beep;
import static com.example.pgyl.pekislib_a.MiscUtils.capitalize;
import static com.example.pgyl.pekislib_a.MiscUtils.toastLong;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_IS_COLOR_TYPE;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.TABLE_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentColorsInColorPickerActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentPresetInPresetsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentColorsInColorPickerActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentPresetInPresetsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setDefaults;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusInColorPickerActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusInPresetsActivity;
import static com.example.pgyl.swtimer_a.CtDisplayTimeRobot.DISPLAY_INITIALIZE;
import static com.example.pgyl.swtimer_a.CtRecord.MODE;
import static com.example.pgyl.swtimer_a.CtRecord.USE_CLOCK_APP;
import static com.example.pgyl.swtimer_a.MainActivity.SWTIMER_SHP_KEY_NAMES;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.copyChronoTimerRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.copyPresetCTRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.ctRecordToChronoTimerRow;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getBackScreenColorBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getChronoTimerById;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getColorItemTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getCurrentColorsInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getDefaultColors;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getPresetsCTTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getTimeButtonsColorBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getTimeButtonsColorOffIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getTimeButtonsColorOnIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.isColdStartStatusInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.saveChronoTimer;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setCurrentColorsInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setGlobalDefaultColors;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setStartStatusInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.timeMessageToPresetCTRow;

public class CtDisplayActivity extends Activity {
    //region Constantes
    public enum COLOR_ITEMS {
        TIME, BUTTONS, BACK_SCREEN
    }

    private enum COMMANDS {
        RUN(R.raw.ct_run), SPLIT(R.raw.ct_split), INVERT_CLOCK_APP_ALARM(R.raw.ct_bell), RESET(R.raw.ct_reset), CHRONO_MODE(R.raw.ct_chrono), TIMER_MODE(R.raw.ct_timer);

        private int valueId;

        COMMANDS(int valueId) {
            this.valueId = valueId;
        }

        public int ID() {
            return valueId;
        }
    }

    private enum BAR_MENU_ITEMS {KEEP_SCREEN}

    public enum CTDISPLAY_EXTRA_KEYS {
        CURRENT_CHRONO_TIMER_ID
    }

    private final long DELAY_ZERO_MS = 0;
    private final int ACTIVITY_CODE_MULTIPLIER = 100;  // Pour différencier les types d'appel à une même activité
    //endregion
    //region Variables
    private CtRecord currentCtRecord;
    private CtDisplayTimeRobot ctDisplayTimeRobot;
    private boolean keepScreen;
    private String[] timeColors;
    private String[] buttonColors;
    private String[] backScreenColors;
    private DotMatrixDisplayView ctDisplayTimeView;
    private SymbolButtonView[] buttons;
    private Menu menu;
    private MenuItem[] barMenuItems;
    private LinearLayout backLayout;
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
        setupCtDisplayTimeView();
        backLayout = findViewById(R.id.BACK_LAYOUT);
        validReturnFromCalledActivity = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        savePreferences();

        ctDisplayTimeRobot.stopAutomatic();
        ctDisplayTimeRobot.close();
        ctDisplayTimeRobot = null;
        setCurrentColorsInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.TIME, timeColors);
        setCurrentColorsInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.BUTTONS, buttonColors);
        setCurrentColorsInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.BACK_SCREEN, backScreenColors);
        saveCurrentChronoTimer();
        currentCtRecord = null;
        stringShelfDatabase.close();
        stringShelfDatabase = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        long nowm = System.currentTimeMillis();
        shpFileName = getPackageName() + SHP_FILE_NAME_SUFFIX;   //  Sans nom d'activité car partagé avec MainActivity
        setupKeepScreen();
        setupStringShelfDatabase();
        currentCtRecord = new CtRecord(this);
        setupCtDisplayTimeRobot();
        int idct = getIntent().getIntExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), NOT_FOUND);
        copyChronoTimerRowToCtRecord(getChronoTimerById(stringShelfDatabase, idct), currentCtRecord);

        timeColors = getCurrentColorsInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.TIME);
        buttonColors = getCurrentColorsInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.BUTTONS);
        backScreenColors = getCurrentColorsInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.BACK_SCREEN);
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
                if (returnsFromColorPickerActivity()) {
                    if (calledActivity.equals(PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.TIME.toString())) {
                        timeColors = getCurrentColorsInColorPickerActivity(stringShelfDatabase, getColorItemTableName(COLOR_ITEMS.TIME));
                    }
                    if (calledActivity.equals(PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.BUTTONS.toString())) {
                        buttonColors = getCurrentColorsInColorPickerActivity(stringShelfDatabase, getColorItemTableName(COLOR_ITEMS.BUTTONS));
                    }
                    if (calledActivity.equals(PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.BACK_SCREEN.toString())) {
                        backScreenColors = getCurrentColorsInColorPickerActivity(stringShelfDatabase, getColorItemTableName(COLOR_ITEMS.BACK_SCREEN));
                    }
                }
            }
        }
        setupCtDisplayTimeColors();
        setupButtonColors();

        currentCtRecord.updateTime(nowm);
        updateCtDisplayTimeView();
        updateDisplayActivityTitle(currentCtRecord.getMessage());
        updateDisplayButtonColors();
        updateDisplayBackScreenColor();
        updateDisplayKeepScreen();
        ctDisplayTimeRobot.startAutomatic(DELAY_ZERO_MS);
        invalidateOptionsMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        validReturnFromCalledActivity = false;
        if (requestCode == PEKISLIB_ACTIVITIES.PRESETS.ordinal()) {
            calledActivity = PEKISLIB_ACTIVITIES.PRESETS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == (PEKISLIB_ACTIVITIES.COLOR_PICKER.ordinal() + 1) * ACTIVITY_CODE_MULTIPLIER + COLOR_ITEMS.TIME.ordinal()) {
            calledActivity = PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.TIME.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == (PEKISLIB_ACTIVITIES.COLOR_PICKER.ordinal() + 1) * ACTIVITY_CODE_MULTIPLIER + COLOR_ITEMS.BUTTONS.ordinal()) {
            calledActivity = PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.BUTTONS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == (PEKISLIB_ACTIVITIES.COLOR_PICKER.ordinal() + 1) * ACTIVITY_CODE_MULTIPLIER + COLOR_ITEMS.BACK_SCREEN.ordinal()) {
            calledActivity = PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.BACK_SCREEN.toString();
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
        updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {  // appelé par invalideOptionsMenu après changement d'orientation
        updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.HELP) {
            launchHelpActivity();
            return true;
        }
        if (item.getItemId() == R.id.BAR_MENU_KEEP_SCREEN) {
            keepScreen = !keepScreen;
            updateDisplayKeepScreen();
            updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        }
        if (item.getItemId() == R.id.SET_TIME_COLORS) {
            setCurrentColorsInColorPickerActivity(stringShelfDatabase, getColorItemTableName(COLOR_ITEMS.TIME), timeColors);
            setGlobalDefaultColors(stringShelfDatabase, COLOR_ITEMS.TIME, getDefaultColors(stringShelfDatabase, COLOR_ITEMS.TIME));
            launchColorPickerActivity(COLOR_ITEMS.TIME);
            return true;
        }
        if (item.getItemId() == R.id.SET_BUTTON_COLORS) {
            setCurrentColorsInColorPickerActivity(stringShelfDatabase, getColorItemTableName(COLOR_ITEMS.BUTTONS), buttonColors);
            setGlobalDefaultColors(stringShelfDatabase, COLOR_ITEMS.BUTTONS, getDefaultColors(stringShelfDatabase, COLOR_ITEMS.BUTTONS));
            launchColorPickerActivity(COLOR_ITEMS.BUTTONS);
            return true;
        }
        if (item.getItemId() == R.id.SET_BACK_SCREEN_COLORS) {
            setCurrentColorsInColorPickerActivity(stringShelfDatabase, getColorItemTableName(COLOR_ITEMS.BACK_SCREEN), backScreenColors);
            setGlobalDefaultColors(stringShelfDatabase, COLOR_ITEMS.BACK_SCREEN, getDefaultColors(stringShelfDatabase, COLOR_ITEMS.BACK_SCREEN));
            launchColorPickerActivity(COLOR_ITEMS.BACK_SCREEN);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    private void onButtonClick(COMMANDS command) {
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
        updateCtDisplayTimeView();
    }

    private void onButtonClickRun(long nowm) {
        if (!currentCtRecord.isRunning()) {
            currentCtRecord.start(nowm);
        } else {
            if (!currentCtRecord.stop(nowm)) {
                currentCtRecord.setClockAppAlarmOff(USE_CLOCK_APP);
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
                    currentCtRecord.setClockAppAlarmOn(USE_CLOCK_APP);
                } else {
                    currentCtRecord.setClockAppAlarmOff(USE_CLOCK_APP);
                }
            }
        }
    }

    private void onButtonClickReset() {
        if (!currentCtRecord.reset()) {
            currentCtRecord.setClockAppAlarmOff(USE_CLOCK_APP);
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
        toastLong(currentCtRecord.getTimeZoneExpirationMessage(), this);
        updateDisplayButtonColors();
        beep(this);
    }

    private void onCtDisplayTimeViewClick() {
        setCurrentPresetInPresetsActivity(stringShelfDatabase, getPresetsCTTableName(), timeMessageToPresetCTRow(currentCtRecord.getTimeDef(), currentCtRecord.getMessage()));
        setDefaults(stringShelfDatabase, getPresetsCTTableName(), timeMessageToPresetCTRow(currentCtRecord.getTimeDefInit(), currentCtRecord.getMessageInit()));
        launchPresetsActivity();
    }

    private void updateCtDisplayTimeView() {
        ctDisplayTimeRobot.updateCtDisplayTimeView(DISPLAY_INITIALIZE);
    }

    private void updateDisplayBackScreenColor() {
        backLayout.setBackgroundColor(Color.parseColor(COLOR_PREFIX + backScreenColors[getBackScreenColorBackIndex()]));
    }

    private void updateDisplayButtonColor(COMMANDS command) {  //   ON/BACK ou OFF/BACK
        int frontColorIndex = ((getButtonState(command)) ? getTimeButtonsColorOnIndex() : getTimeButtonsColorOffIndex());
        int alternateColorIndex = ((getButtonState(command)) ? getTimeButtonsColorOffIndex() : getTimeButtonsColorOnIndex());
        int index = command.ordinal();
        buttons[index].setFrontColorIndex(frontColorIndex);
        buttons[index].setBackColorIndex(getTimeButtonsColorBackIndex());
        buttons[index].setAlternateColorIndex(alternateColorIndex);
        buttons[index].invalidate();
    }

    private void updateDisplayButtonColors() {
        for (COMMANDS command : COMMANDS.values()) {
            updateDisplayButtonColor(command);
        }
    }

    private void updateDisplayActivityTitle(String activityTitle) {
        getActionBar().setTitle(activityTitle);
    }

    private void updateDisplayKeepScreenBarMenuItemIcon(boolean keepScreen) {
        barMenuItems[BAR_MENU_ITEMS.KEEP_SCREEN.ordinal()].setIcon((keepScreen ? R.drawable.main_light_on : R.drawable.main_light_off));
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

    private boolean getSHPKeepScreen() {
        final boolean KEEP_SCREEN_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.KEEP_SCREEN.toString(), KEEP_SCREEN_DEFAULT_VALUE);
    }

    private void savePreferences() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        SharedPreferences.Editor shpEditor = shp.edit();
        shpEditor.putBoolean(SWTIMER_SHP_KEY_NAMES.KEEP_SCREEN.toString(), keepScreen);
        shpEditor.commit();
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

        buttons = new SymbolButtonView[COMMANDS.values().length];
        Class rid = R.id.class;
        for (COMMANDS command : COMMANDS.values()) {
            try {
                int index = command.ordinal();
                buttons[index] = findViewById(rid.getField(BUTTON_XML_NAME_PREFIX + command.toString()).getInt(rid));
                buttons[index].setSVGImageResource(command.ID());
                final COMMANDS fcommand = command;
                buttons[index].setCustomOnClickListener(new SymbolButtonView.onCustomClickListener() {
                    @Override
                    public void onCustomClick() {
                        onButtonClick(fcommand);
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

    private void setupCtDisplayTimeView() {  //  Pour Afficher HH:MM:SS.CC et éventuellement un message
        final int GRID_DISPLAY_WIDTH = 51;   //  7 caracteres avec marge droite x (5+1) + dernier caractere sans marge droite 1 x 5 + 2 doubles points x 2
        final int GRID_DISPLAY_HEIGHT = 8;   //  7 lignes + 1 pour le point décimal (surchargeant le caractère précédent)
        final int GRID_TOTAL_WIDTH = 200;    //  Suffisant pour stocker l'affichage du temps et d'un message de max. 24 caractères 5x7, avec marge droite
        final int GRID_TOTAL_HEIGHT = GRID_DISPLAY_HEIGHT;

        ctDisplayTimeView = findViewById(R.id.DISPLAY_TIME);
        ctDisplayTimeView.setGridDimensions(new Rect(0, 0, GRID_DISPLAY_WIDTH, GRID_DISPLAY_HEIGHT), new Rect(0, 0, GRID_TOTAL_WIDTH, GRID_TOTAL_HEIGHT));
        ctDisplayTimeView.setOnCustomClickListener(new DotMatrixDisplayView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onCtDisplayTimeViewClick();
            }
        });
    }

    private void setupKeepScreen() {
        keepScreen = getSHPKeepScreen();
    }

    private void setupStringShelfDatabase() {
        stringShelfDatabase = new StringShelfDatabase(this);
        stringShelfDatabase.open();
    }

    private void setupCtDisplayTimeRobot() {
        ctDisplayTimeRobot = new CtDisplayTimeRobot(ctDisplayTimeView, currentCtRecord);
        ctDisplayTimeRobot.setOnExpiredTimerListener(new CtDisplayTimeRobot.onExpiredTimerListener() {
            @Override
            public void onExpiredTimer() {
                onExpiredTimerCurrentChronoTimer();
            }
        });
    }

    private void setupCtDisplayTimeColors() {
        ctDisplayTimeView.setColors(timeColors);
        ctDisplayTimeView.setFrontColorIndex(getTimeButtonsColorOnIndex());
        ctDisplayTimeView.setBackColorIndex(getTimeButtonsColorOffIndex());
        ctDisplayTimeView.setAlternateColorIndex(getTimeButtonsColorBackIndex());
    }

    private void setupButtonColors() {
        for (COMMANDS command : COMMANDS.values()) {
            int index = command.ordinal();
            buttons[index].setColors(buttonColors);
        }
    }

    private void setupBarMenuItems() {
        final String MENU_ITEM_XML_PREFIX = "BAR_MENU_";

        barMenuItems = new MenuItem[BAR_MENU_ITEMS.values().length];
        Class rid = R.id.class;
        for (BAR_MENU_ITEMS barMenuItem : BAR_MENU_ITEMS.values())
            try {
                int index = barMenuItem.ordinal();
                barMenuItems[index] = menu.findItem(rid.getField(MENU_ITEM_XML_PREFIX + barMenuItem.toString()).getInt(rid));
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

    private void saveCurrentChronoTimer() {
        saveChronoTimer(stringShelfDatabase, ctRecordToChronoTimerRow(currentCtRecord));
    }

    private void launchColorPickerActivity(COLOR_ITEMS colorItem) {
        setStartStatusInColorPickerActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, ColorPickerActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), capitalize(colorItem.toString()) + " Colors (RRGGBB)");
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getColorItemTableName(colorItem));
        startActivityForResult(callingIntent, (PEKISLIB_ACTIVITIES.COLOR_PICKER.ordinal() + 1) * ACTIVITY_CODE_MULTIPLIER + colorItem.ordinal());  //  Il faut différencier les 3 types de couleur
    }

    private void launchPresetsActivity() {
        final String SEPARATOR = " - ";

        setStartStatusInPresetsActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, PresetsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), "Time / Message Presets");
        callingIntent.putExtra(PRESETS_ACTIVITY_EXTRA_KEYS.SEPARATOR.toString(), SEPARATOR);
        callingIntent.putExtra(PRESETS_ACTIVITY_EXTRA_KEYS.IS_COLOR_TYPE.toString(), String.valueOf((!PRESETS_ACTIVITY_IS_COLOR_TYPE) ? 1 : 0));
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getPresetsCTTableName());
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.PRESETS.ordinal());
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

    private boolean returnsFromColorPickerActivity() {
        String colorPickerActivityBaseName = PEKISLIB_ACTIVITIES.COLOR_PICKER.toString();
        if (calledActivity.length() >= colorPickerActivityBaseName.length()) {
            return (calledActivity.substring(0, colorPickerActivityBaseName.length()).equals(colorPickerActivityBaseName));
        } else return false;
    }

}