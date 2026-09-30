package com.netk.mvolatrack.export;

/** Receives lightweight progress updates while an export is being written. */
public interface ExportProgressListener {
    ExportProgressListener NONE = (processed, total) -> { };

    void onProgress(int processed, int total);
}
