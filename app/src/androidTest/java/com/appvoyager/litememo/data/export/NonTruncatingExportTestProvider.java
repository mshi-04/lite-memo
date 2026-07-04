package com.appvoyager.litememo.data.export;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

public final class NonTruncatingExportTestProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = fileFor(uri);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        int flags;
        if (mode.contains("w")) {
            flags = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE;
            if (mode.contains("t")) {
                flags |= ParcelFileDescriptor.MODE_TRUNCATE;
            }
        } else if (mode.contains("r")) {
            flags = ParcelFileDescriptor.MODE_READ_ONLY;
        } else {
            throw new FileNotFoundException("Unsupported mode: " + mode);
        }
        return ParcelFileDescriptor.open(file, flags);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return fileFor(uri).delete() ? 1 : 0;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "application/json";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private File fileFor(Uri uri) {
        String fileName = uri.getLastPathSegment();
        if (fileName == null) {
            fileName = "export.json";
        }
        return new File(getContext().getCacheDir(), fileName);
    }
}
