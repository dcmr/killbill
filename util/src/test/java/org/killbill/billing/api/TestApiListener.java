/*
 * Copyright 2010-2011 Ning, Inc.
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.api;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.killbill.billing.events.BlockingTransitionInternalEvent;
import org.killbill.billing.events.BroadcastInternalEvent;
import org.killbill.billing.events.CustomFieldEvent;
import org.killbill.billing.events.EffectiveEntitlementInternalEvent;
import org.killbill.billing.events.EffectiveSubscriptionInternalEvent;
import org.killbill.billing.events.InvoiceAdjustmentInternalEvent;
import org.killbill.billing.events.InvoiceCreationInternalEvent;
import org.killbill.billing.events.InvoiceNotificationInternalEvent;
import org.killbill.billing.events.InvoicePaymentErrorInternalEvent;
import org.killbill.billing.events.InvoicePaymentInfoInternalEvent;
import org.killbill.billing.events.PaymentErrorInternalEvent;
import org.killbill.billing.events.PaymentInfoInternalEvent;
import org.killbill.billing.events.PaymentPluginErrorInternalEvent;
import org.killbill.billing.events.RepairSubscriptionInternalEvent;
import org.killbill.billing.events.TagDefinitionInternalEvent;
import org.killbill.billing.events.TagInternalEvent;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;

import static com.jayway.awaitility.Awaitility.await;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestApiListener {

    private static final Logger log = LoggerFactory.getLogger(TestApiListener.class);

    private static final Joiner SPACE_JOINER = Joiner.on(" ");

    private static final long DELAY = 25000;

    private final List<NextEvent> nextExpectedEvent;
    private final IDBI idbi;

    private boolean isListenerFailed = false;
    private String listenerFailedMsg;

    private volatile boolean completed;

    @Inject
    public TestApiListener(final IDBI idbi) {
        nextExpectedEvent = new Stack<NextEvent>();
        this.completed = false;
        this.idbi = idbi;
    }

    public void assertListenerStatus() {
        // Bail early
        if (isListenerFailed) {
            log.error(listenerFailedMsg);
            Assert.fail(listenerFailedMsg);
        }

        try {
            assertTrue(isCompleted(DELAY));
        } catch (final Exception e) {
            fail("assertListenerStatus didn't complete", e);
        }

        if (isListenerFailed) {
            log.error(listenerFailedMsg);
            Assert.fail(listenerFailedMsg);
        }
    }

    public enum NextEvent {
        MIGRATE_ENTITLEMENT,
        MIGRATE_BILLING,
        BROADCAST_SERVICE,
        CREATE,
        TRANSFER,
        RE_CREATE,
        CHANGE,
        CANCEL,
        UNCANCEL,
        PAUSE,
        RESUME,
        PHASE,
        BLOCK,
        INVOICE,
        INVOICE_NOTIFICATION,
        INVOICE_ADJUSTMENT,
        INVOICE_PAYMENT,
        INVOICE_PAYMENT_ERROR,
        PAYMENT,
        PAYMENT_ERROR,
        PAYMENT_PLUGIN_ERROR,
        REPAIR_BUNDLE,
        TAG,
        TAG_DEFINITION,
        CUSTOM_FIELD,
    }


    @Subscribe
    public void handleBroadcastEvents(final BroadcastInternalEvent event) {
        log.info(String.format("Got BroadcastInternalEvent event %s", event.toString()));
        assertEqualsNicely(NextEvent.BROADCAST_SERVICE);
        notifyIfStackEmpty();
    }


    @Subscribe
    public void handleRepairSubscriptionEvents(final RepairSubscriptionInternalEvent event) {
        log.info(String.format("Got RepairSubscriptionEvent event %s", event.toString()));
        assertEqualsNicely(NextEvent.REPAIR_BUNDLE);
        notifyIfStackEmpty();
    }

    @Subscribe
    public void handleEntitlementEvents(final EffectiveEntitlementInternalEvent eventEffective) {
        log.info(String.format("Got entitlement event %s", eventEffective.toString()));
        switch (eventEffective.getTransitionType()) {
            case BLOCK_BUNDLE:
                assertEqualsNicely(NextEvent.PAUSE);
                notifyIfStackEmpty();
                break;
            case UNBLOCK_BUNDLE:
                assertEqualsNicely(NextEvent.RESUME);
                notifyIfStackEmpty();
                break;
        }
    }

    @Subscribe
    public void handleEntitlementEvents(final BlockingTransitionInternalEvent event) {
        log.info(String.format("Got entitlement event %s", event.toString()));
        assertEqualsNicely(NextEvent.BLOCK);
        notifyIfStackEmpty();
    }

    @Subscribe
    public void handleSubscriptionEvents(final EffectiveSubscriptionInternalEvent eventEffective) {
        log.info(String.format("Got subscription event %s", eventEffective.toString()));
        switch (eventEffective.getTransitionType()) {
            case TRANSFER:
                assertEqualsNicely(NextEvent.TRANSFER);
                notifyIfStackEmpty();
                break;
            case MIGRATE_ENTITLEMENT:
                assertEqualsNicely(NextEvent.MIGRATE_ENTITLEMENT);
                notifyIfStackEmpty();
                break;
            case MIGRATE_BILLING:
                assertEqualsNicely(NextEvent.MIGRATE_BILLING);
                notifyIfStackEmpty();
                break;
            case CREATE:
                assertEqualsNicely(NextEvent.CREATE);
                notifyIfStackEmpty();
                break;
            case RE_CREATE:
                assertEqualsNicely(NextEvent.RE_CREATE);
                notifyIfStackEmpty();
                break;
            case CANCEL:
                assertEqualsNicely(NextEvent.CANCEL);
                notifyIfStackEmpty();
                break;
            case CHANGE:
                assertEqualsNicely(NextEvent.CHANGE);
                notifyIfStackEmpty();
                break;
            case UNCANCEL:
                assertEqualsNicely(NextEvent.UNCANCEL);
                notifyIfStackEmpty();
                break;
            case PHASE:
                assertEqualsNicely(NextEvent.PHASE);
                notifyIfStackEmpty();
                break;
            default:
                throw new RuntimeException("Unexpected event type " + eventEffective.getRequestedTransitionTime());
        }
    }

    @Subscribe
    public synchronized void processTagEvent(final TagInternalEvent event) {
        log.info(String.format("Got TagInternalEvent event %s", event.toString()));
        assertEqualsNicely(NextEvent.TAG);
        notifyIfStackEmpty();
    }

    @Subscribe
    public synchronized void processCustomFieldEvent(final CustomFieldEvent event) {
        log.info(String.format("Got CustomFieldEvent event %s", event.toString()));
        assertEqualsNicely(NextEvent.CUSTOM_FIELD);
        notifyIfStackEmpty();
    }

    @Subscribe
    public synchronized void processTagDefinitonEvent(final TagDefinitionInternalEvent event) {
        log.info(String.format("Got TagDefinitionInternalEvent event %s", event.toString()));
        assertEqualsNicely(NextEvent.TAG_DEFINITION);
        notifyIfStackEmpty();
    }

    @Subscribe
    public void handleInvoiceNotificationEvents(final InvoiceNotificationInternalEvent event) {
        log.info(String.format("Got Invoice notification event %s", event.toString()));
        assertEqualsNicely(NextEvent.INVOICE_NOTIFICATION);
        notifyIfStackEmpty();
    }

    @Subscribe
    public void handleInvoiceEvents(final InvoiceCreationInternalEvent event) {
        log.info(String.format("Got Invoice event %s", event.toString()));
        assertEqualsNicely(NextEvent.INVOICE);
        notifyIfStackEmpty();
    }

    @Subscribe
    public void handleInvoiceAdjustmentEvents(final InvoiceAdjustmentInternalEvent event) {
        log.info(String.format("Got Invoice adjustment event %s", event.toString()));
        assertEqualsNicely(NextEvent.INVOICE_ADJUSTMENT);
        notifyIfStackEmpty();
    }

    @Subscribe
    public void handleInvoicePaymentEvents(final InvoicePaymentInfoInternalEvent event) {
        log.info(String.format("Got InvoicePaymentInfo event %s", event.toString()));
        assertEqualsNicely(NextEvent.INVOICE_PAYMENT);
        notifyIfStackEmpty();
    }

    @Subscribe
    public void handleInvoicePaymentErrorEvents(final InvoicePaymentErrorInternalEvent event) {
        log.info(String.format("Got InvoicePaymentError event %s", event.toString()));
        assertEqualsNicely(NextEvent.INVOICE_PAYMENT_ERROR);
        notifyIfStackEmpty();
    }

    @Subscribe
    public void handlePaymentEvents(final PaymentInfoInternalEvent event) {
        log.info(String.format("Got PaymentInfo event %s", event.toString()));
        assertEqualsNicely(NextEvent.PAYMENT);
        notifyIfStackEmpty();
    }

    @Subscribe
    public void handlePaymentErrorEvents(final PaymentErrorInternalEvent event) {
        log.info(String.format("Got PaymentError event %s", event.toString()));
        assertEqualsNicely(NextEvent.PAYMENT_ERROR);
        notifyIfStackEmpty();
    }

    @Subscribe
    public void handlePaymentPluginErrorEvents(final PaymentPluginErrorInternalEvent event) {
        log.info(String.format("Got PaymentPluginError event %s", event.toString()));
        assertEqualsNicely(NextEvent.PAYMENT_PLUGIN_ERROR);
        notifyIfStackEmpty();
    }

    public void reset() {
        synchronized (this) {
            nextExpectedEvent.clear();
            completed = true;

            isListenerFailed = false;
            listenerFailedMsg = null;
        }
    }

    public void pushExpectedEvents(final NextEvent... events) {
        for (final NextEvent event : events) {
            pushExpectedEvent(event);
        }
    }

    public void pushExpectedEvent(final NextEvent next) {
        synchronized (this) {
            nextExpectedEvent.add(next);
            log.debug("Stacking expected event {}, got [{}]", next, SPACE_JOINER.join(nextExpectedEvent));
            completed = false;
        }
    }

    public boolean isCompleted(final long timeout) {
        synchronized (this) {
            long waitTimeMs = timeout;
            do {
                try {
                    final long before = System.currentTimeMillis();
                    wait(100);
                    if (completed) {
                        // TODO PIERRE Kludge alert!
                        // When we arrive here, we got notified by the current thread (Bus listener) that we received
                        // all expected events. But other handlers might still be processing them.
                        // Since there is only one bus thread, and that the test thread waits for all events to be processed,
                        // we're guaranteed that all are processed when the bus events table is empty.
                        // We also need to wait for in-processing notifications (see https://github.com/killbill/killbill/issues/475).
                        // This is really similar to TestResource#waitForNotificationToComplete.
                        await().atMost(timeout, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                final long inProcessing = idbi.withHandle(new HandleCallback<Long>() {
                                    @Override
                                    public Long withHandle(final Handle handle) throws Exception {
                                        return (Long) handle.select("select count(distinct record_id) count from bus_events").get(0).get("count") +
                                               // We assume all ready notifications have been picked up
                                               (Long) handle.select("select count(distinct record_id) count from notifications where processing_state = 'IN_PROCESSING'").get(0).get("count");
                                    }
                                });
                                log.debug("Events still in processing: {}", inProcessing);
                                return inProcessing == 0;
                            }
                        });
                        return completed;
                    }
                    final long after = System.currentTimeMillis();
                    waitTimeMs -= (after - before);
                } catch (final Exception ignore) {
                    final StringBuilder errorBuilder = new StringBuilder("isCompleted got interrupted. Exception: ").append(ignore)
                                                                                                                    .append("\nRemaining bus events:\n");
                    idbi.withHandle(new HandleCallback<Void>() {
                        @Override
                        public Void withHandle(final Handle handle) throws Exception {
                            final List<Map<String, Object>> busEvents = handle.select("select * from bus_events");
                            for (final Map<String, Object> busEvent : busEvents) {
                                errorBuilder.append(busEvent).append("\n");
                            }
                            return null;
                        }
                    });
                    log.error(errorBuilder.toString());
                    return false;
                }
            } while (waitTimeMs > 0 && !completed);
        }

        if (!completed) {
            final Joiner joiner = Joiner.on(" ");
            log.error("TestApiListener did not complete in " + timeout + " ms, remaining events are " + joiner.join(nextExpectedEvent));
        }

        return completed;
    }

    private void notifyIfStackEmpty() {
        log.debug("TestApiListener notifyIfStackEmpty ENTER");
        synchronized (this) {
            if (nextExpectedEvent.isEmpty()) {
                log.debug("notifyIfStackEmpty EMPTY");
                completed = true;
                notify();
            }
        }
        log.debug("TestApiListener notifyIfStackEmpty EXIT");
    }

    private void assertEqualsNicely(final NextEvent received) {

        synchronized (this) {
            boolean foundIt = false;
            final Iterator<NextEvent> it = nextExpectedEvent.iterator();
            while (it.hasNext()) {
                final NextEvent ev = it.next();
                if (ev == received) {
                    it.remove();
                    foundIt = true;
                    log.debug("Found expected event {}. Yeah!", received);
                    break;
                }
            }
            if (!foundIt) {
                log.error("Received unexpected event " + received + "; remaining expected events [" + SPACE_JOINER.join(nextExpectedEvent) + "]");
                failed("TestApiListener [ApiListenerStatus]: Received unexpected event " + received + "; remaining expected events [" + SPACE_JOINER.join(nextExpectedEvent) + "]");
            }
        }
    }

    private void failed(final String msg) {
        this.isListenerFailed = true;
        this.listenerFailedMsg = msg;
    }
}
