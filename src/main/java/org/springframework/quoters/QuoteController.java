/*
 * Copyright 2014-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.quoters;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@RestController
public class QuoteController {

	Logger logger = LoggerFactory.getLogger(QuoteController.class);
	private final static Quote NONE = new Quote("None");
	private final static Random RANDOMIZER = new Random();
	 private final AttributeKey<String> ATTR_METHOD = AttributeKey.stringKey("method");

	  private final Random random = new Random();
	  private final Tracer tracer;
	  private final LongHistogram doWorkHistogram;

	private final QuoteRepository repository;

	public QuoteController(QuoteRepository repository,OpenTelemetry openTelemetry) {
		tracer = openTelemetry.getTracer(QuotersIncorporatedApplication.class.getName());
	    Meter meter = openTelemetry.getMeter(QuotersIncorporatedApplication.class.getName());
	    doWorkHistogram = meter.histogramBuilder("do-work").ofLongs().build();
		this.repository = repository;
	}

	@GetMapping("/api")
	public List<QuoteResource> getAll() {
		
		
		List<QuoteResource> quotes = repository.findAll().stream()
				.map(quote -> new QuoteResource(quote, "success"))
				.collect(Collectors.toList());
		
		logger.debug("Response is:"+quotes.toString());
		
		return quotes;
		
	}

	@GetMapping("/api/{id}")
	public QuoteResource getOne(@PathVariable Long id) {

		QuoteResource quoteResource =  repository.findById(id)
			.map(quote -> new QuoteResource(quote, "success"))
			.orElse(new QuoteResource(NONE, "Quote " + id + " does not exist"));
		
			logger.debug("Response is:"+quoteResource.toString());
		
		return quoteResource;
		
	}

	@GetMapping("/api/random")
	public QuoteResource getRandomOne() {
		return getOne(nextLong(1, repository.count() + 1));
	}
	
	@GetMapping("/ping")
	  public String ping() throws InterruptedException {
	    int sleepTime = random.nextInt(200);
	    doWork(sleepTime);
	    doWorkHistogram.record(sleepTime, Attributes.of(ATTR_METHOD, "ping"));
	    return "pong";
	  }

	  private void doWork(int sleepTime) throws InterruptedException {
	    Span span = tracer.spanBuilder("doWork").startSpan();
	    try (Scope ignored = span.makeCurrent()) {
	      Thread.sleep(sleepTime);
	      logger.info("A sample log message!");
	    } finally {
	      span.end();
	    }
	  }

	private long nextLong(long lowerRange, long upperRange) {
		return (long) (RANDOMIZER.nextDouble() * (upperRange - lowerRange)) + lowerRange;
	}
}
