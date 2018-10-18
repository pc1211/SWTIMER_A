package com.example.pgyl.swtimer_a;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

import com.example.pgyl.pekislib_a.CustomImageButton;
import com.example.pgyl.pekislib_a.HelpActivity;
import com.example.pgyl.pekislib_a.InputButtonsActivity;
import com.example.pgyl.pekislib_a.StateView;
import com.example.pgyl.pekislib_a.StringShelfDatabase;
import com.example.pgyl.pekislib_a.TimeDateUtils;

import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.pgyl.pekislib_a.Constants.ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.Constants.ACTIVITY_START_TYPE;
import static com.example.pgyl.pekislib_a.Constants.BUTTON_STATES;
import static com.example.pgyl.pekislib_a.Constants.PEKISLIB_ACTIVITIES;
import static com.example.pgyl.pekislib_a.Constants.PEKISLIB_TABLES;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.Constants.TABLE_ACTIVITY_INFOS_DATA_FIELDS;
import static com.example.pgyl.pekislib_a.Constants.TABLE_COLORS_REGEXP_HEX_DEFAULT;
import static com.example.pgyl.pekislib_a.Constants.TABLE_IDS;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.TimeDateUtils.convertXhmsToMs;
import static com.example.pgyl.swtimer_a.Constants.COLOR_ITEMS;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_SHP_KEY_NAMES;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_TABLES;
import static com.example.pgyl.swtimer_a.Constants.TABLE_CHRONO_TIMERS_DATA_FIELDS;
import static com.example.pgyl.swtimer_a.Constants.TABLE_COLORS_BACK_SCREEN_DATA_FIELDS;
import static com.example.pgyl.swtimer_a.Constants.TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS;
import static com.example.pgyl.swtimer_a.Constants.TABLE_PRESETS_CT_DATA_FIELDS;
import static com.example.pgyl.swtimer_a.CtDisplayActivity.CTDISPLAY_EXTRA_KEYS;
import static com.example.pgyl.swtimer_a.CtRecord.MODE;

