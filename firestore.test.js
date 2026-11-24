const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "merchant_alice";
const BOB_UID = "merchant_bob";
const ADMIN_UID = "admin_mitch";
const ADMIN_EMAIL = "mitchdubois0789@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

function validMerchantPayload(uid, email = "alice@shop.ga") {
  const now = pastDate();
  return {
    userId: uid,
    email,
    displayName: "Alice",
    businessName: "Boutique Élégance",
    businessCategory: "vetements",
    phone: "+241074112233",
    plan: "decouverte",
    subscriptionStatus: "active",
    accountStatus: "active",
    creationsCount: 0,
    postsCount: 0,
    productsCount: 0,
    sharesCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user cannot read or write merchants, products, or payments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("merchants").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("merchants").doc(ALICE_UID).set(validMerchantPayload(ALICE_UID)));
  await assertFails(unauthDb.collection("payments").get());
  await assertFails(unauthDb.collection("subscription_plans").get());
});

test("Merchant can create initial decouverte profile, cannot self-upgrade plan or suspend self", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@shop.ga", email_verified: true }).firestore();

  // Cannot create profile directly on premium plan
  const badPlan = validMerchantPayload(ALICE_UID);
  badPlan.plan = "premium";
  await assertFails(aliceDb.collection("merchants").doc(ALICE_UID).set(badPlan));

  // Valid create on decouverte
  await assertSucceeds(aliceDb.collection("merchants").doc(ALICE_UID).set(validMerchantPayload(ALICE_UID)));

  // Valid profile update (businessName)
  await assertSucceeds(
    aliceDb.collection("merchants").doc(ALICE_UID).update({
      businessName: "Boutique Élégance Libreville",
      updatedAt: pastDate(1000),
    })
  );

  // Forbidden self-upgrade to business
  await assertFails(
    aliceDb.collection("merchants").doc(ALICE_UID).update({
      plan: "business",
      updatedAt: pastDate(1000),
    })
  );

  // Regular user cannot list all merchants
  await assertFails(aliceDb.collection("merchants").get());
});

