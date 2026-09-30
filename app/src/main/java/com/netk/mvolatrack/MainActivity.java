package com.netk.mvolatrack;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.provider.Settings;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.text.InputType;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.method.DigitsKeyListener;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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
import androidx.core.content.FileProvider;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.drawerlayout.widget.DrawerLayout;

import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.database.AppDatabase;
import com.netk.mvolatrack.backup.SmsBackupManager;
import com.netk.mvolatrack.background.BackgroundExecutionManager;
import com.netk.mvolatrack.history.HistoryTransaction;
import com.netk.mvolatrack.history.TodayHistorySummary;
import com.netk.mvolatrack.verification.BalanceVerificationDialog;
import com.netk.mvolatrack.verification.TransactionBalanceVerification;
import com.netk.mvolatrack.verification.VerificationStatusCell;
import com.netk.mvolatrack.verification.VerificationStatusPresentation;
import com.netk.mvolatrack.notification.BonusNotificationsActivity;
import com.netk.mvolatrack.repository.BonusNotificationRepository;
import com.netk.mvolatrack.overlay.TransactionOverlayCoordinator;
import com.netk.mvolatrack.notification.NotificationHelper;
import com.netk.mvolatrack.notification.ExportNotificationHelper;
import com.netk.mvolatrack.export.ExportTransaction;
import com.netk.mvolatrack.export.PdfExporter;
import com.netk.mvolatrack.export.XlsxExporter;
import com.netk.mvolatrack.sms.ClientNumberNormalizer;
import com.netk.mvolatrack.sms.MvolaMessageParser;
import com.netk.mvolatrack.security.AppLockManager;
import com.netk.mvolatrack.security.SecurityActivity;
import com.netk.mvolatrack.activation.ActivationActivity;
import com.netk.mvolatrack.activation.ActivationStore;
import com.netk.mvolatrack.activation.ActivationVerifier;
import com.netk.mvolatrack.activation.LicenseDisplay;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MVolaCash";
    private static final String EXPORT_TAG = "MVolaCash_EXPORT_DIAGNOSTIC";
    private static final String EXPORT_ERROR_MESSAGE =
            "Export impossible : une erreur est survenue";
    private static final String STATE_SELECTED_SECTION = "selected_section";
    private static final String STATE_STATISTICS_YEAR = "statistics_year";
    private static final String STATE_STATISTICS_MONTH = "statistics_month";
    private static final String STATE_STATISTICS_TAB = "statistics_tab";
    private static final String STATE_HOURLY_DAY = "hourly_day";
    private static final String STATE_TABLE_ZOOM = "table_zoom";
    private static final String STATE_CLIENT_SENDER = "client_sender";
    private static final String STATE_CLIENT_QUERY = "client_query";
    private static final String STATE_CLIENT_FILTER = "client_filter";
    private static final String STATE_SHOW_ALL_HISTORY = "show_all_history";
    private static final int SECTION_MESSAGES = 0;
    private static final int SECTION_HOME = 1;
    private static final int SECTION_STATISTICS = 2;
    private static final int SECTION_HISTORY = 3;
    private static final int SECTION_CLIENT = 4;
    private static final int STATISTICS_TAB_BONUS = 0;
    private static final int STATISTICS_TAB_USER = 1;
    private static final String INSTALLATION_PREFERENCES = "installation_restore";
    private static final String INSTALLATION_STATE = "state";
    private static final String STATE_CONFIGURED = "configured";
    private static final String STATE_RESTORE_PENDING = "restore_pending";
    private static final String BACKGROUND_PREFERENCES = "background_execution";
    private static final String BACKGROUND_PROMPT_SHOWN = "initial_prompt_shown";
    private static final String STATISTICS_PREFERENCES = "statistics_preferences";
    private static final String ACTIVITY_INTERVAL_MINUTES = "activity_interval_minutes";
    private static final String DEPOSIT_PREFERENCES = "deposit_preferences";
    private static final String LAST_DEPOSIT_RECIPIENT = "last_deposit_recipient";
    private static final String DISPLAY_PREFERENCES = "display_preferences";
    private static final String BALANCE_HIDDEN = "balance_hidden";
    private static final String MESSAGE_FILTER_PREFERENCES = "message_filter_preferences";
    private static final String MESSAGES_FILTER_PERIOD = "messages_filter_period";
    private static final String MESSAGES_FILTER_TYPE = "messages_filter_type";
    private static final String MESSAGES_FILTER_CUSTOM_DATE = "messages_filter_custom_date";
    private TextView permissionText;
    private TextView backgroundExecutionText;
    private TextView emptyText;
    private TextView transactionCountText;
    private TextView balanceTitle;
    private ImageButton balanceVisibilityButton;
    private String visibleBalanceTitle = "0 Ar";
    private boolean balanceHidden;
    private View mainHeader;
    private View mainHeaderDivider;
    private ProgressBar loadingIndicator;
    private View exportProgress;
    private NavigationView navigationView;
    private SmsAdapter adapter;
    private float tableZoom = 1f;
    private LinearLayout transactionTable;
    private LinearLayout smsTableHeader;
    private LinearLayout clientTransactionTable;
    private LinearLayout clientTableHeader;
    private SmsViewModel viewModel;
    private SmsBackupManager backupManager;
    private List<SmsMessage> messages = new ArrayList<>();
    private List<HistoryTransaction> preparedHistory = new ArrayList<>();
    private List<ClientMessageGrouper.ClientGroup> preparedClients = new ArrayList<>();
    private Map<String, TransactionBalanceVerification> messageVerifications =
            Collections.emptyMap();
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
    private TextView historyTodayIncomingAmount;
    private TextView historyTodayOutgoingAmount;
    private TextView viewAllTransactions;
    private boolean showAllHistory;
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
    private TextView statisticsUserTab;
    private TextView statisticsBonusTab;
    private TextView bonusMonthText;
    private TextView userMonthLabel;
    private TextView userYearLabel;
    private TextView bonusMonthLabel;
    private TextView bonusYearLabel;
    private StatisticsChartView bonusChart;
    private TextView emptyBonusChartText;
    private StatisticsChartView userChart;
    private TextView emptyUserChartText;
    private HourlyActivityChartView hourlyActivityChart;
    private HourlyActivityChartView hourlyBonusChart;
    private TextView emptyHourlyBonusText;
    private TextView hourlyDateText;
    private TextView hourlyNextDay;
    private TextView hourlyActivityTitle;
    private TextView[] activityIntervalChips;
    private TextView[] bonusIntervalChips;
    private Calendar selectedHourlyDay;
    private int activityIntervalMinutes = 60;
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
    private long todayDepositTransactions = 0;
    private long todayCreditTransactions = 0;
    private long todayWithdrawalTransactions = 0;
    private TextView homeLabel;
    private ImageButton homeButton;
    private TextView clientEmptyText;
    private TextView clientCountText;
    private TextView clientDetailTitle;
    private ClientAdapter clientAdapter;
    private ClientMessageAdapter clientMessageAdapter;
    private String selectedClientSender;
    private EditText clientSearchInput;
    private TextView[] clientFilterChips;
    private ClientMessageGrouper.Filter selectedClientFilter = ClientMessageGrouper.Filter.ALL;
    private int selectedSection = SECTION_HOME;
    private SmsDateFilter.Period selectedMessageFilter = SmsDateFilter.Period.ALL;
    private SmsDateFilter.TransactionType selectedMessageType = SmsDateFilter.TransactionType.ALL;
    private Long customFilterDate;
    private EditText messageSearchInput;
    private ImageButton messagesExportButton;
    private TextView periodFilterDropdown;
    private TextView typeFilterDropdown;
    private View depositCard;
    private View creditCard;
    private boolean depositRequestInProgress;
    private boolean launchDepositAfterPermission;
    private boolean depositCallLaunched;
    private String pendingUssdCode;
    private File pendingExportFile;
    private String pendingExportMime;
    private DrawerLayout drawerLayout;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private Future<?> exportTask;
    private boolean exportInProgress;
    private volatile boolean activityDestroyed;
    private long lastResumeReloadAt;

    @Override
    protected void onStart() {
        super.onStart();
        TransactionOverlayCoordinator.get(this).attach(this);
        handleTransactionIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleTransactionIntent(intent);
    }

    private void handleTransactionIntent(Intent intent) {
        if (intent == null || !intent.hasExtra(NotificationHelper.EXTRA_TRANSACTION_ID)) return;
        long id = intent.getLongExtra(NotificationHelper.EXTRA_TRANSACTION_ID, -1L);
        intent.removeExtra(NotificationHelper.EXTRA_TRANSACTION_ID);
        TransactionOverlayCoordinator.get(this).openFromNotification(id);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: detaching transaction overlay");
        TransactionOverlayCoordinator.get(this).detach(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: UI callbacks will be lifecycle-checked");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        activityDestroyed = true;
        if (exportTask != null) exportTask.cancel(true);
        ioExecutor.shutdownNow();
        Log.d(TAG, "onDestroy: background tasks cancelled");
        super.onDestroy();
    }

    private final ActivityResultLauncher<String> backupExportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) exportMessages(uri);
            });
    private final ActivityResultLauncher<Intent> transactionSaveLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null
                        && result.getData().getData() != null && pendingExportFile != null) {
                    copyExportTo(result.getData().getData(), pendingExportFile);
                }
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
        if (!ActivationStore.hasValidActivation(this)) {
            startActivity(new Intent(this, ActivationActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }
        AppLockManager.showLockIfRequired(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        applySystemBarInsets(findViewById(R.id.mainRoot));

        permissionText = findViewById(R.id.permissionText);
        permissionText.setOnClickListener(view -> requestRequiredPermissions());
        backgroundExecutionText = findViewById(R.id.backgroundExecutionText);
        backgroundExecutionManager = new BackgroundExecutionManager(this);
        backgroundExecutionText.setOnClickListener(view -> showBackgroundPermissionDialog(false));
        emptyText = findViewById(R.id.emptyText);
        transactionCountText = findViewById(R.id.transactionCountText);
        balanceTitle = findViewById(R.id.balanceTitle);
        balanceVisibilityButton = findViewById(R.id.balanceVisibilityButton);
        balanceHidden = getSharedPreferences(DISPLAY_PREFERENCES, MODE_PRIVATE)
                .getBoolean(BALANCE_HIDDEN, false);
        balanceVisibilityButton.setOnClickListener(view -> toggleBalanceVisibility());
        View notificationButton = findViewById(R.id.bonusNotificationButton);
        TextView notificationBadge = findViewById(R.id.bonusNotificationBadge);
        BonusNotificationRepository bonusNotifications = new BonusNotificationRepository(this);
        notificationButton.setContentDescription("Notifications Bonus");
        notificationButton.setOnClickListener(view -> {
            try {
                startActivity(new Intent(this, BonusNotificationsActivity.class));
            } catch (RuntimeException error) {
                CrashLogger.recordException(this, error);
                Toast.makeText(this,
                        "Impossible d’ouvrir les notifications. Exportez le rapport diagnostic.",
                        Toast.LENGTH_LONG).show();
            }
        });
        bonusNotifications.observeUnreadCount().observe(this, count -> {
            int unread = count == null ? 0 : count;
            notificationBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
            notificationBadge.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);
            notificationButton.setContentDescription("Notifications Bonus, " + unread + " non lues");
            if (unread > 0) {
                notificationButton.animate().rotation(-8f).setDuration(90).withEndAction(() ->
                        notificationButton.animate().rotation(0f).setDuration(120).start()).start();
            }
        });
        renderBalanceTitle();
        mainHeader = findViewById(R.id.mainHeader);
        mainHeaderDivider = findViewById(R.id.mainHeaderDivider);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        exportProgress = findViewById(R.id.exportProgress);
        configureMessageFilters(savedInstanceState);
        backupManager = new SmsBackupManager(this);
        configureNavigationDrawer();
        messagesSection = findViewById(R.id.messagesSection);
        homeSection = findViewById(R.id.homeSection);
        historySection = findViewById(R.id.historySection);
        statisticsSection = findViewById(R.id.statisticsSection);
        clientSection = findViewById(R.id.clientSection);
        clientListContent = findViewById(R.id.clientListContent);
        clientDetailContent = findViewById(R.id.clientDetailContent);
        clientCountText = findViewById(R.id.clientCountText);
        messagesNavigationItem = findViewById(R.id.bottomMessages);
        clientNavigationItem = findViewById(R.id.bottomClient);
        historyNavigationItem = findViewById(R.id.bottomHistory);
        statisticsNavigationItem = findViewById(R.id.bottomStatistics);
        depositCard = findViewById(R.id.cardDeposit);
        depositCard.setOnClickListener(view -> showRecipientDialog("", "",
                RecipientWorkflow.DEPOSIT));
        findViewById(R.id.cardOffer).setOnClickListener(view -> showRecipientDialog("", "",
                RecipientWorkflow.OFFER));
        creditCard = findViewById(R.id.cardCredit);
        creditCard.setOnClickListener(view -> showRecipientDialog("", "",
                RecipientWorkflow.CREDIT));
        statisticsSubtitle = findViewById(R.id.statisticsSubtitle);
        statisticsMonthText = findViewById(R.id.statisticsMonthText);
        bonusMonthText = findViewById(R.id.bonusMonthText);
        userMonthLabel = findViewById(R.id.userMonthLabel);
        userYearLabel = findViewById(R.id.userYearLabel);
        bonusMonthLabel = findViewById(R.id.bonusMonthLabel);
        bonusYearLabel = findViewById(R.id.bonusYearLabel);
        bonusChart = findViewById(R.id.bonusChart);
        emptyBonusChartText = findViewById(R.id.emptyBonusChartText);
        userChart = findViewById(R.id.userChart);
        emptyUserChartText = findViewById(R.id.emptyUserChartText);
        hourlyActivityChart = findViewById(R.id.hourlyActivityChart);
        hourlyBonusChart = findViewById(R.id.hourlyBonusChart);
        emptyHourlyBonusText = findViewById(R.id.emptyHourlyBonusText);
        hourlyDateText = findViewById(R.id.hourlyDateText);
        hourlyNextDay = findViewById(R.id.hourlyNextDay);
        hourlyActivityTitle = findViewById(R.id.hourlyActivityTitle);
        activityIntervalChips = new TextView[]{findViewById(R.id.activityInterval15),
                findViewById(R.id.activityInterval30), findViewById(R.id.activityInterval60)};
        bonusIntervalChips = new TextView[]{findViewById(R.id.bonusInterval15),
                findViewById(R.id.bonusInterval30), findViewById(R.id.bonusInterval60)};
        int savedInterval = getSharedPreferences(STATISTICS_PREFERENCES, MODE_PRIVATE)
                .getInt(ACTIVITY_INTERVAL_MINUTES, 60);
        activityIntervalMinutes = savedInterval == 15 || savedInterval == 30 ? savedInterval : 60;
        userStatisticsContent = findViewById(R.id.userStatisticsContent);
        bonusStatisticsContent = findViewById(R.id.bonusStatisticsContent);
        statisticsUserTab = findViewById(R.id.statisticsUserTab);
        statisticsBonusTab = findViewById(R.id.statisticsBonusTab);
        statisticsMonth = Calendar.getInstance();
        statisticsMonth.set(Calendar.DAY_OF_MONTH, 1);
        selectedHourlyDay = Calendar.getInstance();
        if (savedInstanceState != null) {
            statisticsMonth.set(Calendar.YEAR, savedInstanceState.getInt(STATE_STATISTICS_YEAR,
                    statisticsMonth.get(Calendar.YEAR)));
            statisticsMonth.set(Calendar.MONTH, savedInstanceState.getInt(STATE_STATISTICS_MONTH,
                    statisticsMonth.get(Calendar.MONTH)));
            int restoredTab = savedInstanceState.getInt(
                    STATE_STATISTICS_TAB, STATISTICS_TAB_BONUS);
            selectedStatisticsTab = restoredTab == STATISTICS_TAB_USER
                    ? STATISTICS_TAB_USER : STATISTICS_TAB_BONUS;
            selectedHourlyDay.setTimeInMillis(savedInstanceState.getLong(STATE_HOURLY_DAY,
                    selectedHourlyDay.getTimeInMillis()));
            Calendar today = Calendar.getInstance();
            if (isBeforeLocalDay(today, selectedHourlyDay)) selectedHourlyDay = today;
        }
        findViewById(R.id.statisticsPreviousMonth).setOnClickListener(view -> changeMonth(-1));
        findViewById(R.id.statisticsNextMonth).setOnClickListener(view -> changeMonth(1));
        findViewById(R.id.bonusPreviousMonth).setOnClickListener(view -> changeMonth(-1));
        findViewById(R.id.bonusNextMonth).setOnClickListener(view -> changeMonth(1));
        findViewById(R.id.hourlyPreviousDay).setOnClickListener(view -> changeHourlyDay(-1));
        findViewById(R.id.bonusHourlyPreviousDay).setOnClickListener(
                view -> changeHourlyDay(-1));
        hourlyNextDay.setOnClickListener(view -> changeHourlyDay(1));
        findViewById(R.id.bonusHourlyNextDay).setOnClickListener(view -> changeHourlyDay(1));
        activityIntervalChips[0].setOnClickListener(view -> selectActivityInterval(15));
        activityIntervalChips[1].setOnClickListener(view -> selectActivityInterval(30));
        activityIntervalChips[2].setOnClickListener(view -> selectActivityInterval(60));
        bonusIntervalChips[0].setOnClickListener(view -> selectActivityInterval(15));
        bonusIntervalChips[1].setOnClickListener(view -> selectActivityInterval(30));
        bonusIntervalChips[2].setOnClickListener(view -> selectActivityInterval(60));
        statisticsBonusTab.setOnClickListener(view -> selectStatisticsTab(STATISTICS_TAB_BONUS));
        statisticsUserTab.setOnClickListener(view -> selectStatisticsTab(STATISTICS_TAB_USER));
        selectStatisticsTab(selectedStatisticsTab);
        renderUserValues();
        renderBonusValues();
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
        findViewById(R.id.clientDetailOverflowButton).setOnClickListener(
                this::showClientDetailOverflowMenu);
        homeButton.setOnClickListener(view -> showSection(SECTION_HOME));
        View.OnClickListener showHistory = view -> showSection(SECTION_HISTORY);
        historyNavigationItem.setOnClickListener(showHistory);
        findViewById(R.id.balanceBlock).setOnClickListener(showHistory);
        statisticsNavigationItem.setOnClickListener(view -> {
            selectStatisticsTab(STATISTICS_TAB_BONUS);
            showSection(SECTION_STATISTICS);
        });
        showSection(savedInstanceState == null ? SECTION_HOME
                : savedInstanceState.getInt(STATE_SELECTED_SECTION, SECTION_HOME));
        RecyclerView list = findViewById(R.id.transactionsList);
        if (savedInstanceState != null) {
            tableZoom = clampTableZoom(savedInstanceState.getFloat(STATE_TABLE_ZOOM, 1f));
        }
        transactionTable = findViewById(R.id.transactionTable);
        smsTableHeader = findViewById(R.id.smsTableHeader);
        adapter = new SmsAdapter(tableZoom);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        ZoomableTableScrollView tableScroll = findViewById(R.id.transactionTableScroll);
        tableScroll.setZoomListener(new ZoomableTableScrollView.ZoomListener() {
            @Override public void onZoom(float scaleFactor) {
                setTableZoom(tableZoom * scaleFactor);
            }

            @Override public void onResetZoom() {
                setTableZoom(1f);
            }
        });
        applyTableZoom();
        RecyclerView clientList = findViewById(R.id.clientList);
        clientAdapter = new ClientAdapter(this::showClientDetail);
        clientList.setLayoutManager(new LinearLayoutManager(this));
        clientList.setAdapter(clientAdapter);
        RecyclerView clientMessages = findViewById(R.id.clientMessageList);
        clientMessageAdapter = new ClientMessageAdapter();
        clientMessages.setLayoutManager(new LinearLayoutManager(this));
        clientMessages.setAdapter(clientMessageAdapter);
        clientTransactionTable = findViewById(R.id.clientTransactionTable);
        clientTableHeader = findViewById(R.id.clientTableHeader);
        ZoomableTableScrollView clientTableScroll = findViewById(R.id.clientTableScroll);
        clientTableScroll.setZoomListener(new ZoomableTableScrollView.ZoomListener() {
            @Override public void onZoom(float scaleFactor) {
                setTableZoom(tableZoom * scaleFactor);
            }

            @Override public void onResetZoom() {
                setTableZoom(1f);
            }
        });
        applyClientTableZoom();
        historyList = findViewById(R.id.historyList);
        historyEmptyState = findViewById(R.id.historyEmptyState);
        historyTodayIncomingAmount = findViewById(R.id.historyTodayIncomingAmount);
        historyTodayOutgoingAmount = findViewById(R.id.historyTodayOutgoingAmount);
        viewAllTransactions = findViewById(R.id.viewAllTransactions);
        showAllHistory = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_SHOW_ALL_HISTORY, false);
        historyAdapter = new HistoryAdapter();
        historyList.setLayoutManager(new LinearLayoutManager(this));
        historyList.setAdapter(historyAdapter);
        viewAllTransactions.setOnClickListener(view -> {
            showAllHistory = true;
            renderHistory();
        });

        viewModel = new ViewModelProvider(this).get(SmsViewModel.class);
        viewModel.getGlobalBalanceTitle().observe(this, title -> {
            visibleBalanceTitle = title == null ? "0 Ar" : title;
            renderBalanceTitle();
        });
        viewModel.getMessages().observe(this, storedMessages -> {
            prepareMessagesAsync(storedMessages, false);
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

    private void toggleBalanceVisibility() {
        balanceHidden = !balanceHidden;
        getSharedPreferences(DISPLAY_PREFERENCES, MODE_PRIVATE).edit()
                .putBoolean(BALANCE_HIDDEN, balanceHidden)
                .apply();
        renderBalanceTitle();
    }

    private void renderBalanceTitle() {
        balanceTitle.setText(balanceHidden ? "****" : visibleBalanceTitle);
        renderHistorySummaryAmounts();
        balanceVisibilityButton.setImageResource(balanceHidden
                ? R.drawable.ic_visibility_off_24 : R.drawable.ic_visibility_24);
        balanceVisibilityButton.setContentDescription(getString(balanceHidden
                ? R.string.show_balance : R.string.hide_balance));
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

    private enum RecipientWorkflow { DEPOSIT, CREDIT, OFFER }

    /** Shared recipient entry and validation step for every home-screen USSD workflow. */
    private void showRecipientDialog(String recipientValue, String amountValue,
                                     RecipientWorkflow workflow) {
        EditText input = depositInput(InputType.TYPE_CLASS_PHONE,
                "Numéro de téléphone", DepositUssd.formatRecipientInput(recipientValue));
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
                .setTitle("Entrer numéro destinataire")
                .setView(recipientRow)
                .setNegativeButton("ANNULER", (ignored, which) -> {
                    if (workflow == RecipientWorkflow.DEPOSIT) clearDepositWorkflow();
                })
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
                    if (workflow == RecipientWorkflow.OFFER) {
                        hideKeyboard(input);
                    }
                    dialog.dismiss();
                    if (workflow == RecipientWorkflow.DEPOSIT) {
                        showAmountDialog(recipientNumber, amountValue);
                    } else if (workflow == RecipientWorkflow.CREDIT) {
                        showCreditAmountDialog(recipientNumber, amountValue);
                    } else {
                        showOfferConfirmation(recipientNumber);
                    }
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
                        input.setError("Montant compris entre 100 et 2 000 000 Ar");
                        return;
                    }
                    dialog.dismiss();
                    showWithdrawalFeeDialog(recipientNumber, originalAmount);
                }));
        dialog.show();
        focusAndShowNumericKeyboard(dialog, input);
    }

    private void showOfferConfirmation(String recipientNumber) {
        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        summary.setPadding(margin, margin / 2, margin, 0);
        addConfirmationField(summary, "Numéro destinataire",
                DepositUssd.formatRecipientNumber(recipientNumber), false, false);
        addConfirmationField(summary, "Nom",
                ClientNameLookup.findClientNameByPhone(messages, recipientNumber), true, false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Vérifier l'offre")
                .setView(summary)
                .setNeutralButton("MODIFIER", (ignored, which) -> showRecipientDialog(
                        recipientNumber, "", RecipientWorkflow.OFFER))
                .setNegativeButton("ANNULER", null)
                .setPositiveButton("ENVOYER", (ignored, which) ->
                        requestUssdCall(OfferUssd.buildUssdCode(recipientNumber)))
                .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
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
                    hideKeyboard(input);
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
        addConfirmationField(summary, "Nom",
                ClientNameLookup.findClientNameByPhone(messages, recipientNumber), true, false);
        addConfirmationField(summary, "Montant du crédit",
                DepositUssd.formatAmount(String.valueOf(amount)), true, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Vérifier le crédit")
                .setView(summary)
                .setNeutralButton("MODIFIER", (ignored, which) ->
                        showRecipientDialog(recipientNumber, String.valueOf(amount),
                                RecipientWorkflow.CREDIT))
                .setNegativeButton("ANNULER", null)
                .setPositiveButton("ENVOYER", (ignored, which) ->
                        requestUssdCall(CreditUssd.buildUssdCode(recipientNumber, amount)))
                .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
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

    /** Clears input focus and explicitly closes the IME before showing a confirmation. */
    private void hideKeyboard(EditText input) {
        input.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(
                INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
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
                            choiceInput, dialog, errorText, transitionStarted);
                }
            }
        });
        dialog.show();
        focusAndShowNumericKeyboard(dialog, choiceInput);
    }

    private void handleWithdrawalFeeChoice(String recipientNumber, long originalAmount,
                                           boolean includeFee, EditText choiceInput,
                                           AlertDialog dialog,
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
        hideKeyboard(choiceInput);
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
        addConfirmationField(summary, "Nom",
                ClientNameLookup.findClientNameByPhone(messages, recipientNumber), true, false);
        addConfirmationField(summary, "Montant du dépôt",
                DepositUssd.formatAmount(String.valueOf(originalAmount)), true, false);
        addConfirmationField(summary, "Frais", DepositUssd.formatAmount(String.valueOf(
                includeWithdrawalFee ? withdrawalFee : 0L)), true, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Vérifier le dépôt")
                .setView(summary)
                .setNegativeButton("ANNULER", (ignored, which) -> clearDepositWorkflow())
                .setNeutralButton("MODIFIER", (ignored, which) ->
                        showRecipientDialog(recipientNumber, String.valueOf(originalAmount),
                                RecipientWorkflow.DEPOSIT))
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
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
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
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(initialLeft + systemBars.left, initialTop + systemBars.top,
                    initialRight + systemBars.right, 0);
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
        mainHeader.setVisibility(clientSelected ? View.GONE : View.VISIBLE);
        mainHeaderDivider.setVisibility(clientSelected ? View.GONE : View.VISIBLE);

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
        if (messagesSelected) {
            restoreMessageFilters();
            if (messageSearchInput != null) messageSearchInput.setText("");
            updateFilterChips();
            if (adapter != null) renderState();
        }
        if (statisticsSelected) renderStatistics();
        if (clientSelected) renderClients();
        if (historySelected) renderHistory();
    }

    private void renderHistory() {
        if (historyAdapter == null) return;
        List<HistoryTransaction> transactions = preparedHistory == null
                ? Collections.emptyList() : preparedHistory;
        historyAdapter.submitList(showAllHistory
                ? transactions : HistoryTransaction.latest(transactions, 3));
        viewAllTransactions.setVisibility(showAllHistory ? View.GONE : View.VISIBLE);
        TodayHistorySummary summary = TodayHistorySummary.calculate(transactions,
                System.currentTimeMillis(), java.util.TimeZone.getDefault());
        historyTodayIncomingAmount.setTag(summary.incoming);
        historyTodayOutgoingAmount.setTag(summary.outgoing);
        renderHistorySummaryAmounts();
        boolean empty = transactions.isEmpty();
        historyList.setVisibility(empty ? View.GONE : View.VISIBLE);
        historyEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void renderHistorySummaryAmounts() {
        if (historyTodayIncomingAmount == null || historyTodayOutgoingAmount == null) return;
        if (balanceHidden) {
            historyTodayIncomingAmount.setText("****");
            historyTodayOutgoingAmount.setText("****");
            return;
        }
        historyTodayIncomingAmount.setText(formatAriary(summaryAmount(historyTodayIncomingAmount)));
        historyTodayOutgoingAmount.setText(formatAriary(summaryAmount(historyTodayOutgoingAmount)));
    }

    private static long summaryAmount(TextView view) {
        Object value = view.getTag();
        return value instanceof Long ? (Long) value : 0L;
    }

    private void renderClients() {
        if (clientAdapter == null || clientMessageAdapter == null) return;
        List<ClientMessageGrouper.ClientGroup> allClients = preparedClients == null
                ? Collections.emptyList() : preparedClients;
        String query = clientSearchInput == null ? "" : clientSearchInput.getText().toString();
        List<ClientMessageGrouper.ClientGroup> visibleClients = ClientMessageGrouper.filter(
                allClients, query, selectedClientFilter);
        clientAdapter.submitList(visibleClients);
        int clientCount = visibleClients.size();
        String clientCountLabel = clientCount + (clientCount > 1 ? " clients" : " client");
        SpannableString styledClientCount = new SpannableString(clientCountLabel);
        styledClientCount.setSpan(new StyleSpan(Typeface.BOLD), 0,
                Integer.toString(clientCount).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        clientCountText.setText(styledClientCount);
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
                clientMessageAdapter.submitList(clientMessagesWithOriginalNumbers(client.sender),
                        messageVerifications);
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
        if (drawerLayout != null && drawerLayout.isDrawerOpen(android.view.Gravity.START)) {
            drawerLayout.closeDrawer(android.view.Gravity.START);
        } else if (selectedSection == SECTION_CLIENT && selectedClientSender != null) {
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
        outState.putLong(STATE_HOURLY_DAY, selectedHourlyDay.getTimeInMillis());
        outState.putFloat(STATE_TABLE_ZOOM, tableZoom);
        if (selectedClientSender != null) outState.putString(STATE_CLIENT_SENDER, selectedClientSender);
        if (clientSearchInput != null) {
            outState.putString(STATE_CLIENT_QUERY, clientSearchInput.getText().toString());
        }
        outState.putString(STATE_CLIENT_FILTER, selectedClientFilter.name());
        outState.putBoolean(STATE_SHOW_ALL_HISTORY, showAllHistory);
        super.onSaveInstanceState(outState);
    }

    private void configureMessageFilters(Bundle savedInstanceState) {
        messageSearchInput = findViewById(R.id.messageSearchInput);
        messagesExportButton = findViewById(R.id.messagesExportButton);
        periodFilterDropdown = findViewById(R.id.periodFilterDropdown);
        typeFilterDropdown = findViewById(R.id.typeFilterDropdown);
        restoreMessageFilters();
        periodFilterDropdown.setOnClickListener(this::showPeriodFilterMenu);
        typeFilterDropdown.setOnClickListener(this::showTypeFilterMenu);
        messageSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count,
                    int after) {}

            @Override public void onTextChanged(CharSequence value, int start, int before,
                    int count) {
                renderState();
            }

            @Override public void afterTextChanged(Editable value) {}
        });
        messagesExportButton.setOnClickListener(this::showMessagesExportMenu);
        updateFilterChips();
    }

    private void showMessagesExportMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        boolean canExport = !exportInProgress && adapter != null && adapter.getItemCount() > 0;
        menu.getMenu().add("Exporter en PDF").setEnabled(canExport)
                .setOnMenuItemClickListener(item -> {
                    openMessagesExport(false);
                    return true;
                });
        menu.getMenu().add("Exporter en Excel").setEnabled(canExport)
                .setOnMenuItemClickListener(item -> {
                    openMessagesExport(true);
                    return true;
                });
        menu.show();
    }

    private void showPeriodFilterMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        String[] labels = {"Toutes les périodes", "Aujourd’hui", "Hier",
                "7 derniers jours", "30 derniers jours", "Date personnalisée"};
        SmsDateFilter.Period[] periods = SmsDateFilter.Period.values();
        for (int index = 0; index < labels.length; index++) {
            menu.getMenu().add(0, index, index, labels[index])
                    .setCheckable(true).setChecked(selectedMessageFilter == periods[index]);
        }
        menu.setOnMenuItemClickListener(item -> {
            SmsDateFilter.Period period = periods[item.getItemId()];
            if (period == SmsDateFilter.Period.CUSTOM_DATE) showDateFilterPicker();
            else selectMessageFilter(period);
            return true;
        });
        menu.show();
    }

    private void showTypeFilterMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        String[] labels = {"Tous", "Dépôt", "Retrait", "Crédit"};
        SmsDateFilter.TransactionType[] types = SmsDateFilter.TransactionType.values();
        for (int index = 0; index < labels.length; index++) {
            menu.getMenu().add(0, index, index, labels[index])
                    .setCheckable(true).setChecked(selectedMessageType == types[index]);
        }
        menu.setOnMenuItemClickListener(item -> {
            selectMessageType(types[item.getItemId()]);
            return true;
        });
        menu.show();
    }

    private void selectMessageFilter(SmsDateFilter.Period period) {
        selectedMessageFilter = period;
        if (period != SmsDateFilter.Period.CUSTOM_DATE) customFilterDate = null;
        persistMessagePeriodFilter();
        updateFilterChips();
        renderState();
    }

    private void selectMessageType(SmsDateFilter.TransactionType type) {
        selectedMessageType = type;
        getSharedPreferences(MESSAGE_FILTER_PREFERENCES, MODE_PRIVATE).edit()
                .putString(MESSAGES_FILTER_TYPE, type.name())
                .apply();
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
            persistMessagePeriodFilter();
            updateFilterChips();
            renderState();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void persistMessagePeriodFilter() {
        SharedPreferences.Editor editor = getSharedPreferences(
                MESSAGE_FILTER_PREFERENCES, MODE_PRIVATE).edit()
                .putString(MESSAGES_FILTER_PERIOD, selectedMessageFilter.name());
        if (customFilterDate == null) editor.remove(MESSAGES_FILTER_CUSTOM_DATE);
        else editor.putLong(MESSAGES_FILTER_CUSTOM_DATE, customFilterDate);
        editor.apply();
    }

    private void restoreMessageFilters() {
        SharedPreferences preferences = getSharedPreferences(
                MESSAGE_FILTER_PREFERENCES, MODE_PRIVATE);
        try {
            selectedMessageFilter = SmsDateFilter.Period.valueOf(preferences.getString(
                    MESSAGES_FILTER_PERIOD, SmsDateFilter.Period.ALL.name()));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            selectedMessageFilter = SmsDateFilter.Period.ALL;
        }
        try {
            selectedMessageType = SmsDateFilter.TransactionType.valueOf(preferences.getString(
                    MESSAGES_FILTER_TYPE, SmsDateFilter.TransactionType.ALL.name()));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            selectedMessageType = SmsDateFilter.TransactionType.ALL;
        }
        customFilterDate = preferences.contains(MESSAGES_FILTER_CUSTOM_DATE)
                ? preferences.getLong(MESSAGES_FILTER_CUSTOM_DATE, 0L) : null;
        if (selectedMessageFilter == SmsDateFilter.Period.CUSTOM_DATE
                && customFilterDate == null) {
            selectedMessageFilter = SmsDateFilter.Period.ALL;
        }
    }

    private void updateFilterChips() {
        if (periodFilterDropdown == null || typeFilterDropdown == null) return;
        String[] periodLabels = {"Toutes les périodes", "Aujourd’hui", "Hier",
                "7 derniers jours", "30 derniers jours", "Date personnalisée"};
        String periodLabel = periodLabels[selectedMessageFilter.ordinal()];
        if (selectedMessageFilter == SmsDateFilter.Period.CUSTOM_DATE
                && customFilterDate != null) {
            periodLabel = new SimpleDateFormat("dd/MM/yy", Locale.FRENCH)
                    .format(customFilterDate);
        }
        periodFilterDropdown.setText(periodLabel);
        String[] typeLabels = {"Tous", "Dépôt", "Retrait", "Crédit"};
        typeFilterDropdown.setText(typeLabels[selectedMessageType.ordinal()]);
    }

    private void renderStatistics() {
        updateTransactionStatistics();
        String monthLabel = new SimpleDateFormat("MMMM yyyy", Locale.FRENCH)
                .format(statisticsMonth.getTime());
        statisticsMonthText.setText(monthLabel.substring(0, 1).toUpperCase(Locale.FRENCH)
                + monthLabel.substring(1));
        bonusMonthText.setText(statisticsMonthText.getText());
        if (selectedStatisticsTab == STATISTICS_TAB_BONUS) renderBonusValues();
        else renderUserValues();
    }

    private void updateTransactionStatistics() {
        SmsStatistics.TransactionSummary summary = SmsStatistics.summarize(messages,
                System.currentTimeMillis(), statisticsMonth.get(Calendar.YEAR),
                statisticsMonth.get(Calendar.MONTH), java.util.TimeZone.getDefault());
        // In the current business model, every parsed transaction represents one user.
        todayUsers = summary.todayTransactions;
        yesterdayUsers = summary.yesterdayTransactions;
        weekUsers = summary.weekTransactions;
        monthUsers = summary.monthTransactions;
        yearUsers = summary.yearTransactions;
        monthlyUserBars = summary.monthlyTransactions;
        todayBonus = summary.todayBonus;
        yesterdayBonus = summary.yesterdayBonus;
        weekBonus = summary.weekBonus;
        monthBonus = summary.monthBonus;
        yearBonus = summary.yearBonus;
        monthlyBonusBars = summary.monthlyBonus;
        todayDepositTransactions = summary.todayDepositTransactions;
        todayCreditTransactions = summary.todayCreditTransactions;
        todayWithdrawalTransactions = summary.todayWithdrawalTransactions;
    }

    private void selectStatisticsTab(int tab) {
        selectedStatisticsTab = tab;
        boolean showBonus = tab == STATISTICS_TAB_BONUS;
        statisticsBonusTab.setSelected(showBonus);
        statisticsUserTab.setSelected(!showBonus);
        bonusStatisticsContent.setVisibility(showBonus ? View.VISIBLE : View.GONE);
        userStatisticsContent.setVisibility(showBonus ? View.GONE : View.VISIBLE);
        if (showBonus) {
            statisticsSubtitle.setText("Suivi des bonus");
            renderBonusValues();
        } else {
            statisticsSubtitle.setText("Nombre d’utilisateurs");
            renderUserValues();
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
        renderHourlyActivity();
    }

    private void renderHourlyActivity() {
        hourlyDateText.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
                .format(selectedHourlyDay.getTime()));
        hourlyActivityTitle.setText(activityIntervalMinutes == 60
                ? "Activité par heure" : "Activité par intervalle");
        int[] intervals = {15, 30, 60};
        for (int index = 0; index < activityIntervalChips.length; index++) {
            activityIntervalChips[index].setSelected(activityIntervalMinutes == intervals[index]);
            bonusIntervalChips[index].setSelected(activityIntervalMinutes == intervals[index]);
        }
        hourlyActivityChart.setData(SmsStatistics.intervalActivity(messages,
                selectedHourlyDay.getTimeInMillis(), java.util.TimeZone.getDefault(),
                activityIntervalMinutes), activityIntervalMinutes);
        int[] bonusAmounts = SmsStatistics.intervalBonus(messages,
                selectedHourlyDay.getTimeInMillis(), java.util.TimeZone.getDefault(),
                activityIntervalMinutes);
        hourlyBonusChart.setData(bonusAmounts, activityIntervalMinutes, "Bonus", "ariary",
                "ariary");
        boolean hasBonus = false;
        for (int amount : bonusAmounts) hasBonus |= amount > 0;
        hourlyBonusChart.setVisibility(hasBonus ? View.VISIBLE : View.GONE);
        emptyHourlyBonusText.setVisibility(hasBonus ? View.GONE : View.VISIBLE);
        Calendar today = Calendar.getInstance();
        boolean canGoForward = isBeforeLocalDay(selectedHourlyDay, today);
        hourlyNextDay.setEnabled(canGoForward);
        hourlyNextDay.setAlpha(canGoForward ? 1f : 0.35f);
        TextView bonusNextDay = findViewById(R.id.bonusHourlyNextDay);
        bonusNextDay.setEnabled(canGoForward);
        bonusNextDay.setAlpha(canGoForward ? 1f : 0.35f);
        ((TextView) findViewById(R.id.bonusHourlyDateText)).setText(hourlyDateText.getText());
    }

    private void selectActivityInterval(int minutes) {
        if (activityIntervalMinutes == minutes) return;
        activityIntervalMinutes = minutes;
        getSharedPreferences(STATISTICS_PREFERENCES, MODE_PRIVATE).edit()
                .putInt(ACTIVITY_INTERVAL_MINUTES, minutes).apply();
        renderHourlyActivity();
    }

    private void changeHourlyDay(int offset) {
        if (offset > 0 && !isBeforeLocalDay(selectedHourlyDay, Calendar.getInstance())) return;
        selectedHourlyDay.add(Calendar.DAY_OF_MONTH, offset);
        renderHourlyActivity();
    }

    private static boolean isBeforeLocalDay(Calendar first, Calendar second) {
        if (first.get(Calendar.YEAR) != second.get(Calendar.YEAR)) {
            return first.get(Calendar.YEAR) < second.get(Calendar.YEAR);
        }
        return first.get(Calendar.DAY_OF_YEAR) < second.get(Calendar.DAY_OF_YEAR);
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
        renderHourlyActivity();
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

    private void configureNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        int maximumWidth = Math.round(360 * getResources().getDisplayMetrics().density);
        int preferredWidth = Math.round(getResources().getDisplayMetrics().widthPixels * 0.84f);
        ViewGroup.LayoutParams layoutParams = navigationView.getLayoutParams();
        layoutParams.width = Math.min(preferredWidth, maximumWidth);
        navigationView.setLayoutParams(layoutParams);
        drawerLayout.setScrimColor(0x66000000);

        View header = navigationView.getHeaderView(0);
        ((TextView) header.findViewById(R.id.drawerVersion))
                .setText("Version " + applicationVersionName());
        findViewById(R.id.drawerButton).setOnClickListener(view ->
                drawerLayout.openDrawer(android.view.Gravity.START));
        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(android.view.Gravity.START);
            int id = item.getItemId();
            if (id == R.id.nav_settings) showSettingsPage();
            else if (id == R.id.nav_security)
                startActivity(new Intent(this, SecurityActivity.class));
            else if (id == R.id.nav_export_json) launchExport();
            else if (id == R.id.nav_import_json) launchImport();
            else if (id == R.id.nav_permissions) showPermissionStatusPage();
            else if (id == R.id.nav_help) showHelpPage();
            else if (id == R.id.nav_about) showAboutDialog();
            return true;
        });
    }

    private void openMessagesExport(boolean excel) {
        Log.i(EXPORT_TAG, "Clic utilisateur : Export " + (excel ? "Excel" : "PDF"));
        if (exportInProgress || adapter == null || adapter.getItemCount() == 0) return;
        try {
            generateTransactionExport(excel);
        } catch (RuntimeException error) {
            logExportException("MainActivity", "openMessagesExport", error);
            showExportFailure(() -> openMessagesExport(excel));
        }
    }

    private String applicationVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "—" : info.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "—";
        }
    }

    private void showPlaceholderPage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void showSettingsPage() {
        new AlertDialog.Builder(this)
                .setTitle("Paramètres")
                .setItems(new String[]{"Exporter le rapport diagnostic"}, (dialog, which) ->
                        shareDiagnosticReport())
                .setNegativeButton("Fermer", null)
                .show();
    }

    private void shareDiagnosticReport() {
        File report = CrashLogger.getReportFile(this);
        if (report == null || !report.isFile()) {
            Toast.makeText(this, "Aucun rapport diagnostic disponible.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", report);
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_SUBJECT, CrashLogger.FILE_NAME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Exporter le rapport diagnostic"));
        } catch (RuntimeException error) {
            CrashLogger.recordException(this, error);
            Toast.makeText(this, "Impossible de partager le rapport diagnostic.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showHelpPage() {
        new AlertDialog.Builder(this)
                .setTitle("Aide")
                .setMessage("Messages\nConsultation et filtrage des transactions\n\n"
                        + "Clients\nTransactions regroupées par numéro\n\n"
                        + "Historique\nHistorique chronologique\n\n"
                        + "Statistiques\nBonus et utilisateurs\n\n"
                        + "Solde\nEye ON/OFF permet de masquer le montant\n\n"
                        + "Export\nPDF et Excel\n\n"
                        + "Sauvegarde des messages\nPermet de conserver une copie de vos messages.\n\n"
                        + "Restauration des messages\nPermet de récupérer une sauvegarde précédente.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAboutDialog() {
        ActivationVerifier.Verification status = ActivationStore.status(this);
        String license = "";
        if (status.license != null) {
            license = "\n\nLicence\n" + LicenseDisplay.type(status.license);
            if (status.license.isTemporary()) {
                license += "\n\nExpire le\n" + LicenseDisplay.expiry(status.license);
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("MVolaCash")
                .setMessage("Gestion et suivi des transactions\n\nVersion : "
                        + applicationVersionName() + license)
                .setNeutralButton("Gérer la licence", (dialog, which) ->
                        startActivity(new Intent(this, ActivationActivity.class)
                                .putExtra(ActivationActivity.EXTRA_MANAGE_LICENSE, true)))
                .setPositiveButton("OK", null)
                .show();
    }

    private void showPermissionStatusPage() {
        boolean smsAllowed = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean phoneAllowed = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED;
        boolean notificationsAllowed = NotificationManagerCompat.from(this)
                .areNotificationsEnabled();
        boolean overlayAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || Settings.canDrawOverlays(this);
        String[] entries = new String[]{
                permissionLabel("SMS", smsAllowed),
                permissionLabel("Téléphone (USSD)", phoneAllowed),
                permissionLabel("Notifications", notificationsAllowed),
                permissionLabel("Affichage superposé", overlayAllowed)
        };
        new AlertDialog.Builder(this)
                .setTitle("État des permissions")
                .setItems(entries, (dialog, which) -> {
                    if (which == 3) showOverlayPermissionDialog();
                    else openApplicationPermissionSettings(which == 2);
                })
                .setNegativeButton("Fermer", null)
                .show();
    }

    private String permissionLabel(String name, boolean allowed) {
        return name + "\n" + (allowed ? "✓ Autorisé" : "! Non autorisé");
    }

    private void openApplicationPermissionSettings(boolean notifications) {
        Intent intent;
        if (notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
        }
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException exception) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void showClientDetailOverflowMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Exporter").setOnMenuItemClickListener(item -> {
            showClientTransactionExportDialog();
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
        backupExportLauncher.launch("MVolaCash_" + date + ".json");
    }

    private void showClientTransactionExportDialog() {
        if (selectedClientSender == null || clientMessageAdapter == null) {
            Log.w(TAG, "Client export ignored: client or adapter is unavailable");
            showUserError("L’historique du client n’est pas encore disponible.");
            return;
        }
        // This snapshot is the adapter's exact data source, including its current ordering.
        List<SmsDateFilter.DisplayMessage> clientTransactions = clientMessageAdapter.snapshot();
        String normalizedNumber = ClientNumberNormalizer.normalize(selectedClientSender);
        if (normalizedNumber == null) {
            normalizedNumber = selectedClientSender.replaceAll("\\s+", "");
        }
        String fileNumber = normalizedNumber;
        new AlertDialog.Builder(this)
                .setTitle("Exporter les transactions")
                .setItems(new String[]{"Excel (.xlsx)", "PDF"}, (dialog, which) ->
                        generateTransactionExport(which == 0, clientTransactions,
                                "Tous", "Tous", fileNumber))
                .setNegativeButton("Annuler", null)
                .show();
    }

    /** Snapshots the exact rows currently rendered by Messages; no export-time filtering. */
    private List<SmsDateFilter.DisplayMessage> filteredMessagesForExport() {
        return adapter == null ? Collections.emptyList() : adapter.snapshot();
    }

    private void generateTransactionExport(boolean excel) {
        generateTransactionExport(excel, filteredMessagesForExport(), exportPeriodLabel(),
                selectedMessageType == SmsDateFilter.TransactionType.ALL ? "Tous"
                        : selectedMessageType.parsedType, null);
    }

    /** Shared transaction export pipeline; callers only provide their selected source rows. */
    private void generateTransactionExport(boolean excel,
            List<SmsDateFilter.DisplayMessage> source, String period, String type,
            String fileNumber) {
        generateTransactionExport(excel, source, period, type, fileNumber, null);
    }

    private void generateTransactionExport(boolean excel,
            List<SmsDateFilter.DisplayMessage> source, String period, String type,
            String fileNumber, String fileSuffix) {
        final List<SmsDateFilter.DisplayMessage> safeSource;
        try {
            safeSource = source == null ? Collections.emptyList() : new ArrayList<>(source);
            Log.i(EXPORT_TAG, "Transactions reçues : " + safeSource.size());
        } catch (RuntimeException error) {
            logExportException("MainActivity", "generateTransactionExport/data", error);
            showExportFailure(() -> generateTransactionExport(excel, source, period, type,
                    fileNumber, fileSuffix));
            return;
        }
        if (safeSource.isEmpty()) {
            Log.w(EXPORT_TAG, "Export ignored: transaction selection is null or empty");
            showUserError("Aucune transaction à exporter.");
            return;
        }
        if (exportInProgress) return;
        setExportInProgress(true);
        List<ExportTransaction> rows = new ArrayList<>();
        Map<String, TransactionBalanceVerification> verifications = messageVerifications == null
                ? Collections.emptyMap() : new java.util.HashMap<>(messageVerifications);
        for (SmsDateFilter.DisplayMessage displayed : safeSource) {
            if (displayed == null || displayed.message == null) {
                Log.w(EXPORT_TAG, "Skipping an invalid transaction row");
                continue;
            }
            try {
                SmsMessage message = displayed.message;
                if (message.messageBody == null || message.messageBody.trim().isEmpty()) {
                    Log.w(EXPORT_TAG, "Transaction ignorée : message vide");
                    continue;
                }
                MvolaMessageParser.ParsedTransaction parsed = MvolaMessageParser.parse(
                        message.messageBody, Math.max(0L, message.receivedDate));
                if (parsed == null) {
                    Log.w(EXPORT_TAG, "Transaction ignorée : format non reconnu");
                    continue;
                }
                long date = parsed.transactionAt > 0 ? parsed.transactionAt
                        : Math.max(0L, message.receivedDate);
                TransactionBalanceVerification verification = message.uniqueKey == null
                        ? null : verifications.get(message.uniqueKey);
                ExportTransaction row = new ExportTransaction(date,
                        parsed.type, parsed.clientNumber, parsed.clientName, parsed.amount,
                        parsed.reference, parsed.bonus, parsed.fee, parsed.balance,
                        verification == null ? "Non vérifiable"
                                : verificationStatusText(verification));
                rows.add(row);
                Log.d(EXPORT_TAG, "ExportTransaction créée : index=" + rows.size()
                        + ", référence=" + row.reference);
            } catch (Exception error) {
                logExportException("MainActivity", "generateTransactionExport/parsing", error);
            }
        }
        Log.i(EXPORT_TAG, "Transactions valides : " + rows.size());
        if (rows.isEmpty()) {
            Log.w(EXPORT_TAG, "Export ignored: no selected message could be parsed");
            showUserError("Aucune transaction à exporter.");
            setExportInProgress(false);
            return;
        }
        long exportedAt = System.currentTimeMillis();
        String extension = excel ? ".xlsx" : ".pdf";
        String mime = excel ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf";
        String suffix = fileSuffix == null ? new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                .format(new Date(exportedAt)) : fileSuffix;
        String name = "MVolaCash_" + (fileNumber == null ? "" : fileNumber + "_")
                + suffix + extension;
        showUserMessage("Création du fichier...");
        Runnable retry = () -> generateTransactionExport(excel, safeSource, period, type,
                fileNumber, fileSuffix);
        try {
            exportTask = ioExecutor.submit(() -> {
            try {
                Context appContext = getApplicationContext();
                File directory = new File(appContext.getCacheDir(), "exports");
                if (!directory.exists() && !directory.mkdirs()) throw new IOException("Dossier indisponible");
                File file = new File(directory, name);
                Log.i(EXPORT_TAG, "Début génération " + (excel ? "XLSX" : "PDF"));
                Log.i(EXPORT_TAG, "Chemin du fichier : " + file.getAbsolutePath());
                try (OutputStream output = new FileOutputStream(file)) {
                    if (excel) XlsxExporter.write(output, rows, period, type, exportedAt);
                    else PdfExporter.write(output, rows, period, type, exportedAt);
                }
                Log.i(EXPORT_TAG, "Fichier créé : " + file.getAbsolutePath()
                        + " (" + file.length() + " octets)");
                postToActiveUi(() -> showExportCompleted(file, mime));
            } catch (IOException error) {
                handleExportFailure("generateTransactionExport/io", error, retry);
            } catch (SecurityException error) {
                handleExportFailure("generateTransactionExport/security", error, retry);
            } catch (IllegalArgumentException error) {
                handleExportFailure("generateTransactionExport/argument", error, retry);
            } catch (NullPointerException error) {
                handleExportFailure("generateTransactionExport/null", error, retry);
            } catch (RuntimeException error) {
                if (Thread.currentThread().isInterrupted()) {
                    Log.i(EXPORT_TAG, "Transaction export cancelled because Activity was destroyed");
                    return;
                }
                handleExportFailure("generateTransactionExport/unexpected", error, retry);
            }
            });
        } catch (RuntimeException error) {
            handleExportFailure("generateTransactionExport/executor", error, retry);
        }
    }

    private void setExportInProgress(boolean inProgress) {
        exportInProgress = inProgress;
        if (exportProgress != null) exportProgress.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        updateExportActions();
    }

    private void updateExportActions() {
        if (messagesExportButton == null) return;
        // The overflow remains available so its disabled menu entries explain that no export
        // action is currently possible. Availability is evaluated when the popup is opened.
        messagesExportButton.setVisibility(View.VISIBLE);
    }

    private void handleExportFailure(String detail, Exception error, Runnable retry) {
        if (Thread.currentThread().isInterrupted()) {
            Log.i(EXPORT_TAG, detail + ": task cancelled", error);
            return;
        }
        logExportException("MainActivity", detail, error);
        postToActiveUi(() -> {
            setExportInProgress(false);
            showExportFailure(retry);
        });
    }

    private void logExportException(String className, String method, Throwable error) {
        StackTraceElement location = null;
        for (StackTraceElement element : error.getStackTrace()) {
            if (element.getClassName().startsWith("com.netk.mvolatrack")) {
                location = element;
                break;
            }
        }
        String line = location == null ? "inconnue" : String.valueOf(location.getLineNumber());
        Log.e(EXPORT_TAG, "type=" + error.getClass().getName()
                + ", message=" + String.valueOf(error.getMessage())
                + ", classe=" + className + ", méthode=" + method + ", ligne=" + line, error);
    }

    private void showExportFailure(Runnable retry) {
        if (!canUpdateUi()) return;
        try {
            new MaterialAlertDialogBuilder(this).setMessage(EXPORT_ERROR_MESSAGE)
                    .setPositiveButton("Réessayer", (dialog, which) -> retry.run())
                    .setNegativeButton("Fermer", null).show();
        } catch (WindowManager.BadTokenException | IllegalStateException dialogError) {
            logExportException("MainActivity", "showExportFailure", dialogError);
            showUserError(EXPORT_ERROR_MESSAGE);
        }
    }

    private String verificationStatusText(TransactionBalanceVerification verification) {
        VerificationStatusPresentation presentation = VerificationStatusPresentation.from(verification);
        return getString(presentation.label);
    }

    private String exportPeriodLabel() {
        switch (selectedMessageFilter) {
            case TODAY: return "Aujourd’hui";
            case YESTERDAY: return "Hier";
            case SEVEN_DAYS: return "7 derniers jours";
            case THIRTY_DAYS: return "30 derniers jours";
            case CUSTOM_DATE:
                return customFilterDate == null ? "Date" : new SimpleDateFormat("dd/MM/yyyy",
                        Locale.FRENCH).format(new Date(customFilterDate));
            default: return "Tous";
        }
    }

    private void showExportCompleted(File file, String mime) {
        setExportInProgress(false);
        if (!canUpdateUi() || file == null || !file.isFile()) {
            Log.w(EXPORT_TAG, "Completion dialog skipped: inactive Activity or missing file");
            return;
        }
        pendingExportFile = file;
        pendingExportMime = mime;
        Log.i(EXPORT_TAG, "Fichier prêt pour partage ou enregistrement : " + file.getName());
        try {
            new AlertDialog.Builder(this).setTitle("Export terminé")
                    .setMessage(file.getName())
                    .setPositiveButton("Enregistrer", (dialog, which) -> savePendingExport())
                    .setNeutralButton("Partager", (dialog, which) -> shareExport(file, mime))
                    .setNegativeButton("Fermer", null).show();
        } catch (WindowManager.BadTokenException | IllegalStateException error) {
            Log.e(EXPORT_TAG, "Completion dialog rejected by Activity lifecycle", error);
        }
    }

    private void savePendingExport() {
        if (!canUpdateUi() || pendingExportFile == null || !pendingExportFile.isFile()) {
            Log.w(EXPORT_TAG, "Save ignored: inactive Activity or missing export");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(pendingExportMime)
                .putExtra(Intent.EXTRA_TITLE, pendingExportFile.getName());
        try {
            Log.i(EXPORT_TAG, "Demande d’enregistrement : " + pendingExportFile.getName());
            transactionSaveLauncher.launch(intent);
        } catch (ActivityNotFoundException error) {
            Log.e(EXPORT_TAG, "No document provider can save the export", error);
            showUserError("Aucune application ne permet d’enregistrer ce fichier.");
        } catch (IllegalArgumentException | NullPointerException error) {
            logExportException("MainActivity", "savePendingExport", error);
            showExportFailure(this::savePendingExport);
        } catch (SecurityException error) {
            logExportException("MainActivity", "savePendingExport/security", error);
            showExportFailure(this::savePendingExport);
        } catch (RuntimeException error) {
            logExportException("MainActivity", "savePendingExport/unexpected", error);
            showExportFailure(this::savePendingExport);
        }
    }

    private void copyExportTo(Uri destination, File source) {
        String mime = pendingExportMime;
        Context appContext = getApplicationContext();
        new Thread(() -> {
            try (InputStream input = new FileInputStream(source);
                 OutputStream output = appContext.getContentResolver().openOutputStream(destination)) {
                if (output == null) throw new IOException("Destination indisponible");
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                String fileName = displayName(destination, source.getName());
                postToActiveUi(() -> {
                    Toast.makeText(appContext, "Export terminé", Toast.LENGTH_SHORT).show();
                    ExportNotificationHelper.showExportCompletedNotification(appContext, fileName,
                            destination, mime);
                });
            } catch (IOException error) {
                Log.e(EXPORT_TAG, "I/O failure while saving export", error);
                showTransferError("Enregistrement impossible", error);
            } catch (IllegalArgumentException error) {
                Log.e(EXPORT_TAG, "Invalid export destination", error);
                showTransferError("Enregistrement impossible", error);
            } catch (NullPointerException error) {
                Log.e(EXPORT_TAG, "Unexpected null while saving export", error);
                showTransferError("Enregistrement impossible", error);
            } catch (SecurityException error) {
                logExportException("MainActivity", "copyExportTo/security", error);
                postToActiveUi(() -> showExportFailure(() -> copyExportTo(destination, source)));
            } catch (RuntimeException error) {
                logExportException("MainActivity", "copyExportTo/unexpected", error);
                postToActiveUi(() -> showExportFailure(() -> copyExportTo(destination, source)));
            }
        }, "transaction-export-save").start();
    }

    private void shareExport(File file, String mime) {
        if (!canUpdateUi() || file == null || !file.isFile()) {
            Log.w(EXPORT_TAG, "Share ignored: inactive Activity or missing export");
            return;
        }
        Context appContext = getApplicationContext();
        try {
            Uri uri = FileProvider.getUriForFile(appContext,
                    appContext.getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND).setType(
                    mime == null ? "application/octet-stream" : mime)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Partager l’export"));
            Log.i(EXPORT_TAG, "Intent de partage lancé : " + uri);
        } catch (ActivityNotFoundException error) {
            Log.e(EXPORT_TAG, "No Activity can share the export", error);
            showUserError("Aucune application ne permet de partager ce fichier.");
        } catch (IllegalArgumentException error) {
            Log.e(EXPORT_TAG, "FileProvider rejected export path: " + file, error);
            showUserError("Le fichier d’export ne peut pas être partagé.");
        } catch (NullPointerException error) {
            Log.e(EXPORT_TAG, "Unexpected null while sharing export", error);
            showUserError("Partage impossible.");
        } catch (SecurityException error) {
            logExportException("MainActivity", "shareExport/FileProvider", error);
            showExportFailure(() -> shareExport(file, mime));
        } catch (RuntimeException error) {
            logExportException("MainActivity", "shareExport/unexpected", error);
            showExportFailure(() -> shareExport(file, mime));
        }
    }

    private void refreshMessages(boolean announce) {
        if (viewModel == null) {
            Log.w(TAG, "Reload skipped: ViewModel is unavailable");
            return;
        }
        viewModel.refresh(latest -> prepareMessagesAsync(latest, announce));
    }

    /** Parses and verifies the Room snapshot away from the main thread, then atomically renders it. */
    private void prepareMessagesAsync(List<SmsMessage> latest, boolean announce) {
        List<SmsMessage> snapshot = latest == null
                ? new ArrayList<>() : new ArrayList<>(latest);
        ioExecutor.submit(() -> {
            try {
                Map<String, TransactionBalanceVerification> verifications =
                        HistoryTransaction.verificationsByMessageKey(snapshot);
                List<HistoryTransaction> history = HistoryTransaction.fromMessages(snapshot);
                new BonusNotificationRepository(getApplicationContext()).synchronize(history);
                List<ClientMessageGrouper.ClientGroup> clients =
                        ClientMessageGrouper.group(snapshot);
                postToActiveUi(() -> {
                    messages = snapshot;
                    messageVerifications = verifications;
                    preparedHistory = history;
                    preparedClients = clients;
                    roomLoaded = true;
                    ensureRecyclerAdapters();
                    renderState();
                    renderClients();
                    renderHistory();
                    if (selectedSection == SECTION_STATISTICS) renderStatistics();
                    if (announce) Toast.makeText(this, "Liste actualisée",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (RuntimeException error) {
                Log.e(TAG, "Unable to prepare SMS/history snapshot", error);
                postToActiveUi(() -> showUserError(
                        "Impossible d’actualiser les transactions. Veuillez réessayer."));
            }
        });
    }

    private void ensureRecyclerAdapters() {
        if (adapter == null) {
            RecyclerView list = findViewById(R.id.transactionsList);
            adapter = new SmsAdapter(tableZoom);
            list.setLayoutManager(new LinearLayoutManager(this));
            list.setAdapter(adapter);
            Log.w(TAG, "Messages adapter recreated after lifecycle restoration");
        }
        if (historyAdapter == null && historyList != null) {
            historyAdapter = new HistoryAdapter();
            historyList.setAdapter(historyAdapter);
            Log.w(TAG, "History adapter recreated after lifecycle restoration");
        }
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
                writer.flush();
                String fileName = displayName(destination, "MVolaCash.json");
                runOnUiThread(() -> {
                    Toast.makeText(this, snapshot.size() + " messages exportés",
                            Toast.LENGTH_LONG).show();
                    ExportNotificationHelper.showExportCompletedNotification(this, fileName,
                            destination, "application/json");
                });
            } catch (IOException | JSONException error) {
                showTransferError("Export impossible", error);
            }
        }, "sms-json-export").start();
    }

    /** Returns the provider's real saved name (which the user may change in the SAF picker). */
    private String displayName(Uri uri, String fallback) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) return name;
            }
        } catch (RuntimeException ignored) {
            // A provider is allowed not to expose metadata; retain the actual requested name.
        }
        return fallback;
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
                showTransferError("Restauration impossible : fichier de sauvegarde invalide", error);
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
        Log.e(TAG, prefix, error);
        postToActiveUi(() -> showUserError(prefix + " : " + error.getMessage()));
    }

    private boolean canUpdateUi() {
        return !activityDestroyed && !isFinishing()
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed());
    }

    private void postToActiveUi(Runnable action) {
        runOnUiThread(() -> {
            if (canUpdateUi()) action.run();
            else Log.d(TAG, "Discarding UI callback for a destroyed Activity");
        });
    }

    private void showUserError(String message) {
        if (canUpdateUi()) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showUserMessage(String message) {
        if (canUpdateUi()) Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: restoring screen=" + selectedSection);
        if (!ActivationStore.hasValidActivation(this)) {
            startActivity(new Intent(this, ActivationActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }
        AppLockManager.showLockIfRequired(this);
        refreshPermissionState();
        // Room remains the source of truth. Throttle the asynchronous snapshot read so returning
        // from a picker/settings page never parses hundreds of messages on the UI thread.
        long now = System.currentTimeMillis();
        if (!firstResume && now - lastResumeReloadAt > 2_000L) {
            lastResumeReloadAt = now;
            refreshMessages(false);
        }
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
        if (!canUpdateUi() || adapter == null || permissionText == null
                || loadingIndicator == null || transactionCountText == null
                || emptyText == null) {
            Log.w(TAG, "renderState ignored: Activity views are unavailable");
            return;
        }
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
                messageSearchInput == null ? "" : messageSearchInput.getText().toString(),
                selectedMessageType);
        adapter.submitList(displayed, messageVerifications);
        updateExportActions();
        int displayedCount = displayed.size();
        transactionCountText.setText(displayedCount
                + (displayedCount > 1 ? " transactions" : " transaction"));
        boolean noDisplayedMessages = displayed.isEmpty();
        boolean searching = messageSearchInput != null
                && !messageSearchInput.getText().toString().trim().isEmpty();
        emptyText.setText(searching ? "Aucun message trouvé"
                : selectedMessageFilter == SmsDateFilter.Period.ALL
                && selectedMessageType == SmsDateFilter.TransactionType.ALL
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
            holder.time.setText(HistoryDisplayFormatter.time(item.timestamp));
            VerificationStatusPresentation status =
                    VerificationStatusPresentation.from(item.verification);
            int statusColor = ContextCompat.getColor(holder.itemView.getContext(), status.color);
            holder.verification.setText(status.label);
            holder.verification.setTextColor(statusColor);
            holder.verificationIcon.setImageResource(status.icon);
            androidx.core.widget.ImageViewCompat.setImageTintList(holder.verificationIcon,
                    ColorStateList.valueOf(statusColor));
            holder.icon.setImageResource(HistoryDisplayFormatter.direction(item)
                    == HistoryDisplayFormatter.Direction.NORTH_EAST
                    ? R.drawable.ic_north_east_24 : R.drawable.ic_south_east_24);
            holder.itemView.setContentDescription(item.type + ", " + holder.number.getText()
                    + ", " + holder.amount.getText() + ", " + holder.reference.getText()
                    + ", bonus " + holder.bonus.getText() + ", "
                    + holder.verification.getText() + ", " + holder.time.getText());
            holder.itemView.setOnClickListener(view -> BalanceVerificationDialog.show(
                    view.getContext(), item.verification));
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final TextView date;
        final TextView number;
        final TextView reference;
        final TextView amount;
        final TextView bonus;
        final TextView time;
        final TextView verification;
        final ImageView verificationIcon;
        final ImageView icon;

        HistoryViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.historyDateHeader);
            number = itemView.findViewById(R.id.historyTransactionNumber);
            reference = itemView.findViewById(R.id.historyTransactionReference);
            amount = itemView.findViewById(R.id.historyTransactionAmount);
            bonus = itemView.findViewById(R.id.historyTransactionBonus);
            time = itemView.findViewById(R.id.transactionTime);
            verification = itemView.findViewById(R.id.historyVerificationStatus);
            verificationIcon = itemView.findViewById(R.id.historyVerificationIcon);
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
            holder.count.setText(count + (count == 1 ? " transaction" : " transactions"));
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
        private Map<String, TransactionBalanceVerification> verifications =
                Collections.emptyMap();
        private float zoom = 1f;

        void setZoom(float zoom) {
            this.zoom = zoom;
            notifyItemRangeChanged(0, getItemCount(), "zoom");
        }

        void submitList(List<SmsDateFilter.DisplayMessage> messages,
                        Map<String, TransactionBalanceVerification> verifications) {
            items = new ArrayList<>(messages);
            this.verifications = verifications == null ? Collections.emptyMap() : verifications;
            notifyDataSetChanged();
        }

        List<SmsDateFilter.DisplayMessage> snapshot() {
            return new ArrayList<>(items);
        }

        @Override public ClientMessageViewHolder onCreateViewHolder(android.view.ViewGroup parent,
                                                                     int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_client_message, parent, false);
            return new ClientMessageViewHolder(view);
        }

        @Override public void onBindViewHolder(ClientMessageViewHolder holder, int position) {
            SmsDateFilter.DisplayMessage displayed = items.get(position);
            SmsTableRow row = SmsTableRow.from(displayed,
                    verifications.get(displayed.message.uniqueKey));
            holder.reference.setText(SmsTableRow.display(row.reference));
            holder.dateTime.setText(row.dateTime);
            bindTypeBadge(holder.type, row.type);
            holder.name.setText(SmsTableRow.display(row.nom));
            holder.amount.setText(SmsTableRow.display(row.montant));
            holder.bonus.setText(SmsTableRow.display(row.bonus));
            holder.fees.setText(SmsTableRow.displayFee(row.frais));
            holder.balance.setText(SmsTableRow.display(row.solde));
            VerificationStatusCell.bind(holder.status, row.verification);
            holder.applyZoom(zoom);
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class ClientMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView reference;
        final TextView dateTime;
        final TextView type;
        final TextView name;
        final TextView amount;
        final TextView bonus;
        final TextView fees;
        final TextView balance;
        final View status;
        ClientMessageViewHolder(View itemView) {
            super(itemView);
            reference = itemView.findViewById(R.id.clientMessageReference);
            dateTime = itemView.findViewById(R.id.clientMessageDateTime);
            type = itemView.findViewById(R.id.clientMessageType);
            name = itemView.findViewById(R.id.clientMessageName);
            amount = itemView.findViewById(R.id.clientMessageAmount);
            bonus = itemView.findViewById(R.id.clientMessageBonus);
            fees = itemView.findViewById(R.id.clientMessageFees);
            balance = itemView.findViewById(R.id.clientMessageBalance);
            status = itemView.findViewById(R.id.clientMessageStatus);
        }

        void applyZoom(float zoom) {
            applyRowZoom(itemView, new TextView[]{reference, dateTime, type, name, amount,
                    bonus, fees, balance},
                    new int[]{R.dimen.sms_column_reference_width,
                            R.dimen.sms_column_datetime_width, R.dimen.sms_column_type_width,
                            R.dimen.sms_column_name_width, R.dimen.sms_column_money_width,
                            R.dimen.sms_column_money_width, R.dimen.sms_column_money_width,
                            R.dimen.sms_column_money_width}, zoom);
            applyStatusZoom(status, zoom);
        }
    }

    private void setTableZoom(float zoom) {
        float clamped = clampTableZoom(zoom);
        if (Math.abs(clamped - tableZoom) < 0.001f) return;
        tableZoom = clamped;
        applyTableZoom();
    }

    private static float clampTableZoom(float zoom) {
        return Math.max(0.7f, Math.min(1.5f, zoom));
    }

    private void applyTableZoom() {
        if (transactionTable == null || smsTableHeader == null || adapter == null) return;
        transactionTable.getLayoutParams().width = scaledDimension(R.dimen.sms_table_width, tableZoom);
        transactionTable.requestLayout();
        smsTableHeader.getLayoutParams().height = dp(40f * tableZoom);
        int[] widths = {R.dimen.sms_column_reference_width, R.dimen.sms_column_datetime_width,
                R.dimen.sms_column_type_width, R.dimen.sms_column_sender_width,
                R.dimen.sms_column_name_width, R.dimen.sms_column_money_width,
                R.dimen.sms_column_money_width, R.dimen.sms_column_money_width,
                R.dimen.sms_column_money_width,
                R.dimen.sms_column_status_width};
        for (int index = 0; index < smsTableHeader.getChildCount(); index++) {
            View column = smsTableHeader.getChildAt(index);
            column.getLayoutParams().width = scaledDimension(widths[index], tableZoom);
            if (column instanceof TextView) {
                ((TextView) column).setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * tableZoom);
            }
        }
        smsTableHeader.requestLayout();
        adapter.setZoom(tableZoom);
        applyClientTableZoom();
    }

    private void applyClientTableZoom() {
        if (clientTransactionTable == null || clientTableHeader == null
                || clientMessageAdapter == null) return;
        clientTransactionTable.getLayoutParams().width = scaledDimension(
                R.dimen.client_table_width, tableZoom);
        clientTransactionTable.requestLayout();
        clientTableHeader.getLayoutParams().height = dp(40f * tableZoom);
        int[] widths = {R.dimen.sms_column_reference_width, R.dimen.sms_column_datetime_width,
                R.dimen.sms_column_type_width, R.dimen.sms_column_name_width,
                R.dimen.sms_column_money_width, R.dimen.sms_column_money_width,
                R.dimen.sms_column_money_width,
                R.dimen.sms_column_money_width, R.dimen.sms_column_status_width};
        for (int index = 0; index < clientTableHeader.getChildCount(); index++) {
            View column = clientTableHeader.getChildAt(index);
            column.getLayoutParams().width = scaledDimension(widths[index], tableZoom);
            if (column instanceof TextView) {
                ((TextView) column).setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * tableZoom);
            }
        }
        clientTableHeader.requestLayout();
        clientMessageAdapter.setZoom(tableZoom);
    }

    private int scaledDimension(int resource, float zoom) {
        return Math.round(getResources().getDimension(resource) * zoom);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class SmsAdapter extends RecyclerView.Adapter<SmsViewHolder> {
        private List<SmsDateFilter.DisplayMessage> items = new ArrayList<>();
        private Map<String, TransactionBalanceVerification> verifications =
                Collections.emptyMap();
        private float zoom;

        SmsAdapter(float zoom) {
            this.zoom = zoom;
        }

        void setZoom(float zoom) {
            this.zoom = zoom;
            notifyItemRangeChanged(0, getItemCount(), "zoom");
        }

        void submitList(List<SmsDateFilter.DisplayMessage> messages,
                        Map<String, TransactionBalanceVerification> verifications) {
            items = new ArrayList<>(messages);
            this.verifications = verifications == null ? Collections.emptyMap() : verifications;
            notifyDataSetChanged();
        }

        List<SmsDateFilter.DisplayMessage> snapshot() {
            return new ArrayList<>(items);
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
            SmsTableRow row = SmsTableRow.from(displayed,
                    verifications.get(displayed.message.uniqueKey));
            holder.bindReference(row.reference);
            holder.dateTime.setText(row.dateTime);
            bindTypeBadge(holder.type, row.type);
            holder.sender.setText(SmsTableRow.display(row.numero));
            holder.name.setText(SmsTableRow.display(row.nom));
            holder.amount.setText(SmsTableRow.display(row.montant));
            holder.bonus.setText(SmsTableRow.display(row.bonus));
            holder.fees.setText(SmsTableRow.displayFee(row.frais));
            holder.balance.setText(SmsTableRow.display(row.solde));
            VerificationStatusCell.bind(holder.status, row.verification);
            holder.applyZoom(zoom);
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static void applyRowZoom(View itemView, TextView[] columns, int[] dimensions,
                                     float zoom) {
        android.content.res.Resources resources = itemView.getResources();
        for (int index = 0; index < columns.length; index++) {
            View widthTarget = index == 2 ? (View) columns[index].getParent() : columns[index];
            widthTarget.getLayoutParams().width = Math.round(
                    resources.getDimension(dimensions[index]) * zoom);
            columns[index].setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    (index < 3 ? 12f : 13f) * zoom);
        }
        if (itemView instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) itemView;
            if (group.getChildCount() > 0 && group.getChildAt(0) instanceof LinearLayout) {
                LinearLayout content = (LinearLayout) group.getChildAt(0);
                content.setMinimumHeight(Math.round(48f
                        * resources.getDisplayMetrics().density * zoom));
                int verticalPadding = Math.round(7f
                        * resources.getDisplayMetrics().density * zoom);
                content.setPadding(content.getPaddingLeft(), verticalPadding,
                        content.getPaddingRight(), verticalPadding);
            }
        }
        TextView type = columns[2];
        type.getLayoutParams().height = Math.round(22f
                * resources.getDisplayMetrics().density * zoom);
        int badgePadding = Math.round(7f * resources.getDisplayMetrics().density * zoom);
        type.setPadding(badgePadding, type.getPaddingTop(), badgePadding,
                type.getPaddingBottom());
        itemView.requestLayout();
    }

    private static void applyStatusZoom(View status, float zoom) {
        android.content.res.Resources resources = status.getResources();
        status.getLayoutParams().width = Math.round(
                resources.getDimension(R.dimen.sms_column_status_width) * zoom);
        // Keep the touch target accessible even when the table content is zoomed out.
        status.getLayoutParams().height = Math.max(Math.round(48f
                * resources.getDisplayMetrics().density), Math.round(48f
                * resources.getDisplayMetrics().density * zoom));
        int horizontalPadding = Math.round(resources.getDimension(
                R.dimen.sms_status_cell_horizontal_padding) * zoom);
        status.setPaddingRelative(horizontalPadding, status.getPaddingTop(),
                horizontalPadding, status.getPaddingBottom());
        androidx.appcompat.widget.AppCompatImageView icon = status.findViewById(R.id.statusIcon);
        int iconSize = Math.round(resources.getDimension(R.dimen.sms_status_icon_size) * zoom);
        icon.getLayoutParams().width = iconSize;
        icon.getLayoutParams().height = iconSize;
        TextView text = status.findViewById(R.id.statusText);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * zoom);
        android.view.ViewGroup.MarginLayoutParams textParams =
                (android.view.ViewGroup.MarginLayoutParams) text.getLayoutParams();
        textParams.setMarginStart(Math.round(
                resources.getDimension(R.dimen.sms_status_icon_spacing) * zoom));
        status.requestLayout();
    }

    /** Styles only the compact type value; the table row itself always keeps its white surface. */
    private static void bindTypeBadge(TextView view, String type) {
        String displayedType = SmsTableRow.display(type);
        view.setText(displayedType);

        int background = 0;
        int textColor = R.color.sms_text_secondary;
        if ("Dépôt".equalsIgnoreCase(type)) {
            background = R.drawable.bg_type_deposit;
            textColor = R.color.sms_type_deposit_text;
        } else if ("Retrait".equalsIgnoreCase(type)) {
            background = R.drawable.bg_type_withdrawal;
            textColor = R.color.sms_type_withdrawal_text;
        } else if ("Crédit".equalsIgnoreCase(type)) {
            background = R.drawable.bg_type_credit;
            textColor = R.color.sms_type_credit_text;
        }

        view.setBackgroundResource(background);
        view.setTextColor(ContextCompat.getColor(view.getContext(), textColor));
    }

    private static class SmsViewHolder extends RecyclerView.ViewHolder {
        final TextView reference;
        final TextView dateTime;
        final TextView type;
        final TextView sender;
        final TextView name;
        final TextView amount;
        final TextView bonus;
        final TextView fees;
        final TextView balance;
        final View status;
        private String boundReference;

        SmsViewHolder(View itemView) {
            super(itemView);
            reference = itemView.findViewById(R.id.itemReference);
            dateTime = itemView.findViewById(R.id.itemDateTime);
            type = itemView.findViewById(R.id.itemType);
            sender = itemView.findViewById(R.id.itemSender);
            name = itemView.findViewById(R.id.itemName);
            amount = itemView.findViewById(R.id.itemAmount);
            bonus = itemView.findViewById(R.id.itemBonus);
            fees = itemView.findViewById(R.id.itemFees);
            balance = itemView.findViewById(R.id.itemBalance);
            status = itemView.findViewById(R.id.itemStatus);
            reference.setOnClickListener(view -> copyReference());
        }

        void bindReference(String value) {
            boundReference = value;
            reference.setText(SmsTableRow.display(value));
        }

        private void copyReference() {
            String value = boundReference;
            if (value == null || value.trim().isEmpty()) {
                Toast.makeText(reference.getContext(), R.string.reference_unavailable,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager clipboard = (ClipboardManager) reference.getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Référence", value));
                Toast.makeText(reference.getContext(), R.string.reference_copied,
                        Toast.LENGTH_SHORT).show();
            }
        }

        void applyZoom(float zoom) {
            int[] dimensions = {R.dimen.sms_column_reference_width,
                    R.dimen.sms_column_datetime_width, R.dimen.sms_column_type_width,
                    R.dimen.sms_column_sender_width, R.dimen.sms_column_name_width,
                    R.dimen.sms_column_money_width, R.dimen.sms_column_money_width,
                    R.dimen.sms_column_money_width,
                    R.dimen.sms_column_money_width};
            TextView[] columns = {reference, dateTime, type, sender, name, amount,
                    bonus, fees, balance};
            applyRowZoom(itemView, columns, dimensions, zoom);
            applyStatusZoom(status, zoom);
        }
    }
}
