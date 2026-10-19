package com.learnflow.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.learnflow.backend.auth.AuthService;
import com.learnflow.backend.auth.RegistrationMailService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class StoreIntegrationTest extends AbstractIntegrationTest {

    @Test
    void onlyAdminCanUploadAndReturnedPhotoIsPubliclyReadable() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(3, 4, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        var file = new org.springframework.mock.web.MockMultipartFile("file", "photo.png", "image/png", bytes.toByteArray());
        mvc.perform(multipart("/api/admin/product-images").file(file)).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/admin/product-images").file(file).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        var result = mvc.perform(multipart("/api/admin/product-images").file(file).header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk()).andReturn();
        String url = mapper.readTree(result.getResponse().getContentAsString()).get("imageUrl").asText();
        try {
            mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        } finally {
            java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("target/test-images", url.substring("/api/images/".length())));
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    AuthService users;

    @MockBean
    RegistrationMailService mail;

    private final Map<String, String> codes = new HashMap<>();

    private String token, admin;

    @BeforeEach
    void account() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            codes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        })
            .when(mail)
            .sendCode(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        String name = "test" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        token = register(name);
        users.bootstrapAdmin("admin@test.local", "AdminTestPassword123");
        admin = json(
            mvc
                .perform(
                    post("/api/auth/login")
                        .contentType("application/json")
                        .content(
                            mapper.writeValueAsString(
                                Map.of("email", "admin@test.local", "password", "AdminTestPassword123")
                            )
                        )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
        )
            .get("accessToken")
            .asText();
    }

    private String register(String name) throws Exception {
        mvc.perform(
            post("/api/auth/register/email-code")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("email", name + "@test.local")))
        ).andExpect(status().isOk());
        return json(
            mvc
                .perform(
                    post("/api/auth/register")
                        .contentType("application/json")
                        .content(
                            mapper.writeValueAsString(
                                Map.of(
                                    "email",
                                    name + "@test.local",
                                    "password",
                                    "TestPassword123",
                                    "fullName",
                                    "Test Customer",
                                    "username",
                                    name,
                                    "phoneNumber",
                                    "0901234567",
                                    "verificationCode",
                                    codes.get(name + "@test.local")
                                )
                            )
                        )
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
        )
            .get("accessToken")
            .asText();
    }

    private JsonNode json(String s) throws Exception {
        return mapper.readTree(s);
    }

    private JsonNode postJson(String path, Object body, String t, int expected) throws Exception {
        return json(
            mvc
                .perform(
                    post(path)
                        .header("Authorization", "Bearer " + t)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(body))
                )
                .andExpect(status().is(expected))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private long address() throws Exception {
        return postJson(
            "/api/addresses",
            Map.of(
                "receiverName",
                "Customer",
                "phoneNumber",
                "0901234567",
                "province",
                "Ha Noi",
                "detailAddress",
                "123 Local Street",
                "isDefault",
                true
            ),
            token,
            200
        )
            .get("addressId")
            .asLong();
    }

    private JsonNode order(String method, String code) throws Exception {
        postJson("/api/carts/current/items", Map.of("variantId", 1, "quantity", 2), token, 200);
        return postJson(
            "/api/orders",
            Map.of("addressId", address(), "paymentMethod", method, "couponCode", code, "note", "test"),
            token,
            200
        );
    }

    private void transition(long id, String target, int expected) throws Exception {
        mvc.perform(
            patch("/api/admin/orders/" + id + "/status")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("status", target)))
        ).andExpect(status().is(expected));
    }

    @Test
    void optionalVariantSizesUseFreesizeAndCanBeOrdered() throws Exception {
        List<Map<String, Object>> variants = new ArrayList<>();
        for (int n = 0; n < 3; n++) {
            Map<String, Object> variant = new HashMap<>();
            variant.put("color", "Màu " + n);
            variant.put("stockQuantity", 5);
            variant.put("sku", "OPTIONAL-SIZE-" + UUID.randomUUID());
            if (n == 1) variant.put("size", null);
            if (n == 2) variant.put("size", "   ");
            variants.add(variant);
        }
        var product = postJson(
            "/api/admin/products",
            Map.of(
                "categoryId",
                4,
                "productName",
                "Phụ kiện không phân kích cỡ",
                "gender",
                "Unisex",
                "price",
                100000,
                "status",
                "Active",
                "variants",
                variants,
                "images",
                List.of("/assets/photos/product-1.jpg")
            ),
            admin,
            200
        );
        assertEquals(3, product.get("variants").size());
        for (var variant : product.get("variants")) assertEquals("Freesize", variant.get("size").asText());
        long variantId = product.get("variants").get(0).get("variantId").asLong();
        var cart = postJson(
            "/api/carts/current/items",
            Map.of("variantId", variantId, "quantity", 1),
            token,
            200
        );
        assertEquals("Freesize", cart.get("items").get(0).get("size").asText());
        var order = postJson(
            "/api/orders",
            Map.of("addressId", address(), "paymentMethod", "COD"),
            token,
            200
        );
        assertEquals("Freesize", order.get("items").get(0).get("size").asText());
    }

    @Test
    void usernameLoginAndBlockingInvalidateExistingTokens() throws Exception {
        JsonNode me = json(
            mvc
                .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
        );
        postJson(
            "/api/auth/login",
            Map.of("identifier", me.get("username").asText(), "password", "TestPassword123"),
            "",
            200
        );
        mvc.perform(
            patch("/api/admin/users/" + me.get("userId").asLong() + "/status")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content("{\"status\":\"Blocked\"}")
        ).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(
            status().isUnauthorized()
        );
    }

    @Test
    void authorizationValidationAndOwnership() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        postJson("/api/carts/current/items", Map.of("variantId", 1, "quantity", 0), token, 400);
        long id = address();
        String other = register("other" + UUID.randomUUID().toString().substring(0, 8));
        postJson("/api/orders", Map.of("addressId", id, "paymentMethod", "COD"), other, 404);
        mvc.perform(get("/api/products?page=-1").header("Authorization", "Bearer " + token)).andExpect(
            status().isBadRequest()
        );
    }

    @Test
    void checkoutUsesServerPricesAndCancellationRestoresStock() throws Exception {
        int before = json(
            mvc
                .perform(get("/api/products/1").header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
        )
            .get("variants")
            .get(0)
            .get("stockQuantity")
            .asInt();
        JsonNode o = order("COD", "WELCOME10");
        assertEquals(732000, o.get("totalAmount").asInt());
        assertEquals(78000, o.get("discountAmount").asInt());
        long id = o.get("orderId").asLong();
        postJson("/api/orders/" + id + "/cancel", Map.of(), token, 200);
        postJson("/api/orders/" + id + "/cancel", Map.of(), token, 400);
        int after = json(
            mvc
                .perform(get("/api/products/1").header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
        )
            .get("variants")
            .get(0)
            .get("stockQuantity")
            .asInt();
        assertEquals(before, after);
        mvc.perform(get("/api/carts/current").header("Authorization", "Bearer " + token)).andExpect(
            jsonPath("$.items").isEmpty()
        );
    }

    @Test
    void deliveredPurchaseAllowsSingleReviewAndCodSettles() throws Exception {
        postJson("/api/products/1/reviews", Map.of("rating", 5, "comment", "Great"), token, 403);
        JsonNode o = order("COD", "");
        long id = o.get("orderId").asLong();
        mvc.perform(get("/api/products/1/reviews/eligibility").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canReview").value(false));
        postJson("/api/products/1/reviews", Map.of("rating", 5, "comment", "Pending order"), token, 403);
        transition(id, "Delivered", 400);
        transition(id, "Confirmed", 200);
        postJson("/api/products/1/reviews", Map.of("rating", 5, "comment", "Confirmed order"), token, 403);
        transition(id, "Shipping", 200);
        postJson("/api/products/1/reviews", Map.of("rating", 5, "comment", "Shipping order"), token, 403);
        transition(id, "Delivered", 200);
        mvc.perform(get("/api/products/1/reviews/eligibility").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canReview").value(true));
        String other = register("reviewer" + UUID.randomUUID().toString().substring(0, 8));
        postJson(
            "/api/products/1/reviews",
            Map.of("rating", 5, "comment", "Someone else's order"),
            other,
            403
        );
        mvc.perform(get("/api/products/1/reviews/eligibility").header("Authorization", "Bearer " + other))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canReview").value(false));
        postJson("/api/products/2/reviews", Map.of("rating", 5, "comment", "Not purchased"), token, 403);
        mvc.perform(get("/api/orders/" + id).header("Authorization", "Bearer " + token)).andExpect(
            jsonPath("$.payment.paymentStatus").value("Paid")
        );
        postJson("/api/products/1/reviews", Map.of("rating", 5, "comment", "Comfortable"), token, 200);
        postJson("/api/products/1/reviews", Map.of("rating", 5, "comment", "Again"), token, 409);
        mvc.perform(get("/api/products/1/reviews/eligibility").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canReview").value(false))
            .andExpect(jsonPath("$.alreadyReviewed").value(true));
    }

    @Test
    void cancelledPurchaseCannotBeReviewed() throws Exception {
        long id = order("COD", "").get("orderId").asLong();
        postJson("/api/orders/" + id + "/cancel", Map.of(), token, 200);
        postJson("/api/products/1/reviews", Map.of("rating", 5, "comment", "Cancelled order"), token, 403);
        mvc.perform(get("/api/products/1/reviews/eligibility").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canReview").value(false));
    }

    @Autowired
    com.learnflow.backend.order.PaymentRepository payments;

    @Test
    void demoQrIsPrivateToOwnerAndCannotSettleManualPayments() throws Exception {
        long id = order("DemoQR", "").get("orderId").asLong();
        String other = register("other" + UUID.randomUUID().toString().substring(0, 8));
        postJson("/api/orders/" + id + "/demo-payment", Map.of(), other, 404);
        mvc.perform(post("/api/orders/" + id + "/demo-payment")).andExpect(status().isUnauthorized());
        var session = postJson("/api/orders/" + id + "/demo-payment", Map.of(), token, 200);
        String code = session.get("token").asText();
        var preview = postJson("/api/demo-payments/view", Map.of("token", code), "", 200);
        assertFalse(preview.has("shippingAddress"));
        assertFalse(preview.has("receiverPhone"));
        assertTrue(preview.get("token").isNull());
        assertEquals(64, payments.findByOrderId(id).orElseThrow().demoTokenHash.length());
        assertNotEquals(code, payments.findByOrderId(id).orElseThrow().demoTokenHash);
        var paid = postJson(
            "/api/demo-payments/complete",
            Map.of("token", code, "outcome", "Paid", "amount", 1),
            "",
            200
        );
        assertEquals(session.get("amount"), paid.get("amount"));
        assertEquals("Paid", paid.get("paymentStatus").asText());
        String tx = payments.findByOrderId(id).orElseThrow().transactionId;
        assertTrue(tx.startsWith("DEMO-"));
        postJson("/api/demo-payments/complete", Map.of("token", code, "outcome", "Paid"), "", 200);
        assertEquals(tx, payments.findByOrderId(id).orElseThrow().transactionId);
        postJson("/api/demo-payments/complete", Map.of("token", code, "outcome", "Failed"), "", 400);
        postJson("/api/orders/" + id + "/demo-payment", Map.of(), token, 400);
        transition(id, "Confirmed", 200);
        transition(id, "Shipping", 200);
        long manual = order("VietQR", "").get("orderId").asLong();
        postJson("/api/orders/" + manual + "/demo-payment", Map.of(), token, 400);
    }

    @Test
    void demoFailureCanRetryAndRotatingQrInvalidatesPreviousSession() throws Exception {
        long id = order("DemoQR", "").get("orderId").asLong();
        String first = postJson("/api/orders/" + id + "/demo-payment", Map.of(), token, 200)
            .get("token")
            .asText();
        postJson("/api/demo-payments/complete", Map.of("token", first, "outcome", "Failed"), "", 200);
        transition(id, "Confirmed", 200);
        transition(id, "Shipping", 400);
        String second = postJson("/api/orders/" + id + "/demo-payment", Map.of(), token, 200)
            .get("token")
            .asText();
        assertNotEquals(first, second);
        postJson("/api/demo-payments/complete", Map.of("token", first, "outcome", "Paid"), "", 404);
        postJson("/api/demo-payments/complete", Map.of("token", second, "outcome", "Paid"), "", 200);
        transition(id, "Shipping", 200);
    }

    @Test
    void demoQrRejectsExpiredCancelledAndMalformedRequests() throws Exception {
        long id = order("DemoQR", "").get("orderId").asLong();
        String code = postJson("/api/orders/" + id + "/demo-payment", Map.of(), token, 200)
            .get("token")
            .asText();
        var payment = payments.findByOrderId(id).orElseThrow();
        payment.demoExpiresAt = java.time.Instant.parse("2000-01-01T00:00:00Z");
        payments.saveAndFlush(payment);
        postJson("/api/demo-payments/complete", Map.of("token", code, "outcome", "Paid"), "", 400);
        postJson("/api/demo-payments/view", Map.of("token", "invalid"), "", 400);
        postJson("/api/demo-payments/complete", Map.of("token", code), "", 400);
        String fresh = postJson("/api/orders/" + id + "/demo-payment", Map.of(), token, 200)
            .get("token")
            .asText();
        postJson("/api/orders/" + id + "/cancel", Map.of(), token, 200);
        postJson("/api/demo-payments/complete", Map.of("token", fresh, "outcome", "Paid"), "", 400);
        assertEquals("Failed", payments.findByOrderId(id).orElseThrow().paymentStatus);
        postJson("/api/orders/" + id + "/demo-payment", Map.of(), token, 400);
    }

    @Test
    void pendingOnlinePaymentCannotShip() throws Exception {
        long id = order("VietQR", "").get("orderId").asLong();
        transition(id, "Confirmed", 200);
        transition(id, "Shipping", 400);
        postJson(
            "/api/admin/orders/" + id + "/payment",
            Map.of("transactionId", "BANK-LOCAL-123"),
            admin,
            200
        );
        transition(id, "Shipping", 200);
    }

    @Test
    void failedCheckoutRollsBackAndKeepsCart() throws Exception {
        postJson("/api/carts/current/items", Map.of("variantId", 1, "quantity", 99), token, 400);
        postJson("/api/carts/current/items", Map.of("variantId", 1, "quantity", 1), token, 200);
        postJson(
            "/api/orders",
            Map.of("addressId", address(), "paymentMethod", "COD", "couponCode", "MISSING"),
            token,
            400
        );
        mvc.perform(get("/api/carts/current").header("Authorization", "Bearer " + token)).andExpect(
            jsonPath("$.items[0].quantity").value(1)
        );
    }

    @Test
    void aiDisabledAndOrderLookupProtectsOwnership() throws Exception {
        postJson("/api/ai/chat", Map.of("message", "Help me choose"), token, 200);
        long id = order("COD", "").get("orderId").asLong();
        String other = register("other" + UUID.randomUUID().toString().substring(0, 8));
        postJson("/api/ai/chat", Map.of("message", "Track", "orderId", id), other, 404);
    }

    @Test
    void photographedColorsHaveDifferentImagesAndCartUsesSelectedColor() throws Exception {
        for (long productId = 1; productId <= 8; productId++) {
            JsonNode product = json(
                mvc
                    .perform(get("/api/products/" + productId).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
            );
            Set<String> colors = new HashSet<>();
            Set<String> photos = new HashSet<>();
            for (var variant : product.get("variants")) {
                colors.add(variant.get("color").asText());
                assertTrue(variant.get("imageUrl").asText().startsWith("/assets/photos/"));
                photos.add(variant.get("imageUrl").asText());
            }
            assertTrue(colors.size() >= 2);
            assertEquals(colors.size(), photos.size());
        }
        JsonNode product = json(
            mvc
                .perform(get("/api/products/1").header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
        );
        JsonNode pink = null;
        for (var variant : product.get("variants"))
            if (variant.get("color").asText().equals("Hồng đỗ")) {
                pink = variant;
                break;
            }
        assertNotNull(pink);
        JsonNode cart = postJson(
            "/api/carts/current/items",
            Map.of("variantId", pink.get("variantId").asLong(), "quantity", 1),
            token,
            200
        );
        assertEquals(pink.get("imageUrl").asText(), cart.get("items").get(0).get("imageUrl").asText());
        assertEquals("Hồng đỗ", cart.get("items").get(0).get("color").asText());
    }
}
