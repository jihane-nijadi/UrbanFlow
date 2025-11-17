# Messaging Architecture - Detailed Flow Guide

## 📁 Folder Structure Overview

```
messaging/
├── consumer/              📥 RECEIVES messages FROM other services
│   └── TicketEventConsumer.java
│
├── event/                 📦 MESSAGE DATA STRUCTURES
│   ├── TicketCreatedEvent.java       (Outgoing)
│   ├── TicketCancelledEvent.java     (Outgoing)
│   ├── TicketUsedEvent.java          (Outgoing)
│   ├── PaymentCompletedEvent.java    (Incoming)
│   └── RouteCancelledEvent.java      (Incoming)
│
└── publisher/             📤 SENDS messages TO other services
    └── TicketEventPublisher.java
```

---

## 🎯 Component Responsibilities

### 1. Event Classes (event/)

**Purpose:** Define the structure and data of messages exchanged between services.

Think of these as **standardized forms** that services fill out when communicating.

#### 📨 TicketCreatedEvent.java
```java
@Data
@Builder
public class TicketCreatedEvent {
    private Long ticketId;           // Which ticket was created
    private String userId;           // Who booked it
    private Long routeId;            // Which bus route
    private String origin;           // Starting point
    private String destination;      // End point
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal price;        // Ticket cost
    private String seatNumber;       // Assigned seat
    private LocalDateTime purchaseDate;
    private String eventType = "TICKET_CREATED";
    private LocalDateTime eventTimestamp;
}
```

**When Created:** Immediately after a ticket is saved to the database

**Who Uses It:** 
- Ticket Service creates it
- Payment Service receives it → processes payment
- Notification Service receives it → sends confirmation email
- Analytics Service receives it → records booking statistics

---

#### 📨 TicketCancelledEvent.java
```java
@Data
@Builder
public class TicketCancelledEvent {
    private Long ticketId;
    private String userId;
    private Long routeId;
    private BigDecimal refundAmount;      // Amount to refund
    private String seatNumber;            // Seat now available
    private LocalDateTime departureTime;
    private String cancellationReason;    // Why cancelled
    private String eventType = "TICKET_CANCELLED";
    private LocalDateTime eventTimestamp;
}
```

**When Created:** 
1. User manually cancels ticket
2. Route gets cancelled (auto-cancellation)
3. Payment fails

**Who Uses It:**
- Ticket Service creates it
- Refund Service receives it → processes refund
- Notification Service receives it → sends cancellation email
- Analytics Service receives it → records cancellation stats
- Route Service receives it → marks seat as available

---

#### 📨 TicketUsedEvent.java
```java
@Data
@Builder
public class TicketUsedEvent {
    private Long ticketId;
    private String userId;
    private Long routeId;
    private String seatNumber;
    private LocalDateTime usedAt;       // When scanned
    private String scanLocation;        // Where scanned (gate, bus, etc.)
    private String eventType = "TICKET_USED";
    private LocalDateTime eventTimestamp;
}
```

**When Created:** When user's ticket is scanned/validated

**Who Uses It:**
- Ticket Service creates it
- Analytics Service receives it → records usage patterns
- Route Service receives it → confirms passenger boarding

---

#### 📨 PaymentCompletedEvent.java
```java
@Data
@Builder
public class PaymentCompletedEvent {
    private Long paymentId;
    private Long ticketId;              // Links back to ticket
    private String userId;
    private BigDecimal amount;
    private String paymentMethod;       // CREDIT_CARD, DEBIT_CARD, etc.
    private String transactionId;       // Bank transaction ID
    private String status;              // "SUCCESS" or "FAILED"
    private String eventType = "PAYMENT_COMPLETED";
    private LocalDateTime eventTimestamp;
}
```

**When Created:** After Payment Service finishes processing payment

**Who Creates It:** Payment Service (NOT Ticket Service)

**Who Uses It:**
- Payment Service creates it
- Ticket Service receives it → updates ticket status

---

#### 📨 RouteCancelledEvent.java
```java
@Data
@Builder
public class RouteCancelledEvent {
    private Long routeId;
    private String routeName;
    private LocalDateTime scheduledDepartureTime;
    private String cancellationReason;  // "Weather", "Maintenance", etc.
    private LocalDateTime affectedDate;
    private String eventType = "ROUTE_CANCELLED";
    private LocalDateTime eventTimestamp;
}
```

