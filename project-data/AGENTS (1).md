# AGENTS.md — LearnFlow conventions contract

> Nguồn: `plan/phases/00-overview.md` §1. File này tồn tại để mọi agent/phiên làm việc trong repo
> tự động đọc được quy ước mà không cần dẫn tới `plan/`. Khi `00-overview.md` §1 đổi, cập nhật lại file này.

## 1. Bản giao kèo quy ước (Conventions Contract)

> Phần này quan trọng nhất cho yêu cầu "thống nhất, nhất quán".

### 1.1. Cấu trúc module backend

Mỗi domain là **một package phẳng** dưới `com.learnflow.backend`:

```
com.learnflow.backend.<module>/
├── <Xxx>Controller.java        // @RestController — chỉ map HTTP <-> DTO, không có business logic
├── <Xxx>Service.java           // @Service @Transactional — toàn bộ business logic
├── <Xxx>Repository.java        // interface extends JpaRepository<Entity, Long>
├── domain/<Entity>.java        // @Entity
└── dto/<Xxx>Request.java, <Xxx>Response.java   // java record + jakarta validation
```

Module list (tạo dần theo phase): `common`, `auth`, `language`, `vocabulary`, `srs`, `study`, `progress`, `dashboard`, `ai`, `mistake`, `dailyplan`.

**Ba luật ranh giới — không được vi phạm:**
1. Controller **không bao giờ** inject Repository. Luôn đi qua Service.
2. Service của module A **không** inject Repository của module B. Chỉ gọi Service public của B.
3. Module `ai` **không** được depend vào bất kỳ repository nào của `srs`. AI không được ghi lịch ôn. → Chặn cứng bằng ArchUnit test ở Phase 5.

### 1.2. Quy ước API

- Base path `/api`. Tất cả endpoint yêu cầu `Authorization: Bearer <jwt>`, **trừ** `/api/auth/register`, `/api/auth/login`, `/api/health`.
- Resource dạng số nhiều, kebab-case: `/api/vocabulary`, `/api/reviews/due`, `/api/study-sessions`, `/api/daily-plan/today`.
- Request/Response body là **java `record`**, đặt tên `XxxRequest` / `XxxResponse`. Không trả entity trực tiếp ra ngoài.
- JSON field: `camelCase`. Thời gian: ISO-8601 UTC (`Instant`).
- Lỗi: **RFC 7807 `ProblemDetail`** (built-in Spring). Lỗi validate trả 400 kèm extension `errors: { "<field>": "<message>" }`.
- Phân trang: query `?page=0&size=20`, trả `PageResponse<T> { content, page, size, totalElements, totalPages }` (tự định nghĩa ở `common/web`, **không** trả `Page` của Spring ra ngoài).

### 1.3. Quy ước database

- Migration: `backend/src/main/resources/db/migration/V{n}__{snake_case_desc}.sql`, đánh số tăng dần, **không bao giờ sửa file đã apply** — sai thì thêm migration mới.
- `spring.jpa.hibernate.ddl-auto=validate` **luôn luôn**. Schema chỉ do Flyway tạo.
- `spring.jpa.open-in-view=false`.
- Tên bảng/cột: `snake_case`, số ít (`vocabulary`, `review_history`, `app_user`).
- Mọi cột thời gian: `TIMESTAMPTZ`, map sang `Instant`.
- Khoá chính: `BIGSERIAL` → `Long` (trừ `language` dùng `SMALLSERIAL`).

### 1.4. Thời gian & tính xác định

- **Luôn inject `java.time.Clock`**, không bao giờ gọi `Instant.now()` trực tiếp trong service.
- `common/config/ClockConfig` cung cấp `Clock.systemUTC()`; test inject `Clock.fixed(...)`.
- Đây là điều kiện để test SRS deterministic được.

### 1.5. Quy ước frontend

