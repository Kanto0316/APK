package com.netk.mvolatrack;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.netk.mvolatrack.activation.ActivationStore;
import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.history.HistoryTransaction;
import com.netk.mvolatrack.repository.BonusNotificationRepository;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Overview page backed only by the SMS, notification and licence data already persisted. */
public final class DashboardActivity extends AppCompatActivity {
    private LinearLayout summaryCards;
    private LinearLayout analysisCards;
    private LinearLayout latestTransactions;
    private HourlyActivityChartView activityChart;
    private List<SmsMessage> messages = Collections.emptyList();
    private int unreadNotifications;
    private int intervalMinutes = 60;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_dashboard);
        applyInsets(findViewById(R.id.dashboardRoot));
        findViewById(R.id.dashboardBack).setOnClickListener(view -> finish());
        summaryCards = findViewById(R.id.dashboardSummaryCards);
        analysisCards = findViewById(R.id.dashboardAnalysisCards);
        latestTransactions = findViewById(R.id.dashboardLatestTransactions);
        activityChart = findViewById(R.id.dashboardActivityChart);
        configureInterval(R.id.dashboardInterval15, 15);
        configureInterval(R.id.dashboardInterval30, 30);
        configureInterval(R.id.dashboardInterval60, 60);

        SmsViewModel viewModel = new ViewModelProvider(this).get(SmsViewModel.class);
        viewModel.getMessages().observe(this, current -> {
            messages = current == null ? Collections.emptyList() : current;
            render();
        });
        new BonusNotificationRepository(this).observeUnreadCount().observe(this, count -> {
            unreadNotifications = count == null ? 0 : count;
            render();
        });
    }

    private void configureInterval(int viewId, int minutes) {
        findViewById(viewId).setOnClickListener(view -> {
            intervalMinutes = minutes;
            renderChart();
        });
    }

    private void render() {
        DashboardSummary data = DashboardSummary.from(HistoryTransaction.fromMessages(messages),
                System.currentTimeMillis(), TimeZone.getDefault());
        summaryCards.removeAllViews();
        addPair(summaryCards,
                card("Solde actuel", formatAriary(data.balance), "Dernière mise à jour : "
                        + formatDate(data.lastUpdate)),
                card("Transactions", String.valueOf(data.transactionCount),
                        data.todayTransactionCount + " aujourd’hui"));
        addPair(summaryCards,
                card("Clients", String.valueOf(data.clientCount),
                        data.newClientCount + " nouveaux aujourd’hui"),
                card("Bonus", formatAriary(data.bonusTotal), data.verifiedBonusCount
                        + " vérifiés · " + data.bonusAttentionCount + " à vérifier"));

        analysisCards.removeAllViews();
        addSection("Analyse financière", new String[]{"Total Crédit", "Total Retrait",
                "Total Dépôt", "Total Bonus"}, new String[]{formatAriary(data.creditTotal),
                formatAriary(data.withdrawalTotal), formatAriary(data.depositTotal),
                formatAriary(data.bonusTotal)});
        addSection("Résumé bonus", new String[]{"Bonus vérifiés", "Bonus non vérifiables",
                "Bonus non reçus", "Bonus supérieur"}, new String[]{
                String.valueOf(data.verifiedBonusCount), String.valueOf(data.unverifiableBonusCount),
                String.valueOf(data.missingBonusCount), String.valueOf(data.superiorBonusCount)});
        addSection("Activité clients", new String[]{"Clients actifs", "Client le plus actif",
                "Nombre de transactions"}, new String[]{String.valueOf(data.activeClientCount),
                data.mostActiveClient, String.valueOf(data.mostActiveClientTransactions)});
        addSection("Alertes", new String[]{"Notifications importantes",
                "Bonus nécessitant une vérification"}, new String[]{
                String.valueOf(unreadNotifications), String.valueOf(data.bonusAttentionCount)});
        addSection("Licence", new String[]{"État licence"}, new String[]{
                ActivationStore.hasValidActivation(this) ? "Active" : "À vérifier"});

        latestTransactions.removeAllViews();
        if (data.latestTransactions.isEmpty()) {
            latestTransactions.addView(label("Aucune transaction", false));
        } else {
            latestTransactions.addView(transactionHeader());
            for (HistoryTransaction transaction : data.latestTransactions) {
                latestTransactions.addView(transactionRow(transaction));
            }
        }
        renderChart();
    }

    private void renderChart() {
        int[] options = {15, 30, 60};
        int[] ids = {R.id.dashboardInterval15, R.id.dashboardInterval30,
                R.id.dashboardInterval60};
        for (int i = 0; i < ids.length; i++) findViewById(ids[i])
                .setSelected(intervalMinutes == options[i]);
        activityChart.setData(SmsStatistics.intervalActivity(messages,
                System.currentTimeMillis(), TimeZone.getDefault(), intervalMinutes), intervalMinutes);
    }

    private void addSection(String title, String[] labels, String[] values) {
        TextView heading = label(title, true);
        heading.setTextSize(18);
        heading.setPadding(0, dp(14), 0, dp(12));
        analysisCards.addView(heading);
        for (int index = 0; index < labels.length; index += 2) {
            View second = index + 1 < labels.length ? card(labels[index + 1], values[index + 1], "")
                    : spacerCard();
            addPair(analysisCards, card(labels[index], values[index], ""), second);
        }
    }

    private View card(String title, String value, String detail) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_dashboard_card);
        card.setElevation(dp(2));
        card.setMinimumHeight(dp(116));
        card.addView(label(title, true));
        TextView primary = label(value, true);
        primary.setTextSize(22);
        primary.setPadding(0, dp(7), 0, 0);
        card.addView(primary);
        if (!detail.isEmpty()) {
            TextView secondary = label(detail, false);
            secondary.setTextSize(12);
            secondary.setTextColor(ContextCompat.getColor(this, R.color.sms_text_secondary));
            secondary.setPadding(0, dp(5), 0, 0);
            card.addView(secondary);
        }
        return card;
    }

    private View spacerCard() {
        View view = new View(this);
        view.setVisibility(View.INVISIBLE);
        return view;
    }

    private void addPair(LinearLayout parent, View first, View second) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        first.setLayoutParams(params);
        row.addView(first);
        View gap = new View(this);
        row.addView(gap, new LinearLayout.LayoutParams(dp(12), 1));
        second.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(second);
        parent.addView(row);
    }

    private TextView transactionHeader() {
        return transactionCells("Type", "Montant", "Client", "Date", true);
    }

    private TextView transactionRow(HistoryTransaction transaction) {
        return transactionCells(transaction.type, formatAriary(transaction.amount),
                transaction.clientNumber, new SimpleDateFormat("dd/MM HH:mm", Locale.FRENCH)
                        .format(transaction.timestamp), false);
    }

    private TextView transactionCells(String type, String amount, String client, String date,
                                      boolean header) {
        TextView row = label(String.format(Locale.FRENCH, "%-10s  %-14s\n%s  ·  %s",
                safe(type), safe(amount), safe(client), safe(date)), header);
        row.setTextSize(header ? 12 : 13);
        row.setPadding(dp(6), dp(9), dp(6), dp(9));
        if (!header) row.setBackgroundColor(ContextCompat.getColor(this, R.color.sms_surface));
        return row;
    }

    private TextView label(String text, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(this, R.color.sms_text_primary));
        view.setTextSize(14);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private String formatAriary(long value) {
        return String.format(Locale.FRENCH, "%,d Ar", value).replace('\u00a0', ' ');
    }

    private String formatDate(long timestamp) {
        return timestamp <= 0 ? "—" : new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
                .format(timestamp);
    }

    private static String safe(String value) { return value == null ? "—" : value; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void applyInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