public class MainActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        SHOW_EXPIRATION_TIME(R.drawable.main_clock), ADD_NEW_CHRONOTIMER_TO_LIST(R.drawable.main_tolist), NEW_CHRONO(R.drawable.main_chrono), NEW_TIMER(R.drawable.main_timer), INVERT_SELECTION_ALL_CT(R.drawable.main_inv), SELECT_ALL_CT(R.drawable.main_all), START_SELECTED_CT(R.drawable.main_start), STOP_SELECTED_CT(R.drawable.main_stop), SPLIT_SELECTED_CT(R.drawable.main_split), RESET_SELECTED_CT(R.drawable.main_reset), REMOVE_SELECTED_CT(R.drawable.main_remove);

        private int valueId;

        COMMANDS(int valueId) {
            this.valueId = valueId;
        }

        public int ID() {
            return valueId;
        }
    }

    private enum BAR_MENU_ITEMS {KEEP_SCREEN}

    private final int UPDATE_MAIN_CTLIST_TIME_INTERVAL_MS = 1000;
    private final long DELAY_ZERO_MS = 0;
    //endregion
    //region Variables
    EnumMap<COMMANDS, StateView> commandStateViewsMap;
    private CustomImageButton[] buttons;
    private Menu menu;
    private MenuItem[] barMenuItems;
    private CtRecordsHandler ctRecordsHandler;
    private MainCtListRobot mainCtListRobot;
    private boolean showExpirationTime;
    private boolean addNewChronoTimerToList;
    private boolean keepScreen;
    private ListView mainCtListView;
    private MainCtListItemAdapter mainCtListItemAdapter;
    private boolean validReturnFromCalledActivity;
    private String calledActivity;
    private StringShelfDatabase stringShelfDatabase;
    private String shpFileName;
    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String ACTIVITY_TITLE = "Swtimer";

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setTitle(ACTIVITY_TITLE);
        setContentView(R.layout.main);
        setupButtons();
        mainCtListView = (ListView) findViewById(R.id.CT_LIST);
        showExpirationTime = false;
        addNewChronoTimerToList = false;
        keepScreen = false;
        validReturnFromCalledActivity = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        mainCtListRobot.stopAutomatic();
        mainCtListRobot.close();
        mainCtListRobot = null;
        mainCtListItemAdapter.close();
        mainCtListItemAdapter = null;
        ctRecordsHandler.saveAndclose();
        ctRecordsHandler = null;
        stringShelfDatabase.close();
        stringShelfDatabase = null;
        commandStateViewsMap.clear();
        commandStateViewsMap = null;
        menu = null;
        savePreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();

        shpFileName = getPackageName() + SHP_FILE_NAME_SUFFIX;   //  Sans nom d'activité car sera partagé avec CtDisplayActivity
        setupStates();
        setupShowExpirationTime();
        setupAddNewChronoTimerToList();
        setupKeepScreen();
        setupStringShelfDatabase();

        if (validReturnFromCalledActivity) {
            validReturnFromCalledActivity = false;
            if (returnsFromCtDisplay()) {
                //  NOP
            }
            if (returnsFromHelp()) {
                //  NOP
            }
        }

        setupButtonColors();
        setupCtRecordsHandler();
        setupMainCtList();
        setupMainCtListRobot();
        rebuildList();
        mainCtListRobot.startAutomatic(UPDATE_MAIN_CTLIST_TIME_INTERVAL_MS);
        invalidateOptionsMenu();
    }
    //endregion

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        validReturnFromCalledActivity = false;
        if (requestCode == SWTIMER_ACTIVITIES.CTDISPLAY.ordinal()) {
            calledActivity = SWTIMER_ACTIVITIES.CTDISPLAY.toString();
            validReturnFromCalledActivity = true;
        }
        if (requestCode == PEKISLIB_ACTIVITIES.HELP.ordinal()) {
            calledActivity = PEKISLIB_ACTIVITIES.HELP.toString();
            validReturnFromCalledActivity = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {  //  Non appelé après changement d'orientation
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        setupBarMenuItems();
        setKeepScreenBarMenuItemIcon(keepScreen);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {  // appelé par invalideOptionsMenu après changement d'orientation
        setKeepScreenBarMenuItemIcon(keepScreen);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            launchHelpActivity();
            return true;
        }
        if (item.getItemId() == R.id.BAR_MENU_KEEP_SCREEN) {
            keepScreen = !keepScreen;
            setScreen(keepScreen);
            setKeepScreenBarMenuItemIcon(keepScreen);
        }
        return super.onOptionsItemSelected(item);
    }

    private void onButtonClick(COMMANDS command) {
        long nowm = System.currentTimeMillis();
        if (command.equals(COMMANDS.NEW_CHRONO)) {
            onButtonClickAddNewChrono(nowm);
        }
        if (command.equals(COMMANDS.NEW_TIMER)) {
            onButtonClickAddNewTimer(nowm);
        }
        if (command.equals(COMMANDS.SHOW_EXPIRATION_TIME)) {
            onButtonClickShowExpirationTime();
        }
        if (command.equals(COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST)) {
            onButtonClickAddNewChronoTimerToList();
        }
        if ((command.equals(COMMANDS.INVERT_SELECTION_ALL_CT)) || (command.equals(COMMANDS.SELECT_ALL_CT))) {
            onButtonClickActionOnAll(command);
        }
        if ((command.equals(COMMANDS.START_SELECTED_CT)) || (command.equals(COMMANDS.STOP_SELECTED_CT)) || (command.equals(COMMANDS.SPLIT_SELECTED_CT)) || (command.equals(COMMANDS.RESET_SELECTED_CT)) || (command.equals(COMMANDS.REMOVE_SELECTED_CT))) {
            onButtonClickActionOnSelection(command, nowm);
        }
    }

    private void onButtonClickActionOnAll(COMMANDS command) {
        if (ctRecordsHandler.getCountAll() >= 1) {
            mainCtListRobot.stopAutomatic();
            if (command.equals(COMMANDS.INVERT_SELECTION_ALL_CT)) {
                ctRecordsHandler.invertSelectionAll();
            }
            if (command.equals(COMMANDS.SELECT_ALL_CT)) {
                ctRecordsHandler.selectAll();
            }
            mainCtListRobot.startAutomatic(DELAY_ZERO_MS);
        } else {
            Toast.makeText(this, "The list must contain at least one Chrono or Timer", Toast.LENGTH_LONG).show();
        }
    }

    private void onButtonClickActionOnSelection(COMMANDS command, long nowm) {
        if (ctRecordsHandler.getCountSelection() >= 1) {
            mainCtListRobot.stopAutomatic();
            if (command.equals(COMMANDS.START_SELECTED_CT)) {
                ctRecordsHandler.startSelection(nowm);
            }
            if (command.equals(COMMANDS.STOP_SELECTED_CT)) {
                ctRecordsHandler.stopSelection(nowm);
            }
            if (command.equals(COMMANDS.SPLIT_SELECTED_CT)) {
                ctRecordsHandler.splitSelection(nowm);
            }
            if (command.equals(COMMANDS.RESET_SELECTED_CT)) {
                ctRecordsHandler.resetSelection();
            }
            if (command.equals(COMMANDS.REMOVE_SELECTED_CT)) {
                removeSelection();
            }
            rebuildList();
            mainCtListRobot.startAutomatic(DELAY_ZERO_MS);
        } else {
            Toast.makeText(this, "The list must contain at least one selected Chrono or Timer", Toast.LENGTH_LONG).show();
        }
    }

    private void onButtonClickAddNewChrono(long nowm) {
        setNewChronoTimer(MODE.CHRONO, nowm);
    }

    private void onButtonClickAddNewTimer(long nowm) {
        setNewChronoTimer(MODE.TIMER, nowm);
    }

    private void onButtonClickShowExpirationTime() {
        showExpirationTime = !showExpirationTime;
        updateStateColor(commandStateViewsMap.get(COMMANDS.SHOW_EXPIRATION_TIME), showExpirationTime);
        mainCtListItemAdapter.setShowExpirationTime(showExpirationTime);
        rebuildList();
    }

    private void onButtonClickAddNewChronoTimerToList() {
        addNewChronoTimerToList = !addNewChronoTimerToList;
        updateStateColor(commandStateViewsMap.get(COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST), addNewChronoTimerToList);
    }

    private void onCtListExpiredTimers() {
        mainCtListRobot.stopAutomatic();
        rebuildList();
        mainCtListRobot.startAutomatic(DELAY_ZERO_MS);
    }

    private void onCtListItemButtonClick() {
        mainCtListRobot.stopAutomatic();
        rebuildList();
        mainCtListRobot.startAutomatic(DELAY_ZERO_MS);
    }

    private void removeSelection() {
        String s = String.valueOf(ctRecordsHandler.getCountSelection());
        if (ctRecordsHandler.getCountSelection() == ctRecordsHandler.getCountAll()) {
            s = "All";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove " + s + " Chronos/Timers");
        builder.setMessage("Are you sure ?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int id) {
                ctRecordsHandler.removeSelection();
                rebuildList();
            }
        });
        builder.setNegativeButton("No", null);
        Dialog dialog = builder.create();
        dialog.show();
    }

    private void setNewChronoTimer(MODE mode, long nowm) {
        final String MESSAGE_INIT_DEFAULT_VALUE = "Message";
        final int TIMEDEFINIT_DEFAULT_VALUE = 0;

        mainCtListRobot.stopAutomatic();
        int idct = ctRecordsHandler.createNewChronoTimer(mode, TIMEDEFINIT_DEFAULT_VALUE, MESSAGE_INIT_DEFAULT_VALUE);
        if (addNewChronoTimerToList) {
            rebuildList();
            mainCtListRobot.startAutomatic(DELAY_ZERO_MS);
        } else {
            launchCtDisplayActivity(idct);
        }
    }

    private void setScreen(boolean keepScreen) {
        if (keepScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void setKeepScreenBarMenuItemIcon(boolean keepScreen) {
        int id;

        if (keepScreen) {
            id = R.drawable.main_light_on;
        } else {
            id = R.drawable.main_light_off;
        }
        barMenuItems[BAR_MENU_ITEMS.KEEP_SCREEN.ordinal()].setIcon(id);
    }

    private void updateStateColor(StateView stateView, boolean state) {
        if (state) {
            stateView.setStateOn();
        } else {
            stateView.setStateOff();
        }
    }

    private void updateButtonColor(COMMANDS command) {
        int index = command.ordinal();
        buttons[index].updateColor();
    }

    private void rebuildList() {
        long nowm = System.currentTimeMillis();
        ctRecordsHandler.updateTimeAll(nowm);
        ctRecordsHandler.sortCtRecords();
        mainCtListItemAdapter.setItems(ctRecordsHandler.getChronoTimers());
        mainCtListItemAdapter.notifyDataSetChanged();
    }

    private void savePreferences() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        SharedPreferences.Editor shpEditor = shp.edit();
        shpEditor.putBoolean(SWTIMER_SHP_KEY_NAMES.SHOW_EXPIRATION_TIME.toString(), showExpirationTime);
        shpEditor.putBoolean(SWTIMER_SHP_KEY_NAMES.ADD_NEW_CHRONOTIMER_TO_LIST.toString(), addNewChronoTimerToList);
        shpEditor.putBoolean(SWTIMER_SHP_KEY_NAMES.KEEP_SCREEN.toString(), keepScreen);
        shpEditor.commit();
    }

    private boolean getSHPShowExpirationTime() {
        final boolean SHOW_EXPIRATION_TIME_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.SHOW_EXPIRATION_TIME.toString(), SHOW_EXPIRATION_TIME_DEFAULT_VALUE);
    }

    private boolean getSHPaddNewChronoTimerToList() {
        final boolean ADD_NEW_CHRONOTIMER_TO_LIST_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.ADD_NEW_CHRONOTIMER_TO_LIST.toString(), ADD_NEW_CHRONOTIMER_TO_LIST_DEFAULT_VALUE);
    }

    private boolean getSHPKeepScreen() {
        final boolean KEEP_SCREEN_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.KEEP_SCREEN.toString(), KEEP_SCREEN_DEFAULT_VALUE);
    }

    private void setupShowExpirationTime() {
        showExpirationTime = getSHPShowExpirationTime();
        updateStateColor(commandStateViewsMap.get(COMMANDS.SHOW_EXPIRATION_TIME), showExpirationTime);
    }

    private void setupAddNewChronoTimerToList() {
        addNewChronoTimerToList = getSHPaddNewChronoTimerToList();
        updateStateColor(commandStateViewsMap.get(COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST), addNewChronoTimerToList);
    }

    private void setupKeepScreen() {
        keepScreen = getSHPKeepScreen();
        setScreen(keepScreen);
    }

    private void setupButtonColors() {
        final String NEW_CHRONO_TIMER_UNPRESSED_COLOR_DEFAULT = "668CFF";
        final String NEW_CHRONO_TIMER_PRESSED_COLOR_DEFAULT = "0040FF";

        for (final COMMANDS command : COMMANDS.values()) {
            String unpressedColor = BUTTON_STATES.UNPRESSED.DEFAULT_COLOR();
            String pressedColor = BUTTON_STATES.PRESSED.DEFAULT_COLOR();
            if (command.equals(COMMANDS.NEW_CHRONO) || (command.equals(COMMANDS.NEW_TIMER))) {
                unpressedColor = NEW_CHRONO_TIMER_UNPRESSED_COLOR_DEFAULT;
                pressedColor = NEW_CHRONO_TIMER_PRESSED_COLOR_DEFAULT;
            }
            int index = command.ordinal();
            buttons[index].setUnpressedColor(unpressedColor);
            buttons[index].setPressedColor(pressedColor);
            updateButtonColor(command);
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

    private void setupButtons() {
        final String BUTTON_COMMAND_XML_PREFIX = "BTN_";

        buttons = new CustomImageButton[COMMANDS.values().length];
        Class rid = R.id.class;
        for (COMMANDS command : COMMANDS.values())
            try {
                int index = command.ordinal();
                buttons[index] = (CustomImageButton) findViewById(rid.getField(BUTTON_COMMAND_XML_PREFIX + command.toString()).getInt(rid));
                buttons[index].setImageResource(command.ID());
                final COMMANDS fcommand = command;
                buttons[index].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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

    private void setupStates() {
        final String BUTTON_STATE_XML_PREFIX = "STATE_";

        Class rid = R.id.class;
        commandStateViewsMap = new EnumMap<COMMANDS, StateView>(COMMANDS.class);
        for (COMMANDS command : COMMANDS.values())
            try {
                if ((command.equals(COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST)) || (command.equals(COMMANDS.SHOW_EXPIRATION_TIME))) {
                    StateView stateView = (StateView) findViewById(rid.getField(BUTTON_STATE_XML_PREFIX + command.toString()).getInt(rid));
                    commandStateViewsMap.put(command, stateView);
                }
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

    private void setupCtRecordsHandler() {
        ctRecordsHandler = new CtRecordsHandler(this, stringShelfDatabase);
    }

    private void setupMainCtList() {
        mainCtListItemAdapter = new MainCtListItemAdapter(this, stringShelfDatabase);
        mainCtListItemAdapter.setShowExpirationTime(showExpirationTime);
        mainCtListItemAdapter.setOnItemButtonClick(new MainCtListItemAdapter.onButtonClickListener() {
            @Override
            public void onButtonClick() {
                onCtListItemButtonClick();
            }
        });
        mainCtListView.setAdapter(mainCtListItemAdapter);
    }

    private void setupMainCtListRobot() {
        mainCtListRobot = new MainCtListRobot(mainCtListView, ctRecordsHandler);
        mainCtListRobot.setUpdateInterval(UPDATE_MAIN_CTLIST_TIME_INTERVAL_MS);
        mainCtListRobot.setOnExpiredTimersListener(new MainCtListRobot.onExpiredTimersListener() {
            @Override
            public void onExpiredTimers() {
                onCtListExpiredTimers();
            }
        });
    }

    private void setupStringShelfDatabase() {
        stringShelfDatabase = new StringShelfDatabase(this);
        stringShelfDatabase.open();
        setupStringShelfDatabaseTableActivityInfos(stringShelfDatabase);
        setupStringShelfDatabaseTableColors(stringShelfDatabase);
        setupStringShelfDatabaseTableChronoTimers(stringShelfDatabase);
        setupStringShelfDatabaseTablePresetsCT(stringShelfDatabase);
    }

    private void setupStringShelfDatabaseTableActivityInfos(StringShelfDatabase stringShelfDatabase) {
        if (!stringShelfDatabase.tableExists(PEKISLIB_TABLES.ACTIVITY_INFOS.toString())) {
            stringShelfDatabase.createTable(PEKISLIB_TABLES.ACTIVITY_INFOS.toString(), 1 + TABLE_ACTIVITY_INFOS_DATA_FIELDS.values().length);   //  Champ ID + Données
        }
    }

    private void setupStringShelfDatabaseTableColors(StringShelfDatabase stringShelfDatabase) {
        final String[][] TABLE_COLOR_TIME_BUTTONS_INITS = {
                {TABLE_IDS.LABEL.toString(), TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS.ON.LABEL(), TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS.OFF.LABEL(), TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT, TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString() + COLOR_ITEMS.TIME.toString(), "999900", "303030", "000000"},
                {TABLE_IDS.DEFAULT.toString() + COLOR_ITEMS.BUTTONS.toString(), "0061F3", "696969", "000000"}};

        final String[][] TABLE_COLOR_BACK_SCREEN_INITS = {
                {TABLE_IDS.LABEL.toString(), TABLE_COLORS_BACK_SCREEN_DATA_FIELDS.BACK.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.HEX.toString()},
                {TABLE_IDS.REGEXP.toString(), TABLE_COLORS_REGEXP_HEX_DEFAULT},
                {TABLE_IDS.DEFAULT.toString() + COLOR_ITEMS.BACK_SCREEN.toString(), "000000"}};

        if (!stringShelfDatabase.tableExists(SWTIMER_TABLES.COLORS_TIMEBUTTONS.toString())) {
            stringShelfDatabase.createTable(SWTIMER_TABLES.COLORS_TIMEBUTTONS.toString(), 1 + TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS.values().length);   //  Champ ID + Données;
            stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.COLORS_TIMEBUTTONS.toString(), TABLE_COLOR_TIME_BUTTONS_INITS);
            setCurrentColorsForCtDisplay(COLOR_ITEMS.TIME, getDefaultColors(COLOR_ITEMS.TIME));
            setCurrentColorsForCtDisplay(COLOR_ITEMS.BUTTONS, getDefaultColors(COLOR_ITEMS.BUTTONS));
        }
        if (!stringShelfDatabase.tableExists(SWTIMER_TABLES.COLORS_BACK_SCREEN.toString())) {
            stringShelfDatabase.createTable(SWTIMER_TABLES.COLORS_BACK_SCREEN.toString(), 1 + TABLE_COLORS_BACK_SCREEN_DATA_FIELDS.values().length);   //  Champ ID + Données;
            stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.COLORS_BACK_SCREEN.toString(), TABLE_COLOR_BACK_SCREEN_INITS);
            setCurrentColorsForCtDisplay(COLOR_ITEMS.BACK_SCREEN, getDefaultColors(COLOR_ITEMS.BACK_SCREEN));
        }
    }

    private void setupStringShelfDatabaseTableChronoTimers(StringShelfDatabase stringShelfDatabase) {
        if (!stringShelfDatabase.tableExists(SWTIMER_TABLES.CHRONO_TIMERS.toString())) {
            stringShelfDatabase.createTable(SWTIMER_TABLES.CHRONO_TIMERS.toString(), 1 + TABLE_CHRONO_TIMERS_DATA_FIELDS.values().length);   //  Champ ID + Données
        }
    }

    private void setupStringShelfDatabaseTablePresetsCT(StringShelfDatabase stringShelfDatabase) {
        final String[][] TABLE_PRESETS_CT_INITS = {
                {TABLE_IDS.LABEL.toString(), TABLE_PRESETS_CT_DATA_FIELDS.TIME.LABEL(), TABLE_PRESETS_CT_DATA_FIELDS.MESSAGE.LABEL()},
                {TABLE_IDS.KEYBOARD.toString(), InputButtonsActivity.KEYBOARDS.TIME_XHMS.toString(), InputButtonsActivity.KEYBOARDS.ALPHANUM.toString()},
                {TABLE_IDS.REGEXP.toString(), "^([0-9]+(" + TimeDateUtils.TIMEUNITS.HOUR.SYMBOL() + "|$))?([0-9]+(" + TimeDateUtils.TIMEUNITS.MIN.SYMBOL() + "|$))?([0-9]+(" + TimeDateUtils.TIMEUNITS.SEC.SYMBOL() + "|$))?([0-9]+(" + TimeDateUtils.TIMEUNITS.CS.SYMBOL() + "|$))?$", null},
                {TABLE_IDS.MAX.toString(), String.valueOf(convertXhmsToMs("23" + TimeDateUtils.TIMEUNITS.HOUR.SYMBOL() + "59" + TimeDateUtils.TIMEUNITS.MIN.SYMBOL() + "59" + TimeDateUtils.TIMEUNITS.SEC.SYMBOL() + "99" + TimeDateUtils.TIMEUNITS.CS.SYMBOL())), null},    //  23h59m59s99c
                {TABLE_IDS.TIMEUNIT.toString(), TimeDateUtils.TIMEUNITS.CS.toString(), null}};

        if (!stringShelfDatabase.tableExists(SWTIMER_TABLES.PRESETS_CT.toString())) {
            stringShelfDatabase.createTable(SWTIMER_TABLES.PRESETS_CT.toString(), 1 + TABLE_PRESETS_CT_DATA_FIELDS.values().length);   //  Champ ID + Données
            stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.PRESETS_CT.toString(), TABLE_PRESETS_CT_INITS);
        }
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpmainactivity);
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.HELP.ordinal());
    }

    private void launchCtDisplayActivity(int idct) {
        setColdStartForCtDisplay();
        Intent callingIntent = new Intent(this, CtDisplayActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), idct);
        startActivityForResult(callingIntent, SWTIMER_ACTIVITIES.CTDISPLAY.ordinal());
    }

    private void setColdStartForCtDisplay() {
        stringShelfDatabase.insertOrReplaceFieldById(PEKISLIB_TABLES.ACTIVITY_INFOS.toString(), SWTIMER_ACTIVITIES.CTDISPLAY.toString(), TABLE_ACTIVITY_INFOS_DATA_FIELDS.START_TYPE.INDEX(), ACTIVITY_START_TYPE.COLD.toString());
    }

    private String[] getDefaultColors(COLOR_ITEMS colorItem) {
        return stringShelfDatabase.selectRowByIdOrCreate(colorItem.TABLE_NAME(), TABLE_IDS.DEFAULT.toString() + colorItem.toString());
    }

    private void setCurrentColorsForCtDisplay(COLOR_ITEMS colorItem, String[] values) {
        stringShelfDatabase.insertOrReplaceRowById(colorItem.TABLE_NAME(), TABLE_IDS.CURRENT.toString() + colorItem.toString() + SWTIMER_ACTIVITIES.CTDISPLAY.toString(), values);
    }

    private boolean returnsFromCtDisplay() {
        return (calledActivity.equals(SWTIMER_ACTIVITIES.CTDISPLAY.toString()));
    }

    private boolean returnsFromHelp() {
        return (calledActivity.equals(PEKISLIB_ACTIVITIES.HELP.toString()));
    }

}