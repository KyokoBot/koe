package moe.kyokobot.koe.testbot.util;
/*
 * Copyright (c) 2019 amy, All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. Neither the name of the copyright holder nor the names of its contributors
 *     may be used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import com.mewna.catnip.rest.Routes.Route;
import com.mewna.catnip.rest.ratelimit.RateLimiter;
import com.mewna.catnip.rest.requester.AbstractRequester;
import com.mewna.catnip.rest.requester.Requester;
import com.mewna.catnip.util.CatnipMeta;
import com.mewna.catnip.util.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.mewna.catnip.rest.Routes.HttpMethod.GET;

public class UserBotBurstRequester extends AbstractRequester {
    private static String userAgent = "DiscordBot (https://github.com/mewna/catnip, " + CatnipMeta.VERSION + ')';
    private final Bucket bucket = new BurstBucket();
    private static Field startField;

    static {
        try {
            startField = QueuedRequest.class.getDeclaredField("start");
            startField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace(); // NOSONAR
        }
    }

    public static String userAgent() {
        return userAgent;
    }

    public static void userAgent(String userAgent) {
        UserBotBurstRequester.userAgent = userAgent;
    }

    public UserBotBurstRequester(@Nonnull final RateLimiter rateLimiter) {
        super(rateLimiter);
    }

    @Nonnull
    @Override
    protected Bucket getBucket(@Nonnull final Route route) {
        return bucket;
    }


    protected void executeHttpRequest(@Nonnull final Route route, @Nullable final HttpRequest.BodyPublisher body,
                                      @Nonnull final QueuedRequest request, @Nonnull final String mediaType) {
        final HttpRequest.Builder builder;
        final String apiHostVersion = catnip.options().apiHost() + "/api/v" + catnip.options().apiVersion();

        if (route.method() == GET) {
            // No body
            builder = HttpRequest.newBuilder(URI.create(apiHostVersion + route.baseRoute())).GET();
        } else {
            final var fakeBody = request.request().emptyBody();
            builder = HttpRequest.newBuilder(URI.create(apiHostVersion + route.baseRoute()))
                    .setHeader("Content-Type", mediaType)
                    .method(route.method().name(), fakeBody ? HttpRequest.BodyPublishers.ofString(" ") : body);
            if (fakeBody) {
                // If we don't have a body, then the body param is null, which
                // seems to not set a Content-Length. This explicitly tries to set
                // up a request shaped in a way that makes Discord not complain.
                catnip.logAdapter().trace("Set fake body due to lack of body.");
            }
        }

        // Required by Discord
        builder.setHeader("User-Agent", UserBotBurstRequester.userAgent);
        // Request more precise ratelimit headers for better timing
        // NOTE: THIS SHOULD NOT BE CONFIGURABLE BY THE END USER
        // This is pretty important for getting timing of things like reaction
        // routes correct, so there's no use for the end-user messing around
        // with this.
        // If they REALLY want it, they can write their own requester.
        builder.setHeader("X-RateLimit-Precision", "millisecond");

        if (request.request().needsToken()) {
            builder.setHeader("Authorization", catnip.options().token());
        }
        if (request.request().reason() != null) {
            catnip.logAdapter().trace("Adding reason header due to specific needs.");
            builder.header(Requester.REASON_HEADER, Utils.encodeUTF8(request.request().reason()));
        }

        // Update request start time as soon as possible
        // See QueuedRequest docs for why we do this
        //request.start = System.nanoTime();

        try {
            // i don't want to pr, it's just a hack lol
            startField.setLong(request, System.nanoTime());
        } catch (Exception e) {
            catnip.logAdapter().error("Failed to set start field lol.", e);
        }

        catnip.options().httpClient().sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    final int code = res.statusCode();
                    final String message = "Unavailable to due Java's HTTP client.";
                    final long requestEnd = System.nanoTime();

                    catnip.rxScheduler().scheduleDirect(() ->
                            handleResponse(route, code, message, requestEnd, res.body(), res.headers(), request));
                })
                .exceptionally(e -> {
                    request.bucket().failedRequest(request, e);
                    return null;
                });
    }

    private class BurstBucket implements Bucket {
        @Override
        public void queueRequest(@Nonnull final QueuedRequest request) {
            //noinspection ResultOfMethodCallIgnored
            rateLimiter.requestExecution(request.route())
                    .subscribe(() -> executeRequest(request),
                            request.future()::completeExceptionally);
        }

        @Override
        public void failedRequest(@Nonnull final QueuedRequest request, @Nonnull final Throwable failureCause) {
            request.failed();
            if (request.shouldRetry()) {
                queueRequest(request);
            } else {
                catnip.logAdapter().debug("Request {} failed, giving up!", request.request());
                request.future().completeExceptionally(failureCause);
                requestDone();
            }
        }

        @Override
        public void requestDone() {
            //noop
        }
    }
}
