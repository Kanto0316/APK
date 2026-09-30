package com.netk.mvolatrack.notification;

import android.os.Bundle;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.netk.mvolatrack.R;
import com.netk.mvolatrack.database.NotificationBonus;
import com.netk.mvolatrack.database.NotificationType;
import com.netk.mvolatrack.repository.BonusNotificationRepository;
import com.netk.mvolatrack.verification.VerificationStatus;
import com.google.android.material.card.MaterialCardView;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Material-style persistent inbox for every application notification category. */
public final class BonusNotificationsActivity extends AppCompatActivity {
    private static final String TAG = "BonusNotification";
    private final Adapter adapter = new Adapter();
    private BonusNotificationRepository repository;
    private RecyclerView notificationList;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_bonus_notifications);
        findViewById(R.id.notificationsBack).setOnClickListener(view -> finish());
        RecyclerView list = findViewById(R.id.bonusNotificationsList);
        notificationList = list;
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        showNotifications(Collections.emptyList());
        try {
            repository = new BonusNotificationRepository(this);
            LiveData<List<NotificationBonus>> notifications = repository.observeAll();
            if (notifications != null) notifications.observe(this, this::showNotifications);
        } catch (Exception error) {
            showInitializationError(error);
        } catch (Throwable error) {
            showInitializationError(error);
        }
    }

    private void showNotifications(List<NotificationBonus> values) {
        List<NotificationBonus> safeValues = values == null ? Collections.emptyList() : values;
        adapter.submit(safeValues);
        findViewById(R.id.notificationsEmpty).setVisibility(
                adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void markRead(long notificationId) {
        if (repository != null && notificationId > 0) repository.markRead(notificationId);
    }

    private void showInitializationError(Throwable error) {
        Log.e(TAG, "inbox initialization failed (" + error.getClass().getName() + "): "
                + error.getMessage(), error);
        repository = null;
        showNotifications(Collections.emptyList());
    }

    private void showDetail(NotificationBonus item) {
        if (item == null) return;
        if (!NotificationType.BONUS.name().equals(item.type)) {
            new AlertDialog.Builder(this).setTitle(item.title).setMessage(item.message
                    + (item.metadata == null || item.metadata.trim().isEmpty() ? ""
                    : "\n\n" + formatMetadata(item.metadata)))
                    .setPositiveButton(android.R.string.ok, null).show();
            return;
        }
        String detected = item.detectedBonus == null ? "-"
                : item.detectedBonus + " Ar";
        new AlertDialog.Builder(this)
                .setTitle(label(item.anomalyType))
                .setMessage("Client : " + safe(item.clientNumber) + "\nRéférence : "
                        + safe(item.transactionReference) + "\nDate : " + formatDate(item.transactionDate)
                        + "\nBonus attendu : " + item.expectedBonus + " Ar\nBonus reçu : "
                        + detected + "\n\n" + explanation(item.explanation))
                .setPositiveButton(android.R.string.ok, null).show();
    }
    private static String formatMetadata(String metadata) {
        String[] lines = metadata.split("\\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            int separator = line.indexOf(':');
            String value = separator < 0 ? "" : line.substring(separator + 1).trim();
            if ((line.startsWith("Date d’activation") || line.startsWith("Date d’expiration"))
                    && value.matches("\\d+")) {
                line = line.substring(0, separator + 1) + " "
                        + DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
                        .format(new Date(Long.parseLong(value)));
            }
            if (result.length() > 0) result.append('\n');
            result.append(line);
        }
        return result.toString();
    }

    private static String safe(String value) { return value == null || value.trim().isEmpty() ? "-" : value; }
    private static String explanation(String value) {
        return value == null || value.trim().isEmpty() ? "Aucune information" : value;
    }
    private static String formatDate(long value) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(value));
    }
    private static String label(String type) {
        if (VerificationStatus.BONUS_SUPERIEUR.name().equals(type)) return "Bonus supérieur";
        if (VerificationStatus.BONUS_NON_CREDITE.name().equals(type)) return "Bonus non crédité";
        if (VerificationStatus.BONUS_PARTIEL.name().equals(type)) return "Bonus partiel";
        return "Non vérifiable";
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
            items.clear();
            if (values != null) {
                for (NotificationBonus item : values) if (item != null) items.add(item);
            }
            notifyDataSetChanged();
        }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_bonus_notification, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            NotificationBonus item = items.get(position);
            boolean bonus = NotificationType.BONUS.name().equals(item.type);
            boolean licence = NotificationType.LICENCE.name().equals(item.type);
            holder.icon.setImageResource(licence ? R.drawable.ic_verified_24
                    : bonus ? icon(item.anomalyType) : R.drawable.ic_info_24);
            holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(),
                    licence ? R.color.notification_license : bonus
                    ? (VerificationStatus.BONUS_NON_CREDITE.name().equals(item.anomalyType)
                    ? R.color.verification_error : R.color.verification_warning) : R.color.sms_accent));
            holder.title.setText(item.title);
            holder.client.setText(bonus ? "Client " + safe(item.clientNumber) + "  •  Réf. "
                    + safe(item.transactionReference) : item.message);
            holder.date.setText(formatDate(item.date));
            holder.amounts.setVisibility(bonus ? View.VISIBLE : View.GONE);
            holder.amounts.setText(bonus ? "Attendu : " + item.expectedBonus + " Ar   •   Reçu : "
                    + (item.detectedBonus == null ? "-" : item.detectedBonus + " Ar") : "");
            holder.explanation.setVisibility(bonus ? View.VISIBLE : View.GONE);
            holder.explanation.setText(bonus ? explanation(item.message) : "");
            holder.notificationId = item.id;
            holder.newIndicator.setVisibility(item.isRead ? View.GONE : View.VISIBLE);
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),
                    item.isRead ? R.color.notification_read_background
                            : R.color.notification_unread_background));
            int textStyle = item.isRead ? Typeface.NORMAL : Typeface.BOLD;
            holder.title.setTypeface(holder.title.getTypeface(), textStyle);
            holder.client.setTypeface(holder.client.getTypeface(), textStyle);
            holder.amounts.setTypeface(holder.amounts.getTypeface(), textStyle);
            holder.itemView.setOnClickListener(view -> {
                item.isRead = true;
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) notifyItemChanged(currentPosition);
                markRead(item.id);
                showDetail(item);
            });
            holder.itemView.setContentDescription(item.title + ", "
                    + holder.client.getText() + (item.isRead ? "" : ", nouveau"));
        }
        @Override public int getItemCount() { return items.size(); }
    }
    private static final class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card; final ImageView icon;
        final TextView title, client, date, amounts, explanation, newIndicator;
        long notificationId;
        Holder(View view) { super(view); card=(MaterialCardView) view;
            icon=view.findViewById(R.id.notificationTypeIcon);
            title=view.findViewById(R.id.notificationType); client=view.findViewById(R.id.notificationClient);
            date=view.findViewById(R.id.notificationDate); amounts=view.findViewById(R.id.notificationAmounts);
            explanation=view.findViewById(R.id.notificationExplanation);
            newIndicator=view.findViewById(R.id.notificationNew); }
    }
}
