package com.us.unix.restfull;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Completable future support for OkHttp Responses.
 */
public class OkHttpResponseFuture implements Callback {
  /**
   * Completable future object.
   */
  public final CompletableFuture<Response> future = new CompletableFuture<>();

  /**
   * Class constructor.
   */
  public OkHttpResponseFuture() {
  }

  @Override
  public void onFailure(@NotNull Call call, IOException e) {
    future.completeExceptionally(e);
  }

  @Override
  public void onResponse(@NotNull Call call, @NotNull Response response) {
    future.complete(response);
  }
}
