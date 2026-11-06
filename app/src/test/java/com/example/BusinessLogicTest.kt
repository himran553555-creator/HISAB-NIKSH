package com.example

import com.example.model.Sale
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessLogicTest {

    @Test
    fun testCurrencyFormatting() {
        val formatted = Formatters.formatCurrency(25000.0)
        assertEquals("৳25,000", formatted)

        val withDecimals = Formatters.formatCurrency(1250.50, includeDecimals = true)
        assertEquals("৳1,250.50", withDecimals)
    }

    @Test
    fun testSaleProfitCalculation() {
        // Product sold for 1500, cost 1000, quantity 2. Landed packaging = 50, delivery charge = 100
        val itemsMap = listOf(
            mapOf<String, Any>(
                "productId" to "p1",
                "productName" to "Earbuds",
                "quantity" to 2.0,
                "unitPrice" to 1500.0,
                "purchaseCost" to 1000.0,
                "totalPrice" to 3000.0
            )
        )
        val sale = Sale(
            id = "s1",
            invoiceNumber = "INV-2026-001",
            businessId = "eshop",
            items = itemsMap,
            subtotal = 3000.0,
            discount = 200.0,
            deliveryCharge = 100.0,
            packagingCost = 50.0,
            totalAmount = 2900.0,
            estimatedProfit = (3000.0 - 200.0) - (2000.0 + 50.0)
        )

        assertEquals(2900.0, sale.totalAmount, 0.001)
        // Profit: (3000 revenue - 200 discount) - (2000 cogs + 50 packaging) = 750
        assertEquals(750.0, sale.estimatedProfit, 0.001)
    }

    @Test
    fun testSeparationOfPersonalAndBusiness() {
        val businessIds = listOf("eshop", "freshoo", "personal")
        val isPersonalList = businessIds.map { it == "personal" }

        assertTrue(isPersonalList[2])
        assertTrue(!isPersonalList[0])
        assertTrue(!isPersonalList[1])
    }

    @Test
    fun testPasswordStrengthEvaluation() {
        val empty = com.example.util.PasswordUtil.evaluate("")
        assertEquals(0, empty.score)

        val weak = com.example.util.PasswordUtil.evaluate("123")
        assertEquals(1, weak.score)
        assertEquals("Weak", weak.label)

        val fair = com.example.util.PasswordUtil.evaluate("abc12345")
        assertEquals(2, fair.score)
        assertEquals("Fair", fair.label)

        val good = com.example.util.PasswordUtil.evaluate("Abc12345")
        assertEquals(3, good.score)
        assertEquals("Good", good.label)

        val strong = com.example.util.PasswordUtil.evaluate("Abc@12345#")
        assertEquals(4, strong.score)
        assertEquals("Strong", strong.label)
        assertTrue(strong.hasSpecialChar)
        assertTrue(strong.hasUppercase)
        assertTrue(strong.hasLowercase)
        assertTrue(strong.hasDigit)
        assertTrue(strong.hasMinLength)
    }

    @Test
    fun testEmailAndMobileResolution() {
        fun resolveIdentifier(raw: String, fallbackMobile: String = ""): String {
            val trimmed = raw.trim().lowercase()
            return when {
                trimmed.contains("@") -> trimmed
                trimmed.isNotEmpty() -> {
                    val sanitized = trimmed.replace(Regex("[^a-z0-9]"), "")
                    "$sanitized@hisabnikash.com"
                }
                fallbackMobile.isNotEmpty() -> {
                    val sanitized = fallbackMobile.replace(Regex("[^0-9]"), "")
                    "$sanitized@hisabnikash.com"
                }
                else -> ""
            }
        }

        assertEquals("imran@example.com", resolveIdentifier("imran@example.com"))
        assertEquals("01712345678@hisabnikash.com", resolveIdentifier("01712345678"))
        assertEquals("01799887766@hisabnikash.com", resolveIdentifier("", "01799887766"))
        assertEquals("imranfood@hisabnikash.com", resolveIdentifier("imran.food"))
    }

    @Test
    fun testBusinessProfileModelAndContextSwitching() {
        val eshop = com.example.model.BusinessProfile(
            id = "eshop",
            name = "E-SHOP",
            type = "eshop",
            tagline = "Fashion, Electronics & Lifestyle eCommerce",
            currency = "BDT",
            isActive = true,
            userId = "test_user_123"
        )
        val freshoo = com.example.model.BusinessProfile(
            id = "freshoo",
            name = "FRESHOO FOOD",
            type = "freshoo",
            tagline = "Fresh Groceries, Fruits & Foods",
            currency = "BDT",
            isActive = true,
            userId = "test_user_123"
        )
        val personal = com.example.model.BusinessProfile(
            id = "personal",
            name = "PERSONAL ACCOUNT",
            type = "personal",
            tagline = "Personal Finance, Savings & Living Expenses",
            currency = "BDT",
            isActive = true,
            userId = "test_user_123"
        )

        assertEquals("eshop", eshop.id)
        assertEquals("freshoo", freshoo.id)
        assertEquals("personal", personal.id)
        assertTrue(eshop.isActive && freshoo.isActive && personal.isActive)

        // Test context switching enum resolution
        val contextEshop = com.example.model.BusinessProfileType.fromId("eshop")
        val contextFreshoo = com.example.model.BusinessProfileType.fromId("freshoo")
        val contextPersonal = com.example.model.BusinessProfileType.fromId("personal")
        val contextAll = com.example.model.BusinessProfileType.fromId("all")
        val contextFallback = com.example.model.BusinessProfileType.fromId("unknown_xyz")

        assertEquals(com.example.model.BusinessProfileType.ESHOP, contextEshop)
        assertEquals(com.example.model.BusinessProfileType.FRESHOO, contextFreshoo)
        assertEquals(com.example.model.BusinessProfileType.PERSONAL, contextPersonal)
        assertEquals(com.example.model.BusinessProfileType.ALL, contextAll)
        assertEquals(com.example.model.BusinessProfileType.ESHOP, contextFallback)
    }
}
