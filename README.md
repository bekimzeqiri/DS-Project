# Homework: Transactions and Concurrency Control in Distributed Systems

*Course: Distributed Systems*  
*Professor: Dardan Iseni*  
*Total Points: 100*

## Part A: Theoretical Questions (30 points)

### 1. ACID Properties (15 points)

**Atomicity in Distributed Transactions:**  
Atomicity ensures that a transaction is treated as a single, indivisible unit, which either completes entirely or not at all. In distributed systems, this means that operations spread across multiple nodes must all succeed or all fail together.

*Real-world example:* In an airline reservation system distributed across multiple data centers, when a customer books a flight, the system must atomically update both the seat availability database and the customer's reservation record. If the seat reservation succeeds but the customer record fails to update due to a node failure, atomicity ensures the entire transaction is rolled back, preventing an inconsistent state where a seat appears booked but no customer is assigned to it.

**Consistency in Distributed Transactions:**  
Consistency ensures that a transaction brings the database from one valid state to another valid state, maintaining all predefined rules and constraints. In distributed systems, this means ensuring that the global state remains consistent across all nodes.

*Real-world example:* In a distributed banking system, a funds transfer between accounts must maintain the invariant that the total money in the system remains constant. If Account A is debited $100, Account B must be credited exactly $100, even if these accounts are stored on different nodes. Consistency ensures that this balance constraint is preserved despite the distributed nature of the transaction.

**Isolation in Distributed Transactions:**  
Isolation ensures that concurrent transactions do not interfere with each other, making it appear as if each transaction executes in isolation. In distributed systems, this requires coordination across multiple nodes to prevent interference.

*Real-world example:* In an e-commerce inventory system distributed across multiple regions, two customers might simultaneously attempt to purchase the last item in stock. Isolation ensures that the transactions don't interfere with each other - one transaction will complete successfully and the other will see that the item is no longer available, rather than both transactions believing they succeeded in purchasing the same item.

**Durability in Distributed Transactions:**  
Durability guarantees that once a transaction is committed, its effects persist even in the event of system failures. In distributed systems, this means ensuring that committed data is stored reliably across multiple nodes.

*Real-world example:* When a user submits a payment on an e-commerce platform, the payment information is stored across multiple database nodes. If one data center experiences a power outage immediately after the transaction is committed, durability ensures that the payment record is not lost because it has been replicated to other nodes in the system.

### 2. Concurrency Issues (15 points)

**1. Lost Updates:**  
This occurs when two or more transactions concurrently read and update the same data, and one transaction's update overwrites another's without incorporating its changes.

*Example scenario:* Two bank tellers at different branches access a customer's account balance of $1000 simultaneously. Teller A adds $200, calculating a new balance of $1200. Teller B adds $300, calculating a new balance of $1300. If Teller A's update is processed first and then Teller B's update is processed second, the final balance would be $1300, not the correct $1500, because Teller B's calculation was based on the original balance and "lost" Teller A's update.

**2. Dirty Reads:**  
This occurs when a transaction reads data that has been modified by another concurrent transaction that has not yet committed.

*Example scenario:* In a distributed inventory system, Transaction A updates the stock level of a product from 10 to 5 units but hasn't committed. Transaction B reads the updated but uncommitted value (5 units) and makes a business decision based on this value. If Transaction A then aborts and rolls back, Transaction B has made a decision based on data that never actually existed in the committed database state.

**3. Phantom Reads:**  
This occurs when a transaction retrieves a set of rows based on a condition, and then a second transaction modifies or inserts rows that match this condition, causing the first transaction to see different rows on subsequent reads.

*Example scenario:* In a distributed hotel reservation system, Transaction A queries for available rooms for a specific date range and finds 5 rooms. Before Transaction A completes, Transaction B books one of these rooms and commits. When Transaction A performs the same query again within its transaction scope, it now finds only 4 available rooms, encountering a "phantom" change in the result set.

## Part B: Practical Scenario (40 points)

### 6. Concurrency Control Mechanisms (20 points)

**a. Describe what could go wrong if there is no concurrency control:**

Without concurrency control in the distributed banking scenario, several problematic outcomes could occur:

1. Both clients could read the initial balance of $1000 simultaneously.
2. Client A calculates a new balance of $800 after withdrawing $200.
3. Client B calculates a new balance of $500 after withdrawing $500.
4. Client A writes the new balance of $800 to the database.
5. Client B then overwrites this with its calculated balance of $500.

The final balance would be $500, meaning that only Client B's withdrawal of $500 is effectively recorded, while Client A's $200 withdrawal occurs (money is given to the customer) but is not properly deducted from the account balance. The bank loses $200 due to this inconsistency.