**When Created:** When a bus route is cancelled by Route Service

**Who Creates It:** Route Service (NOT Ticket Service)

**Who Uses It:**
- Route Service creates it
- Ticket Service receives it → auto-cancels all tickets for that route
- Notification Service receives it → notifies affected passengers

---

### 2. Publisher (publisher/)

#### 📤 TicketEventPublisher.java

**Purpose:** Responsible for SENDING messages to RabbitMQ when important events happen in the Ticket Service.

**Think of it as:** The postal service's "sending department"

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    
    // Method 1: Publish when ticket is created
    public void publishTicketCreated(TicketCreatedEvent event) {
        log.info("Publishing ticket created event for ticket ID: {}", event.getTicketId());
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TICKET_EXCHANGE,      // Where to send: "ticket.events"
            RabbitMQConfig.TICKET_CREATED_KEY,   // Routing key: "ticket.created"
            event                                 // What to send: TicketCreatedEvent object
        );
        
        log.info("Successfully published ticket created event");
    }
    
    // Method 2: Publish when ticket is cancelled
    public void publishTicketCancelled(TicketCancelledEvent event) {
        log.info("Publishing ticket cancelled event for ticket ID: {}", event.getTicketId());
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TICKET_EXCHANGE,        // Exchange: "ticket.events"
            RabbitMQConfig.TICKET_CANCELLED_KEY,   // Routing key: "ticket.cancelled"
            event                                   // TicketCancelledEvent object
        );
        
        log.info("Successfully published ticket cancelled event");
    }
    
    // Method 3: Publish when ticket is used
    public void publishTicketUsed(TicketUsedEvent event) {
        log.info("Publishing ticket used event for ticket ID: {}", event.getTicketId());
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TICKET_EXCHANGE,    // Exchange: "ticket.events"
            RabbitMQConfig.TICKET_USED_KEY,    // Routing key: "ticket.used"
            event                               // TicketUsedEvent object
        );
        
        log.info("Successfully published ticket used event");
    }
}
```

**How It Works:**

1. **Service Layer** calls the publisher
2. **Publisher** creates the message
3. **RabbitTemplate** sends it to RabbitMQ
4. **RabbitMQ** routes it to appropriate queues
5. **Other services** receive and process the message

**Key Points:**
- Uses `RabbitTemplate` (Spring's RabbitMQ client)
- Includes error logging for debugging
- Non-blocking (doesn't wait for response)
- Converts Java objects to JSON automatically

---

### 3. Consumer (consumer/)

#### 📥 TicketEventConsumer.java

**Purpose:** RECEIVES and PROCESSES messages from other services via RabbitMQ.

**Think of it as:** The postal service's "receiving department"

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEventConsumer {

    private final TicketRepository ticketRepository;
    private final TicketEventPublisher ticketEventPublisher;
    
    // Listener 1: Handle payment completion
    @RabbitListener(queues = RabbitMQConfig.TICKET_PAYMENT_QUEUE)
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment completed event for ticket ID: {}", event.getTicketId());
        
        // Find the ticket
        Ticket ticket = ticketRepository.findById(event.getTicketId())
            .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        // Update ticket status based on payment result
        if ("SUCCESS".equals(event.getStatus())) {
            ticket.setStatus(TicketStatus.ACTIVE);  // Payment successful
            log.info("Ticket {} activated after successful payment", event.getTicketId());
        } else {
            ticket.setStatus(TicketStatus.CANCELLED);  // Payment failed
            log.warn("Ticket {} cancelled due to payment failure", event.getTicketId());
        }
        
        ticketRepository.save(ticket);
    }
    
    // Listener 2: Handle route cancellation
    @RabbitListener(queues = RabbitMQConfig.TICKET_ROUTE_QUEUE)
    @Transactional
    public void handleRouteCancelled(RouteCancelledEvent event) {
        log.info("Received route cancelled event for route ID: {}", event.getRouteId());
        
        // Find all active tickets for this route
        List<Ticket> activeTickets = ticketRepository.findByRouteId(event.getRouteId())
            .stream()
            .filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE)
            .toList();
        
        log.info("Found {} active tickets to cancel", activeTickets.size());
        
        // Cancel each ticket and trigger refunds
        for (Ticket ticket : activeTickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
            
            // Publish cancellation event for refund processing
            TicketCancelledEvent cancelledEvent = TicketCancelledEvent.builder()
                .ticketId(ticket.getId())
                .userId(ticket.getUserId())
                .routeId(ticket.getRouteId())
                .refundAmount(ticket.getPrice())
                .seatNumber(ticket.getSeatNumber())
                .departureTime(ticket.getDepartureTime())
                .cancellationReason("Route cancelled: " + event.getCancellationReason())
                .eventTimestamp(LocalDateTime.now())
                .build();
            
            ticketEventPublisher.publishTicketCancelled(cancelledEvent);
        }
        
        log.info("Successfully cancelled {} tickets", activeTickets.size());
    }
}
```