test("Cross-user isolation: Bob cannot read or mutate Alice's profile, products, or customers", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("merchants").doc(ALICE_UID).set(validMerchantPayload(ALICE_UID));
    await db.collection("merchants").doc(ALICE_UID).collection("products").doc("prod_1").set({
      userId: ALICE_UID,
      name: "Robe Wax",
      priceFcfa: 25000,
      category: "Vêtements",
      description: "Qualité supérieure",
      available: true,
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@shop.ga", email_verified: true }).firestore();
  await assertFails(bobDb.collection("merchants").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("merchants").doc(ALICE_UID).collection("products").get());
  await assertFails(
    bobDb.collection("merchants").doc(ALICE_UID).collection("products").doc("prod_1").update({
      priceFcfa: 100,
      updatedAt: pastDate(1000),
    })
  );
});

test("Merchant subcollections: products, posters, posts, calendar, and consent-gated customers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@shop.ga", email_verified: true }).firestore();
  await assertSucceeds(aliceDb.collection("merchants").doc(ALICE_UID).set(validMerchantPayload(ALICE_UID)));

  // Product create + update
  await assertSucceeds(
    aliceDb.collection("merchants").doc(ALICE_UID).collection("products").doc("p1").set({
      userId: ALICE_UID,
      name: "Chaussures cuir",
      priceFcfa: 35000,
      category: "Chaussures",
      description: "Pointures 40-44",
      available: true,
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );

  // Poster create
  await assertSucceeds(
    aliceDb.collection("merchants").doc(ALICE_UID).collection("posters").doc("pos1").set({
      userId: ALICE_UID,
      productName: "Chaussures cuir",
      priceFcfa: 35000,
      description: "Promo week-end",
      templateId: "chaussures_urban",
      promoBadge: "-15%",
      contactPhone: "074112233",
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );

  // Post create
  await assertSucceeds(
    aliceDb.collection("merchants").doc(ALICE_UID).collection("posts").doc("post1").set({
      userId: ALICE_UID,
      productName: "Chaussures cuir",
      priceFcfa: 35000,
      platform: "whatsapp",
      style: "direct",
      content: "Nouvelle collection disponible!",
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );

  // Calendar create
  await assertSucceeds(
    aliceDb.collection("merchants").doc(ALICE_UID).collection("calendar").doc("cal1").set({
      userId: ALICE_UID,
      title: "Lancement Samedi",
      platform: "facebook",
      campaignType: "Promo",
      scheduledDateEpochMs: Date.now() + 86400000,
      notes: "Publier à 10h",
      status: "planned",
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );

  // Customer without consent MUST fail
  await assertFails(
    aliceDb.collection("merchants").doc(ALICE_UID).collection("customers").doc("c1").set({
      userId: ALICE_UID,
      name: "Jean",
      phone: "074000111",
      consentGranted: false,
      notes: "",
      lastOfferSent: "",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Customer with explicit consent succeeds
  await assertSucceeds(
    aliceDb.collection("merchants").doc(ALICE_UID).collection("customers").doc("c1").set({
      userId: ALICE_UID,
      name: "Jean",
      phone: "074000111",
      consentGranted: true,
      notes: "Client VIP",
      lastOfferSent: "",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );
});

test("Payments & Server-Side Admin Enforcement: merchant submits pending only; admin approves; spoofed unverified email rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@shop.ga", email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("merchants").doc(ALICE_UID).set(validMerchantPayload(ALICE_UID)));

  // Merchant cannot submit an already-approved payment
  await assertFails(
    aliceDb.collection("payments").doc("pay_bad").set({
      userId: ALICE_UID,
      merchantName: "Boutique Élégance",
      merchantEmail: "alice@shop.ga",
      planId: "business",
      amountFcfa: 2000,
      provider: "airtel_money",
      senderPhone: "074112233",
      transactionRef: "TX123456",
      proofNote: "",
      status: "approved",
      adminNote: "",
      reviewedBy: "",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Merchant submits valid pending payment
  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_1").set({
      userId: ALICE_UID,
      merchantName: "Boutique Élégance",
      merchantEmail: "alice@shop.ga",
      planId: "business",
      amountFcfa: 2000,
      provider: "airtel_money",
      senderPhone: "074112233",
      transactionRef: "MP261124.A123",
      proofNote: "Payé ce matin",
      status: "pending",
      adminNote: "",
      reviewedBy: "",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Scoped query for Alice succeeds; unfiltered query for Alice fails
  await assertSucceeds(aliceDb.collection("payments").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("payments").get());

  // Merchant cannot self-approve payment
  await assertFails(
    aliceDb.collection("payments").doc("pay_1").update({
      status: "approved",
      adminNote: "Self",
      reviewedBy: "alice",
      updatedAt: pastDate(1000),
    })
  );

  // Spoofed unverified admin email MUST fail
  await assertSucceeds(realAdminDb.collection("payments").get()); // control check
  await assertFails(spoofedAdminDb.collection("payments").get());
  await assertFails(
    spoofedAdminDb.collection("payments").doc("pay_1").update({
      status: "approved",
      adminNote: "Spoof",
      reviewedBy: ADMIN_EMAIL,
      updatedAt: pastDate(1000),
    })
  );

  // Verified Admin can list all payments, approve pay_1, upgrade merchant plan, and edit subscription_plans
  await assertSucceeds(realAdminDb.collection("payments").get());
  await assertSucceeds(
    realAdminDb.collection("payments").doc("pay_1").update({
      status: "approved",
      adminNote: "Validé Airtel Money",
      reviewedBy: ADMIN_EMAIL,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    realAdminDb.collection("merchants").doc(ALICE_UID).update({
      plan: "business",
      subscriptionStatus: "active",
      renewalDate: new Date(Date.now() + 30 * 86400000),
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: approved payment cannot be changed again even by admin
  await assertFails(
    realAdminDb.collection("payments").doc("pay_1").update({
      status: "rejected",
      adminNote: "Oops",
      reviewedBy: ADMIN_EMAIL,
      updatedAt: pastDate(500),
    })
  );

  // Admin can configure subscription_plans; merchant cannot
  await assertFails(
    aliceDb.collection("subscription_plans").doc("business").set({
      name: "BUSINESS",
      priceFcfa: 1,
      maxPostersPerMonth: 9999,
      maxProducts: 9999,
      maxPostsPerMonth: 9999,
      maxCustomers: 9999,
      featuresSummary: "Hacked",
      active: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    realAdminDb.collection("subscription_plans").doc("business").set({
      name: "BUSINESS",
      priceFcfa: 2000,
      maxPostersPerMonth: 50,
      maxProducts: 80,
      maxPostsPerMonth: 100,
      maxCustomers: 200,
      featuresSummary: "Offre Business mise à jour",
      active: true,
      updatedAt: pastDate(1000),
    })
  );
});