**b. Explain how locking can prevent inconsistencies:**

Locking can prevent this inconsistency through the following mechanism:

1. When Client A initiates a withdrawal, it acquires a lock on the account record.
2. Client B attempts to withdraw but must wait until the lock is released.
3. Client A reads the balance ($1000), calculates the new balance ($800), updates the database, and commits.
4. The lock is released, and Client B can now proceed.
5. Client B reads the updated balance ($800), calculates its new balance ($300), updates the database, and commits.

With locking, the final balance correctly reflects both withdrawals: $300. The operations are serialized, ensuring consistency of the account balance across the distributed system.

**c. What potential issues can locking introduce? Suggest a solution:**

Locking can introduce several issues:

1. **Deadlocks:** Two transactions might each hold a lock that the other needs, leading to a standstill. For example, Transaction A holds a lock on Account 1 and needs Account 2, while Transaction B holds Account 2 and needs Account 1.

2. **Performance degradation:** Excessive locking can significantly reduce system throughput, especially in distributed systems where lock management involves network communication.

3. **Long-duration locks:** If a client acquires a lock but experiences a failure, the lock might be held indefinitely, blocking other transactions.

**Solution: Deadlock Prevention and Detection**

1. **Timeouts:** Implement lock timeouts so that if a transaction holds a lock for too long, it is automatically aborted and its locks are released.

2. **Deadlock detection:** Implement a deadlock detection algorithm that periodically checks for cycles in the "waits-for" graph. When a cycle is detected, one of the transactions is selected as a victim and aborted to break the deadlock.

3. **Lock ordering:** Enforce a global ordering for acquiring locks (e.g., always lock accounts in ascending order by account ID). This prevents the circular wait condition necessary for deadlocks.

4. **Optimistic concurrency control:** Instead of locking, use timestamping or versioning to detect conflicts only at commit time. This approach works well in environments with low contention rates.

### 7. Distributed Transaction Coordination (20 points)

**a. How would you ensure atomicity across these services?**

To ensure atomicity across the inventory, payment, and order services, I would implement a distributed transaction coordinator that employs the Two-Phase Commit (2PC) protocol:

1. **Preparation Phase:**
   - The coordinator sends a "prepare" message to all three services.
   - Each service performs its operation tentatively (without committing):
     - Inventory service: Marks the product as "reserved" but doesn't confirm the reduction
     - Payment service: Authorizes the payment but doesn't settle it
     - Order service: Prepares the order record but doesn't finalize it
   - Each service reports back whether it can commit the operation.

2. **Commit Phase:**
   - If all services vote "yes" (can commit), the coordinator sends a "commit" message to all services.
   - If any service votes "no" (cannot commit), the coordinator sends an "abort" message to all services.
   - Upon receiving a "commit" message, each service finalizes its operation.
   - Upon receiving an "abort" message, each service rolls back any tentative changes.

This ensures that either all operations complete successfully or none of them do, maintaining atomicity across the distributed services.

**b. Which transaction protocol would you use and why?**

I would use the **Two-Phase Commit (2PC)** protocol enhanced with **saga patterns** for the following reasons:

1. **Two-Phase Commit (2PC)**:
   - Provides strong atomicity guarantees for distributed transactions
   - Well-established protocol with widespread implementation in distributed transaction managers
   - Ensures all participants either commit or abort together

2. **Saga pattern enhancement**:
   - Helps address the blocking nature of 2PC
   - Provides compensating transactions for more complex failure scenarios
   - Improves system resilience and availability

The combination of these approaches is beneficial because:
- 2PC provides the atomicity guarantees needed for critical transactions
- The saga pattern adds flexibility for handling long-running transactions and partial failures
- Together, they balance consistency requirements with operational resilience

**c. How would you handle a failure after the payment is processed but before the order is created?**

If a failure occurs after the payment is processed but before the order is created, I would implement the following recovery mechanism:

1. **Transaction Log/Journal:** Maintain a durable transaction log that records the state of the distributed transaction, including which services have committed or prepared to commit.

2. **Compensating Transactions:** Implement compensating actions for each service that can reverse its effects:
   - For the payment service: Implement a refund mechanism
   - For the inventory service: Release the reserved stock

3. **Recovery Procedure:**
   - When the system recovers, check the transaction log to identify incomplete transactions
   - For the specific scenario where payment succeeded but order creation failed:
     - Initiate a compensating transaction to refund the payment
     - Release any reserved inventory
     - Notify the customer about the failed transaction and refund