```
frontend/src/
├── api/
│   ├── client.ts               // fetch wrapper: gắn Bearer, parse ProblemDetail, ném ApiError, 401 -> logout
│   └── <domain>.ts             // hàm thuần gọi API: fetchVocabularyList, createVocabulary...
├── hooks/use<Domain>.ts        // wrap react-query, export <domain>Keys
├── pages/<Name>Page.tsx
├── components/
│   ├── ui/                     // Button, Input, Select, Card, Dialog, Badge — dùng chung
│   └── <domain>/<Name>.tsx
├── stores/authStore.ts, uiStore.ts   // zustand (+ persist cho auth)
├── types/domain.ts             // mirror DTO backend, viết tay
├── lib/cn.ts, lib/format.ts
└── router.tsx
```

- Tên file component = tên component, PascalCase. Hook `useXxx`. Hàm API dùng động từ (`fetch*`, `create*`, `update*`, `delete*`).
- Query key tập trung mỗi domain: `export const vocabularyKeys = { all: ['vocabulary'], list: (p) => [...], detail: (id) => [...] }`. Không viết mảng key rời rạc trong component.
- Server state **chỉ** ở react-query. Zustand chỉ giữ auth token + UI state (ngôn ngữ đang chọn).
- UI copy: **tiếng Anh**.
- Không thêm dependency ngoài danh sách đã chốt nếu không có lý do ghi vào decision log.

### 1.6. Quy ước test

| Loại | Đặt tên | Công cụ |
|---|---|---|
| Unit (không Spring context) | `XxxTest.java` | JUnit 5 (+ Mockito khi cần) |
| Integration (có DB thật) | `XxxIntegrationTest.java` extends `AbstractIntegrationTest` | `@SpringBootTest` + Testcontainers `@ServiceConnection` |
| Ranh giới kiến trúc | `ArchitectureTest.java` | ArchUnit (từ Phase 5) |
| Frontend | `Xxx.test.tsx` cạnh file | Vitest + React Testing Library |

- `AbstractIntegrationTest` giữ **một** `PostgreSQLContainer` static dùng chung cho cả suite.
- **Không test nào được gọi API Claude thật.** Luôn mock `AIProvider`.
- Ưu tiên tuyệt đối: `Sm2AlgorithmTest` phải phủ đủ mọi rating × mọi trạng thái.

### 1.7. Quy ước git & vòng lặp làm việc

- **`main` và `dev` không bao giờ nhận commit/push trực tiếp.** Mọi thay đổi — kể cả 1 task nhỏ trong 1 phase —
  đi qua branch riêng rồi mở Pull Request merge vào `dev`. `dev` merge vào `main` khi có bản release/deploy.
- Branch đặt từ `dev`, đặt tên `phase/<n>-<slug>` cho cả phase, hoặc `phase/<n>-<slug>/<task-slug>` khi một
  task đủ lớn để tách branch riêng trong phase (vd `phase/3-srs-review`, `phase/3-srs-review/sm2-algorithm`).
  Tên branch phải cho biết ngay đang làm phase nào để dễ theo dõi tiến độ trên GitHub.
- Mỗi branch → một Pull Request nhắm vào `dev`, tiêu đề nêu rõ phase/task, mô tả liệt kê task nào trong
  checklist của file phase đã xong. Merge PR xong mới tick `- [x]` các task tương ứng trong file phase.
- Commit: Conventional Commits, scope = tên module — `feat(vocabulary): add attribute validation per language`.
- **Thứ tự làm trong mỗi phase** (giữ nguyên ở mọi phase để nhất quán):
  1. Migration SQL → 2. Entity → 3. Repository → 4. Service + unit test → 5. Controller + integration test
  → 6. `types/domain.ts` → 7. `api/<domain>.ts` → 8. `hooks/use<Domain>.ts` → 9. Component/Page → 10. FE test
- **Cổng kiểm tra trước khi mở PR / đóng phase** (bắt buộc chạy hết, xanh hết):
  ```
  cd backend  && ./mvnw verify
  cd frontend && npm run lint && npm run build && npm run test
  ```
- Đóng phase: PR cuối cùng của phase merge vào `dev` xong, tick hết checkbox trong file phase tương ứng,
  cập nhật decision log ở đây nếu có thay đổi.

---

