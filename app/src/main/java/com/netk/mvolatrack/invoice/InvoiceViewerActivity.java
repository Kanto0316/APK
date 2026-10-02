package com.netk.mvolatrack.invoice;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.netk.mvolatrack.R;

/** Displays the already-persisted PNG without generating or saving another invoice. */
public final class InvoiceViewerActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URI = "invoice_image_uri";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_invoice_viewer);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);
        applyHeaderInsets(findViewById(R.id.invoiceViewerHeader));
        applyImageInsets(findViewById(R.id.invoiceViewerImage));
        findViewById(R.id.invoiceViewerBack).setOnClickListener(view -> finish());
        String value = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (value == null || value.trim().isEmpty()) {
            missing();
            return;
        }
        try {
            Uri uri = Uri.parse(value);
            // Opening now makes a deleted/inaccessible MediaStore item fail predictably.
            try (android.os.ParcelFileDescriptor ignored =
                         getContentResolver().openFileDescriptor(uri, "r")) {
                if (ignored == null) throw new java.io.FileNotFoundException();
            }
            ((ImageView) findViewById(R.id.invoiceViewerImage)).setImageURI(uri);
        } catch (Exception error) {
            missing();
        }
    }

    /** Keeps the original 56dp toolbar below both status bars and display cutouts. */
    private static void applyHeaderInsets(View header) {
        final int initialHeight = header.getLayoutParams().height;
        final int initialLeft = header.getPaddingLeft();
        final int initialTop = header.getPaddingTop();
        final int initialRight = header.getPaddingRight();
        final int initialBottom = header.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(header, (view, windowInsets) -> {
            Insets topInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                            | WindowInsetsCompat.Type.displayCutout());
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = initialHeight + topInsets.top;
            view.setLayoutParams(layoutParams);
            view.setPadding(initialLeft + topInsets.left, initialTop + topInsets.top,
                    initialRight + topInsets.right, initialBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    /** Prevents fitCenter content from extending below navigation bars or into a cutout. */
    private static void applyImageInsets(View image) {
        final int initialLeft = image.getPaddingLeft();
        final int initialTop = image.getPaddingTop();
        final int initialRight = image.getPaddingRight();
        final int initialBottom = image.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(image, (view, windowInsets) -> {
            Insets bottomInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
                            | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(initialLeft + bottomInsets.left, initialTop,
                    initialRight + bottomInsets.right, initialBottom + bottomInsets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(image);
    }

    private void missing() {
        Toast.makeText(this, "Image de la facture introuvable", Toast.LENGTH_LONG).show();
        finish();
    }
}