**How It Works:**

1. **@RabbitListener** annotation makes the method automatically listen to a queue
2. When a message arrives, the method is **automatically invoked**
3. Spring converts JSON message back to Java object
4. Method processes the event
5. **@Transactional** ensures database changes are atomic

**Key Features:**
- Automatic message deserialization (JSON → Java object)
- Built-in retry mechanism if processing fails
- Transaction management
- Error handling and logging

---

## 🔄 Complete Message Flows

### Flow 1: User Books a Ticket

```
┌─────────┐
│  USER   │ Clicks "Book Ticket"
└────┬────┘
     │
     ▼
┌─────────────────┐
│TicketController │ POST /api/tickets
└────┬────────────┘
     │
     ▼
┌─────────────────┐
│ TicketService   │ createTicket(request)
└────┬────────────┘
     │
     ├─► 1. Save to database
     │   └─► ticket.setStatus(PENDING)
     │
     ├─► 2. Create event
     │   └─► TicketCreatedEvent event = TicketCreatedEvent.builder()
     │           .ticketId(ticket.getId())
     │           .userId(ticket.getUserId())
     │           ... (all ticket data)
     │           .build();
     │
     ▼
┌──────────────────────┐
│TicketEventPublisher │ publishTicketCreated(event)
└────┬─────────────────┘
     │
     ├─► rabbitTemplate.convertAndSend(
     │       "ticket.events",     // Exchange
     │       "ticket.created",    // Routing Key
     │       event                // Message
     │   )
     │
     ▼
┌─────────────────┐
│    RabbitMQ     │ ticket.events Exchange
└────┬────────────┘
     │
     │ Routes based on "ticket.created" key
     │
     ├─────────────────┬──────────────────┬─────────────────┐
     │                 │                  │                 │
     ▼                 ▼                  ▼                 ▼
┌──────────┐    ┌──────────────┐  ┌──────────────┐  ┌──────────┐
│ Payment  │    │Notification  │  │  Analytics   │  │  Refund  │
│ Service  │    │   Service    │  │   Service    │  │ Service  │
└────┬─────┘    └──────┬───────┘  └──────┬───────┘  └────┬─────┘
     │                 │                  │                │
     │                 │                  │                │
     ▼                 ▼                  ▼                ▼
 Process          Send email         Record stats    Setup for
 Payment          to user           in database    future refund
     │
     │ Payment processed...
     │
     ▼
┌──────────────────────┐
│  Payment Service     │ Creates PaymentCompletedEvent
│  Event Publisher     │
└────┬─────────────────┘
     │
     ├─► Publishes to "payment.events" exchange
     │   with "payment.completed" routing key
     │
     ▼
┌─────────────────┐
│    RabbitMQ     │ payment.events Exchange
└────┬────────────┘
     │
     │ Routes to "ticket.payment.completed" queue
     │
     ▼
┌──────────────────────┐
│TicketEventConsumer  │ @RabbitListener listens to queue
└────┬─────────────────┘
     │
     ├─► handlePaymentCompleted(PaymentCompletedEvent)
     │
     ├─► Find ticket by ID
     │
     ├─► Check payment status
     │   ├─► If SUCCESS: ticket.setStatus(ACTIVE) ✅
     │   └─► If FAILED: ticket.setStatus(CANCELLED) ❌
     │
     └─► Save ticket to database
     
┌─────────┐
│  USER   │ Receives confirmation email
└─────────┘ Ticket is now ACTIVE! 🎉
```

---

### Flow 2: Route Gets Cancelled

