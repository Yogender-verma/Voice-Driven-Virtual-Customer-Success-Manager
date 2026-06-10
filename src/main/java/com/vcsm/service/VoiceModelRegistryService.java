package com.vcsm.service;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class VoiceModelRegistryService {

    private final AtomicReference<VoiceModelStorageService.StoredVoiceModel> activeModel = new AtomicReference<>();

    public void activate(VoiceModelStorageService.StoredVoiceModel model) {
        activeModel.set(model);
    }

    public Optional<VoiceModelStorageService.StoredVoiceModel> getActiveModel() {
        return Optional.ofNullable(activeModel.get());
    }
}
