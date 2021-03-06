/*
 * Copyright 2014 Bazaarvoice, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bazaarvoice.dropwizard.caching;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Cache-control header override configurations.
 */
public class CacheControlConfiguration {
    private List<CacheControlConfigurationItem> _items = ImmutableList.of();

    public CacheControlConfiguration() {
        // Do nothing
    }

    @JsonCreator
    public CacheControlConfiguration(List<CacheControlConfigurationItem> items) {
        checkNotNull(items);
        _items = ImmutableList.copyOf(items);
    }

    @JsonValue
    public List<CacheControlConfigurationItem> getItems() {
        return _items;
    }

    public Function<String, Optional<String>> buildMapper() {
        if (_items.size() == 0) {
            return new Function<String, Optional<String>>() {
                public Optional<String> apply(@Nullable String input) {
                    return Optional.absent();
                }
            };
        }

        final List<CacheControlMap> maps = FluentIterable
                .from(_items)
                .transform(new Function<CacheControlConfigurationItem, CacheControlMap>() {
                    public CacheControlMap apply(CacheControlConfigurationItem input) {
                        return new CacheControlMap(input);
                    }
                })
                .toList();

        return CacheBuilder.newBuilder()
                .maximumSize(100)
                .build(
                        new CacheLoader<String, Optional<String>>() {
                            @Override
                            public Optional<String> load(String key) throws Exception {
                                for (CacheControlMap map : maps) {
                                    if (map.groupMatcher.apply(key)) {
                                        return Optional.of(map.options);
                                    }
                                }

                                return Optional.absent();
                            }
                        }
                );
    }

    private static class CacheControlMap {
        public final Predicate<String> groupMatcher;
        public final String options;

        public CacheControlMap(CacheControlConfigurationItem item) {
            this.groupMatcher = item.buildGroupMatcher();
            this.options = item.buildCacheControl().toString();
        }
    }
}
