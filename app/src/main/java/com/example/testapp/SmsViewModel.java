package com.example.testapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.testapp.database.SmsMessage;
import com.example.testapp.repository.SmsRepository;

import java.util.List;

public class SmsViewModel extends AndroidViewModel {
    private final SmsRepository repository;
    private final LiveData<List<SmsMessage>> messages;

    public SmsViewModel(@NonNull Application application) {
        super(application);
        repository = new SmsRepository(application);
        messages = repository.observeMessages();
    }

    public LiveData<List<SmsMessage>> getMessages() { return messages; }

    public void importInboxOnce(Runnable onComplete, Runnable onPermissionDenied) {
        repository.importInboxOnce(onComplete, onPermissionDenied);
    }
}
