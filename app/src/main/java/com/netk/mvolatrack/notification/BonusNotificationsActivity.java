package com.netk.mvolatrack.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.netk.mvolatrack.R;
import com.netk.mvolatrack.database.NotificationBonus;
import com.netk.mvolatrack.repository.BonusNotificationRepository;
import com.netk.mvolatrack.verification.VerificationStatus;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Material-style persistent inbox for bonus verification anomalies. */
public final class BonusNotificationsActivity extends AppCompatActivity {
    private final Adapter adapter = new Adapter();
    private BonusNotificationRepository repository;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_bonus_notifications);
        findViewById(R.id.notificationsBack).setOnClickListener(view -> finish());
        RecyclerView list = findViewById(R.id.bonusNotificationsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        repository = new BonusNotificationRepository(this);
        repository.observeAll().observe(this, values -> {
            adapter.submit(values);
            findViewById(R.id.notificationsEmpty).setVisibility(
                    values == null || values.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        repository.markAllRead();
    }

    private void showDetail(NotificationBonus item) {
        String detected = item.detectedBonus == null ? "Indisponible"
                : item.detectedBonus + " Ar";
        new AlertDialog.Builder(this)
                .setTitle(label(item.anomalyType))
                .setMessage("Client : " + safe(item.clientNumber) + "\nRéférence : "
                        + safe(item.transactionReference) + "\nDate : " + formatDate(item.transactionDate)
                        + "\nBonus attendu : " + item.expectedBonus + " Ar\nBonus reçu : "
                        + detected + "\n\n" + item.explanation)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private static String safe(String value) { return value == null || value.trim().isEmpty() ? "—" : value; }
    private static String formatDate(long value) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(value));
    }
    private static String label(String type) {
        if (VerificationStatus.BONUS_SUPERIEUR.name().equals(type)) return "Bonus supérieur";
        if (VerificationStatus.BONUS_NON_CREDITE.name().equals(type)) return "Bonus non crédité";
        if (VerificationStatus.BONUS_PARTIEL.name().equals(type)) return "Bonus partiel";
        return "Bonus non vérifiable";
    }
    private static int icon(String type) {
        if (VerificationStatus.BONUS_SUPERIEUR.name().equals(type))
            return R.drawable.ic_arrow_up_20;
        return VerificationStatus.BONUS_NON_CREDITE.name().equals(type)
                ? R.drawable.ic_status_error_20 : R.drawable.ic_status_warning_20;
    }

    private final class Adapter extends RecyclerView.Adapter<Holder> {
        private final List<NotificationBonus> items = new ArrayList<>();
        void submit(List<NotificationBonus> values) {
            items.clear(); if (values != null) items.addAll(values); notifyDataSetChanged();
        }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_bonus_notification, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            NotificationBonus item = items.get(position);
            holder.icon.setImageResource(icon(item.anomalyType));
            holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(),
                    VerificationStatus.BONUS_NON_CREDITE.name().equals(item.anomalyType)
                            ? R.color.verification_error : R.color.verification_warning));
            holder.title.setText(label(item.anomalyType));
            holder.client.setText("Client " + safe(item.clientNumber) + "  •  Réf. " + safe(item.transactionReference));
            holder.date.setText(formatDate(item.transactionDate));
            holder.amounts.setText("Attendu : " + item.expectedBonus + " Ar   •   Reçu : "
                    + (item.detectedBonus == null ? "—" : item.detectedBonus + " Ar"));
            holder.explanation.setText(item.explanation);
            holder.itemView.setOnClickListener(view -> showDetail(item));
            holder.itemView.setContentDescription(label(item.anomalyType) + ", " + holder.client.getText());
        }
        @Override public int getItemCount() { return items.size(); }
    }
    private static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon; final TextView title, client, date, amounts, explanation;
        Holder(View view) { super(view); icon=view.findViewById(R.id.notificationTypeIcon);
            title=view.findViewById(R.id.notificationType); client=view.findViewById(R.id.notificationClient);
            date=view.findViewById(R.id.notificationDate); amounts=view.findViewById(R.id.notificationAmounts);
            explanation=view.findViewById(R.id.notificationExplanation); }
    }
}
