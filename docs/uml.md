# LabLink UML Models

All diagrams describe the code in the repository as of Milestone 2.

## 1. Use case diagram

Actors are on the left; use cases (ovals) sit inside the LabLink system boundary. Faculty and Lab Manager inherit every use case of the actor they generalize.

```mermaid
flowchart LR
    student(["🧑 Student"])
    faculty(["🧑‍🏫 Faculty"])
    manager(["🧑‍💼 Lab Manager"])

    subgraph LabLink["LabLink system"]
        UC1(["Register account"])
        UC2(["Log in / log out"])
        UC3(["Browse labs"])
        UC4(["Browse equipment"])
        UC5(["Request reservation"])
        UC6(["View my reservations"])
        UC7(["Cancel reservation"])
        UC13(["Auto-approve reservation"])
        UC8(["Manage labs (CRUD)"])
        UC9(["Manage equipment (CRUD)"])
        UC10(["Review pending requests"])
        UC5a(["Check for conflicts"])
        UC11(["Approve request"])
        UC12(["Reject request"])
    end

    student --- UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC7
    faculty --- UC13
    manager --- UC8 & UC9 & UC10
    faculty -- "generalizes ▷" --> student
    manager -- "generalizes ▷" --> faculty

    UC5 -. "«include»" .-> UC5a
    UC13 -. "«extend»" .-> UC5
    UC10 -. "«include»" .-> UC11
    UC10 -. "«include»" .-> UC12
```

| Use case | Actor | Summary |
|---|---|---|
| Register account | Student | Create a STUDENT account with name, email and password |
| Log in / log out | All | Authenticate with email and password |
| Browse labs / equipment | All | List rooms and items with location and status |
| Request reservation | All | Pick a resource and time window; the system checks for conflicts |
| Auto-approve | Faculty, Lab Manager | Requests from these roles skip the approval queue |
| View / cancel my reservations | All | See own reservations; cancel ones still pending or approved |
| Manage labs / equipment | Lab Manager | Create, edit, delete inventory |
| Review / approve / reject | Lab Manager | Decide on students' pending requests |

## 2. Class diagram

Domain entities, the services that hold business rules, and how controllers and repositories connect to them. (Getters/setters omitted.)

```mermaid
classDiagram
    direction LR

    class Role {
        <<enumeration>>
        STUDENT
        FACULTY
        LAB_MANAGER
    }
    class EquipmentStatus {
        <<enumeration>>
        AVAILABLE
        MAINTENANCE
        RETIRED
    }
    class ReservationStatus {
        <<enumeration>>
        PENDING
        APPROVED
        REJECTED
        CANCELLED
        +ACTIVE$ List~ReservationStatus~
    }

    class User {
        -Long id
        -String email
        -String fullName
        -String passwordHash
        -Role role
        -LocalDateTime createdAt
    }
    class Lab {
        -Long id
        -String name
        -String building
        -String roomNumber
        -int capacity
        -String description
        -boolean active
    }
    class Equipment {
        -Long id
        -String name
        -String serialNumber
        -String description
        -EquipmentStatus status
    }
    class Reservation {
        -Long id
        -LocalDateTime startTime
        -LocalDateTime endTime
        -String purpose
        -ReservationStatus status
        -LocalDateTime createdAt
        -LocalDateTime reviewedAt
        +getResourceName() String
        +getResourceType() String
    }

    User "1" <-- "0..*" Reservation : requestedBy
    User "0..1" <-- "0..*" Reservation : reviewedBy
    Lab "0..1" <-- "0..*" Reservation : reserves
    Equipment "0..1" <-- "0..*" Reservation : reserves
    Lab "0..1" <-- "0..*" Equipment : locatedIn
    User --> Role
    Equipment --> EquipmentStatus
    Reservation --> ReservationStatus

    class UserService {
        +registerStudent(fullName, email, rawPassword) User
        +emailTaken(email) boolean
        +getByEmail(email) User
    }
    class InventoryService {
        +allLabs() List~Lab~
        +saveLab(Lab) Lab
        +deleteLab(Long)
        +allEquipment() List~Equipment~
        +saveEquipment(Equipment) Equipment
        +deleteEquipment(Long)
    }
    class ReservationService {
        +requestLab(User, labId, start, end, purpose) Reservation
        +requestEquipment(User, equipmentId, start, end, purpose) Reservation
        +approve(reservationId, User manager)
        +reject(reservationId, User manager)
        +cancel(reservationId, User requester)
        +forUser(User) List~Reservation~
        +pending() List~Reservation~
        -validateTimes(start, end)
    }
    class ReservationRepository {
        <<interface>>
        +existsByLabAndStatusInAndStartTimeBeforeAndEndTimeAfter(...) boolean
        +existsByEquipmentAndStatusInAndStartTimeBeforeAndEndTimeAfter(...) boolean
        +findByUserOrderByStartTimeDesc(User) List~Reservation~
    }
    class ReservationController {
        +newForm(labId, equipmentId) String
        +create(ReservationForm) String
        +cancel(id) String
        +manage() String
        +approve(id) String
        +reject(id) String
    }

    ReservationController --> ReservationService
    ReservationController --> InventoryService
    ReservationController --> UserService
    ReservationService --> ReservationRepository
    ReservationService ..> Reservation : creates
    ReservationService ..> ReservationException : throws
    class ReservationException
```

## 3. Sequence diagram — Request a lab reservation (with conflict check)

