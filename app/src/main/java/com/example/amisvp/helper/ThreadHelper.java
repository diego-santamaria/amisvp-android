package com.example.amisvp.helper;

import com.example.amisvp.interfaces.IThreadCompleteListener;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class ThreadHelper extends Thread {

    private final Set<IThreadCompleteListener> listeners
            = new CopyOnWriteArraySet<IThreadCompleteListener>();
    public final void addListener(final IThreadCompleteListener listener) {
        listeners.add(listener);
    }
    public final void removeListener(final IThreadCompleteListener listener) {
        listeners.remove(listener);
    }
    private final void notifyListeners() {
        for (IThreadCompleteListener listener : listeners) {
            listener.notifyOfThreadComplete(this);
        }
    }
    @Override
    public final void run() {
        try {
            doRun();
        } finally {
            notifyListeners();
        }
    }
    public abstract void doRun();
}
