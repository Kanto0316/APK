package com.example.testapp;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Build;
import android.text.InputType;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

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
import com.example.testapp.history.HistoryTransaction;
import com.example.testapp.overlay.TransactionOverlayCoordinator;

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
    private static final String STATE_STATISTICS_TAB = "statistics_tab";
    private static final String STATE_MESSAGE_FILTER = "message_filter";
    private static final String STATE_MESSAGE_QUERY = "message_query";
    private static final String STATE_CUSTOM_FILTER_DATE = "custom_filter_date";
    private static final String STATE_CLIENT_SENDER = "client_sender";
    private static final String STATE_CLIENT_QUERY = "client_query";
    private static final String STATE_CLIENT_FILTER = "client_filter";
    private static final int SECTION_MESSAGES = 0;
    private static final int SECTION_HOME = 1;
    private static final int SECTION_STATISTICS = 2;
    private static final int SECTION_HISTORY = 3;
    private static final int SECTION_CLIENT = 4;
    private static final int STATISTICS_TAB_BONUS = 0;
    private static final int STATISTICS_TAB_USER = 1;
    private static final int STATISTICS_TAB_TRANSACTION = 2;
    private static final String INSTALLATION_PREFERENCES = "installation_restore";
    private static final String INSTALLATION_STATE = "state";
    private static final String STATE_CONFIGURED = "configured";
    private static final String STATE_RESTORE_PENDING = "restore_pending";
    private static final String BACKGROUND_PREFERENCES = "background_execution";
    private static final String BACKGROUND_PROMPT_SHOWN = "initial_prompt_shown";
    private static final String DEPOSIT_PREFERENCES = "deposit_preferences";
    private static final String LAST_DEPOSIT_RECIPIENT = "last_deposit_recipient";
    private TextView permissionText;
    private TextView backgroundExecutionText;
    private TextView emptyText;
    private TextView balanceTitle;
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
    private View historySection;
    private RecyclerView historyList;
    private View historyEmptyState;
    private HistoryAdapter historyAdapter;
    private View statisticsSection;
    private View clientSection;
    private View clientListContent;
    private View clientDetailContent;
    private TextView messagesNavigationItem;
    private TextView clientNavigationItem;
    private TextView historyNavigationItem;
    private TextView statisticsNavigationItem;
    private TextView statisticsSubtitle;
    private TextView statisticsMonthText;
    private Calendar statisticsMonth;
    private View userStatisticsContent;
    private View bonusStatisticsContent;
    private View transactionStatisticsContent;
    private TextView statisticsUserTab;
    private TextView statisticsBonusTab;
    private TextView statisticsTransactionTab;
    private TextView bonusMonthText;
    private TextView transactionMonthText;
    private TextView userMonthLabel;
    private TextView userYearLabel;
    private TextView bonusMonthLabel;
    private TextView bonusYearLabel;
    private TextView transactionMonthLabel;
    private TextView transactionYearLabel;
    private StatisticsChartView bonusChart;
    private TextView emptyBonusChartText;
    private StatisticsChartView userChart;
    private TextView emptyUserChartText;
    private StatisticsChartView transactionChart;
    private TextView emptyTransactionChartText;
    private int selectedStatisticsTab = STATISTICS_TAB_BONUS;
    private long todayUsers = 0;
    private long yesterdayUsers = 0;
    private long weekUsers = 0;
    private long monthUsers = 0;
    private long yearUsers = 0;
    private List<SmsStatistics.DailyCount> monthlyUserBars = new ArrayList<>();
    // Bonus totals and bars are refreshed together from parsed MVola transactions.
    private long todayBonus = 0;
    private long yesterdayBonus = 0;
    private long weekBonus = 0;
    private long monthBonus = 0;
    private long yearBonus = 0;
    private List<SmsStatistics.DailyCount> monthlyBonusBars = new ArrayList<>();
    // UI-only placeholders ready for the future transaction statistics layer.
    private long todayTransactions = 0;
    private long yesterdayTransactions = 0;
    private long weekTransactions = 0;
    private long monthlyTransactions = 0;
    private long yearlyTransactions = 0;
    private List<SmsStatistics.DailyCount> monthlyTransactionBars = new ArrayList<>();
    private long todayDepositTransactions = 0;
    private long todayCreditTransactions = 0;
    private long todayWithdrawalTransactions = 0;
    private TextView homeLabel;
    private ImageButton homeButton;
    private TextView clientEmptyText;
    private TextView clientDetailTitle;
    private ClientAdapter clientAdapter;
    private ClientMessageAdapter clientMessageAdapter;
    private String selectedClientSender;
    private EditText clientSearchInput;
    private TextView[] clientFilterChips;
    private ClientMessageGrouper.Filter selectedClientFilter = ClientMessageGrouper.Filter.ALL;
    private int selectedSection = SECTION_MESSAGES;
    private SmsDateFilter.Period selectedMessageFilter = SmsDateFilter.Period.ALL;
    private Long customFilterDate;
    private EditText messageSearchInput;
    private TextView[] filterChips;
    private View depositCard;
    private View creditCard;
    private boolean depositRequestInProgress;
    private boolean launchDepositAfterPermission;
    private boolean depositCallLaunched;
    private String pendingUssdCode;

    @Override
    protected void onStart() {
        super.onStart();
        TransactionOverlayCoordinator.get(this).attach(this);
    }

    @Override
    protected void onStop() {
        TransactionOverlayCoordinator.get(this).detach(this);
        super.onStop();
    }

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
                    launchUssdWithCallIntent(pendingUssdCode);
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
        balanceTitle = findViewById(R.id.balanceTitle);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        configureMessageFilters(savedInstanceState);
        backupManager = new SmsBackupManager(this);
        findViewById(R.id.overflowButton).setOnClickListener(this::showOverflowMenu);
        messagesSection = findViewById(R.id.messagesSection);
        homeSection = findViewById(R.id.homeSection);
        historySection = findViewById(R.id.historySection);
        statisticsSection = findViewById(R.id.statisticsSection);
        clientSection = findViewById(R.id.clientSection);
        clientListContent = findViewById(R.id.clientListContent);
        clientDetailContent = findViewById(R.id.clientDetailContent);
        messagesNavigationItem = findViewById(R.id.bottomMessages);
        clientNavigationItem = findViewById(R.id.bottomClient);
        historyNavigationItem = findViewById(R.id.bottomHistory);
        statisticsNavigationItem = findViewById(R.id.bottomStatistics);
        depositCard = findViewById(R.id.cardDeposit);
        depositCard.setOnClickListener(view -> showRecipientDialog("", ""));
        creditCard = findViewById(R.id.cardCredit);
        creditCard.setOnClickListener(view -> showCreditRecipientDialog("", ""));
        statisticsSubtitle = findViewById(R.id.statisticsSubtitle);
        statisticsMonthText = findViewById(R.id.statisticsMonthText);
        bonusMonthText = findViewById(R.id.bonusMonthText);
        transactionMonthText = findViewById(R.id.transactionMonthText);
        userMonthLabel = findViewById(R.id.userMonthLabel);
        userYearLabel = findViewById(R.id.userYearLabel);
        bonusMonthLabel = findViewById(R.id.bonusMonthLabel);
        bonusYearLabel = findViewById(R.id.bonusYearLabel);
        transactionMonthLabel = findViewById(R.id.transactionMonthLabel);
        transactionYearLabel = findViewById(R.id.transactionYearLabel);
        bonusChart = findViewById(R.id.bonusChart);
        emptyBonusChartText = findViewById(R.id.emptyBonusChartText);
        userChart = findViewById(R.id.userChart);
        emptyUserChartText = findViewById(R.id.emptyUserChartText);
        transactionChart = findViewById(R.id.transactionChart);
        emptyTransactionChartText = findViewById(R.id.emptyTransactionChartText);
        userStatisticsContent = findViewById(R.id.userStatisticsContent);
        bonusStatisticsContent = findViewById(R.id.bonusStatisticsContent);
        transactionStatisticsContent = findViewById(R.id.transactionStatisticsContent);
        statisticsUserTab = findViewById(R.id.statisticsUserTab);
        statisticsBonusTab = findViewById(R.id.statisticsBonusTab);
        statisticsTransactionTab = findViewById(R.id.statisticsTransactionTab);
        statisticsMonth = Calendar.getInstance();
        statisticsMonth.set(Calendar.DAY_OF_MONTH, 1);
        if (savedInstanceState != null) {
            statisticsMonth.set(Calendar.YEAR, savedInstanceState.getInt(STATE_STATISTICS_YEAR,
                    statisticsMonth.get(Calendar.YEAR)));
            statisticsMonth.set(Calendar.MONTH, savedInstanceState.getInt(STATE_STATISTICS_MONTH,
                    statisticsMonth.get(Calendar.MONTH)));
            selectedStatisticsTab = savedInstanceState.getInt(
                    STATE_STATISTICS_TAB, STATISTICS_TAB_BONUS);
        }
        findViewById(R.id.statisticsPreviousMonth).setOnClickListener(view -> changeMonth(-1));
        findViewById(R.id.statisticsNextMonth).setOnClickListener(view -> changeMonth(1));
        findViewById(R.id.bonusPreviousMonth).setOnClickListener(view -> changeMonth(-1));
        findViewById(R.id.bonusNextMonth).setOnClickListener(view -> changeMonth(1));
        findViewById(R.id.transactionPreviousMonth).setOnClickListener(view -> changeMonth(-1));
        findViewById(R.id.transactionNextMonth).setOnClickListener(view -> changeMonth(1));
        statisticsBonusTab.setOnClickListener(view -> selectStatisticsTab(STATISTICS_TAB_BONUS));
        statisticsUserTab.setOnClickListener(view -> selectStatisticsTab(STATISTICS_TAB_USER));
        statisticsTransactionTab.setOnClickListener(
                view -> selectStatisticsTab(STATISTICS_TAB_TRANSACTION));
        selectStatisticsTab(selectedStatisticsTab);
        renderUserValues();
        renderBonusValues();
        renderTransactionValues();
        homeLabel = findViewById(R.id.homeLabel);
        homeButton = findViewById(R.id.homeButton);
        clientEmptyText = findViewById(R.id.clientEmptyText);
        clientDetailTitle = findViewById(R.id.clientDetailTitle);
        clientSearchInput = findViewById(R.id.clientSearchInput);
        clientFilterChips = new TextView[]{findViewById(R.id.clientFilterAll),
                findViewById(R.id.clientFilterNew), findViewById(R.id.clientFilterExisting)};
        selectedClientSender = savedInstanceState == null ? null
                : savedInstanceState.getString(STATE_CLIENT_SENDER);
        if (savedInstanceState != null) {
            clientSearchInput.setText(savedInstanceState.getString(STATE_CLIENT_QUERY, ""));
            try {
                selectedClientFilter = ClientMessageGrouper.Filter.valueOf(
                        savedInstanceState.getString(STATE_CLIENT_FILTER,
                                ClientMessageGrouper.Filter.ALL.name()));
            } catch (IllegalArgumentException ignored) {
                selectedClientFilter = ClientMessageGrouper.Filter.ALL;
            }
        }
        configureClientFilters();
        messagesNavigationItem.setOnClickListener(view -> showSection(SECTION_MESSAGES));
        clientNavigationItem.setOnClickListener(view -> {
            selectedClientSender = null;
            showSection(SECTION_CLIENT);
        });
        findViewById(R.id.clientBackButton).setOnClickListener(view -> showClientList());
        homeButton.setOnClickListener(view -> showSection(SECTION_HOME));
        historyNavigationItem.setOnClickListener(view -> showSection(SECTION_HISTORY));
        statisticsNavigationItem.setOnClickListener(view -> {
            selectStatisticsTab(STATISTICS_TAB_BONUS);
            showSection(SECTION_STATISTICS);
        });
        showSection(savedInstanceState == null ? SECTION_MESSAGES
                : savedInstanceState.getInt(STATE_SELECTED_SECTION, SECTION_MESSAGES));
        RecyclerView list = findViewById(R.id.transactionsList);
        adapter = new SmsAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        RecyclerView clientList = findViewById(R.id.clientList);
        clientAdapter = new ClientAdapter(this::showClientDetail);
        clientList.setLayoutManager(new LinearLayoutManager(this));
        clientList.setAdapter(clientAdapter);
        RecyclerView clientMessages = findViewById(R.id.clientMessageList);
        clientMessageAdapter = new ClientMessageAdapter();
        clientMessages.setLayoutManager(new LinearLayoutManager(this));
        clientMessages.setAdapter(clientMessageAdapter);
        historyList = findViewById(R.id.historyList);
        historyEmptyState = findViewById(R.id.historyEmptyState);
        historyAdapter = new HistoryAdapter();
        historyList.setLayoutManager(new LinearLayoutManager(this));
        historyList.setAdapter(historyAdapter);

        viewModel = new ViewModelProvider(this).get(SmsViewModel.class);
        viewModel.getGlobalBalanceTitle().observe(this, balanceTitle::setText);
        viewModel.getMessages().observe(this, storedMessages -> {
            messages = storedMessages == null ? new ArrayList<>() : storedMessages;
            roomLoaded = true;
            renderState();
            renderClients();
            renderHistory();
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

    private EditText depositInput(int inputType, String hint, String value) {
        EditText input = new EditText(this);
        input.setInputType(inputType);
        if (inputType == InputType.TYPE_CLASS_NUMBER) {
            // Keep a numeric keyboard while allowing the formatter (not the user) to display spaces.
            input.setKeyListener(DigitsKeyListener.getInstance("0123456789 "));
            input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        }
        input.setHint(hint);
        input.setText(value);
        input.setSelection(input.getText().length());
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        input.setPadding(margin, input.getPaddingTop(), margin, input.getPaddingBottom());
        return input;
    }

    private interface DepositDisplayFormatter {
        String format(String value);
    }

    /** Keeps formatting visual only and restores the caret by its digit position. */
    private void addDepositFormatter(EditText input, DepositDisplayFormatter formatter) {
        input.addTextChangedListener(new TextWatcher() {
            private boolean updating;
            private boolean separatorWasDeleted;
            private int deletedSeparatorPosition;

            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                separatorWasDeleted = count == 1 && after == 0 && start < text.length()
                        && text.charAt(start) == ' ';
                deletedSeparatorPosition = start;
            }

            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable editable) {
                if (updating) return;
                int caret = input.getSelectionStart();
                String edited = editable.toString();
                if (separatorWasDeleted) {
                    int precedingDigit = deletedSeparatorPosition - 1;
                    while (precedingDigit >= 0 && !Character.isDigit(edited.charAt(precedingDigit))) {
                        precedingDigit--;
                    }
                    if (precedingDigit >= 0) {
                        edited = edited.substring(0, precedingDigit) + edited.substring(precedingDigit + 1);
                        caret = precedingDigit;
                    }
                }
                int digitsBeforeCaret = countDigits(edited, Math.min(caret, edited.length()));
                String display = formatter.format(edited);
                int restoredCaret = positionAfterDigits(display, digitsBeforeCaret);
                updating = true;
                try {
                    editable.replace(0, editable.length(), display);
                    input.setSelection(clampSelection(restoredCaret, editable.length()));
                } finally {
                    updating = false;
                }
            }
        });
    }

    private interface BoundedAmountFormatter {
        String format(String input, String lastValidDisplay);
    }

    /** Formats an amount safely while preserving the caret and last value within its ceiling. */
    private void addBoundedAmountFormatter(EditText input, BoundedAmountFormatter formatter,
                                           String maximumError) {
        input.addTextChangedListener(new TextWatcher() {
            private boolean formatting;
            private String lastValidDisplay = formatter.format(input.getText().toString(), "");

            @Override public void beforeTextChanged(CharSequence text, int start, int count,
                                                    int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before,
                                                int count) {}

            @Override public void afterTextChanged(Editable editable) {
                if (formatting) return;
                int selection = clampSelection(input.getSelectionStart(), editable.length());
                int digitsBeforeSelection = countDigits(editable.toString(), selection);
                String edited = editable.toString();
                String display = formatter.format(edited, lastValidDisplay);
                boolean limitExceeded = !display.equals(DepositUssd.formatAmountInput(edited));
                if (!limitExceeded) {
                    lastValidDisplay = display;
                    input.setError(null);
                }
                if (display.contentEquals(editable)) return;
                formatting = true;
                try {
                    editable.replace(0, editable.length(), display);
                    input.setSelection(clampSelection(positionAfterDigits(
                            editable.toString(), digitsBeforeSelection), editable.length()));
                    if (limitExceeded && input.getError() == null) input.setError(maximumError);
                } finally {
                    formatting = false;
                }
            }
        });
    }

    private void addAmountFormatter(EditText input) {
        addBoundedAmountFormatter(input, DepositUssd::formatBoundedAmountInput,
                "Montant maximum : 2 000 000 Ar");
    }

    private void addCreditAmountFormatter(EditText input) {
        addBoundedAmountFormatter(input, CreditUssd::formatBoundedAmountInput,
                "Montant maximum : 500 000 Ar");
    }

    private static int clampSelection(int selection, int textLength) {
        return Math.max(0, Math.min(selection, textLength));
    }

    private static int countDigits(String value, int end) {
        int count = 0;
        for (int index = 0; index < end; index++) if (Character.isDigit(value.charAt(index))) count++;
        return count;
    }

    private static int positionAfterDigits(String value, int digitCount) {
        if (digitCount == 0) return value.startsWith("+") ? value.length() : 0;
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (Character.isDigit(value.charAt(index)) && ++count == digitCount) return index + 1;
        }
        return value.length();
    }

    private void showRecipientDialog(String recipientValue, String amountValue) {
        EditText input = depositInput(InputType.TYPE_CLASS_PHONE,
                "Ex. 034 14 110 58", DepositUssd.formatRecipientInput(recipientValue));
        addDepositFormatter(input, DepositUssd::formatRecipientInput);

        SharedPreferences depositPreferences = getSharedPreferences(
                DEPOSIT_PREFERENCES, MODE_PRIVATE);
        String lastRecipient = DepositUssd.normalizeRecipientNumber(
                depositPreferences.getString(LAST_DEPOSIT_RECIPIENT, null));

        LinearLayout recipientRow = new LinearLayout(this);
        recipientRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        recipientRow.addView(input, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ImageButton useLastRecipient = new ImageButton(this);
        useLastRecipient.setImageResource(R.drawable.ic_history_24);
        useLastRecipient.setContentDescription("Utiliser le dernier numéro");
        TypedValue buttonBackground = new TypedValue();
        getTheme().resolveAttribute(
                androidx.appcompat.R.attr.selectableItemBackgroundBorderless,
                buttonBackground, true);
        useLastRecipient.setBackgroundResource(buttonBackground.resourceId);
        useLastRecipient.setFocusable(false);
        useLastRecipient.setEnabled(lastRecipient != null);
        useLastRecipient.setAlpha(lastRecipient == null ? 0.38f : 1f);
        int touchTarget = (int) (48 * getResources().getDisplayMetrics().density);
        int iconPadding = (int) (12 * getResources().getDisplayMetrics().density);
        useLastRecipient.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
        recipientRow.addView(useLastRecipient, new LinearLayout.LayoutParams(
                touchTarget, touchTarget));
        useLastRecipient.setOnClickListener(view -> {
            input.setText(lastRecipient);
            input.setSelection(input.getText().length());
            input.requestFocus();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Numéro destinataire")
                .setView(recipientRow)
                .setNegativeButton("ANNULER", (ignored, which) -> clearDepositWorkflow())
                .setPositiveButton("SUIVANT", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String recipientNumber = DepositUssd.normalizeRecipientNumber(
                            input.getText().toString());
                    if (recipientNumber == null) {
                        input.setError("Numéro malgache invalide");
                        return;
                    }
                    depositPreferences.edit()
                            .putString(LAST_DEPOSIT_RECIPIENT, recipientNumber)
                            .apply();
                    dialog.dismiss();
                    showAmountDialog(recipientNumber, amountValue);
                }));
        dialog.show();
        focusAndShowNumericKeyboard(dialog, input);
    }

    private void showAmountDialog(String recipientNumber, String amountValue) {
        EditText input = depositInput(InputType.TYPE_CLASS_NUMBER, "Montant",
                DepositUssd.formatAmountInput(amountValue));
        addAmountFormatter(input);
        LinearLayout amountRow = new LinearLayout(this);
        amountRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        amountRow.addView(input, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView currency = new TextView(this);
        currency.setText("Ar");
        int currencyPadding = (int) (24 * getResources().getDisplayMetrics().density);
        currency.setPadding(0, 0, currencyPadding, 0);
        amountRow.addView(currency);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Entrer le montant")
                .setView(amountRow)
                .setNegativeButton("ANNULER", (ignored, which) -> clearDepositWorkflow())
                .setPositiveButton("SUIVANT", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String normalizedAmount = DepositUssd.normalizeAmount(input.getText().toString());
                    if (normalizedAmount == null) {
                        input.setError("Montant invalide");
                        return;
                    }
                    long originalAmount;
                    try {
                        originalAmount = Long.parseLong(normalizedAmount);
                    } catch (NumberFormatException error) {
                        input.setError("Montant trop élevé");
                        return;
                    }
                    if (!DepositUssd.isAllowedInputAmount(originalAmount)) {
                        input.setError("Montant maximum : 2 000 000 Ar");
                        return;
                    }
                    dialog.dismiss();
                    showWithdrawalFeeDialog(recipientNumber, originalAmount);
                }));
        dialog.show();
        focusAndShowNumericKeyboard(dialog, input);
    }

    private void showCreditRecipientDialog(String recipientValue, String amountValue) {
        EditText input = depositInput(InputType.TYPE_CLASS_PHONE,
                "Ex. 034 14 110 58", DepositUssd.formatRecipientInput(recipientValue));
        addDepositFormatter(input, DepositUssd::formatRecipientInput);
        SharedPreferences preferences = getSharedPreferences(DEPOSIT_PREFERENCES, MODE_PRIVATE);
        String lastRecipient = DepositUssd.normalizeRecipientNumber(
                preferences.getString(LAST_DEPOSIT_RECIPIENT, null));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.addView(input, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        ImageButton history = new ImageButton(this);
        history.setImageResource(R.drawable.ic_history_24);
        history.setContentDescription("Utiliser le dernier numéro");
        TypedValue background = new TypedValue();
        getTheme().resolveAttribute(androidx.appcompat.R.attr.selectableItemBackgroundBorderless,
                background, true);
        history.setBackgroundResource(background.resourceId);
        history.setFocusable(false);
        history.setEnabled(lastRecipient != null);
        history.setAlpha(lastRecipient == null ? 0.38f : 1f);
        int touchTarget = (int) (48 * getResources().getDisplayMetrics().density);
        int iconPadding = (int) (12 * getResources().getDisplayMetrics().density);
        history.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
        row.addView(history, new LinearLayout.LayoutParams(touchTarget, touchTarget));
        history.setOnClickListener(view -> {
            input.setText(lastRecipient);
            input.setSelection(input.getText().length());
            input.requestFocus();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Numéro destinataire")
                .setView(row)
                .setNegativeButton("ANNULER", null)
                .setPositiveButton("SUIVANT", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String recipient = DepositUssd.normalizeRecipientNumber(
                            input.getText().toString());
                    if (recipient == null) {
                        input.setError("Numéro malgache invalide");
                        return;
                    }
                    preferences.edit().putString(LAST_DEPOSIT_RECIPIENT, recipient).apply();
                    dialog.dismiss();
                    showCreditAmountDialog(recipient, amountValue);
                }));
        dialog.show();
        focusAndShowNumericKeyboard(dialog, input);
    }

    private void showCreditAmountDialog(String recipientNumber, String amountValue) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        addWithdrawalFeeChoice(content, "1   500 Ar");
        addWithdrawalFeeChoice(content, "2   1 000 Ar");

        EditText input = depositInput(InputType.TYPE_CLASS_NUMBER, "Montant",
                DepositUssd.formatAmountInput(amountValue));
        input.setPadding(0, input.getPaddingTop(), input.getPaddingRight(),
                input.getPaddingBottom());
        addCreditAmountFormatter(input);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(padding, 0, 0, 0);
        row.addView(input, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView currency = new TextView(this);
        currency.setText("Ar");
        currency.setPadding(0, 0, padding, 0);
        row.addView(currency);
        content.addView(row);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Entrer montant du crédit")
                .setView(content)
                .setNegativeButton("ANNULER", null)
                .setPositiveButton("SUIVANT", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    long amount = CreditUssd.resolveAmount(input.getText().toString());
                    if (amount < 0) {
                        input.setError("Montant compris entre 100 et 500 000 Ar");
                        return;
                    }
                    dialog.dismiss();
                    showCreditConfirmation(recipientNumber, amount);
                }));
        dialog.show();
        focusAndShowNumericKeyboard(dialog, input);
    }

    private void showCreditConfirmation(String recipientNumber, long amount) {
        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        summary.setPadding(margin, margin / 2, margin, 0);
        addConfirmationField(summary, "Numéro destinataire",
                DepositUssd.formatRecipientNumber(recipientNumber), false, false);
        addConfirmationField(summary, "Montant du crédit",
                DepositUssd.formatAmount(String.valueOf(amount)), true, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Vérifier le crédit")
                .setView(summary)
                .setNeutralButton("MODIFIER", (ignored, which) ->
                        showCreditRecipientDialog(recipientNumber, String.valueOf(amount)))
                .setNegativeButton("ANNULER", null)
                .setPositiveButton("ENVOYER", (ignored, which) ->
                        requestUssdCall(CreditUssd.buildUssdCode(recipientNumber, amount)))
                .create();
        dialog.show();
    }

    /** Focuses a dialog input only after its window is attached and visible. */
    private void focusAndShowNumericKeyboard(AlertDialog dialog, EditText input) {
        input.post(() -> {
            input.requestFocus();
            input.setSelection(input.getText().length());
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
    }

    private void showWithdrawalFeeDialog(String recipientNumber, long originalAmount) {
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = (int) (24 * getResources().getDisplayMetrics().density);
        String formattedAmount = DepositUssd.formatAmount(String.valueOf(originalAmount));
        Long withdrawalFee = DepositUssd.calculateWithdrawalFee(originalAmount);
        String formattedFee = withdrawalFee == null
                ? "indisponibles"
                : DepositUssd.formatAmount(String.valueOf(withdrawalFee));
        addWithdrawalFeeChoice(choices,
                "1    Oui, (" + formattedAmount + " + Frais " + formattedFee + ")");
        addWithdrawalFeeChoice(choices, "2    Non, " + formattedAmount);

        EditText choiceInput = new EditText(this);
        choiceInput.setHint("1 ou 2");
        choiceInput.setSingleLine(true);
        choiceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        choiceInput.setKeyListener(DigitsKeyListener.getInstance("12"));
        choiceInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.setMargins(horizontalPadding, horizontalPadding / 3, horizontalPadding, 0);
        choices.addView(choiceInput, inputParams);

        TextView errorText = new TextView(this);
        errorText.setText("Frais de retrait non disponibles pour ce montant.");
        errorText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        errorText.setPadding(horizontalPadding, 0, horizontalPadding, horizontalPadding / 2);
        errorText.setVisibility(View.GONE);
        choices.addView(errorText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Avec frais de retrait ?")
                .setView(choices)
                .setNegativeButton("ANNULER", (ignored, which) -> clearDepositWorkflow())
                .setNeutralButton("MODIFIER", (ignored, which) ->
                        showAmountDialog(recipientNumber, String.valueOf(originalAmount)))
                .create();
        boolean[] transitionStarted = {false};
        choiceInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count,
                                                    int after) {}

            @Override public void onTextChanged(CharSequence text, int start, int before,
                                                int count) {}

            @Override public void afterTextChanged(Editable text) {
                if (text.length() != 1) return;
                char choice = text.charAt(0);
                if (choice == '1' || choice == '2') {
                    handleWithdrawalFeeChoice(recipientNumber, originalAmount, choice == '1',
                            dialog, errorText, transitionStarted);
                }
            }
        });
        dialog.show();
        focusAndShowNumericKeyboard(dialog, choiceInput);
    }

    private void handleWithdrawalFeeChoice(String recipientNumber, long originalAmount,
                                           boolean includeFee, AlertDialog dialog,
                                           TextView errorText, boolean[] transitionStarted) {
        if (transitionStarted[0]) return;

        Long withdrawalFee = includeFee
                ? DepositUssd.calculateWithdrawalFee(originalAmount) : 0L;
        if (withdrawalFee == null) {
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        transitionStarted[0] = true;
        long finalAmount = DepositUssd.calculateFinalAmount(originalAmount, includeFee);
        dialog.dismiss();
        showDepositConfirmation(recipientNumber, originalAmount, includeFee,
                withdrawalFee, finalAmount);
    }

    private void addWithdrawalFeeChoice(LinearLayout choices, String text) {
        TextView choice = new TextView(this);
        choice.setText(text);
        choice.setTextSize(18);
        choice.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        choice.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int horizontalPadding = (int) (24 * getResources().getDisplayMetrics().density);
        choice.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        choice.setMinHeight((int) (28 * getResources().getDisplayMetrics().density));
        choices.addView(choice, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void showDepositConfirmation(String recipientNumber, long originalAmount,
                                         boolean includeWithdrawalFee, long withdrawalFee,
                                         long finalAmount) {
        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        summary.setPadding(margin, margin / 2, margin, 0);
        addConfirmationField(summary, "Numéro destinataire",
                DepositUssd.formatRecipientNumber(recipientNumber), false, false);
        addConfirmationField(summary, "Montant",
                DepositUssd.formatAmount(String.valueOf(originalAmount)), true, false);
        addConfirmationField(summary, "Frais de retrait", includeWithdrawalFee
                ? DepositUssd.formatAmount(String.valueOf(withdrawalFee)) : "Non", true, false);
        addConfirmationField(summary, "TOTAL À ENVOYER",
                DepositUssd.formatAmount(String.valueOf(finalAmount)), true, true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Vérifier le dépôt")
                .setView(summary)
                .setNegativeButton("ANNULER", (ignored, which) -> clearDepositWorkflow())
                .setNeutralButton("MODIFIER", (ignored, which) ->
                        showRecipientDialog(recipientNumber, String.valueOf(originalAmount)))
                .setPositiveButton("ENVOYER", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    if (depositRequestInProgress) return;
                    depositRequestInProgress = true;
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    depositCard.setEnabled(false);
                    dialog.dismiss();
                    requestDepositCall(recipientNumber, String.valueOf(finalAmount));
                }));
        dialog.show();
    }

    private void addConfirmationField(LinearLayout summary, String labelText, String valueText,
                                      boolean separateFromPrevious, boolean emphasize) {
        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(14);
        label.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (separateFromPrevious) labelParams.topMargin = (int) (24 * getResources().getDisplayMetrics().density);
        summary.addView(label, labelParams);

        TextView value = new TextView(this);
        value.setText(valueText);
        value.setTextSize(emphasize ? 22 : 18);
        value.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        valueParams.topMargin = (int) (6 * getResources().getDisplayMetrics().density);
        summary.addView(value, valueParams);
    }

    private void requestDepositCall(String recipientNumber, String amount) {
        requestUssdCall(DepositUssd.buildUssdCode(recipientNumber, amount));
    }

    private void requestUssdCall(String ussdCode) {
        pendingUssdCode = ussdCode;
        if (hasPermission(Manifest.permission.CALL_PHONE)) {
            launchUssdWithCallIntent(ussdCode);
        } else {
            launchDepositAfterPermission = true;
            phonePermissionLauncher.launch(Manifest.permission.CALL_PHONE);
        }
    }

    private void launchUssdWithCallIntent(String ussdCode) {
        Intent callIntent = new Intent(Intent.ACTION_CALL,
                Uri.fromParts("tel", ussdCode, null));
        try {
            depositCallLaunched = true;
            startActivity(callIntent);
        } catch (SecurityException | android.content.ActivityNotFoundException error) {
            depositCallLaunched = false;
            Toast.makeText(this, "Impossible de lancer le service USSD sur cet appareil.",
                    Toast.LENGTH_LONG).show();
            finishDepositRequest();
        }
    }

    private void clearDepositWorkflow() {
        launchDepositAfterPermission = false;
        pendingUssdCode = null;
        finishDepositRequest();
    }

    private void finishDepositRequest() {
        depositRequestInProgress = false;
        pendingUssdCode = null;
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
        boolean clientSelected = section == SECTION_CLIENT;
        boolean homeSelected = section == SECTION_HOME;
        boolean historySelected = section == SECTION_HISTORY;
        boolean statisticsSelected = section == SECTION_STATISTICS;
        messagesSection.setVisibility(messagesSelected ? View.VISIBLE : View.GONE);
        homeSection.setVisibility(homeSelected ? View.VISIBLE : View.GONE);
        historySection.setVisibility(historySelected ? View.VISIBLE : View.GONE);
        statisticsSection.setVisibility(statisticsSelected ? View.VISIBLE : View.GONE);
        clientSection.setVisibility(clientSelected ? View.VISIBLE : View.GONE);

        int active = ContextCompat.getColor(this, R.color.sms_bottom_item_active);
        int inactive = ContextCompat.getColor(this, R.color.sms_bottom_item);
        messagesNavigationItem.setTextColor(messagesSelected ? active : inactive);
        messagesNavigationItem.setCompoundDrawableTintList(
                ColorStateList.valueOf(messagesSelected ? active : inactive));
        clientNavigationItem.setTextColor(clientSelected ? active : inactive);
        clientNavigationItem.setCompoundDrawableTintList(
                ColorStateList.valueOf(clientSelected ? active : inactive));
        historyNavigationItem.setTextColor(historySelected ? active : inactive);
        historyNavigationItem.setCompoundDrawableTintList(
                ColorStateList.valueOf(historySelected ? active : inactive));
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
        clientNavigationItem.setSelected(clientSelected);
        historyNavigationItem.setSelected(historySelected);
        statisticsNavigationItem.setSelected(statisticsSelected);
        if (statisticsSelected) renderStatistics();
        if (clientSelected) renderClients();
        if (historySelected) renderHistory();
    }

    private void renderHistory() {
        if (historyAdapter == null) return;
        List<HistoryTransaction> transactions = HistoryTransaction.fromMessages(messages);
        historyAdapter.submitList(transactions);
        boolean empty = transactions.isEmpty();
        historyList.setVisibility(empty ? View.GONE : View.VISIBLE);
        historyEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void renderClients() {
        if (clientAdapter == null || clientMessageAdapter == null) return;
        List<ClientMessageGrouper.ClientGroup> allClients = ClientMessageGrouper.group(messages);
        String query = clientSearchInput == null ? "" : clientSearchInput.getText().toString();
        List<ClientMessageGrouper.ClientGroup> visibleClients = ClientMessageGrouper.filter(
                allClients, query, selectedClientFilter);
        clientAdapter.submitList(visibleClients);
        boolean empty = visibleClients.isEmpty();
        clientEmptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            if (allClients.isEmpty()) {
                clientEmptyText.setText("Aucun client");
            } else if (!query.trim().isEmpty()) {
                clientEmptyText.setText("Aucun client trouvé");
            } else if (selectedClientFilter == ClientMessageGrouper.Filter.NEW) {
                clientEmptyText.setText("Aucun nouveau client");
            } else if (selectedClientFilter == ClientMessageGrouper.Filter.EXISTING) {
                clientEmptyText.setText("Aucun ancien client");
            } else {
                clientEmptyText.setText("Aucun client");
            }
        }

        if (selectedClientSender == null) {
            clientListContent.setVisibility(View.VISIBLE);
            clientDetailContent.setVisibility(View.GONE);
            return;
        }
        for (ClientMessageGrouper.ClientGroup client : allClients) {
            if (selectedClientSender.equals(client.sender)) {
                clientDetailTitle.setText(SmsDisplayFormatter.sender(client.sender));
                clientMessageAdapter.submitList(clientMessagesWithOriginalNumbers(client.sender));
                clientListContent.setVisibility(View.GONE);
                clientDetailContent.setVisibility(View.VISIBLE);
                return;
            }
        }
        // A deleted sender naturally returns the user to the derived client list.
        selectedClientSender = null;
        clientListContent.setVisibility(View.VISIBLE);
        clientDetailContent.setVisibility(View.GONE);
    }

    private void showClientDetail(ClientMessageGrouper.ClientGroup client) {
        selectedClientSender = client.sender;
        renderClients();
    }

    /** Keeps the number assigned in the unfiltered Messages table when viewing one client. */
    private List<SmsDateFilter.DisplayMessage> clientMessagesWithOriginalNumbers(String sender) {
        List<SmsDateFilter.DisplayMessage> result = new ArrayList<>();
        List<SmsDateFilter.DisplayMessage> all = SmsDateFilter.apply(messages,
                SmsDateFilter.Period.ALL, null, System.currentTimeMillis(),
                java.util.TimeZone.getDefault());
        for (SmsDateFilter.DisplayMessage displayed : all) {
            if (sender.equals(ClientMessageGrouper.clientKey(displayed.message))) {
                result.add(displayed);
            }
        }
        return result;
    }

    private void showClientList() {
        selectedClientSender = null;
        renderClients();
    }

    private void configureClientFilters() {
        clientFilterChips[0].setOnClickListener(view ->
                selectClientFilter(ClientMessageGrouper.Filter.ALL));
        clientFilterChips[1].setOnClickListener(view ->
                selectClientFilter(ClientMessageGrouper.Filter.NEW));
        clientFilterChips[2].setOnClickListener(view ->
                selectClientFilter(ClientMessageGrouper.Filter.EXISTING));
        clientSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count,
                    int after) {}

            @Override public void onTextChanged(CharSequence value, int start, int before,
                    int count) {
                renderClients();
            }

            @Override public void afterTextChanged(Editable value) {}
        });
        updateClientFilterChips();
    }

    private void selectClientFilter(ClientMessageGrouper.Filter filter) {
        selectedClientFilter = filter;
        updateClientFilterChips();
        renderClients();
    }

    private void updateClientFilterChips() {
        ClientMessageGrouper.Filter[] filters = ClientMessageGrouper.Filter.values();
        for (int index = 0; index < clientFilterChips.length; index++) {
            clientFilterChips[index].setSelected(selectedClientFilter == filters[index]);
        }
    }

    @Override
    public void onBackPressed() {
        if (selectedSection == SECTION_CLIENT && selectedClientSender != null) {
            showClientList();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_SECTION, selectedSection);
        outState.putInt(STATE_STATISTICS_YEAR, statisticsMonth.get(Calendar.YEAR));
        outState.putInt(STATE_STATISTICS_MONTH, statisticsMonth.get(Calendar.MONTH));
        outState.putInt(STATE_STATISTICS_TAB, selectedStatisticsTab);
        outState.putString(STATE_MESSAGE_FILTER, selectedMessageFilter.name());
        if (messageSearchInput != null) {
            outState.putString(STATE_MESSAGE_QUERY, messageSearchInput.getText().toString());
        }
        if (selectedClientSender != null) outState.putString(STATE_CLIENT_SENDER, selectedClientSender);
        if (clientSearchInput != null) {
            outState.putString(STATE_CLIENT_QUERY, clientSearchInput.getText().toString());
        }
        outState.putString(STATE_CLIENT_FILTER, selectedClientFilter.name());
        if (customFilterDate != null) {
            outState.putLong(STATE_CUSTOM_FILTER_DATE, customFilterDate);
        }
        super.onSaveInstanceState(outState);
    }

    private void configureMessageFilters(Bundle savedInstanceState) {
        messageSearchInput = findViewById(R.id.messageSearchInput);
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
            messageSearchInput.setText(savedInstanceState.getString(STATE_MESSAGE_QUERY, ""));
        }
        filterChips[0].setOnClickListener(view -> selectMessageFilter(SmsDateFilter.Period.ALL));
        filterChips[1].setOnClickListener(view -> selectMessageFilter(SmsDateFilter.Period.TODAY));
        filterChips[2].setOnClickListener(view -> selectMessageFilter(SmsDateFilter.Period.YESTERDAY));
        filterChips[3].setOnClickListener(view -> selectMessageFilter(SmsDateFilter.Period.SEVEN_DAYS));
        filterChips[4].setOnClickListener(view -> selectMessageFilter(SmsDateFilter.Period.THIRTY_DAYS));
        filterChips[5].setOnClickListener(view -> showDateFilterPicker());
        messageSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count,
                    int after) {}

            @Override public void onTextChanged(CharSequence value, int start, int before,
                    int count) {
                renderState();
            }

            @Override public void afterTextChanged(Editable value) {}
        });
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
        updateTransactionStatistics();
        String monthLabel = new SimpleDateFormat("MMMM yyyy", Locale.FRENCH)
                .format(statisticsMonth.getTime());
        statisticsMonthText.setText(monthLabel.substring(0, 1).toUpperCase(Locale.FRENCH)
                + monthLabel.substring(1));
        bonusMonthText.setText(statisticsMonthText.getText());
        transactionMonthText.setText(statisticsMonthText.getText());
        if (selectedStatisticsTab == STATISTICS_TAB_BONUS) renderBonusValues();
        else if (selectedStatisticsTab == STATISTICS_TAB_USER) renderUserValues();
        else renderTransactionValues();
    }

    private void updateTransactionStatistics() {
        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(messages,
                System.currentTimeMillis(), statisticsMonth.get(Calendar.YEAR),
                statisticsMonth.get(Calendar.MONTH), java.util.TimeZone.getDefault());
        todayUsers = summary.todayClients.size();
        yesterdayUsers = summary.yesterdayClients.size();
        weekUsers = summary.weekClients.size();
        monthUsers = summary.monthClients.size();
        yearUsers = summary.yearClients.size();
        monthlyUserBars = summary.monthlyUsers;
        todayBonus = summary.todayBonus;
        yesterdayBonus = summary.yesterdayBonus;
        weekBonus = summary.weekBonus;
        monthBonus = summary.monthBonus;
        yearBonus = summary.yearBonus;
        monthlyBonusBars = summary.monthlyBonus;
        todayTransactions = summary.todayTransactions;
        todayDepositTransactions = summary.todayDepositTransactions;
        todayCreditTransactions = summary.todayCreditTransactions;
        todayWithdrawalTransactions = summary.todayWithdrawalTransactions;
        yesterdayTransactions = summary.yesterdayTransactions;
        weekTransactions = summary.weekTransactions;
        monthlyTransactions = summary.monthTransactions;
        yearlyTransactions = summary.yearTransactions;
        monthlyTransactionBars = summary.monthlyTransactions;
    }

    private void selectStatisticsTab(int tab) {
        selectedStatisticsTab = tab;
        boolean showBonus = tab == STATISTICS_TAB_BONUS;
        boolean showUser = tab == STATISTICS_TAB_USER;
        boolean showTransaction = tab == STATISTICS_TAB_TRANSACTION;
        statisticsBonusTab.setSelected(showBonus);
        statisticsUserTab.setSelected(showUser);
        statisticsTransactionTab.setSelected(showTransaction);
        bonusStatisticsContent.setVisibility(showBonus ? View.VISIBLE : View.GONE);
        userStatisticsContent.setVisibility(showUser ? View.VISIBLE : View.GONE);
        transactionStatisticsContent.setVisibility(showTransaction ? View.VISIBLE : View.GONE);
        if (showBonus) {
            statisticsSubtitle.setText("Suivi des bonus");
            renderBonusValues();
        } else if (showUser) {
            statisticsSubtitle.setText("Nombre d’utilisateurs");
            renderUserValues();
        } else {
            statisticsSubtitle.setText("Nombre de transactions");
            renderTransactionValues();
        }
    }

    private void renderUserValues() {
        ((TextView) findViewById(R.id.todayUsersValue)).setText(String.valueOf(todayUsers));
        ((TextView) findViewById(R.id.depositTransactionsValue))
                .setText(String.valueOf(todayDepositTransactions));
        ((TextView) findViewById(R.id.creditTransactionsValue))
                .setText(String.valueOf(todayCreditTransactions));
        ((TextView) findViewById(R.id.withdrawalTransactionsValue))
                .setText(String.valueOf(todayWithdrawalTransactions));
        ((TextView) findViewById(R.id.yesterdayUsersValue)).setText(String.valueOf(yesterdayUsers));
        ((TextView) findViewById(R.id.weekUsersValue)).setText(String.valueOf(weekUsers));
        ((TextView) findViewById(R.id.monthUsersValue)).setText(String.valueOf(monthUsers));
        ((TextView) findViewById(R.id.yearUsersValue)).setText(String.valueOf(yearUsers));
        renderPeriodLabels(userMonthLabel, userYearLabel);
        userChart.setUserData(monthlyUserBars);
        boolean hasUsers = !monthlyUserBars.isEmpty();
        userChart.setVisibility(hasUsers ? View.VISIBLE : View.GONE);
        emptyUserChartText.setVisibility(hasUsers ? View.GONE : View.VISIBLE);
    }

    private void renderBonusValues() {
        ((TextView) findViewById(R.id.todayBonusValue)).setText(formatAriary(todayBonus));
        ((TextView) findViewById(R.id.yesterdayBonusValue)).setText(formatAriary(yesterdayBonus));
        ((TextView) findViewById(R.id.weekBonusValue)).setText(formatAriary(weekBonus));
        ((TextView) findViewById(R.id.monthBonusValue)).setText(formatAriary(monthBonus));
        ((TextView) findViewById(R.id.yearBonusValue)).setText(formatAriary(yearBonus));
        renderPeriodLabels(bonusMonthLabel, bonusYearLabel);
        bonusChart.setBonusData(monthlyBonusBars);
        boolean hasBonus = !monthlyBonusBars.isEmpty();
        bonusChart.setVisibility(hasBonus ? View.VISIBLE : View.GONE);
        emptyBonusChartText.setVisibility(hasBonus ? View.GONE : View.VISIBLE);
    }

    private void renderTransactionValues() {
        ((TextView) findViewById(R.id.totalTransactionsValue))
                .setText(String.valueOf(todayTransactions));
        ((TextView) findViewById(R.id.yesterdayTransactionsValue))
                .setText(String.valueOf(yesterdayTransactions));
        ((TextView) findViewById(R.id.weekTransactionsValue))
                .setText(String.valueOf(weekTransactions));
        ((TextView) findViewById(R.id.monthlyTransactionsValue))
                .setText(String.valueOf(monthlyTransactions));
        ((TextView) findViewById(R.id.yearlyTransactionsValue))
                .setText(String.valueOf(yearlyTransactions));

        renderPeriodLabels(transactionMonthLabel, transactionYearLabel);
        transactionChart.setTransactionData(monthlyTransactionBars);
        boolean hasTransactions = !monthlyTransactionBars.isEmpty();
        transactionChart.setVisibility(hasTransactions ? View.VISIBLE : View.GONE);
        emptyTransactionChartText.setVisibility(hasTransactions ? View.GONE : View.VISIBLE);
    }

    private void renderPeriodLabels(TextView monthLabel, TextView yearLabel) {
        Calendar currentMonth = Calendar.getInstance();
        StatisticsPeriodLabels.Labels labels = StatisticsPeriodLabels.getPeriodLabels(
                statisticsMonth.get(Calendar.MONTH), statisticsMonth.get(Calendar.YEAR),
                currentMonth.get(Calendar.MONTH), currentMonth.get(Calendar.YEAR));
        monthLabel.setText(labels.month);
        yearLabel.setText(labels.year);
    }

    private String formatAriary(long value) {
        return String.format(Locale.FRENCH, "%,d Ar", value).replace('\u00a0', ' ');
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
        menu.getMenu().add("Affichage au-dessus des applications")
                .setOnMenuItemClickListener(item -> {
                    showOverlayPermissionDialog();
                    return true;
                });
        menu.show();
    }

    private void showOverlayPermissionDialog() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "L’affichage au-dessus des applications est autorisé.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Afficher les transactions")
                .setMessage("Autoriser l’affichage des transactions au-dessus des autres "
                        + "applications.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Ouvrir les réglages", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException exception) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    }
                })
                .show();
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
        if (depositCallLaunched) {
            depositCallLaunched = false;
            finishDepositRequest();
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
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
                System.currentTimeMillis(), java.util.TimeZone.getDefault(),
                messageSearchInput == null ? "" : messageSearchInput.getText().toString());
        adapter.submitList(displayed);
        boolean noDisplayedMessages = displayed.isEmpty();
        boolean searching = messageSearchInput != null
                && !messageSearchInput.getText().toString().trim().isEmpty();
        emptyText.setText(searching ? "Aucun message trouvé"
                : selectedMessageFilter == SmsDateFilter.Period.ALL
                ? "Aucun message enregistré" : "Aucun message pour cette période");
        emptyText.setVisibility(!denied && !loading && noDisplayedMessages
                ? View.VISIBLE : View.GONE);
    }

    private interface ClientClickListener {
        void onClientClick(ClientMessageGrouper.ClientGroup client);
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
        private List<HistoryTransaction> items = new ArrayList<>();

        void submitList(List<HistoryTransaction> transactions) {
            items = new ArrayList<>(transactions);
            notifyDataSetChanged();
        }

        @Override public HistoryViewHolder onCreateViewHolder(android.view.ViewGroup parent,
                                                               int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_transaction, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override public void onBindViewHolder(HistoryViewHolder holder, int position) {
            HistoryTransaction item = items.get(position);
            String date = dateFormat.format(new Date(item.timestamp));
            String previousDate = position == 0 ? null
                    : dateFormat.format(new Date(items.get(position - 1).timestamp));
            holder.date.setText(date);
            holder.date.setVisibility(date.equals(previousDate) ? View.GONE : View.VISIBLE);
            holder.number.setText(HistoryDisplayFormatter.number(item.clientNumber));
            holder.reference.setText(HistoryDisplayFormatter.reference(item.reference));
            holder.amount.setText(HistoryDisplayFormatter.amount(item));
            holder.bonus.setText(HistoryDisplayFormatter.bonus(item.bonus));
            holder.icon.setImageResource(HistoryDisplayFormatter.direction(item)
                    == HistoryDisplayFormatter.Direction.NORTH_EAST
                    ? R.drawable.ic_north_east_24 : R.drawable.ic_south_east_24);
            holder.itemView.setContentDescription(item.type + ", " + holder.number.getText()
                    + ", " + holder.amount.getText() + ", " + holder.reference.getText()
                    + ", bonus " + holder.bonus.getText());
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final TextView date;
        final TextView number;
        final TextView reference;
        final TextView amount;
        final TextView bonus;
        final ImageView icon;

        HistoryViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.historyDateHeader);
            number = itemView.findViewById(R.id.historyTransactionNumber);
            reference = itemView.findViewById(R.id.historyTransactionReference);
            amount = itemView.findViewById(R.id.historyTransactionAmount);
            bonus = itemView.findViewById(R.id.historyTransactionBonus);
            icon = itemView.findViewById(R.id.historyTransactionIcon);
        }
    }

    private static class ClientAdapter extends RecyclerView.Adapter<ClientViewHolder> {
        private final ClientClickListener listener;
        private List<ClientMessageGrouper.ClientGroup> items = new ArrayList<>();

        ClientAdapter(ClientClickListener listener) { this.listener = listener; }

        void submitList(List<ClientMessageGrouper.ClientGroup> clients) {
            items = new ArrayList<>(clients);
            notifyDataSetChanged();
        }

        @Override public ClientViewHolder onCreateViewHolder(android.view.ViewGroup parent,
                                                              int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_client, parent, false);
            return new ClientViewHolder(view);
        }

        @Override public void onBindViewHolder(ClientViewHolder holder, int position) {
            ClientMessageGrouper.ClientGroup client = items.get(position);
            holder.name.setText(SmsDisplayFormatter.sender(client.sender));
            int count = client.messages.size();
            holder.count.setText(count + (count == 1 ? " message" : " messages"));
            holder.itemView.setContentDescription(SmsDisplayFormatter.sender(client.sender)
                    + ", " + holder.count.getText());
            holder.itemView.setOnClickListener(view -> listener.onClientClick(client));
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class ClientViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView count;
        ClientViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.clientName);
            count = itemView.findViewById(R.id.clientMessageCount);
        }
    }

    private static class ClientMessageAdapter extends RecyclerView.Adapter<ClientMessageViewHolder> {
        private List<SmsDateFilter.DisplayMessage> items = new ArrayList<>();

        void submitList(List<SmsDateFilter.DisplayMessage> messages) {
            items = new ArrayList<>(messages);
            notifyDataSetChanged();
        }

        @Override public ClientMessageViewHolder onCreateViewHolder(android.view.ViewGroup parent,
                                                                     int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_client_message, parent, false);
            return new ClientMessageViewHolder(view);
        }

        @Override public void onBindViewHolder(ClientMessageViewHolder holder, int position) {
            SmsTableRow row = SmsTableRow.from(items.get(position));
            holder.number.setText(String.valueOf(row.number));
            holder.dateTime.setText(row.dateTime);
            holder.type.setText(SmsTableRow.display(row.type));
            holder.amount.setText(SmsTableRow.display(row.montant));
            holder.reference.setText(SmsTableRow.display(row.reference));
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class ClientMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView dateTime;
        final TextView type;
        final TextView amount;
        final TextView reference;
        ClientMessageViewHolder(View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.clientMessageNumber);
            dateTime = itemView.findViewById(R.id.clientMessageDateTime);
            type = itemView.findViewById(R.id.clientMessageType);
            amount = itemView.findViewById(R.id.clientMessageAmount);
            reference = itemView.findViewById(R.id.clientMessageReference);
        }
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
            SmsTableRow row = SmsTableRow.from(items.get(position));
            holder.number.setText(String.valueOf(row.number));
            holder.dateTime.setText(row.dateTime);
            holder.type.setText(SmsTableRow.display(row.type));
            holder.sender.setText(SmsTableRow.display(row.numero));
            holder.name.setText(SmsTableRow.display(row.nom));
            holder.amount.setText(SmsTableRow.display(row.montant));
            holder.reference.setText(SmsTableRow.display(row.reference));
            holder.bonus.setText(SmsTableRow.display(row.bonus));
            holder.fees.setText(SmsTableRow.display(row.frais));
            holder.balance.setText(SmsTableRow.display(row.solde));
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class SmsViewHolder extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView dateTime;
        final TextView type;
        final TextView sender;
        final TextView name;
        final TextView amount;
        final TextView reference;
        final TextView bonus;
        final TextView fees;
        final TextView balance;

        SmsViewHolder(View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.itemNumber);
            dateTime = itemView.findViewById(R.id.itemDateTime);
            type = itemView.findViewById(R.id.itemType);
            sender = itemView.findViewById(R.id.itemSender);
            name = itemView.findViewById(R.id.itemName);
            amount = itemView.findViewById(R.id.itemAmount);
            reference = itemView.findViewById(R.id.itemReference);
            bonus = itemView.findViewById(R.id.itemBonus);
            fees = itemView.findViewById(R.id.itemFees);
            balance = itemView.findViewById(R.id.itemBalance);
        }
    }
}
