package com.netk.mvolatrack.invoice;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.netk.mvolatrack.R;

/** Displays the already-persisted PNG without generating or saving another invoice. */
public final class InvoiceViewerActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URI = "invoice_image_uri";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_invoice_viewer);
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

    private void missing() {
        Toast.makeText(this, "Image de la facture introuvable", Toast.LENGTH_LONG).show();
        finish();
    }
}
