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
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_DISPLAY_TYPE;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.TABLE_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentColorsInColorPickerActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getCurrentPresetInPresetsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentColorsInColorPickerActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setCurrentPresetInPresetsActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setDefaults;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusInColorPickerActivity;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.setStartStatusInPresetsActivity;
import static com.example.pgyl.swtimer_a.CtDisplayTimeUpdater.DISPLAY_INITIALIZE;
import static com.example.pgyl.swtimer_a.CtRecord.MODE;
import static com.example.pgyl.swtimer_a.CtRecord.USE_CLOCK_APP;
import static com.example.pgyl.swtimer_a.MainActivity.SWTIMER_SHP_KEY_NAMES;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.chronoTimerRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.copyPresetCTRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.ctRecordToChronoTimerRow;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getBackScreenColorBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getButtonsColorBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getButtonsColorOffIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getButtonsColorOnIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getChronoTimerById;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getColorsBackScreenTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getColorsButtonsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getColorsTimeTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getCurrentValuesInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getMessagesTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getPresetsCTTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.isColdStartStatusInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.saveChronoTimer;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setCurrentValuesInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setStartStatusInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.timeMessageToPresetCTRow;

public class CtDisplayActivity extends Activity {
    //region Constantes
    public enum COLOR_ITEMS {
        TIME(getColorsTimeTableName()), BUTTONS(getColorsButtonsTableName()), BACK_SCREEN(getColorsBackScreenTableName());

        private String tableName;

        COLOR_ITEMS(String tableName) {
            this.tableName = tableName;
        }

        public int INDEX() {
            return ordinal();
        }

        public String getTableName() {
            return tableName;
        }
    }

    public enum PRESETS_ITEMS {
        MESSAGES(getMessagesTableName()), PRESETS_CT(getPresetsCTTableName());

        private String tableName;

        PRESETS_ITEMS(String tableName) {
            this.tableName = tableName;
        }

        public int INDEX() {
            return ordinal();
        }

        public String getTableName() {
            return tableName;
        }
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

        public int INDEX() {
            return ordinal();
        }
    }

    public enum CTDISPLAY_EXTRA_KEYS {
        CURRENT_CHRONO_TIMER_ID
    }