```mermaid
sequenceDiagram
    autonumber
    actor S as Student
    participant B as Browser
    participant SEC as Security filter chain
    participant RC as ReservationController
    participant US as UserService
    participant RS as ReservationService
    participant LR as LabRepository
    participant RR as ReservationRepository
    participant DB as Database

    S->>B: Fill start, end, purpose and click "Submit request"
    B->>SEC: POST /reservations (labId, startTime, endTime, purpose, CSRF token)
    SEC->>SEC: Check session is authenticated and CSRF token valid
    SEC->>RC: create(form)
    RC->>RC: Bean validation (@NotNull, @NotBlank)
    RC->>US: getByEmail(principal)
    US-->>RC: User (role = STUDENT)
    RC->>RS: requestLab(user, labId, start, end, purpose)
    RS->>LR: findById(labId)
    LR->>DB: SELECT … FROM labs WHERE id = ?
    DB-->>LR: lab row
    LR-->>RS: Lab (active = true)
    RS->>RS: validateTimes(start, end)
    RS->>RR: existsByLabAndStatusIn…(lab, [PENDING, APPROVED], end, start)
    RR->>DB: SELECT … WHERE lab_id = ? AND status IN (…) AND start_time < ? AND end_time > ?
    DB-->>RR: no rows
    RR-->>RS: false
    alt overlap found
        RS-->>RC: throw ReservationException("already booked")
        RC-->>B: Re-render form with error message
    else no overlap
        RS->>RS: status = PENDING (student) / APPROVED (faculty)
        RS->>RR: save(reservation)
        RR->>DB: INSERT INTO reservations …
        DB-->>RR: new id
        RR-->>RS: Reservation
        RS-->>RC: Reservation
        RC-->>B: 302 redirect to /reservations with "Reservation submitted."
        B->>S: Shows "My reservations" with status PENDING
    end
```

## 4. Sequence diagram — Register and log in

```mermaid
sequenceDiagram
    autonumber
    actor U as New user
    participant B as Browser
    participant AC as AuthController
    participant US as UserService
    participant PE as PasswordEncoder (BCrypt)
    participant UR as UserRepository
    participant SEC as Spring Security
    participant UDS as LabLinkUserDetailsService

    U->>B: Submit registration form
    B->>AC: POST /register (fullName, email, password, confirmPassword)
    AC->>AC: Validate fields, check passwords match
    AC->>US: emailTaken(email)
    US->>UR: existsByEmail(email)
    UR-->>US: false
    US-->>AC: false
    AC->>US: registerStudent(fullName, email, password)
    US->>PE: encode(password)
    PE-->>US: $2a$10$… hash
    US->>UR: save(User role=STUDENT)
    UR-->>US: User
    AC-->>B: 302 redirect /login "Account created"

    U->>B: Enter email + password on login page
    B->>SEC: POST /login
    SEC->>UDS: loadUserByUsername(email)
    UDS->>UR: findByEmail(email)
    UR-->>UDS: User
    UDS-->>SEC: UserDetails (hash, ROLE_STUDENT)
    SEC->>PE: matches(password, hash)
    PE-->>SEC: true
    SEC->>SEC: Create session, store authentication
    SEC-->>B: 302 redirect to / (dashboard), session cookie
```

## 5. Sequence diagram — Lab manager approves a request

```mermaid
sequenceDiagram
    autonumber
    actor M as Lab Manager
    participant B as Browser
    participant SEC as Security filter chain
    participant RC as ReservationController
    participant RS as ReservationService
    participant RR as ReservationRepository

    M->>B: Open "Manage requests", click Approve
    B->>SEC: POST /reservations/42/approve
    SEC->>SEC: hasRole('LAB_MANAGER')?
    alt not a lab manager
        SEC-->>B: 403 Forbidden
    else lab manager
        SEC->>RC: approve(42)
        RC->>RS: approve(42, manager)
        RS->>RS: manager.role == LAB_MANAGER?
        RS->>RR: findById(42)
        RR-->>RS: Reservation (PENDING)
        RS->>RS: status = APPROVED, reviewedBy = manager, reviewedAt = now
        Note over RS,RR: Transaction commits → UPDATE reservations
        RS-->>RC: done
        RC-->>B: 302 redirect /reservations/manage "Reservation approved."
    end
```

## 6. Activity diagram — Reservation life cycle

```mermaid
flowchart TD
    start((●)) --> pick["User selects a lab or equipment item<br/>and clicks Reserve"]
    pick --> fill["Enter start, end and purpose"]
    fill --> valid{"Fields filled in and<br/>end after start and<br/>start in the future?"}
    valid -- No --> err1["Show validation error"] --> fill
    valid -- Yes --> avail{"Resource active /<br/>AVAILABLE?"}
    avail -- No --> err2["Show 'not available'"] --> stop1((◉))
    avail -- Yes --> conflict{"Overlaps a PENDING or<br/>APPROVED reservation?"}
    conflict -- Yes --> err3["Show 'already booked'"] --> fill
    conflict -- No --> role{"Requester role?"}
    role -- "Faculty / Lab Manager" --> approved["Save as APPROVED"]
    role -- Student --> pending["Save as PENDING"]
    pending --> review{"Lab manager reviews"}
    review -- Approve --> approved
    review -- Reject --> rejected["Status = REJECTED"] --> stop2((◉))
    pending -. "owner cancels" .-> cancelled["Status = CANCELLED<br/>(slot freed)"]
    approved -. "owner cancels" .-> cancelled
    cancelled --> stop3((◉))
    approved --> use["Reservation time arrives:<br/>user uses the lab/equipment"] --> stop4((◉))
```
