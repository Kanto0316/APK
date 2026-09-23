package com.example.testapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.testapp.database.SmsMessage;
import com.example.testapp.repository.SmsRepository;

import java.util.List;

public class SmsViewModel extends AndroidViewModel {
    private final SmsRepository repository;
    private final LiveData<List<SmsMessage>> messages;
    private final LiveData<String> globalBalanceTitle;

    public SmsViewModel(@NonNull Application application) {
        super(application);
        repository = new SmsRepository(application);
        messages = repository.observeMessages();
        globalBalanceTitle = Transformations.map(messages, BalanceTitle::from);
    }

    public LiveData<List<SmsMessage>> getMessages() { return messages; }
    public LiveData<String> getGlobalBalanceTitle() { return globalBalanceTitle; }

    /** Reads the current Room snapshot without changing capture permissions or storage. */
    public void refresh(SmsRepository.MessagesCallback onLoaded) {
        repository.loadMessages(onLoaded);
    }

}