```
┌─────────────────┐
│  Route Service  │ Detects problem (weather, breakdown)
└────┬────────────┘
     │
     ├─► Creates RouteCancelledEvent
     │
     ▼
┌──────────────────────┐
│ Route Service        │
│ Event Publisher      │
└────┬─────────────────┘
     │
     ├─► Publishes to "route.events" exchange
     │   with "route.cancelled" routing key
     │
     ▼
┌─────────────────┐
│    RabbitMQ     │ route.events Exchange
└────┬────────────┘
     │
     │ Routes to "ticket.route.cancelled" queue
     │
     ▼
┌──────────────────────┐
│TicketEventConsumer  │ @RabbitListener automatically invoked
└────┬─────────────────┘
     │
     ├─► handleRouteCancelled(RouteCancelledEvent)
     │
     ├─► Find all ACTIVE tickets for this route
     │   └─► ticketRepository.findByRouteId(routeId)
     │
     ├─► For EACH active ticket:
     │   │
     │   ├─► 1. Set status to CANCELLED
     │   │   └─► ticket.setStatus(CANCELLED)
     │   │
     │   ├─► 2. Save to database
     │   │
     │   ├─► 3. Create TicketCancelledEvent
     │   │   └─► Include refund amount, reason, etc.
     │   │
     │   └─► 4. Publish event
     │       └─► ticketEventPublisher.publishTicketCancelled(event)
     │
     ▼
┌─────────────────┐
│    RabbitMQ     │ ticket.events Exchange
└────┬────────────┘
     │
     │ Routes "ticket.cancelled" to multiple services
     │
     ├────────────────┬─────────────────┬──────────────────┐
     │                │                 │                  │
     ▼                ▼                 ▼                  ▼
┌──────────┐   ┌──────────────┐  ┌──────────────┐  ┌──────────┐
│  Refund  │   │Notification  │  │  Analytics   │  │  Route   │
│ Service  │   │   Service    │  │   Service    │  │ Service  │
└────┬─────┘   └──────┬───────┘  └──────┬───────┘  └────┬─────┘
     │                │                  │                │
     ▼                ▼                  ▼                ▼
Process          Send email         Record          Mark seats
refunds         to all users      cancellation     as available
automatically                        stats           again

┌─────────┐
│  USERS  │ All affected users receive:
└─────────┘ 1. Cancellation notification email
            2. Automatic refund processed
            3. Apology message with reason
```

---

### Flow 3: User Uses Ticket (Scans QR Code)

```
┌─────────┐
│  USER   │ Scans QR code at bus station
└────┬────┘
     │
     ▼
┌─────────────────┐
│TicketController │ PUT /api/tickets/{id}/use
└────┬────────────┘
     │
     ▼
┌─────────────────┐
│ TicketService   │ useTicket(ticketId, userId)
└────┬────────────┘
     │
     ├─► 1. Validate ticket
     │   ├─► Check if ACTIVE
     │   ├─► Check if not expired
     │   └─► Check ownership
     │
     ├─► 2. Update status
     │   └─► ticket.setStatus(USED)
     │
     ├─► 3. Save to database
     │
     ├─► 4. Create event
     │   └─► TicketUsedEvent event = TicketUsedEvent.builder()
     │           .ticketId(ticket.getId())
     │           .userId(ticket.getUserId())
     │           .usedAt(LocalDateTime.now())
     │           .scanLocation("Gate A")
     │           .build();
     │
     ▼
┌──────────────────────┐
│TicketEventPublisher │ publishTicketUsed(event)
└────┬─────────────────┘
     │
     ├─► rabbitTemplate.convertAndSend(
     │       "ticket.events",
     │       "ticket.used",
     │       event
     │   )
     │
     ▼
┌─────────────────┐
│    RabbitMQ     │ ticket.events Exchange
└────┬────────────┘
     │
     │ Routes "ticket.used" to services
     │
     ├────────────────┬─────────────────┐
     │                │                 │
     ▼                ▼                 ▼
┌──────────┐   ┌──────────────┐  ┌──────────┐
│Analytics │   │    Route     │  │  Fraud   │
│ Service  │   │   Service    │  │Detection │
└────┬─────┘   └──────┬───────┘  └────┬─────┘
     │                │                │
     ▼                ▼                ▼
Record           Confirm          Check for
usage           passenger         duplicate
patterns         boarded          scans

┌─────────┐
│  USER   │ Ticket validated! Boards bus 🚌
└─────────┘
```

---

## 🎭 Message Flow Patterns

### Pattern 1: One-to-Many (Broadcast)
```
       Ticket Service
             │
             │ publishes "ticket.created"
             │
             ▼
         RabbitMQ
             │
    ┌────────┼────────┬────────┐
    │        │        │        │
    ▼        ▼        ▼        ▼
Payment  Notification Analytics Refund
Service   Service     Service  Service

One event → Multiple consumers
```