    private final int ACTIVITY_CODE_MULTIPLIER = 100;  // Pour différencier les types d'appel à une même activité
    //endregion
    //region Variables
    private CtRecord currentCtRecord;
    private DotMatrixDisplayView timeDotMatrixDisplayView;
    private CtDisplayTimeUpdater ctDisplayTimeUpdater;
    private SymbolButtonView[] buttons;
    private Menu menu;
    private MenuItem barMenuItemKeepScreen;
    private LinearLayout backLayout;
    private boolean keepScreen;
    private String[] timeColors;
    private String[] buttonColors;
    private String[] backScreenColors;
    private String[] messages;
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
        setupTimeDotMatrixDisplayView();
        backLayout = findViewById(R.id.BACK_LAYOUT);
        validReturnFromCalledActivity = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        ctDisplayTimeUpdater.stopAutomatic();
        ctDisplayTimeUpdater.close();
        ctDisplayTimeUpdater = null;
        saveCurrentChronoTimer();
        currentCtRecord = null;
        setCurrentValuesInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.TIME.getTableName(), timeColors);
        setCurrentValuesInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.BUTTONS.getTableName(), buttonColors);
        setCurrentValuesInCtDisplayActivity(stringShelfDatabase, COLOR_ITEMS.BACK_SCREEN.getTableName(), backScreenColors);
        setCurrentValuesInCtDisplayActivity(stringShelfDatabase, PRESETS_ITEMS.MESSAGES.getTableName(), messages);
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
        keepScreen = getSHPKeepScreen();
        setupStringShelfDatabase();
        int idct = getIntent().getIntExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), NOT_FOUND);
        currentCtRecord = chronoTimerRowToCtRecord(getChronoTimerById(stringShelfDatabase, idct), this);
        setupCtDisplayTimeUpdater();

        timeColors = getCurrentValuesInCtDisplayActivity(stringShelfDatabase, getColorsTimeTableName());
        buttonColors = getCurrentValuesInCtDisplayActivity(stringShelfDatabase, getColorsButtonsTableName());
        backScreenColors = getCurrentValuesInCtDisplayActivity(stringShelfDatabase, getColorsBackScreenTableName());
        messages = getCurrentValuesInCtDisplayActivity(stringShelfDatabase, getMessagesTableName());
        if (isColdStartStatusInCtDisplayActivity(stringShelfDatabase)) {
            setStartStatusInCtDisplayActivity(stringShelfDatabase, ACTIVITY_START_STATUS.HOT);
        } else {
            if (validReturnFromCalledActivity) {
                validReturnFromCalledActivity = false;
                if (returnsFromPresetsActivity()) {
                    if (calledActivity.equals(PEKISLIB_ACTIVITIES.PRESETS.toString() + PRESETS_ITEMS.PRESETS_CT.toString())) {
                        if (!copyPresetCTRowToCtRecord(getCurrentPresetInPresetsActivity(stringShelfDatabase, getPresetsCTTableName()), currentCtRecord, nowm)) {
                            toastLong("Error updating Timer", this);
                        }
                    }
                    if (calledActivity.equals(PEKISLIB_ACTIVITIES.PRESETS.toString() + PRESETS_ITEMS.MESSAGES.toString())) {
                        messages = getCurrentPresetInPresetsActivity(stringShelfDatabase, PRESETS_ITEMS.MESSAGES.getTableName());
                    }
                }
                if (returnsFromColorPickerActivity()) {
                    if (calledActivity.equals(PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.TIME.toString())) {
                        timeColors = getCurrentColorsInColorPickerActivity(stringShelfDatabase, COLOR_ITEMS.TIME.getTableName());
                    }
                    if (calledActivity.equals(PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.BUTTONS.toString())) {
                        buttonColors = getCurrentColorsInColorPickerActivity(stringShelfDatabase, COLOR_ITEMS.BUTTONS.getTableName());
                    }
                    if (calledActivity.equals(PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.BACK_SCREEN.toString())) {
                        backScreenColors = getCurrentColorsInColorPickerActivity(stringShelfDatabase, COLOR_ITEMS.BACK_SCREEN.getTableName());
                    }
                }
            }
        }

        ctDisplayTimeUpdater.setGridDimensions(messages);
        ctDisplayTimeUpdater.setGridColors(timeColors);
        updateDisplayTime();
        ctDisplayTimeUpdater.startAutomatic();
        updateDisplayButtonColors();
        updateDisplayBackScreenColor();
        updateDisplayKeepScreen();
        updateDisplayActivityTitle(currentCtRecord.getMessage());
        invalidateOptionsMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        validReturnFromCalledActivity = false;
        if (requestCode == (PEKISLIB_ACTIVITIES.PRESETS.INDEX() + 1) * ACTIVITY_CODE_MULTIPLIER + PRESETS_ITEMS.PRESETS_CT.INDEX()) {
            calledActivity = PEKISLIB_ACTIVITIES.PRESETS.toString() + PRESETS_ITEMS.PRESETS_CT.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == (PEKISLIB_ACTIVITIES.PRESETS.INDEX() + 1) * ACTIVITY_CODE_MULTIPLIER + PRESETS_ITEMS.MESSAGES.INDEX()) {
            calledActivity = PEKISLIB_ACTIVITIES.PRESETS.toString() + PRESETS_ITEMS.MESSAGES.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == (PEKISLIB_ACTIVITIES.COLOR_PICKER.INDEX() + 1) * ACTIVITY_CODE_MULTIPLIER + COLOR_ITEMS.TIME.INDEX()) {
            calledActivity = PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.TIME.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == (PEKISLIB_ACTIVITIES.COLOR_PICKER.INDEX() + 1) * ACTIVITY_CODE_MULTIPLIER + COLOR_ITEMS.BUTTONS.INDEX()) {
            calledActivity = PEKISLIB_ACTIVITIES.COLOR_PICKER.toString() + COLOR_ITEMS.BUTTONS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == (PEKISLIB_ACTIVITIES.COLOR_PICKER.INDEX() + 1) * ACTIVITY_CODE_MULTIPLIER + COLOR_ITEMS.BACK_SCREEN.INDEX()) {
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
        setupBarMenuItemKeepScreen();
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
        if (item.getItemId() == R.id.BAR_MENU_ITEM_KEEP_SCREEN) {
            keepScreen = !keepScreen;
            updateDisplayKeepScreen();
            updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        }
        if (item.getItemId() == R.id.SET_TIME_COLORS) {
            setCurrentColorsInColorPickerActivity(stringShelfDatabase, COLOR_ITEMS.TIME.getTableName(), timeColors);
            launchColorPickerActivity(COLOR_ITEMS.TIME);
            return true;
        }
        if (item.getItemId() == R.id.SET_BUTTON_COLORS) {
            setCurrentColorsInColorPickerActivity(stringShelfDatabase, COLOR_ITEMS.BUTTONS.getTableName(), buttonColors);
            launchColorPickerActivity(COLOR_ITEMS.BUTTONS);
            return true;
        }
        if (item.getItemId() == R.id.SET_BACK_SCREEN_COLORS) {
            setCurrentColorsInColorPickerActivity(stringShelfDatabase, COLOR_ITEMS.BACK_SCREEN.getTableName(), backScreenColors);
            launchColorPickerActivity(COLOR_ITEMS.BACK_SCREEN);
            return true;
        }
        if (item.getItemId() == R.id.SET_MESSAGES) {
            setCurrentPresetInPresetsActivity(stringShelfDatabase, PRESETS_ITEMS.MESSAGES.getTableName(), messages);
            launchPresetsActivity(PRESETS_ITEMS.MESSAGES);
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
        updateDisplayTime();
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
        launchPresetsActivity(PRESETS_ITEMS.PRESETS_CT);
    }

    private void updateDisplayTime() {
        ctDisplayTimeUpdater.update(DISPLAY_INITIALIZE);
    }

    private void updateDisplayBackScreenColor() {
        backLayout.setBackgroundColor(Color.parseColor(COLOR_PREFIX + backScreenColors[getBackScreenColorBackIndex()]));
    }

    private void updateDisplayButtonColor(COMMANDS command) {  //   ON/BACK ou OFF/BACK
        buttons[command.INDEX()].setFrontColor(((getButtonState(command)) ? buttonColors[getButtonsColorOnIndex()] : buttonColors[getButtonsColorOffIndex()]));
        buttons[command.INDEX()].setBackColor(buttonColors[getButtonsColorBackIndex()]);
        buttons[command.INDEX()].setExtraColor(((getButtonState(command)) ? buttonColors[getButtonsColorOffIndex()] : buttonColors[getButtonsColorOnIndex()]));
        buttons[command.INDEX()].invalidate();
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
                buttons[command.INDEX()] = findViewById(rid.getField(BUTTON_XML_NAME_PREFIX + command.toString()).getInt(rid));
                buttons[command.INDEX()].setSVGImageResource(command.ID());
                final COMMANDS fcommand = command;
                buttons[command.INDEX()].setCustomOnClickListener(new SymbolButtonView.onCustomClickListener() {
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

    private void setupTimeDotMatrixDisplayView() {  //  Pour Afficher HH:MM:SS.CC et éventuellement un message
        timeDotMatrixDisplayView = findViewById(R.id.DISPLAY_TIME);
        timeDotMatrixDisplayView.setOnCustomClickListener(new DotMatrixDisplayView.onCustomClickListener() {
            @Override
            public void onCustomClick() {
                onCtDisplayTimeViewClick();
            }
        });
    }

    private void setupStringShelfDatabase() {
        stringShelfDatabase = new StringShelfDatabase(this);
        stringShelfDatabase.open();
    }

    private void setupCtDisplayTimeUpdater() {
        ctDisplayTimeUpdater = new CtDisplayTimeUpdater(timeDotMatrixDisplayView, currentCtRecord);
        ctDisplayTimeUpdater.setOnExpiredTimerListener(new CtDisplayTimeUpdater.onExpiredTimerListener() {
            @Override
            public void onExpiredTimer() {
                onExpiredTimerCurrentChronoTimer();
            }
        });
    }

    private void setupBarMenuItemKeepScreen() {
        final String BAR_MENU_ITEM_KEEP_SCREEN_NAME = "BAR_MENU_ITEM_KEEP_SCREEN";

        Class rid = R.id.class;
        try {
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

    private void saveCurrentChronoTimer() {
        saveChronoTimer(stringShelfDatabase, ctRecordToChronoTimerRow(currentCtRecord));
    }

    private void launchPresetsActivity(PRESETS_ITEMS presetsItem) {
        final String SEPARATOR = " - ";

        setStartStatusInPresetsActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, PresetsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), capitalize(presetsItem.toString()));
        callingIntent.putExtra(PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS.SEPARATOR.toString(), SEPARATOR);
        callingIntent.putExtra(PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS.DISPLAY_TYPE.toString(), PRESETS_ACTIVITY_DISPLAY_TYPE.NO_COLORS.toString());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), presetsItem.getTableName());
        startActivityForResult(callingIntent, (PEKISLIB_ACTIVITIES.PRESETS.INDEX() + 1) * ACTIVITY_CODE_MULTIPLIER + presetsItem.INDEX());
    }

    private void launchColorPickerActivity(COLOR_ITEMS colorItem) {
        setStartStatusInColorPickerActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, ColorPickerActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), capitalize(colorItem.toString()) + " Colors");
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), colorItem.getTableName());
        startActivityForResult(callingIntent, (PEKISLIB_ACTIVITIES.COLOR_PICKER.INDEX() + 1) * ACTIVITY_CODE_MULTIPLIER + colorItem.INDEX());  //  Il faut différencier les 3 types de couleur
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpctdisplayactivity);
        startActivity(callingIntent);
    }

    private boolean returnsFromPresetsActivity() {
        String PresetsActivityBaseName = PEKISLIB_ACTIVITIES.PRESETS.toString();
        if (calledActivity.length() >= PresetsActivityBaseName.length()) {
            return (calledActivity.substring(0, PresetsActivityBaseName.length()).equals(PresetsActivityBaseName));
        } else {
            return false;
        }
    }

    private boolean returnsFromColorPickerActivity() {
        String colorPickerActivityBaseName = PEKISLIB_ACTIVITIES.COLOR_PICKER.toString();
        if (calledActivity.length() >= colorPickerActivityBaseName.length()) {
            return (calledActivity.substring(0, colorPickerActivityBaseName.length()).equals(colorPickerActivityBaseName));
        } else {
            return false;
        }
    }

}