4. **Eventual Consistency Approach:**
   - Record the incomplete transaction in a "pending compensation" queue
   - Use a background process to periodically attempt to process these compensations
   - Implement idempotent operations to ensure that repeated compensation attempts don't cause issues

This approach ensures that the system eventually reaches a consistent state, even if not all operations in the distributed transaction can complete atomically.

## Part C: Research and Critical Thinking (30 points)

### 9. Real-World Systems (30 points)

**Google Spanner: Transactions and Concurrency Control in a Globally Distributed Database**

Google Spanner is a globally distributed database system that offers strong consistency guarantees across datacenters worldwide. It's designed to scale to millions of machines across hundreds of datacenters and trillions of database rows.

**How Google Spanner handles transactions and concurrency control:**

1. **TrueTime API and External Consistency:**
   Spanner uses Google's TrueTime API, which provides highly accurate time synchronization across global datacenters. This allows Spanner to assign timestamps to transactions that reflect real-time ordering, enabling it to provide external consistency (linearizability) - a property stronger than serializability.

2. **Two-Phase Locking (2PL) with Two-Phase Commit (2PC):**
   - Spanner uses a combination of 2PL for concurrency control within datacenters and 2PC for distributed transactions.
   - It implements a paxos-based replication system for each shard of data, ensuring fault tolerance.
   - For reads, Spanner uses multi-version concurrency control (MVCC) to allow read operations to proceed without blocking writes.

3. **Transaction Management:**
   - Spanner supports both read-only and read-write transactions.
   - Read-only transactions execute at a specific timestamp and don't acquire locks.
   - Read-write transactions use pessimistic concurrency control with wound-wait deadlock prevention.
   - It uses a hierarchical transaction manager to coordinate distributed transactions.

**Trade-offs regarding consistency, availability, and performance:**

1. **Strong Consistency vs. Availability:**
   - Spanner prioritizes strong consistency (linearizability) over availability.
   - During network partitions, Spanner may become unavailable in some regions rather than sacrifice consistency.
   - This makes Spanner a CP system in the CAP theorem classification.

2. **Latency Trade-offs:**
   - Global consistency comes at the cost of increased latency for write operations.
   - Writes generally require coordination across multiple datacenters.
   - Read operations can be served locally if the data is present in the local datacenter.

3. **Performance Optimizations:**
   - Spanner uses various techniques to mitigate performance issues:
     - Data can be geographically placed close to where it's frequently accessed
     - Read-only transactions have lower latency as they don't require distributed coordination
     - Multi-version concurrency control allows reads to proceed concurrently with writes

**A scenario where Google Spanner would be a good choice:**

Google Spanner would be an excellent choice for a global financial services application that processes transactions across multiple regions. Consider a multinational bank with customers and operations across North America, Europe, and Asia.

**Why Spanner is suitable for this scenario:**

1. **Strong Consistency Requirements:** Banking transactions require strong consistency guarantees to prevent issues like double-spending or incorrect balances, which Spanner provides through its externally consistent transactions.

2. **Global Distribution:** The bank needs to operate 24/7 across multiple time zones, requiring data to be globally distributed while maintaining a consistent view of account balances and transactions.

3. **Regulatory Compliance:** Financial regulations often require data locality (storing customer data in specific regions) while still allowing global operations. Spanner's ability to control data placement while maintaining global consistency addresses this requirement.

4. **Scalability with Consistency:** As the bank grows, it needs a database that can scale horizontally to handle increased transaction volumes while maintaining strict consistency, which is Spanner's core strength.

5. **Disaster Recovery:** Spanner's multi-region replication provides robust disaster recovery capabilities, ensuring that financial data remains accessible even if an entire datacenter goes offline.

The trade-off the bank accepts is slightly higher latency for write operations compared to eventually consistent systems, but this is an acceptable compromise given the critical nature of financial transactions where consistency cannot be sacrificed.

## References

1. Corbett, J. C., Dean, J., Epstein, M., Fikes, A., Frost, C., Furman, J. J., ... & Woodford, D. (2013). Spanner: Google's globally distributed database. ACM Transactions on Computer Systems (TOCS), 31(3), 1-22.

2. Bernstein, P. A., & Newcomer, E. (2009). Principles of transaction processing. Morgan Kaufmann.

3. Gray, J., & Lamport, L. (2006). Consensus on transaction commit. ACM Transactions on Database Systems (TODS), 31(1), 133-160.

4. Kleppmann, M. (2017). Designing data-intensive applications: The big ideas behind reliable, scalable, and maintainable systems. O'Reilly Media, Inc.
