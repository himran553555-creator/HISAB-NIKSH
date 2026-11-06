const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read user profile or subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
});

test("Authenticated user: cannot read another user's profile or subcollections", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      displayName: "Bob Owner",
      email: "bob@example.com",
      currency: "BDT"
    });
    await context.firestore().collection("users").doc(BOB_UID).collection("products").doc("p1").set({
      name: "Organic Ghee",
      businessId: "freshoo",
      purchaseCost: 800,
      sellingPrice: 1200,
      currentStock: 25
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("products").doc("p1").get());
});

test("Authenticated user: can read and write their own profile and products", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Create user profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      displayName: "Alice Imran",
      email: "alice@example.com",
      currency: "BDT"
    })
  );

  // Create product under Alice's account
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod1").set({
      name: "Smart Watch",
      businessId: "eshop",
      sku: "SW-01",
      purchaseCost: 1500,
      sellingPrice: 2200,
      currentStock: 10
    })
  );

  // List products
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").get()
  );
});

test("Validation rejects invalid product payload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Negative purchaseCost should fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("bad_prod").set({
      name: "Bad Product",
      businessId: "eshop",
      sku: "BAD",
      purchaseCost: -100,
      sellingPrice: 200,
      currentStock: 5
    })
  );
});

test("Stock movements cannot be modified or deleted (immutable)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stock_movements").doc("m1").set({
      productId: "prod1",
      businessId: "eshop",
      type: "SALE",
      quantityDelta: -2,
      reason: "Invoice #INV-1001"
    })
  );

  // Update must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("stock_movements").doc("m1").update({
      reason: "Tampered"
    })
  );

  // Delete must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("stock_movements").doc("m1").delete()
  );
});

test("Businesses and Profiles: can create, read, and isolate across users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice creates E-SHOP, FRESHOO FOOD, and PERSONAL ACCOUNT businesses
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("businesses").doc("eshop").set({
      name: "E-SHOP",
      type: "eshop",
      tagline: "Fashion eCommerce"
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("businesses").doc("freshoo").set({
      name: "FRESHOO FOOD",
      type: "freshoo",
      tagline: "Fresh Groceries"
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("businesses").doc("personal").set({
      name: "PERSONAL ACCOUNT",
      type: "personal",
      tagline: "Personal Finance"
    })
  );

  // Alice creates matching profiles collection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profiles").doc("eshop").set({
      name: "E-SHOP",
      type: "eshop",
      tagline: "Fashion eCommerce"
    })
  );

  // Bob cannot read Alice's businesses or profiles
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("businesses").doc("eshop").get.call(bobDb.collection("users").doc(ALICE_UID).collection("businesses").doc("eshop")));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("profiles").doc("eshop").get.call(bobDb.collection("users").doc(ALICE_UID).collection("profiles").doc("eshop")));

  // Unauthenticated user cannot read
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("businesses").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("profiles").get());
});
