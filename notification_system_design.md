Paste this FULL content directly into notification_system_design.md:

# Stage 1
## REST API Design
### Create Notification
POST /notifications
Request:
```json
{
  "studentId": 1042,
  "type": "Placement",
  "message": "Apple Inc. hiring"
}

Response:

{
  "notificationId": "uuid",
  "status": "created"
}

Fetch Notifications

GET /students/{studentId}/notifications

Fetch Unread Notifications

GET /students/{studentId}/notifications/unread

Mark Notification as Read

PATCH /notifications/{notificationId}/read

Delete Notification

DELETE /notifications/{notificationId}

Real-Time Notification Mechanism

WebSocket or Server-Sent Events (SSE) can be used for real-time notification delivery.

Flow:

1. Student connects to WebSocket.
2. Backend stores active connections.
3. When notification is created:
    * save notification in database
    * push event to connected users
4. Frontend updates instantly.

Benefits:

* real-time updates
* reduced polling
* better user experience

⸻

Stage 2

Persistent Storage Choice

PostgreSQL is selected because:

* strong consistency
* relational structure
* indexing support
* ACID compliance
* efficient querying

Tables

students

* id
* name
* email

notifications

* id
* notification_type
* message
* created_at

student_notifications

* id
* student_id
* notification_id
* is_read
* delivered_at

notification_delivery_status

* id
* notification_id
* email_status
* push_status
* retry_count

Scaling Issues

As the number of notifications increases:

* queries become slower
* sorting overhead increases
* table scans become expensive
* storage size grows rapidly

Solutions

* indexing
* pagination
* partitioning
* Redis caching
* read replicas
* archival strategy

⸻

Stage 3

Slow Query Analysis

Original query:

SELECT * FROM notifications
WHERE studentID = 1042 AND isRead = false
ORDER BY createdAt DESC;

Problems:

* SELECT *
* no LIMIT
* sorting large datasets
* missing composite index

Optimized Query

SELECT id, notification_type, message, created_at
FROM notifications
WHERE student_id = 1042
AND is_read = false
ORDER BY created_at DESC
LIMIT 50;

Composite Index

CREATE INDEX idx_notifications_student_unread_created
ON notifications(student_id, is_read, created_at DESC);

Why Not Index Every Column?

Problems:

* slower inserts
* slower updates
* storage overhead
* unnecessary maintenance cost

Indexes should only be created for frequent query patterns.

Placement Notifications in Last 7 Days

SELECT *
FROM notifications
WHERE notification_type = 'Placement'
AND created_at >= NOW() - INTERVAL '7 days'
ORDER BY created_at DESC;

⸻

Stage 4

Problem

Notifications are fetched from the database every time the page loads, increasing database load significantly.

Solutions

Redis Cache

Store recent unread notifications in Redis.

Pros:

* fast access
* reduced database load

Cons:

* cache invalidation complexity

Pagination

Load notifications in batches.

Pros:

* smaller payloads
* reduced response time

Cons:

* additional frontend logic

WebSocket / SSE

Push notifications in real-time.

Pros:

* avoids constant polling

Cons:

* persistent connection overhead

ETag / Last-Modified

Prevent unnecessary refetching.

Pros:

* bandwidth optimization

Cons:

* added caching logic

Final Recommendation

Use:

* Redis cache
* pagination
* WebSocket/SSE
* background refresh

⸻

Stage 5

Problems in Current notify_all Approach

Issues:

* sequential processing
* no retry mechanism
* email failures break execution
* tightly coupled DB and email operations
* poor scalability for 50,000 students

Improved Architecture

1. HR creates notification campaign.
2. Notifications are bulk inserted.
3. Email jobs are pushed to queue.
4. Worker services process jobs asynchronously.
5. Failed jobs are retried automatically.
6. Delivery tracking is maintained.

Recommended Technologies

* RabbitMQ
* Kafka
* BullMQ
* AWS SQS

Revised Pseudocode

create_campaign()
bulk_insert_notifications()
for batch in student_batches:
    queue.publish(batch)
worker():
    process_email()
    update_delivery_status()
    retry_if_failed()

⸻

Stage 6

Priority Inbox Design

Priority order:
Placement > Result > Event

Weights:

* Placement = 3
* Result = 2
* Event = 1

Notifications are sorted using:

1. type priority
2. recency

Algorithm

score = weight + recency

Higher score indicates higher priority.

Efficient Scaling

For large-scale systems:

* use min-heap
* maintain top N efficiently
* avoid sorting complete dataset repeatedly

Current implementation uses sorting because the dataset size is limited.

Final Result

Top N unread notifications are returned with:

* highest priority first
* latest notifications prioritized
* efficient response generation

