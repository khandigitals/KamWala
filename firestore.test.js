const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const path = require("node:path");
const rulesUnitTestingDir = path.dirname(require.resolve("@firebase/rules-unit-testing/package.json"));
const firebaseCompat = require(path.join(rulesUnitTestingDir, "node_modules/firebase/compat/app")).default;
require(path.join(rulesUnitTestingDir, "node_modules/firebase/compat/firestore"));
const serverTimestamp = () => firebaseCompat.firestore.FieldValue.serverTimestamp();

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validWorkspace(uid) {
  return {
    userId: uid,
    displayName: "Ali Khan",
    email: "ali@example.com",
    lang: "en",
    brand: "KaamWala",
    city: "Lahore",
    whatsapp: "923001234567",
    payAccount: "JazzCash / Easypaisa: 0300-1234567",
    leadFee: 50,
    staffPin: "1234",
    liveCustomerMode: false,
    customerCredit: 0,
    walletBalance: 1000,
    appRevenue: 50,
    workerOnline: true,
    workerStrikes: 0,
    usedFirstPromo: false,
    activeWorkerName: "Ustad Bilal Ahmed",
    isSeeded: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validBooking() {
  return {
    isDemo: false,
    serviceId: "ac",
    serviceName: "AC & Cooling",
    fee: 500,
    expressFee: 200,
    promoDisc: 0,
    creditUsed: 0,
    partsTotal: 0,
    addedPartsSummary: "",
    total: 700,
    leadFeePaid: 0,
    express: true,
    femaleSafe: false,
    slot: "Standard (45 Min)",
    area: "Bahria Town",
    customerName: "Hamza Khan",
    customerPhone: "0300-1234567",
    address: "House 14, Block C",
    gpsCoords: "31.5204, 74.3587",
    latitude: 31.5204,
    longitude: 74.3587,
    workerLat: 0.0,
    workerLng: 0.0,
    note: "AC low cooling",
    photoUri: "",
    voiceNotePath: "",
    proofPhotoUri: "",
    otp: "4821",
    status: "PENDING",
    assignedWorkerName: "",
    rating: 0,
    reviewNote: "",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated caller cannot read or write users/bookings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bookings").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validWorkspace(ALICE_UID)));
});

test("Authenticated owner can create and read own workspace and bookings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validWorkspace(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-701").set(validBooking()));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("bookings").get());
});

test("Cross-user access: Bob cannot read or write Alice's workspace or bookings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validWorkspace(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-701").set(validBooking()));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookings").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-701").update({ status: "COMPLETED", updatedAt: serverTimestamp() }));
});

test("Shadow Update & Ghost Field rejected on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badWorkspace = { ...validWorkspace(ALICE_UID), isAdminBypass: true };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(badWorkspace));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-702").set(validBooking()));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-702").update({
      ghostField: "hack",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Terminal state locking: Completed booking cannot change status back to PENDING", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-703").set(validBooking()));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-703").update({
      status: "COMPLETED",
      updatedAt: serverTimestamp(),
    })
  );
  // Status revert blocked by terminal lock
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-703").update({
      status: "PENDING",
      updatedAt: serverTimestamp(),
    })
  );
  // Rating & reviewNote still allowed on COMPLETED booking
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-703").update({
      rating: 5,
      reviewNote: "Great work!",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Email spoofing test: unverified admin email cannot read Bob's data", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).set(validWorkspace(BOB_UID)));

  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: "mnc.sahjavi@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofedAdminDb.collection("users").doc(BOB_UID).get());
});

