package com.example.iotcontroller.services;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class LocalMediaServer extends NanoHTTPD {
    private Context context;
    private Uri fileUri;

    public LocalMediaServer(int port, Context context, Uri uri){
        super(port);
        this.context = context;
        this.fileUri = uri;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        // Nếu TV truy cập vào /video.mp4 -> Trả về file video thật
        if (uri.equals("/video.mp4")) {
            try {
                InputStream is = context.getContentResolver().openInputStream(fileUri);
                AssetFileDescriptor afd = context.getContentResolver().openAssetFileDescriptor(fileUri, "r");
                long len = afd.getLength();
                afd.close();

                Response res = newFixedLengthResponse(Response.Status.OK, "video/mp4", is, len);
                res.addHeader("Accept-Ranges", "bytes");
                return res;
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Video error");
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
    }
}
