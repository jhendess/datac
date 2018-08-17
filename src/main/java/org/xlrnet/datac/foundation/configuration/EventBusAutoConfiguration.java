/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xlrnet.datac.foundation.configuration;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EnableEventBus;
import org.vaadin.spring.events.support.ApplicationContextEventBroker;

import lombok.extern.slf4j.Slf4j;

/**
 * Auto configuration for the Event Bus.
 */
@Slf4j
@Configuration
public class EventBusAutoConfiguration {

    private final EventBus.ApplicationEventBus eventBus;

    @Autowired
    public EventBusAutoConfiguration(EventBus.ApplicationEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Configuration
    @EnableEventBus
    static class EnableEventBusConfiguration implements InitializingBean {

        @Override
        public void afterPropertiesSet() throws Exception {
            LOGGER.info("Vaadin event bus initialized");
        }
    }

    /**
     * Produce an application context event broker which propagates Spring events to the Vaadin bus.
     */
    @Bean
    ApplicationContextEventBroker applicationContextEventBroker() {
        return new ApplicationContextEventBroker(eventBus);
    }
}