test("Parent Gate: ChatMessage requires existing bookingId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatMessages").doc("MSG-1").set({
      bookingId: "NON_EXISTENT",
      senderRole: "CUSTOMER",
      senderName: "Ali",
      text: "Hello",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-704").set(validBooking()));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatMessages").doc("MSG-1").set({
      bookingId: "KW-704",
      senderRole: "CUSTOMER",
      senderName: "Ali",
      text: "Hello",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Reschedule, Worker Live GPS, and CANCELLED terminal lock enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("bookings").doc("KW-705");
  await assertSucceeds(ref.set(validBooking()));

  await assertSucceeds(
    ref.update({
      slot: "Kal Subah 10 AM",
      address: "House 22, Block D",
      workerLat: 31.4712,
      workerLng: 74.4011,
      updatedAt: serverTimestamp(),
    })
  );

  // Invalid worker latitude rejected
  await assertFails(
    ref.update({
      workerLat: 120.0,
      updatedAt: serverTimestamp(),
    })
  );

  // Cancel booking succeeds and locks further updates
  await assertSucceeds(
    ref.update({
      status: "CANCELLED",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    ref.update({
      status: "ACCEPTED",
      updatedAt: serverTimestamp(),
    })
  );
});

function validCustomerProfile(uid) {
  return {
    userId: uid,
    role: "CUSTOMER",
    fullName: "Hamza Khan",
    phone: "03001234567",
    email: "hamza@example.com",
    city: "Lahore",
    area: "Bahria Town",
    address: "House 14, Sector C",
    skill: "",
    experienceYears: 0,
    cnic: "",
    payoutAccount: "",
    femaleSafe: false,
    verificationStatus: "APPROVED",
    isOnline: false,
    walletBalance: 0,
    totalEarnings: 0,
    strikes: 0,
    rating: 5.0,
    jobsCompleted: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validWorkerProfile(uid) {
  return {
    userId: uid,
    role: "WORKER",
    fullName: "Ustad Bilal",
    phone: "03009911223",
    email: "bilal@example.com",
    city: "Lahore",
    area: "DHA Phase 1-8",
    address: "Workshop 4, DHA",
    skill: "AC & Electrical",
    experienceYears: 6,
    cnic: "35202-1234567-1",
    payoutAccount: "JazzCash 03009911223",
    femaleSafe: false,
    verificationStatus: "PENDING",
    isOnline: true,
    walletBalance: 0,
    totalEarnings: 0,
    strikes: 0,
    rating: 5.0,
    jobsCompleted: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validMarketplaceOrder(customerId) {
  return {
    customerId,
    workerId: "",
    city: "Lahore",
    serviceId: "ac",
    serviceName: "AC & Cooling",
    fee: 500,
    expressFee: 200,
    promoDisc: 0,
    creditUsed: 0,
    partsTotal: 0,
    addedPartsSummary: "",
    total: 700,
    leadFeePaid: 0,
    commissionAmount: 0,
    express: true,
    femaleSafe: false,
    slot: "Standard (45 Min)",
    area: "Bahria Town",
    customerName: "Hamza Khan",
    customerPhone: "03001234567",
    address: "House 14, Block C",
    gpsCoords: "31.3676, 74.1815",
    latitude: 31.3676,
    longitude: 74.1815,
    workerLat: 0.0,
    workerLng: 0.0,
    note: "Cooling issue",
    photoUri: "",
    voiceNotePath: "",
    proofPhotoUri: "",
    otp: "4821",
    status: "PENDING",
    assignedWorkerName: "",
    assignedWorkerPhone: "",
    paymentMethod: "JAZZCASH",
    paymentStatus: "UNPAID",
    paymentTid: "",
    rating: 0,
    reviewNote: "",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

test("Marketplace KYC & Roles: Worker cannot self-approve, Admin approves, then Worker can pick & chat", async () => {
  const customerDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const workerDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext("admin_uid_1", {
    email: "mnc.sahjavi@gmail.com",
    email_verified: true,
  }).firestore();

  // Customer registers (auto-APPROVED)
  await assertSucceeds(customerDb.collection("marketplaceProfiles").doc(ALICE_UID).set(validCustomerProfile(ALICE_UID)));

  // Worker cannot self-approve or mint wallet balance
  await assertFails(
    workerDb.collection("marketplaceProfiles").doc(BOB_UID).set({
      ...validWorkerProfile(BOB_UID),
      verificationStatus: "APPROVED",
    })
  );
  await assertFails(
    workerDb.collection("marketplaceProfiles").doc(BOB_UID).set({
      ...validWorkerProfile(BOB_UID),
      walletBalance: 5000,
    })
  );

  // Worker registers cleanly as PENDING
  await assertSucceeds(workerDb.collection("marketplaceProfiles").doc(BOB_UID).set(validWorkerProfile(BOB_UID)));

  // Customer creates shared marketplaceOrder
  const orderRef = customerDb.collection("marketplaceOrders").doc("MKT-101");
  await assertSucceeds(orderRef.set(validMarketplaceOrder(ALICE_UID)));

  // Unapproved worker CANNOT pick order
  await assertFails(
    workerDb.collection("marketplaceOrders").doc("MKT-101").update({
      status: "ACCEPTED",
      workerId: BOB_UID,
      assignedWorkerName: "Ustad Bilal",
      assignedWorkerPhone: "03009911223",
      leadFeePaid: 50,
      updatedAt: serverTimestamp(),
    })
  );

  // Admin approves worker & gives starter Rs. 500 wallet balance
  await assertSucceeds(
    adminDb.collection("marketplaceProfiles").doc(BOB_UID).update({
      verificationStatus: "APPROVED",
      walletBalance: 500,
      updatedAt: serverTimestamp(),
    })
  );

  // Now approved worker picks the order
  await assertSucceeds(
    workerDb.collection("marketplaceOrders").doc("MKT-101").update({
      status: "ACCEPTED",
      workerId: BOB_UID,
      assignedWorkerName: "Ustad Bilal",
      assignedWorkerPhone: "03009911223",
      leadFeePaid: 50,
      updatedAt: serverTimestamp(),
    })
  );

  // Worker deducts own Rs. 50 lead fee & records immutable LEAD_FEE in adminLedger
  await assertSucceeds(
    workerDb.collection("marketplaceProfiles").doc(BOB_UID).update({
      walletBalance: 450,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    workerDb.collection("adminLedger").doc("LED-1").set({
      entryType: "LEAD_FEE",
      amount: 50,
      orderId: "MKT-101",
      actorName: "Ustad Bilal",
      note: "Order MKT-101 pickup",
      payoutMethod: "",
      payoutAccount: "",
      recordedByUid: BOB_UID,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Non-admin CANNOT read adminLedger or record WITHDRAWAL
  await assertFails(workerDb.collection("adminLedger").get());
  await assertFails(
    workerDb.collection("adminLedger").doc("LED-W1").set({
      entryType: "WITHDRAWAL",
      amount: 50,
      orderId: "",
      actorName: "Hacker",
      note: "Steal",
      payoutMethod: "JazzCash",
      payoutAccount: "03000000000",
      recordedByUid: BOB_UID,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Customer and Worker can exchange real-time messages on MKT-101
  await assertSucceeds(
    customerDb.collection("marketplaceOrders").doc("MKT-101").collection("messages").doc("M1").set({
      bookingId: "MKT-101",
      customerId: ALICE_UID,
      workerId: BOB_UID,
      senderUid: ALICE_UID,
      senderRole: "CUSTOMER",
      senderName: "Hamza Khan",
      text: "Gate par aa jayein",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    workerDb.collection("marketplaceOrders").doc("MKT-101").collection("messages").where("workerId", "==", BOB_UID).get()
  );

  // Customer submits digital payment TID
  await assertSucceeds(
    orderRef.update({
      paymentTid: "JC99887766",
      paymentStatus: "PAID_PENDING_VERIFY",
      updatedAt: serverTimestamp(),
    })
  );

  // Worker cannot self-settle digital order or skip OTP (ACCEPTED -> COMPLETED blocked)
  await assertFails(
    workerDb.collection("marketplaceOrders").doc("MKT-101").update({
      status: "COMPLETED",
      paymentStatus: "SETTLED",
      commissionAmount: 70,
      updatedAt: serverTimestamp(),
    })
  );

  // Worker starts job after OTP check (ACCEPTED -> IN_PROGRESS), then completes without touching paymentStatus
  await assertSucceeds(
    workerDb.collection("marketplaceOrders").doc("MKT-101").update({
      status: "IN_PROGRESS",
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    workerDb.collection("marketplaceOrders").doc("MKT-101").update({
      status: "COMPLETED",
      proofPhotoUri: "https://example.com/proof.jpg",
      commissionAmount: 70,
      updatedAt: serverTimestamp(),
    })
  );

  // Admin verifies TID & marks digital payment SETTLED
  await assertSucceeds(
    adminDb.collection("marketplaceOrders").doc("MKT-101").update({
      paymentStatus: "SETTLED",
      commissionAmount: 70,
      updatedAt: serverTimestamp(),
    })
  );

  // Admin can read adminLedger and record WITHDRAWAL
  await assertSucceeds(adminDb.collection("adminLedger").get());
  await assertSucceeds(
    adminDb.collection("adminLedger").doc("LED-W1").set({
      entryType: "WITHDRAWAL",
      amount: 50,
      orderId: "",
      actorName: "Admin",
      note: "Saved to JazzCash",
      payoutMethod: "JazzCash",
      payoutAccount: "03001234567",
      recordedByUid: "admin_uid_1",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Compressed Base64 photoUri & proofPhotoUri up to 65536 chars allowed, oversized rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const mediumDataUri = "data:image/jpeg;base64," + "A".repeat(30000);
  const oversizedUri = "data:image/jpeg;base64," + "B".repeat(66000);

  await assertSucceeds(
    aliceDb.collection("marketplaceOrders").doc("MKT-IMG-OK").set({
      ...validMarketplaceOrder(ALICE_UID),
      photoUri: mediumDataUri,
      proofPhotoUri: mediumDataUri,
    })
  );

  await assertFails(
    aliceDb.collection("marketplaceOrders").doc("MKT-IMG-BAD").set({
      ...validMarketplaceOrder(ALICE_UID),
      photoUri: oversizedUri,
    })
  );
});

test("Shared marketplaceBigJobs & bids: Customer creates OPEN job, approved worker bids, unapproved rejected, Customer awards", async () => {
  const customerDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const workerDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unapprovedDb = testEnv.authenticatedContext("unapproved_uid").firestore();
  const adminDb = testEnv.authenticatedContext("admin_uid_1", {
    email: "mnc.sahjavi@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(workerDb.collection("marketplaceProfiles").doc(BOB_UID).set(validWorkerProfile(BOB_UID)));
  await assertSucceeds(
    adminDb.collection("marketplaceProfiles").doc(BOB_UID).update({
      verificationStatus: "APPROVED",
      updatedAt: serverTimestamp(),
    })
  );

  const jobRef = customerDb.collection("marketplaceBigJobs").doc("BJ-MKT-1");
  await assertSucceeds(
    jobRef.set({
      customerId: ALICE_UID,
      category: "Solar Panel Setup",
      area: "Bahria Town",
      description: "5kW Hybrid setup",
      budget: 50000,
      status: "OPEN",
      acceptedBidId: "",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Unapproved user cannot place bid
  await assertFails(
    unapprovedDb.collection("marketplaceBigJobs").doc("BJ-MKT-1").collection("bids").doc("BID-BAD").set({
      bigJobId: "BJ-MKT-1",
      customerId: ALICE_UID,
      workerUid: "unapproved_uid",
      workerName: "Fake Pro",
      amount: 40000,
      note: "Fast",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Approved worker places bid
  await assertSucceeds(
    workerDb.collection("marketplaceBigJobs").doc("BJ-MKT-1").collection("bids").doc("BID-1").set({
      bigJobId: "BJ-MKT-1",
      customerId: ALICE_UID,
      workerUid: BOB_UID,
      workerName: "Ustad Bilal",
      amount: 45000,
      note: "Complete in 2 days",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Customer can read bids on own job & award job
  await assertSucceeds(
    customerDb.collection("marketplaceBigJobs").doc("BJ-MKT-1").collection("bids").where("customerId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    jobRef.update({
      status: "AWARDED",
      acceptedBidId: "BID-1",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Shared marketplaceB2b: Submit restricted to owner, Admin only list/activate", async () => {
  const customerDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const otherDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext("admin_uid_1", {
    email: "mnc.sahjavi@gmail.com",
    email_verified: true,
  }).firestore();

  const leadRef = customerDb.collection("marketplaceB2b").doc("B2B-MKT-1");
  await assertSucceeds(
    leadRef.set({
      submittedByUid: ALICE_UID,
      businessName: "City Care Clinic",
      phone: "03001112233",
      packageName: "Office Care Rs. 8,000/mo",
      status: "NEW",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Non-admin cannot read another user's B2B lead or activate
  await assertFails(otherDb.collection("marketplaceB2b").doc("B2B-MKT-1").get());
  await assertFails(
    otherDb.collection("marketplaceB2b").doc("B2B-MKT-1").update({
      status: "ACTIVE",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    customerDb.collection("marketplaceB2b").doc("B2B-MKT-1").update({
      status: "ACTIVE",
      updatedAt: serverTimestamp(),
    })
  );

  // Admin can list and activate
  await assertSucceeds(adminDb.collection("marketplaceB2b").get());
  await assertSucceeds(
    adminDb.collection("marketplaceB2b").doc("B2B-MKT-1").update({
      status: "ACTIVE",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Atomic Customer cancel refund, Job completion stats, Quality strike, and Rating sync enforced", async () => {
  const customerDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const workerDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext("admin_uid_1", {
    email: "mnc.sahjavi@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(workerDb.collection("marketplaceProfiles").doc(BOB_UID).set(validWorkerProfile(BOB_UID)));
  await assertSucceeds(
    adminDb.collection("marketplaceProfiles").doc(BOB_UID).update({
      verificationStatus: "APPROVED",
      walletBalance: 500,
      updatedAt: serverTimestamp(),
    })
  );

  // Order A: Customer cancels ACCEPTED order -> atomic lead-fee refund
  const orderARef = customerDb.collection("marketplaceOrders").doc("MKT-REF-1");
  await assertSucceeds(orderARef.set(validMarketplaceOrder(ALICE_UID)));
  await assertSucceeds(
    workerDb.collection("marketplaceOrders").doc("MKT-REF-1").update({
      status: "ACCEPTED",
      workerId: BOB_UID,
      assignedWorkerName: "Ustad Bilal",
      assignedWorkerPhone: "03009911223",
      leadFeePaid: 50,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    workerDb.collection("marketplaceProfiles").doc(BOB_UID).update({
      walletBalance: 450,
      updatedAt: serverTimestamp(),
    })
  );

  // Standalone refund without cancelling order MUST fail
  await assertFails(
    customerDb.collection("marketplaceProfiles").doc(BOB_UID).update({
      walletBalance: 500,
      lastRefundOrderId: "MKT-REF-1",
      updatedAt: serverTimestamp(),
    })
  );

  // Atomic batch cancelling order + refunding leadFeePaid MUST succeed
  const cancelBatch = customerDb.batch();
  cancelBatch.update(orderARef, {
    status: "CANCELLED",
    updatedAt: serverTimestamp(),
  });
  cancelBatch.update(customerDb.collection("marketplaceProfiles").doc(BOB_UID), {
    walletBalance: 500,
    lastRefundOrderId: "MKT-REF-1",
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(cancelBatch.commit());

  // Order B: Worker completes IN_PROGRESS -> COMPLETED and atomically increments jobsCompleted & totalEarnings
  const orderBRef = customerDb.collection("marketplaceOrders").doc("MKT-CMP-1");
  await assertSucceeds(
    orderBRef.set({
      ...validMarketplaceOrder(ALICE_UID),
      paymentMethod: "CASH",
    })
  );
  await assertSucceeds(
    workerDb.collection("marketplaceOrders").doc("MKT-CMP-1").update({
      status: "ACCEPTED",
      workerId: BOB_UID,
      assignedWorkerName: "Ustad Bilal",
      assignedWorkerPhone: "03009911223",
      leadFeePaid: 50,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    workerDb.collection("marketplaceOrders").doc("MKT-CMP-1").update({
      status: "IN_PROGRESS",
      updatedAt: serverTimestamp(),
    })
  );
  const completeBatch = workerDb.batch();
  completeBatch.update(workerDb.collection("marketplaceOrders").doc("MKT-CMP-1"), {
    status: "COMPLETED",
    paymentStatus: "SETTLED",
    commissionAmount: 70,
    updatedAt: serverTimestamp(),
  });
  completeBatch.update(workerDb.collection("marketplaceProfiles").doc(BOB_UID), {
    jobsCompleted: 1,
    totalEarnings: 630,
    lastCompletedOrderId: "MKT-CMP-1",
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(completeBatch.commit());

  // Customer first rating sync updates order rating + worker profile rating atomically
  const rateBatch = customerDb.batch();
  rateBatch.update(orderBRef, {
    rating: 4,
    reviewNote: "Good cooling check",
    updatedAt: serverTimestamp(),
  });
  rateBatch.update(customerDb.collection("marketplaceProfiles").doc(BOB_UID), {
    rating: 4.0,
    lastRatedOrderId: "MKT-CMP-1",
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(rateBatch.commit());

  // Customer quality strike batch adds [STRIKE LOGGED] and increments worker strikes 0 -> 1
  const strikeBatch = customerDb.batch();
  strikeBatch.update(orderBRef, {
    reviewNote: "Good cooling check [STRIKE LOGGED]",
    updatedAt: serverTimestamp(),
  });
  strikeBatch.update(customerDb.collection("marketplaceProfiles").doc(BOB_UID), {
    strikes: 1,
    lastStrikeOrderId: "MKT-CMP-1",
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(strikeBatch.commit());

  // Replay strike on already-struck order MUST fail
  const replayStrikeBatch = customerDb.batch();
  replayStrikeBatch.update(orderBRef, {
    reviewNote: "Duplicate [STRIKE LOGGED]",
    updatedAt: serverTimestamp(),
  });
  replayStrikeBatch.update(customerDb.collection("marketplaceProfiles").doc(BOB_UID), {
    strikes: 2,
    lastStrikeOrderId: "MKT-CMP-1",
    updatedAt: serverTimestamp(),
  });
  await assertFails(replayStrikeBatch.commit());
});
