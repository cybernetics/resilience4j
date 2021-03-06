/*
 * Copyright 2017 Bohdan Storozhuk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.ratelimiter.monitoring.endpoint;

import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratelimiter.monitoring.model.RateLimiterEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;

public class RateLimiterEventsEmitter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterEventsEmitter.class);

    private final SseEmitter sseEmitter;
    private final Disposable disposable;

    public RateLimiterEventsEmitter(Flux<RateLimiterEventDTO> eventStream) {
        this.sseEmitter = new SseEmitter();
        this.sseEmitter.onCompletion(this::unsubscribe);
        this.sseEmitter.onTimeout(this::unsubscribe);
        this.disposable = eventStream.subscribe(this::notify,
            this.sseEmitter::completeWithError,
            this.sseEmitter::complete);
    }

    private void notify(RateLimiterEventDTO rateLimiterEventDTO){
        try {
            sseEmitter.send(rateLimiterEventDTO, MediaType.APPLICATION_JSON);
        } catch (IOException e) {
            LOG.warn("Failed to send circuitbreaker event", e);
        }
    }

    private void unsubscribe() {
        this.disposable.dispose();
    }

    public static SseEmitter createSseEmitter(Flux<RateLimiterEvent> eventStream) {
        Flux<RateLimiterEventDTO> flowable = eventStream.map(RateLimiterEventDTO::createRateLimiterEventDTO);
        return new RateLimiterEventsEmitter(flowable).sseEmitter;
    }
}