### Pattern 2: Request-Response (via Events)
```
Ticket Service                Payment Service
     │                              │
     │ ─────ticket.created────────▶ │
     │                              │
     │                         Process
     │                         Payment
     │                              │
     │ ◀───payment.completed─────── │
     │                              │
Update Ticket                       │
  Status
```

### Pattern 3: Chain Reaction (Event Cascade)
```
Route Service
     │
     │ route.cancelled
     │
     ▼
Ticket Service
     │
     │ Auto-cancel tickets
     │
     ├─► ticket.cancelled (100 events!)
     │
     ▼
Refund Service
     │
     │ Process 100 refunds
     │
     ▼
Notification Service
     │
     │ Send 100 emails
```

---

## 🛡️ Error Handling & Reliability

### 1. Publisher Error Handling
```java
public void publishTicketCreated(TicketCreatedEvent event) {
    try {
        rabbitTemplate.convertAndSend(...);
        log.info("Successfully published");
    } catch (Exception e) {
        log.error("Failed to publish", e);
        // Event publication fails but ticket is still created
        // Can be retried or logged for manual intervention
    }
}
```

**Strategy:**
- Log errors for monitoring
- Don't fail the main transaction
- Can implement retry logic
- Dead letter queues for failed messages

### 2. Consumer Error Handling
```java
@RabbitListener(queues = "ticket.payment.completed")
@Transactional
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    try {
        // Process event
    } catch (Exception e) {
        log.error("Error processing payment event", e);
        throw e;  // Triggers RabbitMQ retry mechanism
    }
}
```

**Strategy:**
- @Transactional ensures atomicity
- Throwing exception triggers retry
- Configure max retries in RabbitMQ
- Dead letter queue for permanent failures

### 3. Message Persistence
```java
@Bean
public Queue paymentQueue() {
    return QueueBuilder.durable(PAYMENT_QUEUE)  // Survives broker restart
        .withArgument("x-message-ttl", 3600000)  // 1 hour TTL
        .build();
}
```

**Features:**
- Durable queues survive restarts
- Messages persist to disk
- TTL prevents old message processing
- Dead letter queues for expired messages

---

## 📊 Monitoring & Observability

### What to Monitor:

1. **Publisher Metrics:**
   - Messages published per minute
   - Publishing failures
   - Average publish time

2. **Consumer Metrics:**
   - Messages consumed per minute
   - Processing failures
   - Average processing time
   - Queue depth (backlog)

3. **Queue Health:**
   - Number of pending messages
   - Message age
   - Consumer count
   - Dead letter queue size

### Logging Strategy:
```java
log.info("Publishing ticket created event for ticket ID: {}", event.getTicketId());
// Includes: timestamp, ticket ID, event type

log.error("Failed to publish event", exception);
// Includes: full stack trace, context

log.info("Successfully processed {} tickets", count);
// Includes: metrics, counts
```

---

## 🔑 Key Takeaways

### Publisher (Sending):
✅ Sends events to RabbitMQ  
✅ Fire and forget (non-blocking)  
✅ Converts Java objects to JSON  
✅ Logs success/failure  

### Consumer (Receiving):
✅ Automatically listens to queues  
✅ Converts JSON back to Java objects  
✅ Processes events transactionally  
✅ Automatic retry on failure  

### Events (Messages):
✅ Define data structure  
✅ Self-documenting  
✅ Versioned (can add fields)  
✅ Immutable once sent  

### Benefits:
✅ **Loose Coupling** - Services don't know about each other  
✅ **Scalability** - Add consumers without changing publishers  
✅ **Reliability** - Messages persist until processed  
✅ **Async Processing** - Fast response times  
✅ **Fault Tolerance** - Retry mechanisms built-in  

---

## 🚀 Summary

The messaging architecture enables:

1. **Ticket Service** to notify others when important events happen
2. **Other Services** to react to these events independently
3. **Loose coupling** between services
4. **High reliability** through message persistence
5. **Easy scalability** by adding more consumers
6. **Fast response times** through asynchronous processing

**Remember:** 
- Publisher = Send messages out 📤
- Consumer = Receive messages in 📥
- Events = The messages themselves 📦
- RabbitMQ = The delivery system 🐰

All working together to create a robust, scalable microservices architecture! 🎯
