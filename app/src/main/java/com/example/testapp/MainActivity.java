package com.example.testapp;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.database.SmsMessage;
import com.example.testapp.database.AppDatabase;
import com.example.testapp.backup.SmsBackupManager;
import com.example.testapp.background.BackgroundExecutionManager;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private static final String STATE_SELECTED_SECTION = "selected_section";
    private static final String STATE_STATISTICS_YEAR = "statistics_year";
    private static final String STATE_STATISTICS_MONTH = "statistics_month";
    private static final String STATE_MESSAGE_FILTER = "message_filter";
    private static final String STATE_CUSTOM_FILTER_DATE = "custom_filter_date";
    private static final int SECTION_MESSAGES = 0;
    private static final int SECTION_HOME = 1;
    private static final int SECTION_STATISTICS = 2;
    private static final String INSTALLATION_PREFERENCES = "installation_restore";
    private static final String INSTALLATION_STATE = "state";
    private static final String STATE_CONFIGURED = "configured";
    private static final String STATE_RESTORE_PENDING = "restore_pending";
    private static final String BACKGROUND_PREFERENCES = "background_execution";
    private static final String BACKGROUND_PROMPT_SHOWN = "initial_prompt_shown";
    private static final String DEPOSIT_USSD_CODE = "#111*1*6#";
    private TextView permissionText;
    private TextView backgroundExecutionText;
    private TextView emptyText;
    private ProgressBar loadingIndicator;
    private SmsAdapter adapter;
    private SmsViewModel viewModel;
    private SmsBackupManager backupManager;
    private List<SmsMessage> messages = new ArrayList<>();
    private boolean roomLoaded;
    private boolean startupRestoreFinished;
    private BackgroundExecutionManager backgroundExecutionManager;
    private boolean backgroundSettingsOpened;
    private boolean firstResume = true;
    private View messagesSection;
    private View homeSection;
    private View statisticsSection;
    private TextView messagesNavigationItem;
    private TextView statisticsNavigationItem;
    private TextView statisticsEmptyText;
    private StatisticsChartView statisticsChart;
    private TextView statisticsMonthText;
    private Calendar statisticsMonth;
    private TextView homeLabel;
    private ImageButton homeButton;
    private int selectedSection = SECTION_MESSAGES;
    private SmsDateFilter.Period selectedMessageFilter = SmsDateFilter.Period.ALL;
    private Long customFilterDate;
    private TextView[] filterChips;
    private View depositCard;
    private boolean depositRequestInProgress;
    private boolean launchDepositAfterPermission;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) exportMessages(uri);
            });
    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importMessages(uri);
            });

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                refreshPermissionState();
                if (!hasSmsPermissions()) {
                    Toast.makeText(this,
                            "Capture SMS inactive : accordez l’autorisation SMS dans les réglages.",
                            Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> phonePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!launchDepositAfterPermission) return;
                launchDepositAfterPermission = false;
                if (granted) {
                    launchDepositWithCallIntent();
                } else {
                    finishDepositRequest();
                    Toast.makeText(this,
                            "L’autorisation Téléphone est nécessaire pour lancer le service USSD.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        applySystemBarInsets(findViewById(R.id.mainRoot));

        permissionText = findViewById(R.id.permissionText);
        permissionText.setOnClickListener(view -> requestRequiredPermissions());
        backgroundExecutionText = findViewById(R.id.backgroundExecutionText);
        backgroundExecutionManager = new BackgroundExecutionManager(this);
        backgroundExecutionText.setOnClickListener(view -> showBackgroundPermissionDialog(false));
        emptyText = findViewById(R.id.emptyText);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        configureMessageFilters(savedInstanceState);
        backupManager = new SmsBackupManager(this);
        findViewById(R.id.overflowButton).setOnClickListener(this::showOverflowMenu);
        messagesSection = findViewById(R.id.messagesSection);
        homeSection = findViewById(R.id.homeSection);
        statisticsSection = findViewById(R.id.statisticsSection);
        messagesNavigationItem = findViewById(R.id.bottomMessages);
        statisticsNavigationItem = findViewById(R.id.bottomStatistics);
        depositCard = findViewById(R.id.cardDeposit);
        depositCard.setOnClickListener(view -> launchDeposit());
        statisticsEmptyText = findViewById(R.id.statisticsEmptyText);
        statisticsChart = findViewById(R.id.statisticsChart);
        statisticsMonthText = findViewById(R.id.statisticsMonthText);
        statisticsMonth = Calendar.getInstance();
        statisticsMonth.set(Calendar.DAY_OF_MONTH, 1);
        if (savedInstanceState != null) {
            statisticsMonth.set(Calendar.YEAR, savedInstanceState.getInt(STATE_STATISTICS_YEAR,
                    statisticsMonth.get(Calendar.YEAR)));
            statisticsMonth.set(Calendar.MONTH, savedInstanceState.getInt(STATE_STATISTICS_MONTH,
                    statisticsMonth.get(Calendar.MONTH)));
        }
        findViewById(R.id.statisticsPreviousMonth).setOnClickListener(view -> changeMonth(-1));
        findViewById(R.id.statisticsNextMonth).setOnClickListener(view -> changeMonth(1));
        homeLabel = findViewById(R.id.homeLabel);
        homeButton = findViewById(R.id.homeButton);
        messagesNavigationItem.setOnClickListener(view -> showSection(SECTION_MESSAGES));
        findViewById(R.id.bottomImport).setOnClickListener(view -> launchImport());
        homeButton.setOnClickListener(view -> showSection(SECTION_HOME));
        findViewById(R.id.bottomExport).setOnClickListener(view -> launchExport());
        statisticsNavigationItem.setOnClickListener(view -> showSection(SECTION_STATISTICS));
        showSection(savedInstanceState == null ? SECTION_MESSAGES
                : savedInstanceState.getInt(STATE_SELECTED_SECTION, SECTION_MESSAGES));
        RecyclerView list = findViewById(R.id.transactionsList);
        adapter = new SmsAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(SmsViewModel.class);
        viewModel.getMessages().observe(this, storedMessages -> {
            messages = storedMessages == null ? new ArrayList<>() : storedMessages;
            roomLoaded = true;
            renderState();
            if (selectedSection == SECTION_STATISTICS) renderStatistics();
        });
        requestRequiredPermissions();
        initializeInstallation();
        boolean initialPromptShown = getSharedPreferences(BACKGROUND_PREFERENCES, MODE_PRIVATE)
                .getBoolean(BACKGROUND_PROMPT_SHOWN, false);
        if (savedInstanceState == null && !initialPromptShown
                && !backgroundExecutionManager.isBackgroundExecutionAllowed()) {
            getSharedPreferences(BACKGROUND_PREFERENCES, MODE_PRIVATE).edit()
                    .putBoolean(BACKGROUND_PROMPT_SHOWN, true).apply();
            showBackgroundPermissionDialog(true);
        }
    }

    private void launchDeposit() {
        if (depositRequestInProgress) return;
        depositRequestInProgress = true;
        depositCard.setEnabled(false);
        if (hasPermission(Manifest.permission.CALL_PHONE)) {
            launchDepositWithCallIntent();
        } else {
            launchDepositAfterPermission = true;
            phonePermissionLauncher.launch(Manifest.permission.CALL_PHONE);
        }
    }

    private void launchDepositWithCallIntent() {
        Intent callIntent = new Intent(Intent.ACTION_CALL,
                Uri.fromParts("tel", DEPOSIT_USSD_CODE, null));
        try {
            startActivity(callIntent);
        } catch (SecurityException | android.content.ActivityNotFoundException error) {
            Toast.makeText(this, "Impossible de lancer le service USSD sur cet appareil.",
                    Toast.LENGTH_LONG).show();
        } finally {
            finishDepositRequest();
        }
    }

    private void finishDepositRequest() {
        depositRequestInProgress = false;
        if (depositCard != null) depositCard.setEnabled(true);
    }

    private void applySystemBarInsets(View root) {
        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(initialLeft + systemBars.left, initialTop + systemBars.top,
                    initialRight + systemBars.right, initialBottom + systemBars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    /** Switches the content in place; transfer actions and the fixed bottom bar stay untouched. */
    private void showSection(int section) {
        selectedSection = section;
        boolean messagesSelected = section == SECTION_MESSAGES;
        boolean homeSelected = section == SECTION_HOME;
        boolean statisticsSelected = section == SECTION_STATISTICS;
        messagesSection.setVisibility(messagesSelected ? View.VISIBLE : View.GONE);
        homeSection.setVisibility(homeSelected ? View.VISIBLE : View.GONE);
        statisticsSection.setVisibility(statisticsSelected ? View.VISIBLE : View.GONE);

        int active = ContextCompat.getColor(this, R.color.sms_bottom_item_active);
        int inactive = ContextCompat.getColor(this, R.color.sms_bottom_item);
        messagesNavigationItem.setTextColor(messagesSelected ? active : inactive);
        messagesNavigationItem.setCompoundDrawableTintList(
                ColorStateList.valueOf(messagesSelected ? active : inactive));
        statisticsNavigationItem.setTextColor(statisticsSelected ? active : inactive);
        statisticsNavigationItem.setCompoundDrawableTintList(
                ColorStateList.valueOf(statisticsSelected ? active : inactive));
        homeLabel.setTextColor(homeSelected ? active : inactive);
        homeButton.setImageTintList(ColorStateList.valueOf(homeSelected
                ? ContextCompat.getColor(this, R.color.white) : inactive));
        homeButton.setBackgroundResource(homeSelected
                ? R.drawable.bg_refresh_button : R.drawable.bg_home_button_inactive);
        homeButton.setSelected(homeSelected);
        messagesNavigationItem.setSelected(messagesSelected);
        statisticsNavigationItem.setSelected(statisticsSelected);
        if (statisticsSelected) renderStatistics();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_SECTION, selectedSection);
        outState.putInt(STATE_STATISTICS_YEAR, statisticsMonth.get(Calendar.YEAR));
        outState.putInt(STATE_STATISTICS_MONTH, statisticsMonth.get(Calendar.MONTH));
        outState.putString(STATE_MESSAGE_FILTER, selectedMessageFilter.name());
        if (customFilterDate != null) {
            outState.putLong(STATE_CUSTOM_FILTER_DATE, customFilterDate);
        }
        super.onSaveInstanceState(outState);
    }

    private void configureMessageFilters(Bundle savedInstanceState) {
        filterChips = new TextView[]{findViewById(R.id.filterAll),
                findViewById(R.id.filterToday), findViewById(R.id.filterYesterday),
                findViewById(R.id.filterSevenDays), findViewById(R.id.filterThirtyDays),
                findViewById(R.id.filterDate)};
        if (savedInstanceState != null) {
            String savedFilter = savedInstanceState.getString(STATE_MESSAGE_FILTER);
            try {
                if (savedFilter != null) selectedMessageFilter = SmsDateFilter.Period.valueOf(savedFilter);
            } catch (IllegalArgumentException ignored) {
                selectedMessageFilter = SmsDateFilter.Period.ALL;
            }
            if (savedInstanceState.containsKey(STATE_CUSTOM_FILTER_DATE)) {
                customFilterDate = savedInstanceState.getLong(STATE_CUSTOM_FILTER_DATE);
            }
        }
        filterChips[0].setOnClickListener(view -> selectMessageFilter(SmsDateFilter.Period.ALL));
        filterChips[1].setOnClickListener(view -> selectMessageFilter(SmsDateFilter.Period.TODAY));
        filterChips[2].setOnClickListener(view -> selectMessageFilter(SmsDateFilter.Period.YESTERDAY));
        filterChips[3].setOnClickListener(view -> selectMessageFilter(SmsDateFilter.Period.SEVEN_DAYS));
        filterChips[4].setOnClickListener(view -> selectMessageFilter(SmsDateFilter.Period.THIRTY_DAYS));
        filterChips[5].setOnClickListener(view -> showDateFilterPicker());
        updateFilterChips();
    }

    private void selectMessageFilter(SmsDateFilter.Period period) {
        selectedMessageFilter = period;
        if (period != SmsDateFilter.Period.CUSTOM_DATE) customFilterDate = null;
        updateFilterChips();
        renderState();
    }

    private void showDateFilterPicker() {
        Calendar initial = Calendar.getInstance();
        if (customFilterDate != null) initial.setTimeInMillis(customFilterDate);
        new DatePickerDialog(this, (picker, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.clear();
            selected.set(year, month, day);
            customFilterDate = selected.getTimeInMillis();
            selectedMessageFilter = SmsDateFilter.Period.CUSTOM_DATE;
            updateFilterChips();
            renderState();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateFilterChips() {
        if (filterChips == null) return;
        SmsDateFilter.Period[] periods = SmsDateFilter.Period.values();
        for (int index = 0; index < filterChips.length; index++) {
            filterChips[index].setSelected(selectedMessageFilter == periods[index]);
        }
        filterChips[5].setText(customFilterDate == null ? "Date"
                : new SimpleDateFormat("dd/MM/yy", Locale.FRENCH).format(customFilterDate));
    }

    private void renderStatistics() {
        String monthLabel = new SimpleDateFormat("MMMM yyyy", Locale.FRENCH)
                .format(statisticsMonth.getTime());
        statisticsMonthText.setText(monthLabel.substring(0, 1).toUpperCase(Locale.FRENCH)
                + monthLabel.substring(1));
        List<SmsStatistics.DailyCount> dailyCounts = SmsStatistics.forMonth(messages,
                statisticsMonth.get(Calendar.YEAR), statisticsMonth.get(Calendar.MONTH),
                statisticsMonth.getTimeZone());
        boolean empty = !SmsStatistics.hasActivity(dailyCounts);
        statisticsEmptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        statisticsChart.setVisibility(empty ? View.GONE : View.VISIBLE);
        Calendar today = Calendar.getInstance();
        boolean currentMonth = today.get(Calendar.YEAR) == statisticsMonth.get(Calendar.YEAR)
                && today.get(Calendar.MONTH) == statisticsMonth.get(Calendar.MONTH);
        statisticsChart.setData(dailyCounts,
                currentMonth ? Math.max(0, today.get(Calendar.DAY_OF_MONTH) - 4) : 0);
    }

    private void changeMonth(int offset) {
        statisticsMonth.add(Calendar.MONTH, offset);
        renderStatistics();
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Importer les messages").setOnMenuItemClickListener(item -> {
            launchImport();
            return true;
        });
        menu.getMenu().add("Exporter les messages").setOnMenuItemClickListener(item -> {
            launchExport();
            return true;
        });
        menu.show();
    }

    private void launchImport() {
        importLauncher.launch(new String[]{"application/json", "text/json", "text/plain"});
    }

    private void launchExport() {
        String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ROOT).format(new Date());
        exportLauncher.launch("suivi-sms_" + date + ".json");
    }

    private void refreshMessages(boolean announce) {
        viewModel.refresh(latest -> runOnUiThread(() -> {
            messages = latest == null ? new ArrayList<>() : latest;
            roomLoaded = true;
            renderState();
            if (announce) {
                Toast.makeText(this, "Liste actualisée", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void exportMessages(Uri destination) {
        List<SmsMessage> snapshot = new ArrayList<>(messages);
        new Thread(() -> {
            try (OutputStream stream = getContentResolver().openOutputStream(destination);
                 OutputStreamWriter writer = stream == null ? null
                         : new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
                if (writer == null) throw new IOException("Impossible d’ouvrir le fichier");
                JSONArray entries = new JSONArray();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.FRENCH);
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.FRENCH);
                for (SmsMessage sms : snapshot) {
                    JSONObject entry = new JSONObject();
                    entry.put("numero", sms.sender);
                    entry.put("message", sms.messageBody);
                    entry.put("date", dateFormat.format(sms.receivedDate));
                    entry.put("heure", timeFormat.format(sms.receivedDate));
                    entry.put("horodatage", sms.receivedDate);
                    if (sms.id > 0) entry.put("identifiant", sms.id);
                    entries.put(entry);
                }
                JSONObject root = new JSONObject().put("messages", entries);
                writer.write(root.toString(2));
                runOnUiThread(() -> Toast.makeText(this,
                        snapshot.size() + " messages exportés", Toast.LENGTH_LONG).show());
            } catch (IOException | JSONException error) {
                showTransferError("Export impossible", error);
            }
        }, "sms-json-export").start();
    }

    private void importMessages(Uri source) {
        new Thread(() -> {
            try {
                List<SmsMessage> imported = parseImport(source);
                List<Long> results = AppDatabase.getInstance(this).smsDao().insertAll(imported);
                int inserted = 0;
                for (Long result : results) if (result != null && result != -1L) inserted++;
                int duplicates = imported.size() - inserted;
                int finalInserted = inserted;
                runOnUiThread(() -> Toast.makeText(this, finalInserted + " messages importés"
                                + (duplicates > 0 ? " • " + duplicates + " doublons ignorés" : ""),
                        Toast.LENGTH_LONG).show());
            } catch (IOException | JSONException | ParseException error) {
                showTransferError("Import impossible : fichier JSON invalide", error);
            }
        }, "sms-json-import").start();
    }

    private List<SmsMessage> parseImport(Uri source)
            throws IOException, JSONException, ParseException {
        StringBuilder json = new StringBuilder();
        try (InputStream stream = getContentResolver().openInputStream(source);
             BufferedReader reader = stream == null ? null : new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            if (reader == null) throw new IOException("Impossible d’ouvrir le fichier");
            String line;
            while ((line = reader.readLine()) != null) json.append(line).append('\n');
        }
        JSONObject root = new JSONObject(json.toString());
        JSONArray entries = root.getJSONArray("messages");
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.FRENCH);
        format.setLenient(false);
        List<SmsMessage> imported = new ArrayList<>();
        for (int index = 0; index < entries.length(); index++) {
            JSONObject entry = entries.getJSONObject(index);
            String sender = entry.getString("numero");
            String body = entry.getString("message");
            long timestamp;
            if (entry.has("horodatage")) {
                timestamp = entry.getLong("horodatage");
                if (timestamp <= 0) throw new JSONException("Horodatage invalide à l’index " + index);
            } else {
                Date received = format.parse(entry.getString("date") + " " + entry.getString("heure"));
                if (received == null) throw new ParseException("Date absente", index);
                timestamp = received.getTime();
            }
            imported.add(SmsMessage.create(sender, body, timestamp, true));
        }
        return imported;
    }

    private void showTransferError(String prefix, Exception error) {
        runOnUiThread(() -> Toast.makeText(this, prefix + " : " + error.getMessage(),
                Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionState();
        if (firstResume) {
            firstResume = false;
            return;
        }
        if (backgroundSettingsOpened) {
            backgroundSettingsOpened = false;
            boolean allowed = backgroundExecutionManager.isBackgroundExecutionAllowed();
            Toast.makeText(this, allowed ? "Capture SMS active"
                    : "L’exécution en arrière-plan reste limitée. Appuyez sur le rappel pour réessayer.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showBackgroundPermissionDialog(boolean firstLaunchCheck) {
        if (isFinishing() || backgroundExecutionManager.isBackgroundExecutionAllowed()) return;
        new AlertDialog.Builder(this)
                .setTitle("Activer la capture en arrière-plan")
                .setMessage("Pour capturer les SMS reçus même lorsque l'application est fermée, "
                        + "veuillez autoriser l'exécution en arrière-plan.\n\nDans la page suivante, "
                        + "activez si disponibles « Démarrage automatique » et « Exécution en arrière-plan ». ")
                .setNegativeButton(firstLaunchCheck ? "Plus tard" : "Annuler", null)
                .setPositiveButton("Autoriser maintenant", (dialog, which) -> {
                    backgroundSettingsOpened = backgroundExecutionManager.openBackgroundSettings();
                    if (!backgroundSettingsOpened) {
                        Toast.makeText(this, "Impossible d’ouvrir les réglages sur cet appareil.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void requestRequiredPermissions() {
        List<String> missing = new ArrayList<>();
        if (!hasPermission(Manifest.permission.RECEIVE_SMS)) missing.add(Manifest.permission.RECEIVE_SMS);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            missing.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (missing.isEmpty()) refreshPermissionState();
        else permissionLauncher.launch(missing.toArray(new String[0]));
    }

    private void initializeInstallation() {
        String state = getSharedPreferences(INSTALLATION_PREFERENCES, MODE_PRIVATE)
                .getString(INSTALLATION_STATE, null);
        if (STATE_CONFIGURED.equals(state)) {
            finishStartupRestore();
            return;
        }
        backupManager.findBackup((date, lookupError) -> runOnUiThread(() -> {
            if (lookupError != null) {
                Toast.makeText(this, "Sauvegarde inaccessible. Démarrage sans restauration.",
                        Toast.LENGTH_LONG).show();
                markConfigured();
                finishStartupRestore();
            } else if (date == null) {
                markConfigured();
                finishStartupRestore();
            } else {
                getSharedPreferences(INSTALLATION_PREFERENCES, MODE_PRIVATE).edit()
                        .putString(INSTALLATION_STATE, STATE_RESTORE_PENDING).apply();
                attemptAutomaticRestore();
            }
        }));
    }

    private void attemptAutomaticRestore() {
        backupManager.restoreAutomatically((result, error) -> runOnUiThread(() -> {
            if (error != null) {
                Toast.makeText(this, "Restauration automatique impossible : " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                finishStartupRestore();
            } else if (result.status == SmsBackupManager.AutomaticRestoreStatus.RESTORED) {
                markConfigured();
                finishStartupRestore();
                Toast.makeText(this, "Restauration réussie : " + result.restoredCount + " SMS restaurés",
                        Toast.LENGTH_LONG).show();
            } else if (result.status == SmsBackupManager.AutomaticRestoreStatus.PASSWORD_REQUIRED) {
                finishStartupRestore();
                requestPassword(true, () -> Toast.makeText(this,
                        "La restauration automatique pourra être réessayée au prochain démarrage.",
                        Toast.LENGTH_LONG).show());
            } else {
                markConfigured();
                finishStartupRestore();
            }
        }));
    }

    private void finishStartupRestore() {
        startupRestoreFinished = true;
        refreshPermissionState();
    }

    private void markConfigured() {
        getSharedPreferences(INSTALLATION_PREFERENCES, MODE_PRIVATE).edit()
                .putString(INSTALLATION_STATE, STATE_CONFIGURED).apply();
    }

    private void requestPassword(boolean restore, Runnable onCancel) {
        EditText input = new EditText(this);
        input.setHint("8 caractères minimum");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, 0, padding, 0);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(restore ? "Mot de passe de sauvegarde" : "Protéger la sauvegarde")
                .setMessage(restore
                        ? "Saisissez le mot de passe utilisé lors de la sauvegarde."
                        : "Ce mot de passe sera requis après une réinstallation. Il ne peut pas être récupéré.")
                .setView(input).setNegativeButton("Annuler", (ignored, which) -> {
                    if (onCancel != null) onCancel.run();
                })
                .setPositiveButton(restore ? "Restaurer" : "Sauvegarder", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            char[] password = input.getText().toString().toCharArray();
            if (password.length < 8) { input.setError("8 caractères minimum"); return; }
            dialog.dismiss();
            if (restore) restore(password); else backup(password);
            java.util.Arrays.fill(password, '\0');
        }));
        dialog.show();
    }

    /** Checks shared storage first so an unnecessary password dialog is never displayed. */
    private void requestRestore() {
        backupManager.findBackup((date, error) -> runOnUiThread(() -> {
            if (error != null) {
                Toast.makeText(this, "Restauration impossible : " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            } else if (date == null) {
                Toast.makeText(this, "Aucune sauvegarde trouvée", Toast.LENGTH_LONG).show();
            } else {
                requestPassword(true, null);
            }
        }));
    }

    private void backup(char[] password) {
        backupManager.backup(password, true, (date, error) -> runOnUiThread(() -> {
            Toast.makeText(this, error == null ? "Sauvegarde créée avec succès"
                    : "Échec : " + error.getMessage(), Toast.LENGTH_LONG).show();
        }));
    }

    private void restore(char[] password) {
        backupManager.restore(password, (count, error) -> runOnUiThread(() -> {
            Toast.makeText(this, error == null ? "Restauration réussie : " + count + " SMS restaurés"
                    : "Restauration impossible : " + error.getMessage(), Toast.LENGTH_LONG).show();
            if (error == null) {
                markConfigured();
            }
        }));
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasSmsPermissions() {
        return hasPermission(Manifest.permission.RECEIVE_SMS);
    }

    private void refreshPermissionState() {
        renderState();
    }

    private void renderState() {
        boolean denied = !hasSmsPermissions();
        boolean loading = !denied && (!roomLoaded || !startupRestoreFinished);
        permissionText.setVisibility(denied ? View.VISIBLE : View.GONE);
        boolean backgroundAllowed = backgroundExecutionManager != null
                && backgroundExecutionManager.isBackgroundExecutionAllowed();
        backgroundExecutionText.setVisibility(backgroundAllowed ? View.GONE : View.VISIBLE);
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        List<SmsDateFilter.DisplayMessage> displayed = denied ? new ArrayList<>()
                : SmsDateFilter.apply(messages, selectedMessageFilter, customFilterDate,
                System.currentTimeMillis(), java.util.TimeZone.getDefault());
        adapter.submitList(displayed);
        boolean noDisplayedMessages = displayed.isEmpty();
        emptyText.setText(selectedMessageFilter == SmsDateFilter.Period.ALL
                ? "Aucun message enregistré" : "Aucun message pour cette période");
        emptyText.setVisibility(!denied && !loading && noDisplayedMessages
                ? View.VISIBLE : View.GONE);
    }

    private static class SmsAdapter extends RecyclerView.Adapter<SmsViewHolder> {
        private List<SmsDateFilter.DisplayMessage> items = new ArrayList<>();

        void submitList(List<SmsDateFilter.DisplayMessage> messages) {
            items = new ArrayList<>(messages);
            notifyDataSetChanged();
        }

        @Override
        public SmsViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new SmsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SmsViewHolder holder, int position) {
            SmsDateFilter.DisplayMessage displayed = items.get(position);
            SmsMessage item = displayed.message;
            holder.number.setText(String.valueOf(displayed.originalNumber));
            holder.sender.setText(SmsDisplayFormatter.sender(item.sender));
            holder.date.setText(new SimpleDateFormat("dd/MM/yy", Locale.FRENCH)
                    .format(item.receivedDate));
            holder.time.setText(new SimpleDateFormat("HH:mm", Locale.FRENCH)
                    .format(item.receivedDate));
            holder.body.setText(item.messageBody);
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class SmsViewHolder extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView sender;
        final TextView date;
        final TextView time;
        final TextView body;

        SmsViewHolder(View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.itemNumber);
            sender = itemView.findViewById(R.id.itemSender);
            date = itemView.findViewById(R.id.itemDate);
            time = itemView.findViewById(R.id.itemTime);
            body = itemView.findViewById(R.id.itemBody);
        }
    }
}
