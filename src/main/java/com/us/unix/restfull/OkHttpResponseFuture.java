package com.us.unix.restfull;

import com.us.unix.restfull.exceptions.HttpResponseException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class OkHttpResponseFuture implements Callback {
  public final CompletableFuture<Response> future = new CompletableFuture<>();

  public OkHttpResponseFuture() {
  }

  @Override
  public void onFailure(@NotNull Call call, IOException e) {
    future.completeExceptionally(e);
  }

  @Override
  public void onResponse(@NotNull Call call, @NotNull Response response) {
//    if (!response.isSuccessful()) {
//      future.completeExceptionally(new HttpResponseException());
//    } else {
    future.complete(response);
//    }
  }
}
