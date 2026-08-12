package com.example.testapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView status = findViewById(R.id.statusText);
        Button button = findViewById(R.id.testButton);

        button.setOnClickListener(v ->
            status.setText("Test réussi ! L'application fonctionne.")
        );
    }
}
